package com.example.auction_management.exception;

public class AuctionNotFoundException extends RuntimeException {
    public AuctionNotFoundException(String message) {
        super(message);
    }
}
