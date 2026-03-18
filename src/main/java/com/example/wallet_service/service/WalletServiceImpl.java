package com.example.wallet_service.service;

import com.example.wallet_service.annotation.WalletRetryable;
import com.example.wallet_service.domain.Wallet;
import com.example.wallet_service.model.BalanceResponseDto;
import com.example.wallet_service.model.OperationRequestDto;
import com.example.wallet_service.model.exception.InsufficientFundsException;
import com.example.wallet_service.model.exception.OperationUnavailableException;
import com.example.wallet_service.model.exception.UnsupportedOperationType;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.service.mapper.WalletToBalanceResponseDtoMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.*;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static java.util.Objects.isNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final String WALLET_NOT_FOUND = "wallet with id: %s not found";
    private static final String INSUFFICIENT_FUNDS = "insufficient funds in the balance";
    private static final String DEPOSIT_SUCCESS = "deposit successful. New balance: {}";
    private static final String WITHDRAW_SUCCESS = "withdrawal successful. New balance: {}";
    private static final String OPERATION_UNAVAILABLE = "operation unavailable, please try again later";
    private static final String PROCESS_FAILED = "failed to process operation after retries for wallet {}: {}";
    private static final String UNSUPPORTED_OPERATION = "Unsupported Operation Type";
    private static final String ILLEGAL_EXCEPTION = "Deposit failed";

    private final WalletRepository walletRepository;
    private final WalletToBalanceResponseDtoMapper mapper;

    @Override
    @Transactional
    @WalletRetryable
    public BalanceResponseDto processOperation(OperationRequestDto request) {
        if (isNull(request.getOperationType())) {
            throw new UnsupportedOperationType(UNSUPPORTED_OPERATION);
        }
        switch (request.getOperationType()) {
            case DEPOSIT:
                int depositCount = walletRepository.deposit(
                        request.getWalletId(),
                        request.getAmount()
                );
                if (depositCount == 0) {
                    if (!walletRepository.existsById(request.getWalletId())) {
                        throw new EntityNotFoundException(WALLET_NOT_FOUND.formatted(request.getWalletId()));
                    }
                    throw new EntityNotFoundException(WALLET_NOT_FOUND.formatted(request.getWalletId()));
                }
                break;
            case WITHDRAW:
                int withdrawCount = walletRepository.withdraw(
                        request.getWalletId(),
                        request.getAmount()
                );
                if (withdrawCount == 0) {
                    if (!walletRepository.existsById(request.getWalletId())) {
                        throw new EntityNotFoundException(WALLET_NOT_FOUND.formatted(request.getWalletId()));
                    }
                    throw new InsufficientFundsException(INSUFFICIENT_FUNDS);
                }
                break;
            default:
                throw new UnsupportedOperationType(UNSUPPORTED_OPERATION);
        }

        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new EntityNotFoundException(WALLET_NOT_FOUND.formatted(request.getWalletId())));
        return mapper.toDto(wallet);
    }

    @Override
    public BalanceResponseDto getBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(
                        () -> new EntityNotFoundException(WALLET_NOT_FOUND.formatted(walletId))
                );
        return mapper.toDto(wallet);
    }


    @Recover
    public BalanceResponseDto recoverProcessOperation(PessimisticLockingFailureException e,
                                                      OperationRequestDto request) {
        log.error("Failed to process operation for wallet {} after retries: lock error",
                request.getWalletId());
        throw new OperationUnavailableException(OPERATION_UNAVAILABLE);
    }

    @Recover
    public BalanceResponseDto recoverProcessOperation(CannotAcquireLockException e,
                                                      OperationRequestDto request) {
        log.error("Failed to process operation for wallet {} after retries: cannot acquire lock",
                request.getWalletId());
        throw new OperationUnavailableException(OPERATION_UNAVAILABLE);
    }

    @Recover
    public BalanceResponseDto recoverProcessOperation(DataIntegrityViolationException e,
                                                      OperationRequestDto request) {
        log.error("Failed to process operation for wallet {} after retries: data integrity",
                request.getWalletId());
        throw new OperationUnavailableException(OPERATION_UNAVAILABLE);
    }

    @Recover
    public BalanceResponseDto recoverProcessOperation(Exception e,
                                                      OperationRequestDto request) {
        log.error("Failed to process operation for wallet {} after retries: {}",
                request.getWalletId(), e.getMessage());
        throw new OperationUnavailableException(OPERATION_UNAVAILABLE);
    }
}

