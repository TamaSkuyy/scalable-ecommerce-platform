package com.shopflow.order.integration;

import com.shopflow.common.api.ApiException;
import com.shopflow.common.api.ServiceUnavailableException;
import com.shopflow.order.integration.dto.ProductSnapshot;
import com.shopflow.order.integration.dto.StockOperation;
import com.shopflow.order.integration.dto.StockView;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pembungkus Feign ke product-service.
 *
 * <p>Kenapa dibungkus, bukan dipanggil langsung dari service?
 * Karena di sini kita bisa memberi nama circuit breaker ({@code productService}) dan
 * menerjemahkan kegagalan teknis menjadi pesan yang bisa dimengerti user.
 */
@Component
public class CatalogGateway {

    private static final Logger log = LoggerFactory.getLogger(CatalogGateway.class);

    private final ProductClient productClient;

    public CatalogGateway(ProductClient productClient) {
        this.productClient = productClient;
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "catalogUnavailable")
    public ProductSnapshot fetch(Long productId) {
        return productClient.findById(productId);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "stockUnavailable")
    public StockView reserve(Long productId, int quantity, String orderNumber) {
        return productClient.reserve(productId, new StockOperation(quantity, orderNumber));
    }

    /**
     * Melepas stok (kompensasi).
     *
     * <p>Tidak memakai circuit breaker fallback yang melempar exception: kalau pelepasan
     * stok gagal, kita hanya mencatatnya. Membatalkan proses yang sedang berjalan gara-gara
     * rollback gagal justru membuat keadaan lebih buruk.
     */
    public void releaseQuietly(Long productId, int quantity, String orderNumber) {
        try {
            productClient.release(productId, new StockOperation(quantity, orderNumber));
            log.info("Stok dilepas: produk={} qty={} order={}", productId, quantity, orderNumber);
        } catch (RuntimeException e) {
            // Di produksi: kirim ke dead-letter queue / tabel kompensasi untuk direkonsiliasi.
            log.error("GAGAL melepas stok produk={} qty={} order={}. Perlu rekonsiliasi manual. Penyebab: {}",
                    productId, quantity, orderNumber, e.toString());
        }
    }

    @SuppressWarnings("unused")
    public ProductSnapshot catalogUnavailable(Long productId, Throwable cause) {
        if (cause instanceof ApiException apiException) {
            throw apiException;
        }
        log.error("Gagal memanggil product-service (produk {}): {}", productId, cause.toString());
        throw new ServiceUnavailableException(
                "Katalog produk sedang tidak dapat diakses. Silakan coba beberapa saat lagi.");
    }

    @SuppressWarnings("unused")
    public StockView stockUnavailable(Long productId, int quantity, String orderNumber, Throwable cause) {
        if (cause instanceof ApiException apiException) {
            throw apiException;
        }
        log.error("Gagal menahan stok produk {}: {}", productId, cause.toString());
        throw new ServiceUnavailableException(
                "Layanan stok sedang tidak dapat diakses. Silakan coba beberapa saat lagi.");
    }
}
