package com.shopflow.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Konfigurasi keamanan user-service.
 *
 * <p><b>Kenapa semua request di-permitAll?</b>
 * Otorisasi sudah dilakukan di API Gateway (satu tempat, mudah diaudit). Service ini
 * hanya bisa dijangkau lewat jaringan internal Docker (port-nya tidak dipublikasikan ke host).
 *
 * <p>Yang tetap kita butuhkan di sini adalah {@link PasswordEncoder} - BCrypt dengan
 * cost factor 12 (sengaja lebih lambat: memperlambat brute-force).
 *
 * <p><b>Latihan lanjutan (produksi):</b> jangan bergantung pada "trust the network".
 * Tambahkan filter yang memverifikasi JWT juga di service ini (defense in depth),
 * atau pakai mTLS antar-service. Lihat docs/02-architecture.md bagian "Hardening".
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
