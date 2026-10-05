package com.shopflow.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Menjamin setiap request punya correlation id.
 *
 * <p>Alur: ambil {@code X-Correlation-Id} dari request (dikirim Gateway). Kalau tidak ada,
 * buat baru. Simpan ke MDC supaya otomatis muncul di setiap baris log, dan kembalikan
 * di response header supaya klien bisa melaporkannya saat ada bug.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(Correlation.HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = Correlation.newId();
        }

        MDC.put(Correlation.MDC_KEY, correlationId);
        response.setHeader(Correlation.HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(Correlation.MDC_KEY);
        }
    }
}
