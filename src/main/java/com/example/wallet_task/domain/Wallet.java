package com.example.wallet_task.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Модель данных кошелька
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "public", name = "wallet")
public class Wallet {
    /**
     * Идентификатор кошелька
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Баланс кошелька
     */
    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
