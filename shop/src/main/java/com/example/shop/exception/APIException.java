package com.example.shop.exception;

public class APIException extends RuntimeException {
    private static final long serialVersionID=1;
    public APIException(String message) {
        super(message);
    }
}
