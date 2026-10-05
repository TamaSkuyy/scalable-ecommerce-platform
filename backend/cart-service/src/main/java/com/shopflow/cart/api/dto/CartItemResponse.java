package com.shopflow.cart.api.dto;

import com.shopflow.cart.domain.CartItem;

public record CartItemResponse(
        Long productId,
        String sku,
        String slug,
        String name,
        String imageUrl,
        long unitPriceCents,
        int quantity,
        long lineTotalCents) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.productId(),
                item.sku(),
                item.slug(),
                item.name(),
                item.imageUrl(),
                item.unitPriceCents(),
                item.quantity(),
                item.lineTotalCents());
    }
}
