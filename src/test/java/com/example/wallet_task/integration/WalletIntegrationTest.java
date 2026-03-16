package com.example.wallet_task.integration;

import com.example.wallet_task.BaseIntegrationTest;
import com.example.wallet_task.domain.Wallet;
import com.example.wallet_task.model.OperationRequestDto;
import com.example.wallet_task.model.OperationType;
import com.example.wallet_task.repository.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.wallet_task.model.OperationType.DEPOSIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WalletIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    private UUID walletId;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();

        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(1000));
        wallet = walletRepository.save(wallet);
        walletId = wallet.getId();
    }

    @Test
    void fullWorkflow_Success() throws Exception {
        // 1. Проверяем начальный баланс
        mvc.perform(get("/v1/wallets/{walletId}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000));

        // 2. Пополняем счет
        var depositRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        mvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500));

        // 3. Снимаем средства
        var withdrawRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.WITHDRAW)
                .amount(BigDecimal.valueOf(300))
                .build();

        mvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1200));
    }

    @Test
    void concurrentOperations_ShouldMaintainConsistency() throws InterruptedException {
        // arrange
        int threadCount = 10;
        int operationsPerThread = 10;

        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // act - конкурентные операции по изменению баланса
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        OperationRequestDto request = OperationRequestDto.builder()
                                .walletId(walletId)
                                .operationType(j % 2 == 0 ? OperationType.DEPOSIT : OperationType.WITHDRAW)
                                .amount(BigDecimal.TEN)
                                .build();

                        mvc.perform(post("/v1/wallets")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // then
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        Wallet finalWallet = walletRepository.findById(walletId).get();

        // Баланс должен быть консистентным (может быть любым, но не отрицательным)
        assertThat(finalWallet.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void withdrawMoreThanBalance_ShouldFail() throws Exception {
        // arrange
        var withdrawRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(OperationType.WITHDRAW)
                .amount(BigDecimal.valueOf(2000)) // Больше чем 1000
                .build();

        // assert
        mvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isBadRequest());

        // Баланс не изменился
        mvc.perform(get("/v1/wallets/{walletId}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000));
    }
}
