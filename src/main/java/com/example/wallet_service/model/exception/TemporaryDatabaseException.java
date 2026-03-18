package com.example.wallet_service.model.exception;

public class TemporaryDatabaseException extends RuntimeException {
    public TemporaryDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
