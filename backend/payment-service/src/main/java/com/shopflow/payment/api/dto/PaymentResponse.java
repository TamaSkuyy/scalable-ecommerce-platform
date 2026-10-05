package com.shopflow.payment.api.dto;

import com.shopflow.payment.domain.Payment;
import com.shopflow.payment.domain.PaymentStatus;

import java.time.Instant;

public record PaymentResponse(
        String paymentId,
        String orderNumber,
        PaymentStatus status,
        String method,
        long amountCents,
        String currency,
        String providerRef,
        Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrderNumber(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getProviderReference(),
                payment.getCreatedAt());
    }
}
