package com.shopflow.common.web;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Correlation ID = "nomor resi" sebuah request.
 *
 * <p>Satu request dari browser akan melewati Gateway -> Service A -> Service B.
 * Dengan menyebarkan header {@code X-Correlation-Id} dan menaruhnya di MDC (Mapped
 * Diagnostic Context) SLF4J, semua log dari request yang sama bisa dicari dengan satu
 * kata kunci di Kibana/Loki. Ini pondasi observability (Tahap 4).
 */
public final class Correlation {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "traceId";

    private Correlation() {
    }

    /** Trace id yang sedang aktif, atau string kosong bila tidak ada konteks. */
    public static String currentTraceId() {
        String value = MDC.get(MDC_KEY);
        return value == null ? "" : value;
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
