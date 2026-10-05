package com.shopflow.common.api;

import org.springframework.http.HttpStatus;

/** 403 - terautentikasi tapi tidak punya hak. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
