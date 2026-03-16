package com.example.wallet_task.service;

import com.example.wallet_task.domain.Wallet;
import com.example.wallet_task.model.BalanceResponseDto;
import com.example.wallet_task.model.OperationRequestDto;
import com.example.wallet_task.model.exception.InsufficientFundsException;
import com.example.wallet_task.model.exception.OperationUnavailableException;
import com.example.wallet_task.repository.WalletRepository;
import com.example.wallet_task.service.mapper.WalletToWalletResponseDtoMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

    private final WalletRepository walletRepository;
    private final WalletToWalletResponseDtoMapper mapper;

    @Override
    @Transactional
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    ObjectOptimisticLockingFailureException.class,
                    DataIntegrityViolationException.class,
                    org.hibernate.exception.LockTimeoutException.class
            },
            maxAttemptsExpression = "${app.wallet.retry.max-attempts:5}",
            backoff = @Backoff(
                    delayExpression = "${app.wallet.retry.delay:100}",
                    multiplierExpression = "${app.wallet.retry.multiplier:2}",
                    maxDelayExpression = "${app.wallet.retry.max-delay:5000}"
            )
    )
    public BalanceResponseDto processOperation(OperationRequestDto request) {
        Wallet wallet = walletRepository.findByIdWithPessimisticLock(request.getWalletId())
                .orElseThrow(
                        () -> new EntityNotFoundException(WALLET_NOT_FOUND.formatted(request.getWalletId()))
                );

        switch (request.getOperationType()) {
            case DEPOSIT:
                wallet.setBalance(wallet.getBalance().add(request.getAmount()));
                log.debug(DEPOSIT_SUCCESS, wallet.getBalance());
                break;
            case WITHDRAW:
                if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                    throw new InsufficientFundsException(INSUFFICIENT_FUNDS);
                }
                wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
                log.debug(WITHDRAW_SUCCESS, wallet.getBalance());
                break;
        }
        return mapper.toDto(walletRepository.save(wallet));
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponseDto getBalance(UUID walletId) {
        Wallet wallet = walletRepository.findByIdWithOptimisticLock(walletId)
                .orElseThrow(
                        () -> new EntityNotFoundException(WALLET_NOT_FOUND.formatted(walletId))
                );
        return mapper.toDto(wallet);
    }

    @Recover
    public void recoverProcessOperation(CannotAcquireLockException e, OperationRequestDto request) {
        log.error(PROCESS_FAILED, request.getWalletId(), e.getMessage());
        throw new OperationUnavailableException(OPERATION_UNAVAILABLE);
    }
}

