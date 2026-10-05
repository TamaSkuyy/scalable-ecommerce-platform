package com.shopflow.common.api;

import org.springframework.http.HttpStatus;

/**
 * Basis semua error "yang kita sengaja lempar" (expected business error).
 *
 * <p>Bedakan dari {@link RuntimeException} biasa: exception yang tidak kita kenali
 * akan dipetakan menjadi HTTP 500 dan pesannya TIDAK dibocorkan ke klien.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }

    protected ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
