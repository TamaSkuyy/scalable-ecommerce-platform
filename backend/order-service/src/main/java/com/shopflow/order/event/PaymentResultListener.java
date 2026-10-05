package com.shopflow.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.events.EventRouting;
import com.shopflow.common.events.PaymentCompletedEvent;
import com.shopflow.common.events.PaymentFailedEvent;
import com.shopflow.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Konsumen hasil pembayaran.
 *
 * <p>Karena satu queue menerima dua jenis event, kita memilih tipe berdasarkan header
 * {@code x-event-type} alih-alih menebak dari isi payload. Ini pola yang sama dipakai
 * banyak sistem produksi.
 *
 * <p>Exception yang dilempar dari sini akan memicu retry (3x, lihat application.yml),
 * lalu pesan masuk ke dead-letter queue - tidak hilang tanpa jejak.
 */
@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public PaymentResultListener(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @RabbitListener(queues = EventRouting.QUEUE_ORDER_PAYMENT_RESULT)
    public void handle(Message message) {
        Object rawEventType = message.getMessageProperties().getHeaders().get(EventRouting.HEADER_EVENT_TYPE);
        String eventType = rawEventType == null ? "" : rawEventType.toString();

        log.info("Menerima event '{}' dari queue {}", eventType, EventRouting.QUEUE_ORDER_PAYMENT_RESULT);

        try {
            switch (eventType) {
                case EventRouting.PAYMENT_COMPLETED -> orderService.applyPaymentCompleted(
                        objectMapper.readValue(message.getBody(), PaymentCompletedEvent.class));
                case EventRouting.PAYMENT_FAILED -> orderService.applyPaymentFailed(
                        objectMapper.readValue(message.getBody(), PaymentFailedEvent.class));
                default -> log.warn("Event '{}' tidak dikenal -> dikirim ke dead-letter queue", eventType);
            }
        } catch (IOException e) {
            // Payload rusak: retry tidak akan menolong. Lempar supaya masuk DLQ.
            throw new IllegalArgumentException("Payload event tidak dapat dibaca: " + e.getMessage(), e);
        }
    }
}
