package com.shopflow.payment.config;

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
 * Topologi RabbitMQ versi payment-service.
 *
 * <p>Queue {@code shopflow.payment.order-created} punya dead-letter exchange: kalau
 * pemrosesan gagal 3x, pesan pindah ke {@code shopflow.events.dlq} alih-alih
 * berputar tanpa henti atau hilang.
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
    public Queue paymentOrderCreatedQueue() {
        return QueueBuilder.durable(EventRouting.QUEUE_PAYMENT_ORDER_CREATED)
                .deadLetterExchange(EventRouting.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(EventRouting.DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding bindOrderCreated(Queue paymentOrderCreatedQueue, TopicExchange shopflowEventsExchange) {
        return BindingBuilder.bind(paymentOrderCreatedQueue)
                .to(shopflowEventsExchange)
                .with(EventRouting.ORDER_CREATED);
    }

    // MessageConverter JSON disediakan platform-common
    // (RabbitEventSupportAutoConfiguration) dengan trusted-packages yang benar.
}
