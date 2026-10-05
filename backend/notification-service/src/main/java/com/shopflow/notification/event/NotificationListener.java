package com.shopflow.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.events.EventRouting;
import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.common.events.PaymentCompletedEvent;
import com.shopflow.common.events.PaymentFailedEvent;
import com.shopflow.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Konsumen tunggal untuk empat jenis event.
 *
 * <p>Dispatch dilakukan berdasarkan header {@code x-event-type}, bukan menebak dari
 * bentuk payload. Header ini diset produsen saat mempublikasikan (lihat
 * {@code OrderEventPublisher} dan {@code PaymentEventPublisher}).
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public NotificationListener(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = EventRouting.QUEUE_NOTIFICATION_EVENTS)
    public void handle(Message message) throws IOException {
        Object rawEventType = message.getMessageProperties().getHeaders().get(EventRouting.HEADER_EVENT_TYPE);
        String eventType = rawEventType == null ? "" : rawEventType.toString();
        byte[] body = message.getBody();

        switch (eventType) {
            case EventRouting.ORDER_CREATED ->
                    notificationService.onOrderCreated(objectMapper.readValue(body, OrderCreatedEvent.class));
            case EventRouting.ORDER_CANCELLED ->
                    notificationService.onOrderCancelled(objectMapper.readValue(body, OrderCancelledEvent.class));
            case EventRouting.PAYMENT_COMPLETED ->
                    notificationService.onPaymentCompleted(objectMapper.readValue(body, PaymentCompletedEvent.class));
            case EventRouting.PAYMENT_FAILED ->
                    notificationService.onPaymentFailed(objectMapper.readValue(body, PaymentFailedEvent.class));
            default -> log.warn("Event '{}' belum ditangani notification-service -> dead-letter queue", eventType);
        }
    }
}
