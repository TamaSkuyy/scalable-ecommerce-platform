package com.shopflow.gateway.config;

import com.shopflow.common.web.Correlation;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Menulis body 401/403 dalam format yang sama dengan service lain.
 *
 * <p>Kenapa penting: default Spring Security mengembalikan body kosong / HTML.
 * Frontend jadi harus menebak-nebak. Dengan format seragam, satu error parser cukup.
 */
@Component
public class GatewayErrorWriter {

    public ServerAuthenticationEntryPoint unauthorized() {
        return (exchange, ex) -> write(exchange, HttpStatus.UNAUTHORIZED,
                "Token tidak ada, kedaluwarsa, atau tidak valid");
    }

    public ServerAccessDeniedHandler forbidden() {
        return (exchange, ex) -> write(exchange, HttpStatus.FORBIDDEN,
                "Kamu tidak punya akses ke resource ini");
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = exchange.getRequest().getHeaders().getFirst(Correlation.HEADER);
        String body = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s","traceId":"%s","violations":[]}
                """.formatted(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value(),
                traceId == null ? "" : traceId);

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
