package com.example.wallet_service.repository;

import com.example.wallet_service.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для радоты с {@link Wallet}
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
}
