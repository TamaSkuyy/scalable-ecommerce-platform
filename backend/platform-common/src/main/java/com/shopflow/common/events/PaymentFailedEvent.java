package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;

/** Diterbitkan payment-service saat charge gagal. Konsumen: order-service, notification-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentFailedEvent(
        String eventId,
        String orderNumber,
        Long userId,
        String paymentId,
        String reason,
        long amountCents,
        String currency,
        Instant occurredAt) implements Serializable {
}
