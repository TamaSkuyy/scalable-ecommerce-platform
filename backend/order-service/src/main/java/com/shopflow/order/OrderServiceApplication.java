package com.shopflow.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Order Service - jantung alur checkout.
 *
 * <p>Service ini adalah contoh <b>orchestration saga</b>: ia mengoordinasi langkah-langkah
 * yang tersebar di beberapa service, dan bertanggung jawab menjalankan kompensasi
 * (rollback) kalau salah satu langkah gagal.
 *
 * <pre>
 * checkout:
 *   1. ambil keranjang          -> cart-service      (sinkron, Feign)
 *   2. tahan stok per item      -> product-service   (sinkron, Feign)  [kompensasi bila gagal]
 *   3. simpan pesanan           -> DB sendiri        (transaksi lokal)
 *   4. [setelah commit] kosongkan keranjang + publish `order.created`
 *
 * hasil pembayaran (asinkron, RabbitMQ):
 *   payment.completed -> status PAID
 *   payment.failed    -> lepas stok + status PAYMENT_FAILED + publish `order.cancelled`
 * </pre>
 *
 * <p>Database sendiri: {@code shopflow_order}.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
