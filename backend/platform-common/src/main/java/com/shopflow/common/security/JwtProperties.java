package com.shopflow.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Konfigurasi JWT, dibaca dari:
 * <pre>
 * shopflow:
 *   jwt:
 *     secret: ${JWT_SECRET}          # minimal 32 karakter untuk HS256
 *     issuer: shopflow-identity
 *     access-token-ttl: 30m
 * </pre>
 *
 * <p>Secret HARUS sama di user-service (yang menandatangani) dan api-gateway
 * (yang memverifikasi). Di produksi, nilai ini datang dari secret manager
 * (Vault / Kubernetes Secret / AWS Secrets Manager), bukan dari file.
 */
@ConfigurationProperties(prefix = "shopflow.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {

    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "shopflow-identity";
        }
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            accessTokenTtl = Duration.ofMinutes(30);
        }
        if (secret == null) {
            secret = "";
        }
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    /** HS256 butuh key >= 256 bit. Gagal cepat (fail fast) lebih baik daripada token lemah. */
    public boolean isUsable() {
        return secretBytes().length >= MIN_SECRET_BYTES;
    }

    public void requireUsable() {
        if (!isUsable()) {
            throw new IllegalStateException(
                    "shopflow.jwt.secret terlalu pendek (" + secretBytes().length + " byte). "
                            + "Minimal " + MIN_SECRET_BYTES + " karakter. Set environment variable JWT_SECRET.");
        }
    }
}
