package com.shopflow.common.api;

import org.springframework.http.HttpStatus;

/** 404 - entitas yang diminta tidak ada. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public static ResourceNotFoundException of(String entity, Object id) {
        return new ResourceNotFoundException(entity + " dengan id '" + id + "' tidak ditemukan");
    }

    public static ResourceNotFoundException by(String entity, String field, Object value) {
        return new ResourceNotFoundException(entity + " dengan " + field + " '" + value + "' tidak ditemukan");
    }
}
