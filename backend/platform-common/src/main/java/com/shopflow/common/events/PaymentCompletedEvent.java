package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;

/** Diterbitkan payment-service setelah charge berhasil. Konsumen: order-service, notification-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCompletedEvent(
        String eventId,
        String orderNumber,
        Long userId,
        String paymentId,
        String providerRef,
        String method,
        long amountCents,
        String currency,
        Instant occurredAt) implements Serializable {
}
