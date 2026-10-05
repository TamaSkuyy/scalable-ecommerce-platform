package com.shopflow.cart.api;

import com.shopflow.cart.api.dto.AddItemRequest;
import com.shopflow.cart.api.dto.CartResponse;
import com.shopflow.cart.api.dto.UpdateItemRequest;
import com.shopflow.cart.service.CartService;
import com.shopflow.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Keranjang belanja (Redis)")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Lihat keranjang user yang sedang login")
    public CartResponse view(CurrentUser currentUser) {
        return cartService.view(CartService.requireUserId(currentUser));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tambah produk ke keranjang (menumpuk kalau sudah ada)")
    public CartResponse addItem(CurrentUser currentUser, @Valid @RequestBody AddItemRequest request) {
        return cartService.addItem(CartService.requireUserId(currentUser), request);
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Ubah jumlah; quantity=0 berarti hapus item")
    public CartResponse updateItem(CurrentUser currentUser,
                                   @PathVariable Long productId,
                                   @Valid @RequestBody UpdateItemRequest request) {
        return cartService.updateItem(CartService.requireUserId(currentUser), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Hapus satu item")
    public void removeItem(CurrentUser currentUser, @PathVariable Long productId) {
        cartService.removeItem(CartService.requireUserId(currentUser), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Kosongkan keranjang")
    public void clear(CurrentUser currentUser) {
        cartService.clear(CartService.requireUserId(currentUser));
    }
}
