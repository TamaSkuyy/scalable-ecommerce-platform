package com.shopflow.order.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartLine(
        Long productId,
        String sku,
        String slug,
        String name,
        String imageUrl,
        long unitPriceCents,
        int quantity,
        long lineTotalCents) {
}
