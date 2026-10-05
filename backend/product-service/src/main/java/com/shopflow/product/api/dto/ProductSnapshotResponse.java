package com.shopflow.product.api.dto;

import com.shopflow.product.domain.Product;

/**
 * Snapshot produk untuk konsumsi service lain (cart-service &amp; order-service).
 *
 * <p>Kenapa ada endpoint/DTO terpisah, bukan memakai endpoint publik?
 * <ul>
 *   <li>Service lain butuh {@code stockQuantity} &amp; {@code active} yang tidak
 *       diekspos ke pengguna umum.</li>
 *   <li>Kontrak "internal" bisa berubah tanpa memutus klien publik.</li>
 * </ul>
 */
public record ProductSnapshotResponse(
        Long id,
        String sku,
        String slug,
        String name,
        String imageUrl,
        long priceCents,
        String currency,
        int stockQuantity,
        boolean active) {

    public static ProductSnapshotResponse from(Product product) {
        return new ProductSnapshotResponse(
                product.getId(),
                product.getSku(),
                product.getSlug(),
                product.getName(),
                product.getImageUrl(),
                product.getPriceCents(),
                product.getCurrency(),
                product.getStockQuantity(),
                product.isActive());
    }
}
