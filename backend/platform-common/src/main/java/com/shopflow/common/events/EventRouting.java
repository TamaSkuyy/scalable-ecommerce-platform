package com.shopflow.common.events;

/**
 * Kontrak "alamat" event di message broker.
 *
 * <p>Semua service menulis nama exchange/queue/routing-key dari konstanta ini supaya
 * tidak ada typo yang menyebabkan pesan hilang tanpa error (masalah klasik AMQP).
 *
 * <p>Topologi:
 * <pre>
 *                       exchange: shopflow.events (topic)
 *   order-service  --order.created-------->  +--> payment.order-created        (payment-service)
 *                                            +--> notification.events          (notification-service)
 *
 *   payment-service --payment.completed--->  +--> order.payment-result         (order-service)
 *                    --payment.failed----->  +--> order.payment-result         (order-service)
 *                                            +--> notification.events          (notification-service)
 *
 *   order-service  --order.cancelled------>  +--> notification.events          (notification-service)
 * </pre>
 *
 * <p>Semua queue punya dead-letter queue ({@link #DEAD_LETTER_QUEUE}) supaya pesan yang
 * gagal diproses berulang kali tidak hilang begitu saja — bisa diperiksa manual.
 */
public final class EventRouting {

    public static final String EXCHANGE = "shopflow.events";
    public static final String DEAD_LETTER_EXCHANGE = "shopflow.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "shopflow.events.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead-letter";

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";

    public static final String QUEUE_PAYMENT_ORDER_CREATED = "shopflow.payment.order-created";
    public static final String QUEUE_ORDER_PAYMENT_RESULT = "shopflow.order.payment-result";
    public static final String QUEUE_NOTIFICATION_EVENTS = "shopflow.notification.events";

    /** Header AMQP berisi jenis event, memudahkan routing/observasi di RabbitMQ UI. */
    public static final String HEADER_EVENT_TYPE = "x-event-type";
    public static final String HEADER_CORRELATION_ID = "x-correlation-id";

    private EventRouting() {
    }
}
