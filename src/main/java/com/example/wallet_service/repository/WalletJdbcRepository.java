package com.example.wallet_service.repository;

import com.example.wallet_service.annotation.WalletRetryable;
import com.example.wallet_service.model.exception.OperationUnavailableException;
import com.example.wallet_service.model.exception.TemporaryDatabaseException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.example.wallet_service.utils.WalletConstants.OPERATION_UNAVAILABLE;
import static com.example.wallet_service.utils.WalletConstants.WALLET_NOT_FOUND;


@Repository
@Slf4j
@RequiredArgsConstructor
@WalletRetryable
public class WalletJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public BigDecimal processDeposit(UUID walletId, BigDecimal amount) {
        try {
            return jdbcTemplate.queryForObject(
                    "UPDATE Wallet SET balance = balance + ? WHERE id = ? RETURNING balance",
                    BigDecimal.class,
                    amount, walletId
            );
        } catch (EmptyResultDataAccessException e) {
            log.debug("Wallet not found for deposit: {}", walletId);
            throw new EntityNotFoundException(WALLET_NOT_FOUND.formatted(walletId));
        }
    }

    public Optional<BigDecimal> processWithdraw(UUID walletId, BigDecimal amount) {
        try {
            BigDecimal balance = jdbcTemplate.queryForObject(
                    "UPDATE Wallet SET balance = balance - ? WHERE id = ? AND balance >= ? RETURNING balance",
                    BigDecimal.class,
                    amount, walletId, amount
            );
            return Optional.ofNullable(balance);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // Единый метод восстановления для deposit
    @Recover
    public BigDecimal recoverDeposit(Exception e, UUID walletId, BigDecimal amount) {
        log.error("Failed to process deposit for wallet {} after all retries", walletId, e);

        if (e instanceof PessimisticLockingFailureException ||
                e instanceof CannotAcquireLockException) {
            throw new OperationUnavailableException(OPERATION_UNAVAILABLE);
        }

        throw new TemporaryDatabaseException("Temporary database error after retries", e);
    }

    // Единый метод восстановления для withdraw
    @Recover
    public Optional<BigDecimal> recoverWithdraw(Exception e, UUID walletId, BigDecimal amount) {
        log.error("Failed to process withdraw for wallet {} after all retries", walletId, e);

        if (e instanceof PessimisticLockingFailureException ||
                e instanceof CannotAcquireLockException) {
            throw new OperationUnavailableException("Operation temporarily unavailable due to lock");
        }

        throw new TemporaryDatabaseException("Temporary database error after retries", e);
    }
}
