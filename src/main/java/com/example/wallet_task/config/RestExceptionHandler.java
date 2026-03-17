package com.example.wallet_task.config;

import com.example.wallet_task.model.exception.BaseException;
import com.example.wallet_task.model.exception.InsufficientFundsException;
import com.example.wallet_task.model.exception.OperationUnavailableException;
import com.example.wallet_task.model.exception.UnsupportedOperationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Обработчик ошибок случившихся во время выплнения запроса.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException e) {
        return buildResponseEntity(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Object> handleInsufficientFundsException(InsufficientFundsException e) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(OperationUnavailableException.class)
    public ResponseEntity<Object> handleOperationUnavailableException(OperationUnavailableException e) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(UnsupportedOperationType.class)
    public ResponseEntity<Object> handleUnsupportedOperationType(UnsupportedOperationType e) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException e) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ResponseEntity<Object> handleHttpMessageNotReadableExceptionException(HttpMessageNotReadableException e) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, e.getMessage());

    }

    protected ResponseEntity<Object> buildResponseEntity(HttpStatus status, String message) {
        BaseException be = BaseException.builder()
                .status(status)
                .error(message)
                .timestamp(Instant.now())
                .build();
        return new ResponseEntity<>(be, status);
    }


}
