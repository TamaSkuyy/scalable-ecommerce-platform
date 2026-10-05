package com.shopflow.user.api.dto;

/**
 * Hasil login.
 *
 * <p>Klien menyimpan {@code accessToken} di <b>httpOnly cookie</b> (lihat BFF di Next.js),
 * bukan di localStorage. Alasannya: JavaScript tidak bisa membacanya, sehingga XSS
 * tidak langsung berarti pencurian token.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {

    public static AuthResponse of(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
