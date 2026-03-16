package com.example.wallet_task.controller;

import com.example.wallet_task.model.BalanceResponseDto;
import com.example.wallet_task.model.OperationRequestDto;
import com.example.wallet_task.model.OperationType;
import com.example.wallet_task.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean  // Вместо @MockBean
    private WalletService walletService;


    @Test
    void processOperation_Deposit_ReturnsOk() throws Exception {
        // arrange
        var request = OperationRequestDto.builder()
                .walletId(UUID.randomUUID())
                .operationType(OperationType.DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        var response = BalanceResponseDto.builder()
                .walletId(request.getWalletId())
                .balance(BigDecimal.valueOf(1500))
                .build();

        when(walletService.processOperation(any())).thenReturn(response);

        // assert
        mockMvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(request.getWalletId().toString()))
                .andExpect(jsonPath("$.balance").value(1500));
    }

    @Test
    void processOperation_InvalidRequest_ReturnsBadRequest() throws Exception {
        // arrange
        var request = OperationRequestDto.builder()
                .walletId(null)
                .operationType(OperationType.DEPOSIT)
                .amount(BigDecimal.valueOf(-100))
                .build();

        // assert
        mockMvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBalance_ReturnsOk() throws Exception {
        // arrange
        UUID walletId = UUID.randomUUID();
        var response = BalanceResponseDto.builder()
                .walletId(walletId)
                .balance(BigDecimal.valueOf(1000))
                .build();

        when(walletService.getBalance(walletId)).thenReturn(response);

        // assert
        mockMvc.perform(get("/v1/wallets/{walletId}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(1000));
    }

    @Test
    void getBalance_WalletNotFound_ReturnsNotFound() throws Exception {
        // arrange
        UUID walletId = UUID.fromString("a07cd10c-fe74-4ee2-abe8-474b90db7130");
        when(walletService.getBalance(walletId)).thenThrow(
                new EntityNotFoundException()
        );

        // assert
        mockMvc.perform(get("/v1/wallets/{walletId}", walletId))
                .andExpect(status().isNotFound());
    }
}
