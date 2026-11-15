package com.saas.platform.subscription.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) { super(message); }
}
