package com.shopflow.cart.api;

import com.shopflow.cart.api.dto.CartResponse;
import com.shopflow.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API internal untuk order-service.
 *
 * <p>Dilindungi {@code InternalApiTokenFilter} dari platform-common: endpoint ini tidak
 * lewat API Gateway dan tidak punya JWT, jadi diverifikasi dengan {@code X-Internal-Token}.
 */
@RestController
@RequestMapping("/api/v1/internal/cart")
@Tag(name = "Internal - Cart", description = "Dipakai order-service")
public class InternalCartController {

    private final CartService cartService;

    public InternalCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Ambil keranjang milik user tertentu (dipakai saat checkout)")
    public CartResponse byUserId(@PathVariable Long userId) {
        return cartService.viewByUserId(userId);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Kosongkan keranjang setelah pesanan dibuat")
    public void clear(@PathVariable Long userId) {
        cartService.clear(userId);
    }
}
