package com.example.wallet_service.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WalletConstants {
    public static final String WALLET_NOT_FOUND = "wallet with id: %s not found";
    public static final String INSUFFICIENT_FUNDS = "insufficient funds in the balance";
    public static final String OPERATION_UNAVAILABLE = "operation unavailable, please try again later";
    public static final String UNSUPPORTED_OPERATION = "Unsupported Operation Type";
}
