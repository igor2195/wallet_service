package com.example.wallet_service.utils;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletServiceTestMocks {
    UUID TEST_WALLET_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    BigDecimal DEPOSIT_AMOUNT = new BigDecimal("500.00");
    BigDecimal WITHDRAW_AMOUNT = new BigDecimal("300.00");
    BigDecimal TEST_AMOUNT = new BigDecimal("100.00");
    BigDecimal AMOUNT = new BigDecimal("50.00");
    BigDecimal BALANCE = new BigDecimal("1000.00");
    BigDecimal NEW_BALANCE = new BigDecimal("1500.00");
    BigDecimal OWER_AMOUNT_BALANCE = new BigDecimal("1500.00");
    String WALLET_NOT_FOUND = "wallet with id: %s not found";
}
