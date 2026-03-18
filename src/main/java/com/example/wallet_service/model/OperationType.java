package com.example.wallet_service.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Тип операции с кошельком
 */
@Schema(description = "Тип операции с кошельком")
public enum OperationType {

    @Schema(description = "Пополнение счета")
    DEPOSIT,

    @Schema(description = "Снятие со счета")
    WITHDRAW
}
