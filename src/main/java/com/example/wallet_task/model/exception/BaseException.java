package com.example.wallet_task.model.exception;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Базовый клас для исключений сервиса
 */
@Data
@Builder
@RequiredArgsConstructor
public class BaseException {

    /**
     * HTTP статус ответа
     */
    private final HttpStatus status;

    /**
     * Текст ошибки
     */
    private final String error;

    /**
     * Время ошибки
     */
    private final Instant timestamp;
}
