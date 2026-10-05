package com.shopflow.order.event;

import com.shopflow.common.events.EventRouting;
import com.shopflow.common.events.OrderCancelledEvent;
import com.shopflow.common.events.OrderCreatedEvent;
import com.shopflow.common.events.OrderLine;
import com.shopflow.common.web.Correlation;
import com.shopflow.order.domain.Order;
import com.shopflow.order.domain.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Menerbitkan event pesanan ke RabbitMQ.
 *
 * <p>Pola <b>publish/subscribe</b>: order-service tidak tahu (dan tidak peduli) siapa yang
 * mendengarkan. Menambah konsumen baru (mis. layanan analitik, fraud detection) tidak
 * memerlukan perubahan apa pun di sini. Itulah kekuatan event-driven.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(Order order) {
        var lines = order.getItems().stream()
                .map(this::toLine)
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getUserEmail(),
                order.getTotalCents(),
                order.getCurrency(),
                lines,
                Instant.now());

        send(EventRouting.ORDER_CREATED, event);
    }

    public void publishOrderCancelled(Order order, String reason) {
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID().toString(),
                order.getOrderNumber(),
                order.getUserId(),
                reason,
                Instant.now());

        send(EventRouting.ORDER_CANCELLED, event);
    }

    private OrderLine toLine(OrderItem item) {
        return new OrderLine(
                item.getProductId(),
                item.getSku(),
                item.getName(),
                item.getImageUrl(),
                item.getUnitPriceCents(),
                item.getQuantity());
    }

    private void send(String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(EventRouting.EXCHANGE, routingKey, payload, message -> {
            // Header ini membuat pesan bisa di-trace & di-dispatch tanpa membuka payload.
            message.getMessageProperties().setHeader(EventRouting.HEADER_EVENT_TYPE, routingKey);
            message.getMessageProperties().setHeader(EventRouting.HEADER_CORRELATION_ID, Correlation.currentTraceId());
            return message;
        });
        log.info("Event diterbitkan: {} (correlationId={})", routingKey, Correlation.currentTraceId());
    }
}
