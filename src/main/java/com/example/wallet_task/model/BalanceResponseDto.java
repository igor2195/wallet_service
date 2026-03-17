package com.example.wallet_task.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ответ с балансом кошелька
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Ответ с балансом кошелька")
public class BalanceResponseDto {
    @Schema(description = "UUID кошелька", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID walletId;

    @Schema(description = "Текущий баланс", example = "1500.00")
    private BigDecimal balance;
}
