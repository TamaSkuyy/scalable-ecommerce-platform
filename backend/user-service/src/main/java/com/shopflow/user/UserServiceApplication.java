package com.shopflow.user;

import com.shopflow.common.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * User Service - identitas &amp; autentikasi.
 *
 * <p>Ini satu-satunya service yang tahu cara MEMBUAT token. Service lain hanya
 * memverifikasi (api-gateway) atau menerima identitas yang sudah diverifikasi.
 *
 * <p>Database sendiri ({@code shopflow_user}) - prinsip <b>database per service</b>.
 * Tidak ada service lain yang boleh menyentuh tabel {@code users}.
 *
 * <p>{@code exclude = UserDetailsServiceAutoConfiguration.class} karena kita
 * <b> tidak memakai Spring Security untuk autentikasi request</b> - itu tugas API Gateway.
 * Tanpa ini, Spring Boot membuat user in-memory acak dan menuliskan password-nya ke log
 * ("Using generated security password: ..."), yang membingungkan saat belajar.
 * Service ini hanya mengambil {@code PasswordEncoder} (BCrypt) dari Spring Security.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties.class)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
