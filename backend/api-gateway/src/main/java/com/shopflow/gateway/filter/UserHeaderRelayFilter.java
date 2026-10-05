package com.shopflow.gateway.filter;

import com.shopflow.common.security.SecurityHeaders;
import com.shopflow.common.web.Correlation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Menyuntikkan identitas user + correlation id ke request yang diteruskan ke hilir.
 *
 * <p>Yang penting: header masuk {@code X-User-Id} dari klien <b>selalu dihapus dulu</b>.
 * Kalau tidak, siapa pun bisa mengirim {@code X-User-Id: 1} dan berpura-pura menjadi
 * admin — ini kerentanan paling umum pada arsitektur "trust the gateway header".
 * Sama seperti pola {@code X-Forwarded-For} yang harus di-overwrite oleh proxy.
 */
@Component
public class UserHeaderRelayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(UserHeaderRelayFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .doOnNext(auth -> log.debug("Meneruskan identitas user '{}' ke {}",
                        auth.getToken().getSubject(), exchange.getRequest().getPath().value()))
                .map(auth -> exchange.mutate().request(enrich(exchange, auth.getToken())).build())
                .defaultIfEmpty(exchange)
                // Selalu pastikan ada correlation id, walau request anonim (katalog publik).
                .map(this::ensureCorrelationId)
                .flatMap(chain::filter);
    }

    private ServerHttpRequest enrich(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();

        // 1) Buang header identitas palsu dari klien.
        builder.headers(headers -> {
            headers.remove(SecurityHeaders.USER_ID);
            headers.remove(SecurityHeaders.USER_EMAIL);
            headers.remove(SecurityHeaders.USER_ROLES);
        });

        // 2) Tulis ulang dari klaim JWT yang sudah diverifikasi.
        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            builder.header(SecurityHeaders.USER_ID, subject);
        }
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            builder.header(SecurityHeaders.USER_EMAIL, email);
        }
        List<String> roles = readRoles(jwt);
        if (!roles.isEmpty()) {
            builder.header(SecurityHeaders.USER_ROLES, String.join(",", roles));
        }
        return builder.build();
    }

    /** Klaim "roles" bisa berupa List<String> atau String tunggal, tergantung issuer. */
    private List<String> readRoles(Jwt jwt) {
        Object raw = jwt.getClaims().get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (raw instanceof String text && !text.isBlank()) {
            return List.of(text.split(","));
        }
        return List.of();
    }

    private ServerWebExchange ensureCorrelationId(ServerWebExchange exchange) {
        String existing = exchange.getRequest().getHeaders().getFirst(Correlation.HEADER);
        if (existing != null && !existing.isBlank()) {
            return exchange;
        }
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(Correlation.HEADER, Correlation.newId())
                .build();
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        // Harus jalan SETELAH security context terisi, tapi SEBELUM routing ke service.
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
