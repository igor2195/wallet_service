package com.example.wallet_service.model.exception;

/**
 * Неподдерживаемый тип операции
 */
public class UnsupportedOperationType extends RuntimeException {

    public UnsupportedOperationType(String message) {
        super(message);
    }
}
