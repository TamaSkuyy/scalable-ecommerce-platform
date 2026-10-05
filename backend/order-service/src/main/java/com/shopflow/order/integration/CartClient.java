package com.shopflow.order.integration;

import com.shopflow.order.integration.dto.CartView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Klien ke cart-service.
 *
 * <p>Kita memakai endpoint internal ({@code /api/v1/internal/cart/**}) karena order-service
 * perlu membaca keranjang <b>atas nama user lain</b> (user yang sedang checkout) - sesuatu
 * yang tidak mungkin dilakukan lewat endpoint publik yang bergantung pada header
 * {@code X-User-Id} dari Gateway.
 */
@FeignClient(name = "cart-service", path = "/api/v1/internal/cart")
public interface CartClient {

    @GetMapping("/{userId}")
    CartView findByUserId(@PathVariable("userId") Long userId);

    @DeleteMapping("/{userId}")
    void clear(@PathVariable("userId") Long userId);
}
