package com.shopflow.order.integration;

import com.shopflow.common.api.ApiException;
import com.shopflow.common.api.ServiceUnavailableException;
import com.shopflow.order.integration.dto.CartView;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Pembungkus Feign ke cart-service dengan circuit breaker. Lihat {@link CatalogGateway} untuk penjelasan pola. */
@Component
public class CartGateway {

    private static final Logger log = LoggerFactory.getLogger(CartGateway.class);

    private final CartClient cartClient;

    public CartGateway(CartClient cartClient) {
        this.cartClient = cartClient;
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "cartUnavailable")
    public CartView fetchCart(Long userId) {
        return cartClient.findByUserId(userId);
    }

    /**
     * Mengosongkan keranjang setelah pesanan tersimpan.
     *
     * <p>Kegagalan di sini TIDAK boleh membatalkan pesanan yang sudah dibuat:
     * pesanan adalah sumber kebenaran, keranjang hanyalah kemudahan. Kalau gagal,
     * user akan melihat keranjang lamanya masih terisi, dan itu bisa ditoleransi.
     */
    public void clearCartQuietly(Long userId) {
        try {
            cartClient.clear(userId);
        } catch (RuntimeException e) {
            log.warn("Gagal mengosongkan keranjang user {}. Pesanan tetap diproses. Penyebab: {}",
                    userId, e.toString());
        }
    }

    @SuppressWarnings("unused")
    public CartView cartUnavailable(Long userId, Throwable cause) {
        if (cause instanceof ApiException apiException) {
            throw apiException;
        }
        log.error("Gagal memanggil cart-service untuk user {}: {}", userId, cause.toString());
        throw new ServiceUnavailableException("Keranjang sedang tidak dapat diakses. Coba beberapa saat lagi.");
    }
}
