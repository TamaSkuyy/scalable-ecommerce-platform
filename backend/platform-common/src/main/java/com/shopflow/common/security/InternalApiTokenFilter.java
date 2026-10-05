package com.shopflow.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Gerbang untuk endpoint {@code /api/v1/internal/**}.
 *
 * <p>Endpoint internal dipakai service lain (order-service, cart-service) dan tidak pernah
 * lewat API Gateway, jadi tidak ada JWT di sana. Daripada "percaya saja karena satu jaringan",
 * kita minta token bersama di header {@code X-Internal-Token}.
 *
 * <p>Perbandingan memakai {@link MessageDigest#isEqual} (constant-time) supaya waktu respons
 * tidak membocorkan berapa digit token yang sudah benar (timing attack).
 *
 * <p><b>Fail closed:</b> kalau token belum dikonfigurasi, semua request internal ditolak.
 * Aktif hanya bila properti {@code shopflow.internal.token} terisi - lihat
 * {@code PlatformCommonAutoConfiguration}.
 */
public class InternalApiTokenFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PREFIX = "/api/v1/internal/";

    private final byte[] expectedToken;

    public InternalApiTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken == null ? new byte[0] : expectedToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PROTECTED_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String providedHeader = request.getHeader(SecurityHeaders.INTERNAL_TOKEN);
        byte[] provided = providedHeader == null ? new byte[0] : providedHeader.getBytes(StandardCharsets.UTF_8);

        if (expectedToken.length == 0 || provided.length == 0 || !MessageDigest.isEqual(expectedToken, provided)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {"status":403,"error":"Forbidden","message":"Endpoint internal: token tidak valid","violations":[]}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
