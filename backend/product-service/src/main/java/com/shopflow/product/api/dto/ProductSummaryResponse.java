package com.shopflow.product.api.dto;

import com.shopflow.product.domain.Product;

/**
 * Versi ringkas produk untuk daftar/katalog.
 *
 * <p>Jangan mengembalikan entity JPA langsung ke klien: (1) bisa memicu lazy-loading di
 * tengah serialisasi JSON, (2) membocorkan field internal, (3) membuat API berubah
 * otomatis saat skema database berubah.
 */
public record ProductSummaryResponse(
        Long id,
        String sku,
        String slug,
        String name,
        long priceCents,
        String currency,
        String categoryName,
        String categorySlug,
        String imageUrl,
        boolean inStock) {

    public static ProductSummaryResponse from(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getSku(),
                product.getSlug(),
                product.getName(),
                product.getPriceCents(),
                product.getCurrency(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getImageUrl(),
                product.isInStock());
    }
}
