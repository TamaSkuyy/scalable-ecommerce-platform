package com.shopflow.cart.client;

import com.shopflow.cart.client.dto.ProductSnapshot;
import com.shopflow.common.api.ApiException;
import com.shopflow.common.api.ResourceNotFoundException;
import com.shopflow.common.api.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pembungkus panggilan ke product-service, dilengkapi <b>circuit breaker</b>.
 *
 * <p>Masalah yang diselesaikan: kalau product-service lambat/mati, tanpa circuit breaker
 * setiap request keranjang akan menunggu timeout penuh. Thread menumpuk, dan cart-service
 * ikut tumbang - inilah <i>cascading failure</i>.
 *
 * <p>Cara kerja circuit breaker:
 * <pre>
 *   CLOSED   --(>50% gagal dari 20 panggilan)-->  OPEN
 *   OPEN     --(setelah 10 detik)-------------->  HALF_OPEN
 *   HALF_OPEN --(3 panggilan uji sukses)------->  CLOSED
 *   HALF_OPEN --(gagal)------------------------>  OPEN
 * </pre>
 *
 * <p>Konfigurasinya ada di {@code application.yml} bagian {@code resilience4j}.
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

    /**
     * Dipanggil otomatis saat panggilan gagal atau circuit sedang terbuka.
     *
     * <p>Urutan parameter harus sama dengan method aslinya, ditambah {@link Throwable} di akhir.
     */
    @SuppressWarnings("unused")
    public ProductSnapshot catalogUnavailable(Long productId, Throwable cause) {
        // 404 / 409 adalah jawaban bisnis yang sah - teruskan apa adanya,
        // jangan diubah menjadi "layanan sedang down".
        if (cause instanceof ApiException apiException) {
            throw apiException;
        }
        log.error("Gagal memanggil product-service untuk produk {}: {}", productId, cause.toString());
        throw new ServiceUnavailableException(
                "Katalog produk sedang tidak dapat diakses. Silakan coba beberapa saat lagi.");
    }

    /** Dipakai saat kita sengaja ingin membedakan "produk tidak ada" secara eksplisit. */
    public ProductSnapshot requireExisting(Long productId) {
        ProductSnapshot snapshot = fetch(productId);
        if (snapshot == null || snapshot.id() == null) {
            throw ResourceNotFoundException.of("Produk", productId);
        }
        return snapshot;
    }
}
