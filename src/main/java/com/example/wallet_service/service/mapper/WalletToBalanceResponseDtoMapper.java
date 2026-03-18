package com.example.wallet_service.service.mapper;

import com.example.wallet_service.domain.Wallet;
import com.example.wallet_service.model.BalanceResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletToBalanceResponseDtoMapper {

    @Mapping(source = "id", target = "walletId")
    BalanceResponseDto toDto(Wallet wallet);
}
