package com.shopflow.product.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotBlank(message = "SKU wajib diisi")
        @Size(max = 64, message = "SKU maksimal 64 karakter")
        String sku,

        @NotBlank(message = "Nama produk wajib diisi")
        @Size(max = 200, message = "Nama maksimal 200 karakter")
        String name,

        @Size(max = 5000, message = "Deskripsi maksimal 5000 karakter")
        String description,

        @NotNull(message = "Harga wajib diisi")
        @Positive(message = "Harga harus lebih dari 0")
        Long priceCents,

        @NotBlank(message = "Kategori wajib diisi")
        String categorySlug,

        @Size(max = 500, message = "URL gambar maksimal 500 karakter")
        String imageUrl,

        @NotNull(message = "Stok awal wajib diisi")
        @Min(value = 0, message = "Stok tidak boleh negatif")
        Integer stockQuantity) {
}
