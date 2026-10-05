package com.shopflow.product.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Permintaan menahan / melepas stok dari service lain (order-service).
 *
 * @param quantity  jumlah unit
 * @param reference nomor pesanan / id keranjang, untuk jejak audit
 */
public record StockOperationRequest(
        @NotNull(message = "quantity wajib diisi")
        @Min(value = 1, message = "quantity minimal 1")
        Integer quantity,

        @Size(max = 64, message = "reference maksimal 64 karakter")
        String reference) {
}
