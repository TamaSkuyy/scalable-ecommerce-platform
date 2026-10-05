package com.shopflow.order.event;

import com.shopflow.order.domain.Order;
import com.shopflow.order.domain.OrderRepository;
import com.shopflow.order.integration.CartGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Efek samping yang hanya boleh berjalan <b>setelah transaksi commit</b>.
 *
 * <p>Kenapa? Kalau kita mengirim event di dalam transaksi lalu transaksi di-rollback,
 * konsumen sudah terlanjur menerima "pesanan dibuat" padahal pesanan tidak ada.
 * {@link TransactionalEventListener} dengan fase {@code AFTER_COMMIT} menghilangkan
 * seluruh kelas bug tersebut.
 *
 * <p><b>Batasan yang perlu kamu tahu:</b> kalau aplikasi mati tepat setelah commit tapi
 * sebelum pesan terkirim, event itu hilang. Solusi industri untuk ini adalah
 * <b>Transactional Outbox</b>: tulis event ke tabel {@code outbox} dalam transaksi yang
 * sama, lalu ada poller yang mengirim & menandainya terkirim. Lihat docs/04-events.md.
 */
@Component
public class OrderPostCommitListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPostCommitListener.class);

    private final OrderRepository orderRepository;
    private final CartGateway cartGateway;
    private final OrderEventPublisher eventPublisher;

    public OrderPostCommitListener(OrderRepository orderRepository,
                                   CartGateway cartGateway,
                                   OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.cartGateway = cartGateway;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedDomainEvent event) {
        Order order = orderRepository.findDetailByOrderNumber(event.orderNumber()).orElse(null);
        if (order == null) {
            log.error("Pesanan {} hilang setelah commit - tidak mungkin terjadi", event.orderNumber());
            return;
        }

        // 1) Keranjang boleh gagal dibersihkan: pesanan tetap sah.
        cartGateway.clearCartQuietly(event.userId());

        // 2) Terbitkan event -> payment-service & notification-service.
        eventPublisher.publishOrderCreated(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledDomainEvent event) {
        orderRepository.findByOrderNumber(event.orderNumber())
                .ifPresent(order -> eventPublisher.publishOrderCancelled(order, event.reason()));
    }
}
