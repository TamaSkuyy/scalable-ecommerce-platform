package com.shopflow.common.api;

import org.springframework.http.HttpStatus;

/**
 * 503 - dependency hilang / circuit terbuka.
 *
 * <p>Bedakan dengan 500: 500 berarti "kode kita salah", 503 berarti "layanan lain
 * sedang bermasalah". Frontend boleh menyarankan user mencoba lagi untuk 503.
 */
public class ServiceUnavailableException extends ApiException {

    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
