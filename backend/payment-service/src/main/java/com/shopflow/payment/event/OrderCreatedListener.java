package com.shopflow.payment.event;

import com.shopflow.common.events.EventRouting;
import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Konsumen event {@code order.created}.
 *
 * <p>Inilah yang membuat alur checkout menjadi asinkron: order-service selesai lebih dulu,
 * pembayaran diproses "belakangan" tanpa user harus menunggu.
 *
 * <p>Kalau exception dilempar dari sini, Spring AMQP akan mencoba ulang (3x), lalu
 * mengirim pesan ke dead-letter queue untuk diperiksa manual.
 */
@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final PaymentService paymentService;

    public OrderCreatedListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = EventRouting.QUEUE_PAYMENT_ORDER_CREATED)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Menerima order.created: order={} total={} {}",
                event.orderNumber(), event.totalCents(), event.currency());
        paymentService.handleOrderCreated(event);
    }
}
