package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Diterbitkan order-service SETELAH pesanan tersimpan di database (status PENDING_PAYMENT).
 *
 * <p>Konsumen: payment-service (melakukan charge) dan notification-service (email konfirmasi).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedEvent(
        String eventId,
        String orderNumber,
        Long userId,
        String userEmail,
        long totalCents,
        String currency,
        List<OrderLine> lines,
        Instant occurredAt) implements Serializable {

    public OrderCreatedEvent {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
