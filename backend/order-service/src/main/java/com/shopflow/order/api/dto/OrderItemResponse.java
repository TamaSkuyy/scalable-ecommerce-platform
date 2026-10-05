package com.shopflow.order.api.dto;

import com.shopflow.order.domain.OrderItem;

public record OrderItemResponse(
        Long productId,
        String sku,
        String name,
        String imageUrl,
        long unitPriceCents,
        int quantity,
        long lineTotalCents) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getSku(),
                item.getName(),
                item.getImageUrl(),
                item.getUnitPriceCents(),
                item.getQuantity(),
                item.lineTotalCents());
    }
}
