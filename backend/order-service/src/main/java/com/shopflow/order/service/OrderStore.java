package com.shopflow.order.service;

import com.shopflow.common.api.ConflictException;
import com.shopflow.common.api.ForbiddenException;
import com.shopflow.common.api.PageResponse;
import com.shopflow.common.api.ResourceNotFoundException;
import com.shopflow.order.api.dto.OrderDetailResponse;
import com.shopflow.order.api.dto.OrderSummaryResponse;
import com.shopflow.order.domain.Order;
import com.shopflow.order.domain.OrderRepository;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.event.OrderCancelledDomainEvent;
import com.shopflow.order.event.OrderPlacedDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Satu-satunya pintu masuk ke database order.
 *
 * <p>Kenapa dipisah dari {@link OrderService}? Supaya transaksi database tetap
 * <b>pendek</b>. Memanggil HTTP (Feign) di dalam transaksi berarti menahan koneksi
 * database selama jaringan bekerja - dan itu sumber utama "connection pool habis"
 * di arsitektur microservices.
 *
 * <p>Selain itu, {@code @TransactionalEventListener(AFTER_COMMIT)} hanya bekerja kalau
 * event dipublikasikan <b>di dalam</b> transaksi. Di sinilah itu dilakukan.
 */
@Component
public class OrderStore {

    private static final Logger log = LoggerFactory.getLogger(OrderStore.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;

    public OrderStore(OrderRepository orderRepository, ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.events = events;
    }

    // ------------------------------------------------------------------
    // Tulis
    // ------------------------------------------------------------------

    @Transactional
    public Order create(Order order, OrderPlacedDomainEvent domainEvent) {
        Order saved = orderRepository.save(order);
        events.publishEvent(domainEvent);
        return saved;
    }

    /**
     * Menandai pesanan sudah dibayar.
     *
     * <p><b>Idempoten</b>: pesan RabbitMQ bisa terkirim lebih dari sekali (at-least-once
     * delivery). Kalau status sudah PAID, kita cukup keluar tanpa efek samping.
     */
    @Transactional
    public Order markPaid(String orderNumber, String paymentReference) {
        Order order = loadDetail(orderNumber);

        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Pesanan {} sudah PAID - event duplikat diabaikan", orderNumber);
            return order;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Pesanan {} berstatus {} tidak bisa diubah ke PAID", orderNumber, order.getStatus());
            return order;
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentReference(paymentReference);
        return orderRepository.save(order);
    }

    /**
     * Idempoten dengan alasan yang sama seperti {@link #markPaid}, tapi mengembalikan
     * {@link Optional#empty()} kalau tidak ada perubahan - supaya pemanggil tahu apakah
     * kompensasi (pelepasan stok) perlu dijalankan. Ini yang mencegah pelepasan stok ganda.
     */
    @Transactional
    public Optional<Order> markPaymentFailed(String orderNumber, String reason) {
        Order order = loadDetail(orderNumber);

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.info("Pesanan {} berstatus {} - hasil pembayaran gagal diabaikan",
                    orderNumber, order.getStatus());
            return Optional.empty();
        }

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order.setCancellationReason(reason);
        return Optional.of(orderRepository.save(order));
    }

    @Transactional
    public Order markCancelled(String orderNumber, Long userId, String reason) {
        Order order = loadDetail(orderNumber);
        requireOwner(order, userId);

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Pesanan berstatus " + order.getStatus() + " tidak dapat dibatalkan");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        Order saved = orderRepository.save(order);

        events.publishEvent(new OrderCancelledDomainEvent(orderNumber, userId, reason));
        return saved;
    }

    // ------------------------------------------------------------------
    // Baca
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderDetailResponse detail(String orderNumber, Long userId) {
        Order order = loadDetail(orderNumber);
        requireOwner(order, userId);
        return OrderDetailResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listForUser(Long userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page, OrderSummaryResponse::from);
    }

    /** Dipakai sebagai dasar kompensasi (release stok) tanpa membuka lazy collection di luar transaksi. */
    @Transactional(readOnly = true)
    public List<ReservedLine> reservationLines(String orderNumber) {
        return loadDetail(orderNumber).getItems().stream()
                .map(item -> new ReservedLine(item.getProductId(), item.getQuantity()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderStatus statusOf(String orderNumber) {
        return loadDetail(orderNumber).getStatus();
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private Order loadDetail(String orderNumber) {
        return orderRepository.findDetailByOrderNumber(orderNumber)
                .orElseThrow(() -> ResourceNotFoundException.by("Pesanan", "nomor", orderNumber));
    }

    private void requireOwner(Order order, Long userId) {
        if (userId != null && !order.getUserId().equals(userId)) {
            // 403, bukan 404: user memang tidak berhak, dan menyembunyikan keberadaan
            // pesanan orang lain dengan 404 juga pilihan yang sah (pilih salah satu, konsisten).
            throw new ForbiddenException("Pesanan ini bukan milikmu");
        }
    }
}
