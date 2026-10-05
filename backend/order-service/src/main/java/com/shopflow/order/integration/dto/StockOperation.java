package com.shopflow.order.integration.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Body untuk endpoint reserve/release stok di product-service. */
public record StockOperation(@NotNull @Min(1) Integer quantity, String reference) {
}
