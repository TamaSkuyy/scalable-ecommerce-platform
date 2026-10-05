package com.shopflow.product.api.dto;

import com.shopflow.product.domain.Product;

import java.time.Instant;

public record ProductDetailResponse(
        Long id,
        String sku,
        String slug,
        String name,
        String description,
        long priceCents,
        String currency,
        String categoryName,
        String categorySlug,
        String imageUrl,
        int stockQuantity,
        boolean inStock,
        Instant createdAt) {

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getSku(),
                product.getSlug(),
                product.getName(),
                product.getDescription(),
                product.getPriceCents(),
                product.getCurrency(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getImageUrl(),
                product.getStockQuantity(),
                product.isInStock(),
                product.getCreatedAt());
    }
}
