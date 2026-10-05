package com.shopflow.common.api;

import org.springframework.http.HttpStatus;

/** 400 - request secara semantik tidak valid (bukan sekadar format field). */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
