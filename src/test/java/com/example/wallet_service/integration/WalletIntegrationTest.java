package com.example.wallet_service.integration;

import com.example.wallet_service.BaseIntegrationTest;
import com.example.wallet_service.domain.Wallet;
import com.example.wallet_service.model.BalanceResponseDto;
import com.example.wallet_service.model.OperationRequestDto;
import com.example.wallet_service.model.OperationType;
import com.example.wallet_service.model.exception.InsufficientFundsException;
import com.example.wallet_service.model.exception.UnsupportedOperationType;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.service.WalletService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.wallet_service.model.OperationType.DEPOSIT;
import static com.example.wallet_service.model.OperationType.WITHDRAW;
import static com.example.wallet_service.utils.WalletServiceTestMocks.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class WalletIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CacheManager cacheManager;

    private UUID existingWalletId;
    private UUID nonExistingWalletId;
    private Wallet savedWallet;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();

        savedWallet = Wallet.builder()
                .balance(BALANCE)
                .build();
        existingWalletId = walletRepository.save(savedWallet).getId();

        nonExistingWalletId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Тесты операций депозита")
    class DepositOperations {

        @Test
        @DisplayName("Депозит должен увеличивать баланс")
        void processDeposit_ShouldIncreaseBalance() {
            // Given
            OperationRequestDto request = OperationRequestDto.builder()
                    .walletId(existingWalletId)
                    .amount(DEPOSIT_AMOUNT)
                    .operationType(DEPOSIT)
                    .build();

            // When
            BalanceResponseDto response = walletService.processOperation(request);

            // Then
            assertAll(
                    () -> assertThat(response.getWalletId()).isEqualTo(existingWalletId),
                    () -> assertThat(response.getBalance()).isEqualByComparingTo(NEW_BALANCE)
            );

            Wallet updatedWallet = walletRepository.findById(existingWalletId).orElseThrow();
            assertThat(updatedWallet.getBalance()).isEqualByComparingTo(NEW_BALANCE);
        }

        @Test
        @DisplayName("Депозит с нулевой суммой не должен менять баланс")
        void processDeposit_WithZeroAmount_ShouldNotChangeBalance() {
            // Given
            OperationRequestDto request = OperationRequestDto.builder()
                    .walletId(existingWalletId)
                    .amount(BigDecimal.ZERO)
                    .operationType(DEPOSIT)
                    .build();

            // When
            BalanceResponseDto response = walletService.processOperation(request);

            // Then
            assertThat(response.getBalance()).isEqualByComparingTo(BALANCE);
        }
    }

    @Nested
    @DisplayName("Тесты операций снятия")
    class WithdrawOperations {

        @Test
        @DisplayName("Снятие при достаточном балансе должно уменьшать баланс")
        void processWithdraw_WithSufficientFunds_ShouldDecreaseBalance() {
            // Given
            OperationRequestDto request = OperationRequestDto.builder()
                    .walletId(existingWalletId)
                    .amount(WITHDRAW_AMOUNT)
                    .operationType(WITHDRAW)
                    .build();

            // When
            BalanceResponseDto response = walletService.processOperation(request);

            // Then
            assertAll(
                    () -> assertThat(response.getWalletId()).isEqualTo(existingWalletId),
                    () -> assertThat(response.getBalance()).isEqualByComparingTo(BALANCE.subtract(WITHDRAW_AMOUNT))
            );

            Wallet updatedWallet = walletRepository.findById(existingWalletId).orElseThrow();
            assertThat(updatedWallet.getBalance()).isEqualByComparingTo(BALANCE.subtract(WITHDRAW_AMOUNT));
        }

        @Test
        @DisplayName("Снятие всей суммы должно обнулять баланс")
        void processWithdraw_WithExactBalance_ShouldSucceed() {
            // Given
            OperationRequestDto request = OperationRequestDto.builder()
                    .walletId(existingWalletId)
                    .amount(BALANCE)
                    .operationType(WITHDRAW)
                    .build();

            // When
            BalanceResponseDto response = walletService.processOperation(request);

            // Then
            assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Снятие при недостаточном балансе должно выбрасывать InsufficientFundsException")
        void processWithdraw_WithInsufficientFunds_ShouldThrowInsufficientFundsException() {
            // Given
            OperationRequestDto request = OperationRequestDto.builder()
                    .walletId(existingWalletId)
                    .amount(OWER_AMOUNT_BALANCE)
                    .operationType(WITHDRAW)
                    .build();

            // When/Then - исключение выбрасывается напрямую, без участия retry
            assertThrows(InsufficientFundsException.class, () -> walletService.processOperation(request));

            // Проверяем, что баланс не изменился
            Wallet unchangedWallet = walletRepository.findById(existingWalletId).orElseThrow();
            assertThat(unchangedWallet.getBalance()).isEqualByComparingTo(BALANCE);
        }

        @Test
        @DisplayName("Снятие с нулевой суммой не должно менять баланс")
        void processWithdraw_WithZeroAmount_ShouldNotChangeBalance() {
            // Given
            OperationRequestDto request = OperationRequestDto.builder()
                    .walletId(existingWalletId)
                    .amount(BigDecimal.ZERO)
                    .operationType(WITHDRAW)
                    .build();

            // When
            BalanceResponseDto response = walletService.processOperation(request);

            // Then
            assertThat(response.getBalance()).isEqualByComparingTo(BALANCE);
        }
    }

    @Nested
    @DisplayName("Тесты обработки ошибок")
    class ErrorHandling {

        @Test
        @DisplayName("Операция с несуществующим кошельком должна выбрасывать EntityNotFoundException")
        void processOperation_WithNonExistingWallet_ShouldThrowEntityNotFoundException() {
            // Given
            OperationRequestDto request = OperationRequestDto.builder()
                    .walletId(nonExistingWalletId)
                    .amount(WITHDRAW_AMOUNT)
                    .operationType(WITHDRAW)
                    .build();

            // When/Then
            assertThrows(EntityNotFoundException.class, () -> walletService.processOperation(request));
        }

        @Test
        @DisplayName("Снятие с удаленного кошелька должно выбрасывать EntityNotFoundException")
        void processWithdraw_OnDeletedWallet_ShouldThrowEntityNotFoundException() {
            // Given
            walletRepository.deleteById(existingWalletId);

            OperationRequestDto request = OperationRequestDto.builder()
                    .walletId(existingWalletId)
                    .amount(WITHDRAW_AMOUNT)
                    .operationType(WITHDRAW)
                    .build();

            // When/Then
            assertThrows(EntityNotFoundException.class, () -> walletService.processOperation(request));
        }

        @Test
        @DisplayName("Операция с null OperationType должна выбрасывать UnsupportedOperationType")
        void processOperation_WithNullOperationType_ShouldThrowUnsupportedOperationType() {
            // Given
            OperationRequestDto request = OperationRequestDto.builder()
                    .walletId(existingWalletId)
                    .amount(WITHDRAW_AMOUNT)
                    .operationType(null)
                    .build();

            // When/Then
            assertThrows(UnsupportedOperationType.class, () -> walletService.processOperation(request));
        }


        @Test
        @DisplayName("Получение баланса несуществующего кошелька должно выбрасывать EntityNotFoundException")
        void getBalance_WithNonExistingWallet_ShouldThrowEntityNotFoundException() {
            // When/Then
            assertThrows(EntityNotFoundException.class, () -> walletService.getBalance(nonExistingWalletId));
        }
    }

    @Nested
    @DisplayName("Тесты получения баланса")
    class GetBalanceTests {

        @Test
        @DisplayName("Получение баланса существующего кошелька должно возвращать корректный баланс")
        void getBalance_WithExistingWallet_ShouldReturnBalance() {
            // When
            BalanceResponseDto response = walletService.getBalance(existingWalletId);

            // Then
            assertAll(
                    () -> assertThat(response.getWalletId()).isEqualTo(existingWalletId),
                    () -> assertThat(response.getBalance()).isEqualByComparingTo(BALANCE)
            );
        }


        @Test
        @DisplayName("Получение баланса должно кэшировать результаты")
        void getBalance_ShouldCacheResults() {
            // Очищаем кэш перед тестом
            Cache cache = cacheManager.getCache("walletBalances");
            cache.clear();

            // Первый вызов - должен попасть в БД
            BalanceResponseDto firstResponse = walletService.getBalance(existingWalletId);

            // Проверяем, что значение попало в кэш
            Cache.ValueWrapper cachedValue = cache.get(existingWalletId);
            assertThat(cachedValue).isNotNull();
            assertThat(((BalanceResponseDto) cachedValue.get()).getBalance())
                    .isEqualByComparingTo(firstResponse.getBalance());

            // Удаляем кошелек из БД
            walletRepository.deleteById(existingWalletId);

            // Второй вызов - должен вернуться из кэша, не обращаясь к БД
            BalanceResponseDto secondResponse = walletService.getBalance(existingWalletId);

            assertAll(
                    () -> assertThat(secondResponse.getWalletId()).isEqualTo(existingWalletId),
                    () -> assertThat(secondResponse.getBalance()).isEqualByComparingTo(firstResponse.getBalance())
            );
        }
    }

    @Nested
    @DisplayName("Тесты конкурентного доступа")
    class ConcurrencyTests {

        @Test
        @DisplayName("Конкурентные депозиты должны сохранять целостность данных")
        void concurrentDeposits_ShouldMaintainConsistency() throws Exception {
            // Given
            int numberOfThreads = 10;
            BigDecimal depositAmount = TEST_AMOUNT;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // When
            for (int i = 0; i < numberOfThreads; i++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    OperationRequestDto request = OperationRequestDto.builder()
                            .walletId(existingWalletId)
                            .amount(depositAmount)
                            .operationType(DEPOSIT)
                            .build();
                    walletService.processOperation(request);
                }, executorService);
                futures.add(future);
            }

            // Ждем завершения всех операций
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);

            // Then
            Wallet finalWallet = walletRepository.findById(existingWalletId).orElseThrow();
            BigDecimal expectedBalance = BALANCE
                    .add(depositAmount.multiply(new BigDecimal(numberOfThreads)));

            assertThat(finalWallet.getBalance()).isEqualByComparingTo(expectedBalance);
        }

        @Test
        @DisplayName("Конкурентные снятия с достаточным общим балансом должны сохранять целостность")
        void concurrentWithdrawals_WithSufficientTotalFunds_ShouldMaintainConsistency() throws Exception {
            // Given
            int numberOfThreads = 5;
            BigDecimal withdrawalAmount = TEST_AMOUNT; // Total: 750 < 1000
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // When
            for (int i = 0; i < numberOfThreads; i++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    OperationRequestDto request = OperationRequestDto.builder()
                            .walletId(existingWalletId)
                            .amount(withdrawalAmount)
                            .operationType(WITHDRAW)
                            .build();
                    walletService.processOperation(request);
                }, executorService);
                futures.add(future);
            }

            // Ждем завершения всех операций
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);

            // Then
            Wallet finalWallet = walletRepository.findById(existingWalletId).orElseThrow();
            BigDecimal expectedBalance = BALANCE
                    .subtract(withdrawalAmount.multiply(new BigDecimal(numberOfThreads)));

            assertThat(finalWallet.getBalance()).isEqualByComparingTo(expectedBalance);
        }

        @Test
        @DisplayName("Смешанные конкурентные операции должны сохранять целостность")
        void concurrentOperations_MixedDepositAndWithdraw_ShouldMaintainConsistency() throws Exception {
            // Given
            int numberOfOperations = 20;
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // When
            for (int i = 0; i < numberOfOperations; i++) {
                final int operationIndex = i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    OperationType type = operationIndex % 2 == 0 ? DEPOSIT : WITHDRAW;
                    BigDecimal amount = TEST_AMOUNT;

                    OperationRequestDto request = OperationRequestDto.builder()
                            .walletId(existingWalletId)
                            .amount(amount)
                            .operationType(type)
                            .build();

                    try {
                        walletService.processOperation(request);
                    } catch (InsufficientFundsException e) {
                        log.debug("Insufficient funds for operation {}", operationIndex);
                    }
                }, executorService);
                futures.add(future);
            }

            // Ждем завершения всех операций
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);

            // Then - Проверим что баланс не отрецательный
            Wallet finalWallet = walletRepository.findById(existingWalletId).orElseThrow();
            assertThat(finalWallet.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Конкурентные операции с разными кошельками не должны блокировать друг друга")
        void concurrentOperations_DifferentWallets_ShouldNotBlock() throws Exception {
            // Given
            int numberOfWallets = 5;
            List<UUID> walletIds = new ArrayList<>();
            for (int i = 0; i < numberOfWallets; i++) {
                Wallet wallet = Wallet.builder()
                        .balance(DEPOSIT_AMOUNT)
                        .build();
                walletIds.add(walletRepository.save(wallet).getId());
            }

            ExecutorService executorService = Executors.newFixedThreadPool(numberOfWallets);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // When
            for (UUID walletId : walletIds) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < 3; j++) {
                        OperationRequestDto depositRequest = OperationRequestDto.builder()
                                .walletId(walletId)
                                .amount(TEST_AMOUNT)
                                .operationType(DEPOSIT)
                                .build();

                        OperationRequestDto withdrawRequest = OperationRequestDto.builder()
                                .walletId(walletId)
                                .amount(AMOUNT)
                                .operationType(WITHDRAW)
                                .build();

                        walletService.processOperation(depositRequest);
                        walletService.processOperation(withdrawRequest);
                    }
                }, executorService);
                futures.add(future);
            }

            // Ждем завершения всех операций
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);

            // Then
            for (UUID walletId : walletIds) {
                Wallet wallet = walletRepository.findById(walletId).orElseThrow();
                // 500,  +300(3*100) -150(3*50) = 650
                assertThat(wallet.getBalance()).isEqualByComparingTo("650.00");
            }
        }
    }
}
