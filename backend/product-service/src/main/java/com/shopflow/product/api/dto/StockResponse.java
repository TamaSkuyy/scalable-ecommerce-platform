package com.shopflow.product.api.dto;

import com.shopflow.product.domain.Product;

public record StockResponse(
        Long productId,
        String sku,
        int stockQuantity,
        boolean inStock) {

    public static StockResponse from(Product product) {
        return new StockResponse(
                product.getId(),
                product.getSku(),
                product.getStockQuantity(),
                product.isInStock());
    }
}
