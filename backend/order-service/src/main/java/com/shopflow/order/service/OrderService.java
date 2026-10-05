package com.shopflow.order.service;

import com.shopflow.common.api.BadRequestException;
import com.shopflow.common.api.ConflictException;
import com.shopflow.common.api.PageResponse;
import com.shopflow.common.events.PaymentCompletedEvent;
import com.shopflow.common.events.PaymentFailedEvent;
import com.shopflow.order.api.dto.CreateOrderRequest;
import com.shopflow.order.api.dto.OrderDetailResponse;
import com.shopflow.order.api.dto.OrderSummaryResponse;
import com.shopflow.order.domain.Order;
import com.shopflow.order.domain.OrderItem;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.domain.ShippingAddress;
import com.shopflow.order.event.OrderPlacedDomainEvent;
import com.shopflow.order.integration.CartGateway;
import com.shopflow.order.integration.CatalogGateway;
import com.shopflow.order.integration.dto.CartLine;
import com.shopflow.order.integration.dto.CartView;
import com.shopflow.order.integration.dto.ProductSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Orkestrasi checkout (saga).
 *
 * <p>Kelas ini <b>tidak</b> bertransaksi: ia mengoordinasi panggilan jaringan, lalu
 * menyerahkan pekerjaan database ke {@link OrderStore} dalam transaksi yang pendek.
 *
 * <p>Perhatikan urutan kompensasi: kalau penahanan stok untuk item ke-3 gagal,
 * item ke-1 dan ke-2 harus dilepas kembali. Tanpa itu, stok akan "bocor" -
 * terjual di sistem tapi tidak pernah keluar dari gudang.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int MAX_PAGE_SIZE = 50;

    private final OrderStore orderStore;
    private final CartGateway cartGateway;
    private final CatalogGateway catalogGateway;

    public OrderService(OrderStore orderStore, CartGateway cartGateway, CatalogGateway catalogGateway) {
        this.orderStore = orderStore;
        this.cartGateway = cartGateway;
        this.catalogGateway = catalogGateway;
    }

    // ------------------------------------------------------------------
    // Checkout
    // ------------------------------------------------------------------

    public OrderDetailResponse checkout(Long userId, String userEmail, CreateOrderRequest request) {
        CartView cart = cartGateway.fetchCart(userId);
        if (cart == null || cart.isEmpty()) {
            throw new BadRequestException("Keranjang kamu masih kosong");
        }

        String orderNumber = generateOrderNumber();
        Order order = new Order(orderNumber, userId, userEmail, toAddress(request), request.notes());

        // Langkah 1: tahan stok di product-service, dengan kompensasi bila gagal di tengah jalan.
        List<ReservedLine> reserved = new ArrayList<>();
        try {
            for (CartLine line : cart.items()) {
                ProductSnapshot product = catalogGateway.fetch(line.productId());
                if (!product.active()) {
                    throw new ConflictException("Produk '" + product.name() + "' sudah tidak dijual");
                }
                catalogGateway.reserve(product.id(), line.quantity(), orderNumber);
                reserved.add(new ReservedLine(product.id(), line.quantity()));

                // Harga diambil ULANG dari katalog, bukan dari keranjang.
                // Keranjang bisa berumur berhari-hari; harga di dalamnya sudah basi.
                order.addItem(new OrderItem(
                        product.id(), product.sku(), product.name(), product.imageUrl(),
                        product.priceCents(), line.quantity()));
            }
        } catch (RuntimeException failure) {
            compensate(orderNumber, reserved);
            throw failure;
        }

        // Langkah 2: simpan pesanan (transaksi DB pendek) + antre event setelah commit.
        Order saved;
        try {
            saved = orderStore.create(order, new OrderPlacedDomainEvent(orderNumber, userId));
        } catch (RuntimeException failure) {
            compensate(orderNumber, reserved);
            throw failure;
        }

        log.info("Pesanan {} dibuat: user={} item={} total={} {}",
                saved.getOrderNumber(), userId, saved.itemCount(), saved.getTotalCents(), saved.getCurrency());

        return OrderDetailResponse.from(saved);
    }

    // ------------------------------------------------------------------
    // Pembatalan
    // ------------------------------------------------------------------

    public OrderDetailResponse cancel(String orderNumber, Long userId) {
        OrderDetailResponse current = orderStore.detail(orderNumber, userId);
        if (current.status() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Pesanan berstatus " + current.status() + " tidak dapat dibatalkan");
        }

        // Urutannya sengaja: UBAH STATUS DULU, baru lepas stok.
        // Kalau dua request pembatalan datang bersamaan, hanya satu yang berhasil mengubah
        // status (dijaga @Version + pengecekan status). Yang kalah tidak akan melepas stok.
        // Risikonya: kalau pelepasan stok gagal setelah status berubah, stok "tertahan" dan
        // perlu rekonsiliasi - jauh lebih aman daripada stok dilepas dua kali.
        Order saved = orderStore.markCancelled(orderNumber, userId, "Dibatalkan oleh pengguna");

        releaseReservations(orderNumber);
        return OrderDetailResponse.from(saved);
    }

    // ------------------------------------------------------------------
    // Hasil pembayaran (dipanggil oleh listener RabbitMQ)
    // ------------------------------------------------------------------

    public void applyPaymentCompleted(PaymentCompletedEvent event) {
        Order order = orderStore.markPaid(event.orderNumber(), event.paymentId());
        log.info("Pesanan {} -> {}", order.getOrderNumber(), order.getStatus());
    }

    public void applyPaymentFailed(PaymentFailedEvent event) {
        orderStore
                .markPaymentFailed(event.orderNumber(), "Pembayaran gagal: " + event.reason())
                // Hanya lepas stok bila status benar-benar berubah (idempoten terhadap duplikat).
                .ifPresent(order -> {
                    log.info("Pesanan {} -> {}", order.getOrderNumber(), order.getStatus());
                    releaseReservations(order.getOrderNumber());
                });
    }

    // ------------------------------------------------------------------
    // Baca
    // ------------------------------------------------------------------

    public OrderDetailResponse detail(String orderNumber, Long userId) {
        return orderStore.detail(orderNumber, userId);
    }

    public PageResponse<OrderSummaryResponse> listForUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return orderStore.listForUser(userId, pageable);
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private void releaseReservations(String orderNumber) {
        for (ReservedLine line : orderStore.reservationLines(orderNumber)) {
            catalogGateway.releaseQuietly(line.productId(), line.quantity(), orderNumber);
        }
    }

    private void compensate(String orderNumber, List<ReservedLine> reserved) {
        if (reserved.isEmpty()) {
            return;
        }
        log.warn("Checkout {} gagal - melepas {} baris stok yang sudah ditahan", orderNumber, reserved.size());
        for (ReservedLine line : reserved) {
            catalogGateway.releaseQuietly(line.productId(), line.quantity(), orderNumber);
        }
    }

    private ShippingAddress toAddress(CreateOrderRequest request) {
        var address = request.shippingAddress();
        return new ShippingAddress(
                address.recipientName().trim(),
                address.phone().trim(),
                address.line1().trim(),
                address.line2() == null ? null : address.line2().trim(),
                address.city().trim(),
                address.postalCode().trim(),
                address.country().trim());
    }

    /**
     * Nomor pesanan yang bisa dibacakan lewat telepon dan diurutkan berdasarkan tanggal.
     * Keunikannya dijamin oleh constraint database, bukan oleh asumsi aplikasi.
     */
    private String generateOrderNumber() {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 6).toUpperCase(Locale.ROOT);
        return "ORD-" + date + "-" + suffix;
    }
}
