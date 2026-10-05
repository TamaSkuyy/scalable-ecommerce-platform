package com.shopflow.gateway;

import com.shopflow.common.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway - satu-satunya pintu masuk dari Next.js.
 *
 * <p>Tanggung jawab di sini (dan HANYA di sini):
 * <ul>
 *   <li>Routing ke service yang tepat berdasarkan path</li>
 *   <li>Verifikasi JWT sekali, lalu meneruskan identitas lewat header internal</li>
 *   <li>CORS untuk browser</li>
 *   <li>Rate limiting / circuit breaker (latihan lanjutan)</li>
 * </ul>
 *
 * <p>Gateway TIDAK berisi logika bisnis. Kalau kamu mulai menulis if-else tentang
 * pesanan atau produk di sini, itu tanda logika itu salah tempat.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties.class)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
