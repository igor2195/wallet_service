package com.example.wallet_service.model.exception;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Базовый клас для исключений сервиса")
public class BaseException {

    @Schema(description = "Статус ответа в текстовом формате")
    private final HttpStatus status;

    @Schema(description = "Текст ошибки")
    private final String error;

    @Schema(description = "Время ошибки")
    private final Instant timestamp;
}
