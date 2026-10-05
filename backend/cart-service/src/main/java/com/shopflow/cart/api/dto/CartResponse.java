package com.shopflow.cart.api.dto;

import com.shopflow.cart.domain.CartState;

import java.time.Instant;
import java.util.List;

public record CartResponse(
        Long userId,
        List<CartItemResponse> items,
        long subtotalCents,
        int itemCount,
        String currency,
        Instant updatedAt) {

    public static CartResponse from(CartState state) {
        return new CartResponse(
                state.userId(),
                state.items().stream().map(CartItemResponse::from).toList(),
                state.subtotalCents(),
                state.itemCount(),
                state.currency(),
                state.updatedAt());
    }
}
