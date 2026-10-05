package com.shopflow.order.api.dto;

import com.shopflow.order.domain.Order;
import com.shopflow.order.domain.OrderStatus;

import java.time.Instant;

public record OrderSummaryResponse(
        String orderNumber,
        OrderStatus status,
        long totalCents,
        String currency,
        int itemCount,
        Instant createdAt) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalCents(),
                order.getCurrency(),
                order.itemCount(),
                order.getCreatedAt());
    }
}
