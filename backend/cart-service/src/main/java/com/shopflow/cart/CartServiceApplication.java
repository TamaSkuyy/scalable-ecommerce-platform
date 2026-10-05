package com.shopflow.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Shopping Cart Service.
 *
 * <p><b>Kenapa Redis, bukan PostgreSQL?</b>
 * Keranjang belanja adalah data sementara yang sangat sering ditulis (setiap klik
 * "tambah ke keranjang") tapi jarang dibaca berulang. Redis memberi:
 * <ul>
 *   <li>Latensi sub-milidetik (in-memory)</li>
 *   <li>TTL otomatis - keranjang yang ditinggalkan hilang sendiri setelah 30 hari,
 *       tanpa job pembersih</li>
 *   <li>Tidak membebani database transaksional (order) dengan trafik tulis yang "kotor"</li>
 * </ul>
 *
 * <p>Tidak ada JPA di service ini - memang sengaja.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class CartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }
}
