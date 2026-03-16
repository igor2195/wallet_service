package com.example.wallet_task.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Данные запроса для операции с кошельком
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationRequestDto {
    /**
     * Идентификатор кошелька
     */
    @NotNull(message = "walletId is required")
    private UUID walletId;

    /**
     * Тип операции
     */
    @NotNull(message = "operationType is required")
    private OperationType operationType;

    /**
     * Сумма операции
     */
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
}
