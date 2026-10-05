package com.shopflow.common.api;

import java.time.Instant;
import java.util.List;

/**
 * Bentuk error yang SERAGAM untuk seluruh microservices.
 *
 * <p>Contoh JSON:
 * <pre>
 * {
 *   "timestamp": "2026-01-01T10:00:00Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Produk tidak ditemukan",
 *   "path": "/api/v1/products/unknown",
 *   "traceId": "5f2c...",
 *   "violations": []
 * }
 * </pre>
 *
 * <p>Klien (Next.js) cukup menulis SATU parser error untuk semua service.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        List<FieldViolation> violations) {

    /** Detail validasi per-field (dari Bean Validation). */
    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(int status, String error, String message, String path, String traceId) {
        return of(status, error, message, path, traceId, List.of());
    }

    public static ApiError of(int status,
                              String error,
                              String message,
                              String path,
                              String traceId,
                              List<FieldViolation> violations) {
        return new ApiError(
                Instant.now(),
                status,
                error,
                message,
                path,
                traceId,
                violations == null ? List.of() : List.copyOf(violations));
    }
}
