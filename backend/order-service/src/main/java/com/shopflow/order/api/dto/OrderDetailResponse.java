package com.shopflow.order.api.dto;

import com.shopflow.order.domain.Order;
import com.shopflow.order.domain.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderDetailResponse(
        String orderNumber,
        OrderStatus status,
        long totalCents,
        String currency,
        List<OrderItemResponse> items,
        ShippingAddressResponse shippingAddress,
        String notes,
        String paymentReference,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalCents(),
                order.getCurrency(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                ShippingAddressResponse.from(order.getShippingAddress()),
                order.getNotes(),
                order.getPaymentReference(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
