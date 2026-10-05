package com.shopflow.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    @DisplayName("nilai default dipakai kalau konfigurasi kosong")
    void appliesDefaults() {
        JwtProperties properties = new JwtProperties(null, null, null);

        assertThat(properties.issuer()).isEqualTo("shopflow-identity");
        assertThat(properties.accessTokenTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(properties.secret()).isEmpty();
    }

    @Test
    @DisplayName("TTL nol atau negatif dikembalikan ke default 30 menit")
    void rejectsNonPositiveTtl() {
        assertThat(new JwtProperties("x", "iss", Duration.ZERO).accessTokenTtl())
                .isEqualTo(Duration.ofMinutes(30));
        assertThat(new JwtProperties("x", "iss", Duration.ofMinutes(-5)).accessTokenTtl())
                .isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("requireUsable gagal cepat kalau secret kurang dari 32 byte")
    void failsFastOnShortSecret() {
        JwtProperties tooShort = new JwtProperties("terlalu-pendek", "iss", Duration.ofMinutes(10));

        assertThat(tooShort.isUsable()).isFalse();
        assertThatThrownBy(tooShort::requireUsable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Minimal 32 karakter");
    }

    @Test
    @DisplayName("secret 32 byte atau lebih dianggap layak untuk HS256")
    void acceptsStrongSecret() {
        JwtProperties ok = new JwtProperties("a".repeat(32), "iss", Duration.ofMinutes(10));

        assertThat(ok.isUsable()).isTrue();
        assertThat(ok.secretBytes()).hasSize(32);
        assertThat(ok.accessTokenTtlSeconds()).isEqualTo(600);
    }
}
