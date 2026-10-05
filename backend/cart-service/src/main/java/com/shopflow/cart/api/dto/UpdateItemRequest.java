package com.shopflow.cart.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateItemRequest(
        @NotNull(message = "quantity wajib diisi")
        @Min(value = 0, message = "quantity tidak boleh negatif")
        @Max(value = 99, message = "quantity maksimal 99")
        Integer quantity) {
}
