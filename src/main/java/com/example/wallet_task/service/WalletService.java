package com.example.wallet_task.service;

import com.example.wallet_task.domain.Wallet;
import com.example.wallet_task.model.OperationRequestDto;
import com.example.wallet_task.model.WalletResponseDto;

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
    WalletResponseDto processOperation(OperationRequestDto request);

    /**
     * Получить баланс кошелька по его Id
     *
     * @param walletId Идентификатор кошелька
     * @return Баланс кошелька по Id
     */
    WalletResponseDto getBalance(UUID walletId);
}
