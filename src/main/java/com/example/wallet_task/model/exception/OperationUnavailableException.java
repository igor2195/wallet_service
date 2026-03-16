package com.example.wallet_task.model.exception;

/**
 * Операция не доступна
 */
public class OperationUnavailableException extends RuntimeException{

    public OperationUnavailableException(String message) {
        super(message);
    }
}
