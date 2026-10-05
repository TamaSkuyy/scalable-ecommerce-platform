package com.shopflow.order.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StockView(Long productId, String sku, int stockQuantity, boolean inStock) {
}
