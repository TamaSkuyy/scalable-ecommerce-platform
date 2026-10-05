package com.shopflow.order.config;

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
 * Topologi RabbitMQ versi order-service.
 *
 * <p>Semua deklarasi di sini <b>idempoten</b>: kalau exchange/queue sudah ada dengan
 * parameter yang sama, RabbitMQ tidak melakukan apa-apa. Jadi setiap service boleh
 * mendeklarasikan topologi yang ia butuhkan, tanpa takut bentrok.
 *
 * <p>Perhatikan setiap queue diberi <b>dead-letter exchange</b>. Tanpa itu, pesan yang
 * gagal diproses 3x akan hilang begitu saja dan kamu tidak akan pernah tahu ada bug.
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

    /** Satu queue menerima dua jenis hasil pembayaran; jenisnya dibedakan lewat header event. */
    @Bean
    public Queue orderPaymentResultQueue() {
        return QueueBuilder.durable(EventRouting.QUEUE_ORDER_PAYMENT_RESULT)
                .deadLetterExchange(EventRouting.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(EventRouting.DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding bindPaymentCompleted(Queue orderPaymentResultQueue, TopicExchange shopflowEventsExchange) {
        return BindingBuilder.bind(orderPaymentResultQueue)
                .to(shopflowEventsExchange)
                .with(EventRouting.PAYMENT_COMPLETED);
    }

    @Bean
    public Binding bindPaymentFailed(Queue orderPaymentResultQueue, TopicExchange shopflowEventsExchange) {
        return BindingBuilder.bind(orderPaymentResultQueue)
                .to(shopflowEventsExchange)
                .with(EventRouting.PAYMENT_FAILED);
    }

    // MessageConverter JSON TIDAK dideklarasikan di sini.
    // platform-common sudah menyediakannya lewat RabbitEventSupportAutoConfiguration,
    // lengkap dengan daftar trusted-packages yang benar. Lihat kelas itu untuk alasannya.
}
