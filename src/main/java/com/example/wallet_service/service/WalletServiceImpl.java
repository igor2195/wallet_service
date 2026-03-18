package com.example.wallet_service.service;

import com.example.wallet_service.model.BalanceResponseDto;
import com.example.wallet_service.model.OperationRequestDto;
import com.example.wallet_service.model.exception.InsufficientFundsException;
import com.example.wallet_service.model.exception.UnsupportedOperationType;
import com.example.wallet_service.repository.WalletJdbcRepository;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.service.mapper.WalletToBalanceResponseDtoMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.example.wallet_service.utils.WalletConstants.*;
import static java.util.Objects.isNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;
    private final WalletJdbcRepository jdbcRepository;
    private final WalletToBalanceResponseDtoMapper mapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BalanceResponseDto processOperation(OperationRequestDto request) {
        if (isNull(request.getOperationType())) {
            throw new UnsupportedOperationType(UNSUPPORTED_OPERATION);
        }
        BigDecimal newBalance = processOperationInternal(request);

        return BalanceResponseDto.builder()
                .walletId(request.getWalletId())
                .balance(newBalance)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "walletBalances", key = "#walletId", unless = "#result == null")
    public BalanceResponseDto getBalance(UUID walletId) {
        return walletRepository.findById(walletId)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException(
                        WALLET_NOT_FOUND.formatted(walletId))
                );
    }


    private BigDecimal processOperationInternal(OperationRequestDto request) {
        return switch (request.getOperationType()) {
            case DEPOSIT -> jdbcRepository.processDeposit(request.getWalletId(), request.getAmount());
            case WITHDRAW -> jdbcRepository.processWithdraw(request.getWalletId(), request.getAmount())
                    .orElseThrow(() -> handleWithdrawFailure(request));
            default -> throw new UnsupportedOperationType(UNSUPPORTED_OPERATION);
        };
    }

    private RuntimeException handleWithdrawFailure(OperationRequestDto request) {
        // Проверяем существование кошелька отдельно, без retry
        Boolean exists = checkWalletExistsWithoutRetry(request.getWalletId());

        if (Boolean.FALSE.equals(exists)) {
            log.debug("Wallet not found during withdrawal: {}", request.getWalletId());
            return new EntityNotFoundException(WALLET_NOT_FOUND.formatted(request.getWalletId()));
        }

        log.debug("Insufficient funds for wallet {}: requested {}",
                request.getWalletId(), request.getAmount());
        return new InsufficientFundsException(INSUFFICIENT_FUNDS);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED) // Без транзакции для проверки
    public Boolean checkWalletExistsWithoutRetry(UUID walletId) {
        try {
            return walletRepository.existsById(walletId);
        } catch (RuntimeException e) {
            log.warn("Failed to check wallet existence: {}", walletId, e);
            return null;
        }
    }
}

