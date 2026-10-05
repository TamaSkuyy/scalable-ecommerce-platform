package com.shopflow.order.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/** Salinan kontrak {@code CartResponse} milik cart-service (lihat catatan di ProductSnapshot). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CartView(
        Long userId,
        List<CartLine> items,
        long subtotalCents,
        int itemCount,
        String currency,
        Instant updatedAt) {

    public CartView {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
