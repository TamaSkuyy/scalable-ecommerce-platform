package com.shopflow.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Product Catalog Service - produk, kategori, dan stok.
 *
 * <p>Service ini adalah sumber kebenaran (source of truth) untuk stok. Order-service
 * memanggilnya untuk menahan (reserve) stok saat checkout.
 *
 * <p>Database sendiri: {@code shopflow_product}.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
