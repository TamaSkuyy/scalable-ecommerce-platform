package com.shopflow.cart.client;

import com.shopflow.cart.client.dto.ProductSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Klien HTTP deklaratif ke product-service.
 *
 * <p>{@code name = "product-service"} BUKAN hostname - itu nama logis di Eureka.
 * Spring Cloud LoadBalancer yang menerjemahkannya ke IP:port instance yang sehat.
 * Karena itu kita tidak pernah menulis alamat IP di kode.
 *
 * <p>Retry/timeout/circuit breaker dikonfigurasi terpusat; lihat
 * {@code CatalogGateway} dan {@code application.yml}.
 */
@FeignClient(name = "product-service", path = "/api/v1/internal/products")
public interface ProductClient {

    @GetMapping("/{id}")
    ProductSnapshot findById(@PathVariable("id") Long id);
}
