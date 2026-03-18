package com.example.wallet_service.integration;

import com.example.wallet_service.BaseIntegrationTest;
import com.example.wallet_service.domain.Wallet;
import com.example.wallet_service.model.BalanceResponseDto;
import com.example.wallet_service.model.OperationRequestDto;
import com.example.wallet_service.model.OperationType;
import com.example.wallet_service.model.exception.InsufficientFundsException;
import com.example.wallet_service.model.exception.OperationUnavailableException;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.service.WalletService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Transactional
@EnableRetry
public class WalletIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    private UUID walletId;
    private Wallet savedWallet;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();

        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(1000));
        savedWallet = walletRepository.save(wallet);
        walletId = savedWallet.getId();
    }

    @Test
    void processOperation_Deposit_Success() {
        // given
        OperationRequestDto request = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        // when
        BalanceResponseDto response = walletService.processOperation(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getWalletId()).isEqualTo(walletId);
        assertThat(response.getBalance()).isEqualByComparingTo("1500");

        // Проверяем в базе
        Wallet updatedWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(updatedWallet.getBalance()).isEqualByComparingTo("1500");
    }

    @Test
    void processOperation_Withdraw_Success() {
        // given
        OperationRequestDto request = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.WITHDRAW)
                .amount(BigDecimal.valueOf(300))
                .build();

        // when
        BalanceResponseDto response = walletService.processOperation(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getWalletId()).isEqualTo(walletId);
        assertThat(response.getBalance()).isEqualByComparingTo("700");

        Wallet updatedWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(updatedWallet.getBalance()).isEqualByComparingTo("700");
    }

    @Test
    void processOperation_Withdraw_InsufficientFunds_ThrowsException() {
        // given
        OperationRequestDto request = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.WITHDRAW)
                .amount(BigDecimal.valueOf(2000))
                .build();

        // when/then
        assertThrows(OperationUnavailableException.class, () -> walletService.processOperation(request));

        // Баланс не изменился
        Wallet unchangedWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(unchangedWallet.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void getBalance_Success() {
        // when
        BalanceResponseDto response = walletService.getBalance(walletId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getWalletId()).isEqualTo(walletId);
        assertThat(response.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void getBalance_WalletNotFound_ThrowsException() {
        // given
        UUID nonExistentId = UUID.randomUUID();

        // when/then
        assertThrows(EntityNotFoundException.class, () -> walletService.getBalance(nonExistentId));
    }

    @Test
    void processOperation_WithdrawExactBalance_Success() {
        // given
        OperationRequestDto request = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.WITHDRAW)
                .amount(BigDecimal.valueOf(1000))
                .build();

        // when
        BalanceResponseDto response = walletService.processOperation(request);

        // then
        assertThat(response.getBalance()).isEqualByComparingTo("0");

        Wallet updatedWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(updatedWallet.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void processOperation_DepositZeroAmount_Success() {
        // given
        OperationRequestDto request = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.DEPOSIT)
                .amount(BigDecimal.ZERO)
                .build();

        // when
        BalanceResponseDto response = walletService.processOperation(request);

        // then
        assertThat(response.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void processOperation_WithdrawZeroAmount_Success() {
        // given
        OperationRequestDto request = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.WITHDRAW)
                .amount(BigDecimal.ZERO)
                .build();

        // when
        BalanceResponseDto response = walletService.processOperation(request);

        // then
        assertThat(response.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void processOperation_MultipleOperations_Success() {
        // given
        OperationRequestDto deposit1 = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        OperationRequestDto withdraw1 = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.WITHDRAW)
                .amount(BigDecimal.valueOf(200))
                .build();

        OperationRequestDto deposit2 = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.DEPOSIT)
                .amount(BigDecimal.valueOf(300))
                .build();

        // when
        walletService.processOperation(deposit1); // 1000 + 500 = 1500
        walletService.processOperation(withdraw1); // 1500 - 200 = 1300
        walletService.processOperation(deposit2); // 1300 + 300 = 1600

        // then
        Wallet finalWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(finalWallet.getBalance()).isEqualByComparingTo("1600");
    }

    @Test
    void concurrentWithdrawals_ShouldMaintainConsistency() throws InterruptedException {
        // given
        int threadCount = 5;
        int operationsPerThread = 2;
        BigDecimal withdrawAmount = BigDecimal.valueOf(100);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        OperationRequestDto request = OperationRequestDto.builder()
                                .walletId(walletId)
                                .operationType(OperationType.WITHDRAW)
                                .amount(withdrawAmount)
                                .build();

                        walletService.processOperation(request);
                        successCount.incrementAndGet();
                    }
                } catch (InsufficientFundsException e) {
                    errorCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        Wallet finalWallet = walletRepository.findById(walletId).orElseThrow();
        System.out.println("Final balance: " + finalWallet.getBalance());
        System.out.println("Success: " + successCount.get());
        System.out.println("Errors: " + errorCount.get());

        // Баланс должен быть неотрицательным
        assertThat(finalWallet.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void concurrentDepositsAndWithdrawals_ShouldMaintainConsistency() throws InterruptedException {
        // given
        int threadCount = 8;
        int operationsPerThread = 5;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger depositCount = new AtomicInteger(0);
        AtomicInteger withdrawCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        OperationType type = Math.random() > 0.5 ? OperationType.DEPOSIT : OperationType.WITHDRAW;
                        BigDecimal amount = BigDecimal.valueOf(50 + (int)(Math.random() * 100));

                        OperationRequestDto request = OperationRequestDto.builder()
                                .walletId(walletId)
                                .operationType(type)
                                .amount(amount)
                                .build();

                        walletService.processOperation(request);

                        if (type == OperationType.DEPOSIT) {
                            depositCount.incrementAndGet();
                        } else {
                            withdrawCount.incrementAndGet();
                        }
                    }
                } catch (InsufficientFundsException e) {
                    // Это ожидаемо при конкурентном снятии
                    errorCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        Wallet finalWallet = walletRepository.findById(walletId).orElseThrow();
        System.out.println("Final balance: " + finalWallet.getBalance());
        System.out.println("Deposits: " + depositCount.get());
        System.out.println("Withdrawals: " + withdrawCount.get());
        System.out.println("Errors: " + errorCount.get());

        // Баланс должен быть консистентным (может быть любым, но неотрицательным)
        assertThat(finalWallet.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void processOperation_ShouldBeAtomic() {
        // given
        BigDecimal originalBalance = savedWallet.getBalance();

        // Пытаемся снять больше чем есть
        OperationRequestDto withdrawRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.WITHDRAW)
                .amount(originalBalance.add(BigDecimal.ONE))  // Больше чем баланс
                .build();

        // when/then
        assertThrows(OperationUnavailableException.class, () -> walletService.processOperation(withdrawRequest));

        // Проверяем, что баланс не изменился (транзакция откатилась)
        Wallet unchangedWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(unchangedWallet.getBalance()).isEqualByComparingTo(originalBalance);
    }


}
