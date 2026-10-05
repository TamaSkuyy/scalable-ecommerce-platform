package com.shopflow.order.event;

public record OrderCancelledDomainEvent(String orderNumber, Long userId, String reason) {
}
