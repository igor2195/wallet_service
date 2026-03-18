package com.example.wallet_service.repository;

import com.example.wallet_service.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Репозиторий для радоты с {@link Wallet}
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    // Возвращает количество обновленных строк (0 или 1)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE Wallet
            SET balance = balance + :amount
            WHERE id = :walletId
            """, nativeQuery = true)
    int deposit(@Param("walletId") UUID walletId,
                @Param("amount") BigDecimal amount);

    // Для withdraw с проверкой баланса
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE Wallet
            SET balance = balance - :amount
            WHERE id = :walletId AND balance >= :amount
            """, nativeQuery = true)
    int withdraw(@Param("walletId") UUID walletId,
                 @Param("amount") BigDecimal amount);
}
