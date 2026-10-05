package com.shopflow.gateway.config;

import com.shopflow.common.security.JwtProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

/**
 * Keamanan di edge.
 *
 * <p>Prinsipnya: <b>verifikasi sekali di Gateway</b>, lalu service hilir menerima
 * identitas yang sudah bersih lewat header internal (lihat {@code UserHeaderRelayFilter}).
 * Dengan begitu tiap service tidak perlu tahu cara memverifikasi token.
 *
 * <p>Secret HS256 HARUS sama dengan yang dipakai user-service untuk menandatangani.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /** Endpoint yang boleh diakses tanpa login. Sisanya wajib JWT valid. */
    private static final String[] PUBLIC_POST = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/payments/webhooks/**"
    };

    private static final String[] PUBLIC_GET = {
            "/api/v1/products/**",
            "/api/v1/categories/**",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         CorsConfigurationSource corsConfigurationSource,
                                                         GatewayErrorWriter errorWriter) {
        http
                // API stateless berbasis token: tidak ada cookie sesi, jadi CSRF tidak relevan.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeExchange(exchange -> exchange
                        // Preflight CORS selalu lolos (browser tidak mengirim Authorization saat OPTIONS).
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .pathMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(errorWriter.unauthorized())
                        .accessDeniedHandler(errorWriter.forbidden()));
        return http.build();
    }

    /**
     * Membangun decoder dari secret yang sama dengan user-service.
     *
     * <p>HS256 = HMAC-SHA256 (simetris). Untuk produksi multi-tim, pertimbangkan RS256:
     * user-service memegang private key, service lain cukup public key lewat endpoint JWKS.
     * Dengan begitu kebocoran satu service tidak otomatis membuat orang bisa menerbitkan token.
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(JwtProperties properties) {
        properties.requireUsable();
        SecretKey key = new SecretKeySpec(properties.secretBytes(), "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Tolak token dengan issuer / exp / nbf yang tidak sesuai.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value(
                    "${shopflow.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
