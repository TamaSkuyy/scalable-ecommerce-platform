package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;

/** Diterbitkan order-service saat pesanan dibatalkan (oleh user atau karena pembayaran gagal). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCancelledEvent(
        String eventId,
        String orderNumber,
        Long userId,
        String reason,
        Instant occurredAt) implements Serializable {
}
