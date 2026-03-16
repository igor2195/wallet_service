package com.example.wallet_task.service.mapper;

import com.example.wallet_task.domain.Wallet;
import com.example.wallet_task.model.BalanceResponseDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WalletToBalanceResponseDtoMapperTest {

    private final WalletToBalanceResponseDtoMapper mapper = new WalletToBalanceResponseDtoMapperImpl();

    @Test
    void WalletToWalletResponseDto() {
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .balance(BigDecimal.valueOf(100.00))
                .build();

        BalanceResponseDto result = mapper.toDto(wallet);

        assertEquals(wallet.getId(), result.getWalletId());
        assertEquals(wallet.getBalance(), result.getBalance());
    }
}
