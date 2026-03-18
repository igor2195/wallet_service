package com.example.wallet_service.service;

import com.example.wallet_service.domain.Wallet;
import com.example.wallet_service.model.BalanceResponseDto;
import com.example.wallet_service.model.OperationRequestDto;
import com.example.wallet_service.model.exception.InsufficientFundsException;
import com.example.wallet_service.model.exception.UnsupportedOperationType;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.service.mapper.WalletToBalanceResponseDtoMapper;
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

import static com.example.wallet_service.model.OperationType.DEPOSIT;
import static com.example.wallet_service.model.OperationType.WITHDRAW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceImplTest {

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
    private BalanceResponseDto balanceResponse;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();

        wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(BigDecimal.valueOf(1000));

        depositRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        withdrawRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(WITHDRAW)
                .amount(BigDecimal.valueOf(300))
                .build();

        balanceResponse = BalanceResponseDto.builder()
                .walletId(walletId)
                .balance(BigDecimal.valueOf(1500))
                .build();
    }

    @Test
    void processOperation_Deposit_Success() {
        // given
        when(walletRepository.findById(walletId)).thenReturn(Optional.ofNullable(wallet));
        when(walletRepository.deposit(walletId, BigDecimal.valueOf(500)))
                .thenReturn(1);
        when(mapper.toDto(wallet)).thenReturn(balanceResponse);

        // when
        BalanceResponseDto result = walletService.processOperation(depositRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getWalletId()).isEqualTo(walletId);
        assertThat(result.getBalance()).isEqualByComparingTo("1500");

        verify(walletRepository).deposit(walletId, BigDecimal.valueOf(500));
        verify(walletRepository, never()).withdraw(any(), any());
        verify(mapper).toDto(wallet);
    }

    @Test
    void processOperation_Withdraw_Success() {
        // given
        when(walletRepository.findById(walletId)).thenReturn(Optional.ofNullable(wallet));
        when(walletRepository.withdraw(walletId, BigDecimal.valueOf(300)))
                .thenReturn(1);
        when(mapper.toDto(wallet)).thenReturn(balanceResponse);

        // when
        BalanceResponseDto result = walletService.processOperation(withdrawRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getWalletId()).isEqualTo(walletId);
        assertThat(result.getBalance()).isEqualByComparingTo("1500");

        verify(walletRepository).withdraw(walletId, BigDecimal.valueOf(300));
        verify(walletRepository, never()).deposit(any(), any());
    }


    @Test
    void processOperation_Deposit_RepositoryReturnsEmpty_ThrowsEntityNotFoundException() {
        // given
        when(walletRepository.existsById(walletId)).thenReturn(false);
        when(walletRepository.deposit(walletId, BigDecimal.valueOf(500)))
                .thenReturn(0);

        // when/then
        assertThrows(EntityNotFoundException.class, () -> walletService.processOperation(depositRequest));
    }

    @Test
    void processOperation_Withdraw_InsufficientFunds_ThrowsInsufficientFundsException() {
        // given
        when(walletRepository.existsById(walletId)).thenReturn(true);
        when(walletRepository.withdraw(walletId, BigDecimal.valueOf(300)))
                .thenReturn(0);

        // when/then
        assertThrows(InsufficientFundsException.class, () -> walletService.processOperation(withdrawRequest));
    }

    @Test
    void processOperation_UnsupportedOperationType_ThrowsUnsupportedOperationType() {
        // given
        OperationRequestDto invalidRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(null)  // null operation type
                .amount(BigDecimal.valueOf(500))
                .build();

        // when/then
        assertThrows(UnsupportedOperationType.class, () -> walletService.processOperation(invalidRequest));
    }

    @Test
    void getBalance_Success() {
        // given
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(mapper.toDto(wallet)).thenReturn(balanceResponse);

        // when
        BalanceResponseDto result = walletService.getBalance(walletId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getWalletId()).isEqualTo(walletId);
        assertThat(result.getBalance()).isEqualByComparingTo("1500");

        verify(walletRepository).findById(walletId);
    }

    @Test
    void getBalance_WalletNotFound_ThrowsEntityNotFoundException() {
        // given
        when(walletRepository.existsById(walletId)).thenReturn(false);

        // when/then
        assertThrows(EntityNotFoundException.class, () -> walletService.processOperation(depositRequest));
    }
}
