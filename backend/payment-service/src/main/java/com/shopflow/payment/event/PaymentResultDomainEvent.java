package com.shopflow.payment.event;

import com.shopflow.payment.domain.Payment;

/**
 * Hasil pembayaran sebagai event <b>domain internal</b>.
 *
 * <p>Dipisahkan dari event RabbitMQ karena keduanya punya siklus hidup berbeda:
 * event domain ini hanya hidup di dalam aplikasi dan dipublikasikan lewat Spring
 * {@code ApplicationEventPublisher}. Konversi ke kontrak lintas-service terjadi
 * setelah transaksi commit (lihat {@link PaymentEventPublisher}).
 */
public record PaymentResultDomainEvent(
        String orderNumber,
        Long userId,
        String paymentId,
        String providerReference,
        String method,
        long amountCents,
        String currency,
        boolean success,
        String failureReason) {

    public static PaymentResultDomainEvent from(Payment payment, Long userId) {
        return new PaymentResultDomainEvent(
                payment.getOrderNumber(),
                userId,
                payment.getPaymentId(),
                payment.getProviderReference(),
                payment.getMethod(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getStatus() == com.shopflow.payment.domain.PaymentStatus.SUCCEEDED,
                payment.getFailureReason());
    }
}
