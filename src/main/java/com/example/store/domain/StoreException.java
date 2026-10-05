package com.example.store.domain;

import org.springframework.http.HttpStatus;

public class StoreException extends RuntimeException {
    private final HttpStatus status;

    private StoreException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() { return status; }

    public static StoreException notFound(String message) { return new StoreException(HttpStatus.NOT_FOUND, message); }
    public static StoreException conflict(String message) { return new StoreException(HttpStatus.CONFLICT, message); }
    public static StoreException badRequest(String message) { return new StoreException(HttpStatus.BAD_REQUEST, message); }
}
