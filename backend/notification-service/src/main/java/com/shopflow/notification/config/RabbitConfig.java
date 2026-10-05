package com.shopflow.notification.config;

import com.shopflow.common.events.EventRouting;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Satu queue untuk SEMUA event yang menarik bagi notification-service.
 *
 * <pre>
 *   order.created      --\
 *   order.cancelled    ---+--> shopflow.notification.events --> NotificationListener
 *   payment.completed  ---+
 *   payment.failed     --/
 * </pre>
 *
 * <p>Pola ini disebut "single consumer queue with event-type dispatch": jumlah binding
 * bertambah tanpa perlu queue baru, dan konsumen memilih handler berdasarkan header
 * {@code x-event-type}.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange shopflowEventsExchange() {
        return new TopicExchange(EventRouting.EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EventRouting.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(EventRouting.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(EventRouting.DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public Queue notificationEventsQueue() {
        return QueueBuilder.durable(EventRouting.QUEUE_NOTIFICATION_EVENTS)
                .deadLetterExchange(EventRouting.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(EventRouting.DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding bindOrderCreated(Queue notificationEventsQueue, TopicExchange shopflowEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(shopflowEventsExchange).with(EventRouting.ORDER_CREATED);
    }

    @Bean
    public Binding bindOrderCancelled(Queue notificationEventsQueue, TopicExchange shopflowEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(shopflowEventsExchange).with(EventRouting.ORDER_CANCELLED);
    }

    @Bean
    public Binding bindPaymentCompleted(Queue notificationEventsQueue, TopicExchange shopflowEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(shopflowEventsExchange).with(EventRouting.PAYMENT_COMPLETED);
    }

    @Bean
    public Binding bindPaymentFailed(Queue notificationEventsQueue, TopicExchange shopflowEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(shopflowEventsExchange).with(EventRouting.PAYMENT_FAILED);
    }

    // MessageConverter JSON disediakan platform-common
    // (RabbitEventSupportAutoConfiguration) dengan trusted-packages yang benar.
}
