package com.shopflow.order.event;

/** Event domain internal, dipublikasikan lewat Spring ApplicationEventPublisher. */
public record OrderPlacedDomainEvent(String orderNumber, Long userId) {
}
