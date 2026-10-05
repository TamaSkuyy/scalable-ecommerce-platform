package com.shopflow.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definisi rute.
 *
 * <p>Kita memakai <b>Java DSL</b> (bukan YAML) karena:
 * <ul>
 *   <li>Dapat di-refactor &amp; di-compile-check (typo path langsung ketahuan)</li>
 *   <li>Tidak terpengaruh perubahan nama properti antar versi Spring Cloud Gateway</li>
 *   <li>Filter kustom bisa dirantai dengan rapi</li>
 * </ul>
 * Padanan YAML-nya bisa dilihat di {@code docs/02-architecture.md}.
 *
 * <p>{@code lb://nama-service} = "LoadBalancer": Gateway menanyakan Eureka alamat
 * instance yang sehat, lalu memilih salah satu (round-robin). Inilah yang membuat
 * kita bisa menaikkan jumlah replika tanpa mengubah konfigurasi apa pun.
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator shopflowRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

                // ---- Identity ----
                .route("user-service", r -> r
                        .path("/api/v1/auth/**", "/api/v1/users/**")
                        .uri("lb://user-service"))

                // ---- Catalog ----
                .route("product-service", r -> r
                        .path("/api/v1/products/**", "/api/v1/categories/**")
                        .uri("lb://product-service"))

                // ---- Cart ----
                .route("cart-service", r -> r
                        .path("/api/v1/cart/**")
                        .uri("lb://cart-service"))

                // ---- Order ----
                .route("order-service", r -> r
                        .path("/api/v1/orders/**")
                        .uri("lb://order-service"))

                // ---- Payment ----
                .route("payment-service", r -> r
                        .path("/api/v1/payments/**")
                        .uri("lb://payment-service"))

                // ---- Notification ----
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .uri("lb://notification-service"))

                .build();
    }
}
