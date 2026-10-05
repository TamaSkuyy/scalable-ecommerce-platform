package com.shopflow.common.feign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.api.BadRequestException;
import com.shopflow.common.api.ConflictException;
import com.shopflow.common.api.ForbiddenException;
import com.shopflow.common.api.ResourceNotFoundException;
import com.shopflow.common.api.ServiceUnavailableException;
import com.shopflow.common.api.UnauthorizedException;
import com.shopflow.common.security.SecurityHeaders;
import com.shopflow.common.web.Correlation;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

/**
 * Dukungan Feign lintas-service.
 *
 * <p>Aktif otomatis hanya di service yang memang memakai Feign (classpath berisi
 * {@code feign.RequestInterceptor}), jadi user-service / api-gateway tidak terpengaruh.
 *
 * <p>Dua hal yang paling sering dilupakan saat pertama memakai Feign:
 * <ol>
 *   <li><b>Autentikasi internal.</b> Endpoint {@code /api/v1/internal/**} butuh
 *       header {@code X-Internal-Token}. Tanpa ini semua panggilan kena 403.</li>
 *   <li><b>Correlation id.</b> Tanpa meneruskan {@code X-Correlation-Id}, log di service
 *       tujuan akan terpisah dari request yang memicunya.</li>
 * </ol>
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignSupportAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FeignSupportAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(name = "internalAuthForwardingInterceptor")
    public RequestInterceptor internalAuthForwardingInterceptor(
            @Value("${shopflow.internal.token:}") String internalToken) {

        return template -> {
            if (!internalToken.isBlank()) {
                template.header(SecurityHeaders.INTERNAL_TOKEN, internalToken);
            }
            String correlationId = Correlation.currentTraceId();
            if (!correlationId.isEmpty()) {
                template.header(Correlation.HEADER, correlationId);
            }
        };
    }

    /**
     * Mengubah error HTTP dari service lain menjadi exception domain kita.
     *
     * <p>Tanpa ini, semua kegagalan menjadi {@code FeignException} dan lapisan bisnis
     * terpaksa memeriksa status code secara manual di mana-mana.
     */
    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    public ErrorDecoder apiErrorDecoder(ObjectMapper objectMapper) {
        return (methodKey, response) -> toException(methodKey, response, objectMapper);
    }

    private Exception toException(String methodKey, Response response, ObjectMapper objectMapper) {
        String message = "Layanan tujuan mengembalikan status " + response.status();
        try {
            if (response.body() != null) {
                byte[] raw = response.body().asInputStream().readAllBytes();
                if (raw.length > 0) {
                    JsonNode node = objectMapper.readTree(raw);
                    if (node.hasNonNull("message")) {
                        message = node.get("message").asText();
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            log.debug("Body error dari {} tidak bisa dibaca: {}", methodKey, e.getMessage());
        }

        return switch (response.status()) {
            case 400 -> new BadRequestException(message);
            case 401 -> new UnauthorizedException(message);
            case 403 -> new ForbiddenException(message);
            case 404 -> new ResourceNotFoundException(message);
            case 409 -> new ConflictException(message);
            default -> new ServiceUnavailableException(message);
        };
    }
}
