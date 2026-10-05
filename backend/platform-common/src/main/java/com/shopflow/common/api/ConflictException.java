package com.shopflow.common.api;

import org.springframework.http.HttpStatus;

/** 409 - konflik state, mis. email sudah terdaftar atau stok tidak cukup. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
