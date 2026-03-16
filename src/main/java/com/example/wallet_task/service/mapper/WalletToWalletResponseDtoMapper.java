package com.example.wallet_task.service.mapper;

import com.example.wallet_task.domain.Wallet;
import com.example.wallet_task.model.BalanceResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletToWalletResponseDtoMapper {

    @Mapping(source = "id", target = "walletId")
    BalanceResponseDto toDto (Wallet wallet);
}
