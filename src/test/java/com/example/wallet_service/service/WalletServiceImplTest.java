package com.example.wallet_service.service;

import com.example.wallet_service.domain.Wallet;
import com.example.wallet_service.model.BalanceResponseDto;
import com.example.wallet_service.model.OperationRequestDto;
import com.example.wallet_service.model.exception.InsufficientFundsException;
import com.example.wallet_service.model.exception.TemporaryDatabaseException;
import com.example.wallet_service.model.exception.UnsupportedOperationType;
import com.example.wallet_service.repository.WalletJdbcRepository;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.service.mapper.WalletToBalanceResponseDtoMapper;
import com.example.wallet_service.utils.WalletServiceTestMocks;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.util.Optional;

import static com.example.wallet_service.model.OperationType.DEPOSIT;
import static com.example.wallet_service.model.OperationType.WITHDRAW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceImplTest implements WalletServiceTestMocks {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletJdbcRepository jdbcRepository;

    @Mock
    private WalletToBalanceResponseDtoMapper mapper;

    @InjectMocks
    private WalletServiceImpl walletService;

    private OperationRequestDto depositRequest;
    private OperationRequestDto withdrawRequest;
    private Wallet wallet;
    private BalanceResponseDto expectedResponse;

    @BeforeEach
    void setUp() {
        depositRequest = OperationRequestDto.builder()
                .walletId(TEST_WALLET_ID)
                .amount(TEST_AMOUNT)
                .operationType(DEPOSIT)
                .build();

        withdrawRequest = OperationRequestDto.builder()
                .walletId(TEST_WALLET_ID)
                .amount(TEST_AMOUNT)
                .operationType(WITHDRAW)
                .build();

        wallet = Wallet.builder()
                .id(TEST_WALLET_ID)
                .balance(NEW_BALANCE)
                .build();

        expectedResponse = BalanceResponseDto.builder()
                .walletId(TEST_WALLET_ID)
                .balance(NEW_BALANCE)
                .build();
    }

    @Test
    void processOperation_Deposit_Success_ShouldReturnNewBalance() {
        // Given
        when(jdbcRepository.processDeposit(TEST_WALLET_ID, TEST_AMOUNT))
                .thenReturn(NEW_BALANCE);

        // When
        BalanceResponseDto result = walletService.processOperation(depositRequest);

        // Then
        assertThat(result.getWalletId()).isEqualTo(TEST_WALLET_ID);
        assertThat(result.getBalance()).isEqualTo(NEW_BALANCE);
        verify(jdbcRepository).processDeposit(TEST_WALLET_ID, TEST_AMOUNT);
        verify(walletRepository, never()).existsById(any());
    }

    @Test
    void processOperation_Deposit_WalletNotFound_ShouldThrowEntityNotFoundException() {
        // Given
        when(jdbcRepository.processDeposit(TEST_WALLET_ID, TEST_AMOUNT))
                .thenThrow(new EntityNotFoundException(
                        String.format(WALLET_NOT_FOUND, TEST_WALLET_ID)));

        // When/Then
        assertThrows(EntityNotFoundException.class, () -> walletService.processOperation(depositRequest));
    }

    @Test
    void processOperation_Withdraw_Success_ShouldReturnNewBalance() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("50.00");
        BigDecimal expectedWithdrawBalance = BALANCE.subtract(withdrawAmount);

        OperationRequestDto withdrawRequest = OperationRequestDto.builder()
                .walletId(TEST_WALLET_ID)
                .amount(withdrawAmount)
                .operationType(WITHDRAW)
                .build();

        when(jdbcRepository.processWithdraw(TEST_WALLET_ID, withdrawAmount))
                .thenReturn(Optional.of(expectedWithdrawBalance));

        // When
        BalanceResponseDto result = walletService.processOperation(withdrawRequest);

        // Then
        assertThat(result.getWalletId()).isEqualTo(TEST_WALLET_ID);
        assertThat(result.getBalance()).isEqualTo(expectedWithdrawBalance);
    }

    @Test
    void processOperation_Withdraw_InsufficientFunds_ShouldThrowInsufficientFundsException() {
        // Given
        when(jdbcRepository.processWithdraw(TEST_WALLET_ID, TEST_AMOUNT))
                .thenReturn(Optional.empty());

        when(walletRepository.existsById(TEST_WALLET_ID))
                .thenReturn(true);

        // When/Then
        assertThrows(InsufficientFundsException.class, () -> walletService.processOperation(withdrawRequest));

        verify(walletRepository).existsById(TEST_WALLET_ID);
    }

    @Test
    void processOperation_Withdraw_WalletNotFound_ShouldThrowEntityNotFoundException() {
        // Given
        when(jdbcRepository.processWithdraw(TEST_WALLET_ID, TEST_AMOUNT))
                .thenReturn(Optional.empty());

        when(walletRepository.existsById(TEST_WALLET_ID))
                .thenReturn(false);

        // When/Then
        assertThrows(EntityNotFoundException.class, () -> walletService.processOperation(withdrawRequest));
    }

    @Test
    void processOperation_Withdraw_TemporaryDatabaseError_ShouldPropagateException() {
        // Given
        when(jdbcRepository.processWithdraw(TEST_WALLET_ID, TEST_AMOUNT))
                .thenThrow(new TemporaryDatabaseException("Temporary database error",
                        new DataAccessException("") {
                        }));

        // When/Then
        assertThrows(TemporaryDatabaseException.class, () -> walletService.processOperation(withdrawRequest));
    }

    @Test
    void processOperation_UnsupportedType_ShouldThrowUnsupportedOperationType() {
        // Given
        OperationRequestDto invalidRequest = OperationRequestDto.builder()
                .walletId(TEST_WALLET_ID)
                .amount(TEST_AMOUNT)
                .operationType(null)
                .build();

        // When/Then
        assertThrows(UnsupportedOperationType.class, () -> walletService.processOperation(invalidRequest));
    }

    @Test
    void getBalance_WalletExists_ShouldReturnBalance() {
        // Given
        when(walletRepository.findById(TEST_WALLET_ID))
                .thenReturn(Optional.of(wallet));

        when(mapper.toDto(wallet)).thenReturn(expectedResponse);

        // When
        BalanceResponseDto result = walletService.getBalance(TEST_WALLET_ID);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(walletRepository).findById(TEST_WALLET_ID);
        verify(mapper).toDto(wallet);
    }

    @Test
    void getBalance_WalletNotFound_ShouldThrowEntityNotFoundException() {
        // Given
        when(walletRepository.findById(TEST_WALLET_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(EntityNotFoundException.class, () -> walletService.getBalance(TEST_WALLET_ID));
    }

    @Test
    void checkWalletExistsWithoutRetry_Success_ShouldReturnTrue() {
        // Given
        when(walletRepository.existsById(TEST_WALLET_ID))
                .thenReturn(true);

        // When
        Boolean result = walletService.checkWalletExistsWithoutRetry(TEST_WALLET_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void checkWalletExistsWithoutRetry_Success_ShouldReturnFalse() {
        // Given
        when(walletRepository.existsById(TEST_WALLET_ID))
                .thenReturn(false);

        // When
        Boolean result = walletService.checkWalletExistsWithoutRetry(TEST_WALLET_ID);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void checkWalletExistsWithoutRetry_Exception_ShouldReturnNull() {
        // Given
        when(walletRepository.existsById(TEST_WALLET_ID))
                .thenThrow(new RuntimeException("DB error"));

        // When
        Boolean result = walletService.checkWalletExistsWithoutRetry(TEST_WALLET_ID);

        // Then
        assertThat(result).isNull();
    }
}
