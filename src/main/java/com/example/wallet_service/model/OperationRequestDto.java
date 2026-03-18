package com.example.wallet_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Запрос на операцию с кошельком
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Запрос на операцию с кошельком")
public class OperationRequestDto {

    @NotNull(message = "walletId is required")
    @Schema(description = "UUID кошелька", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID walletId;

    @NotNull(message = "operationType is required")
    @Schema(description = "Тип операции: DEPOSIT (пополнение) или WITHDRAW (снятие)",
            example = "DEPOSIT", allowableValues = {"DEPOSIT", "WITHDRAW"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private OperationType operationType;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    @Schema(description = "Сумма операции", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;
}
