package com.shopflow.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Notification Service.
 *
 * <p>Contoh <b>consumer murni</b>: ia tidak menyediakan endpoint bisnis apa pun untuk
 * ditulis, ia hanya mendengarkan event dan bereaksi. Service seperti ini bisa ditambah
 * atau dimatikan tanpa mengubah service lain - itu inti dari loose coupling.
 *
 * <p>Pengiriman email/SMS di sini <b>disimulasikan</b> (dicatat ke database + log).
 * Untuk produksi, ganti isi {@code NotificationDispatcher} dengan SendGrid/Twilio.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
