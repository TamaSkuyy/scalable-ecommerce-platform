package com.shopflow.common.api;

import org.springframework.http.HttpStatus;

/** 401 - kredensial tidak ada / salah. Di Gateway ini juga dipakai untuk token tidak valid. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
