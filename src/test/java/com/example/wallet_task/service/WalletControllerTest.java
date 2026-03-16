package com.example.wallet_task.service;

import com.example.wallet_task.domain.Wallet;
import com.example.wallet_task.model.OperationRequestDto;
import com.example.wallet_task.model.OperationType;
import com.example.wallet_task.model.exception.InsufficientFundsException;
import com.example.wallet_task.repository.WalletRepository;
import com.example.wallet_task.service.mapper.WalletToBalanceResponseDtoMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletControllerTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletToBalanceResponseDtoMapper mapper;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID walletId;
    private Wallet wallet;
    private OperationRequestDto depositRequest;
    private OperationRequestDto withdrawRequest;

    @BeforeEach
    void setUp() {
        walletId = UUID.fromString("a07cd10c-fe74-4ee2-abe8-474b90db7130");
        wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.valueOf(1000));

        depositRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        withdrawRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.WITHDRAW)
                .amount(BigDecimal.valueOf(300))
                .build();
    }

    @Test
    void processOperation_Deposit_Success() {
        // arrange
        when(walletRepository.findByIdWithPessimisticLock(walletId))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenReturn(wallet);

        // act
        walletService.processOperation(depositRequest);

        // assert
        verify(walletRepository).save(argThat(savedWallet ->
                savedWallet.getBalance().equals(BigDecimal.valueOf(1500))
        ));
    }

    @Test
    void processOperation_Withdraw_Success() {
        // arrange
        when(walletRepository.findByIdWithPessimisticLock(walletId))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenReturn(wallet);

        // act
        walletService.processOperation(withdrawRequest);

        // assert
        verify(walletRepository).save(argThat(savedWallet ->
                savedWallet.getBalance().equals(BigDecimal.valueOf(700))
        ));
    }

    @Test
    void processOperation_Withdraw_InsufficientFunds_ThrowsException() {
        // arrange
        withdrawRequest.setAmount(BigDecimal.valueOf(2000));
        when(walletRepository.findByIdWithPessimisticLock(walletId))
                .thenReturn(Optional.of(wallet));

        // act
        assertThrows(InsufficientFundsException.class, () -> walletService.processOperation(withdrawRequest));

        // assert
        verify(walletRepository, never()).save(any());
    }

    @Test
    void processOperation_WalletNotFound_ThrowsException() {
        // arrange
        when(walletRepository.findByIdWithPessimisticLock(walletId))
                .thenReturn(Optional.empty());

        // assert
        assertThrows(EntityNotFoundException.class, () -> walletService.processOperation(depositRequest));
    }

    @Test
    void getBalance_Success() {
        // arrange
        when(walletRepository.findByIdWithOptimisticLock(walletId))
                .thenReturn(Optional.of(wallet));

        // act
        walletService.getBalance(walletId);

        // assert
        verify(mapper).toDto(wallet);
    }

    @Test
    void getBalance_WalletNotFound_ThrowsException() {
        // arrange
        when(walletRepository.findByIdWithOptimisticLock(walletId))
                .thenReturn(Optional.empty());

        // assert
        assertThrows(EntityNotFoundException.class, () -> walletService.getBalance(walletId));
    }

}
