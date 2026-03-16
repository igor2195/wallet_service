package com.example.wallet_task.service;

import com.example.wallet_task.domain.Wallet;
import com.example.wallet_task.model.OperationRequestDto;
import com.example.wallet_task.model.WalletResponseDto;
import com.example.wallet_task.model.exception.InsufficientFundsException;
import com.example.wallet_task.repository.WalletRepository;
import com.example.wallet_task.service.mapper.WalletToWalletResponseDtoMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final String WALLET_NOT_FOUND = "wallet with id: %s not found";
    private static final String INSUFFICIENT_FUNDS = "insufficient funds in the balance";

    private final WalletRepository walletRepository;
    private final WalletToWalletResponseDtoMapper mapper;

    @Override
    @Transactional
    public WalletResponseDto processOperation(OperationRequestDto request) {
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(
                        () -> new EntityNotFoundException(WALLET_NOT_FOUND.formatted(request.getWalletId()))
                );

        switch (request.getOperationType()) {
            case DEPOSIT:
                wallet.setBalance(wallet.getBalance().add(request.getAmount()));
                break;
            case WITHDRAW:
                if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                    throw new InsufficientFundsException(INSUFFICIENT_FUNDS);
                }
                wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
                break;
        }

        return mapper.toDto(wallet);
    }

    @Override
    public WalletResponseDto getBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(
                        () -> new EntityNotFoundException(WALLET_NOT_FOUND.formatted(walletId))
                );
        return mapper.toDto(wallet);
    }
}

