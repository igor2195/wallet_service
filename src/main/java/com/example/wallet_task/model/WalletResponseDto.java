package com.example.wallet_task.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ответ с данными о кошельке
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletResponseDto {
    /**
     * Идинтификатор кошелька
     */
    private UUID walletId;
    /**
     * Баланс кошелька
     */
    private BigDecimal balance;
}
