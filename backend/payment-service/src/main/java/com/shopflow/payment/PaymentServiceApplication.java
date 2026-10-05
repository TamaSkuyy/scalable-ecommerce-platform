package com.shopflow.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Payment Service.
 *
 * <p>Service ini <b>event-driven</b>: ia tidak dipanggil oleh user, melainkan bereaksi
 * terhadap event {@code order.created} dari order-service, lalu menerbitkan
 * {@code payment.completed} / {@code payment.failed}.
 *
 * <p>Integrasi dengan Stripe/PayPal sengaja <b>disimulasikan</b> supaya base ini bisa
 * dijalankan tanpa API key. Titik integrasinya ada di {@code SimulatedPaymentGateway} -
 * itulah satu-satunya kelas yang perlu kamu ganti saat memakai gateway sungguhan.
 *
 * <p>Database sendiri: {@code shopflow_payment}.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
