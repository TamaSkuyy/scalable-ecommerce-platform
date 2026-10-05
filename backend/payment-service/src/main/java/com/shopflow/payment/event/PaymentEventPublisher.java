package com.shopflow.payment.event;

import com.shopflow.common.events.EventRouting;
import com.shopflow.common.events.PaymentCompletedEvent;
import com.shopflow.common.events.PaymentFailedEvent;
import com.shopflow.common.web.Correlation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Menerbitkan hasil pembayaran ke RabbitMQ setelah transaksi database commit.
 *
 * <p>Manfaat langsung: order-service TIDAK perlu menunggu pembayaran selesai.
 * Waktu respons checkout tetap cepat, dan kalau payment-service sedang down,
 * pesan cukup menunggu di queue - tidak ada data yang hilang.
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentResult(PaymentResultDomainEvent event) {
        if (event.success()) {
            send(EventRouting.PAYMENT_COMPLETED, new PaymentCompletedEvent(
                    UUID.randomUUID().toString(),
                    event.orderNumber(),
                    event.userId(),
                    event.paymentId(),
                    event.providerReference(),
                    event.method(),
                    event.amountCents(),
                    event.currency(),
                    Instant.now()));
        } else {
            send(EventRouting.PAYMENT_FAILED, new PaymentFailedEvent(
                    UUID.randomUUID().toString(),
                    event.orderNumber(),
                    event.userId(),
                    event.paymentId(),
                    event.failureReason(),
                    event.amountCents(),
                    event.currency(),
                    Instant.now()));
        }
    }

    private void send(String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(EventRouting.EXCHANGE, routingKey, payload, message -> {
            message.getMessageProperties().setHeader(EventRouting.HEADER_EVENT_TYPE, routingKey);
            message.getMessageProperties().setHeader(EventRouting.HEADER_CORRELATION_ID, Correlation.currentTraceId());
            return message;
        });
        log.info("Event pembayaran diterbitkan: {}", routingKey);
    }
}
