package com.example.auction_management.exception;

public class ProductCreationException extends RuntimeException {
    public ProductCreationException(String message) {
        super(message);
    }

    public ProductCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
