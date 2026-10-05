package com.shopflow.order.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductSnapshot(
        Long id,
        String sku,
        String slug,
        String name,
        String imageUrl,
        long priceCents,
        String currency,
        int stockQuantity,
        boolean active) {
}
