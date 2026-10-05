package com.shopflow.product.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Perubahan stok.
 *
 * @param delta    positif = stok masuk, negatif = stok keluar. Tidak boleh 0.
 * @param reason   alasan perubahan, disimpan di log untuk audit.
 */
public record AdjustStockRequest(
        @NotNull(message = "delta wajib diisi")
        Integer delta,

        @Size(max = 200, message = "Alasan maksimal 200 karakter")
        String reason) {
}
