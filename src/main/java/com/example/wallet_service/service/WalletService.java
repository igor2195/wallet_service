package com.example.wallet_service.service;

import com.example.wallet_service.domain.Wallet;
import com.example.wallet_service.model.BalanceResponseDto;
import com.example.wallet_service.model.OperationRequestDto;

import java.util.UUID;

/**
 * Сервис для работы с {@link Wallet}
 */
public interface WalletService {
    /**
     * Операция с кошельком
     *
     * @param request Данные запроса для операции с кошельком
     * @return Обновленные данные баланса
     */
    BalanceResponseDto processOperation(OperationRequestDto request);

    /**
     * Получить баланс кошелька по его Id
     *
     * @param walletId Идентификатор кошелька
     * @return Баланс кошелька по Id
     */
    BalanceResponseDto getBalance(UUID walletId);
}
