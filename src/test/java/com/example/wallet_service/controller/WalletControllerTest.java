package com.example.wallet_service.controller;

import com.example.wallet_service.config.SecurityConfig;
import com.example.wallet_service.model.BalanceResponseDto;
import com.example.wallet_service.model.OperationRequestDto;
import com.example.wallet_service.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

import static com.example.wallet_service.model.OperationType.DEPOSIT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@Import(SecurityConfig.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

    private UUID walletId;
    private OperationRequestDto depositRequest;
    private BalanceResponseDto balanceResponse;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        depositRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .build();
        balanceResponse = BalanceResponseDto.builder()
                .walletId(walletId)
                .balance(BigDecimal.valueOf(1500))
                .build();
    }

    private String createBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    @Test
    void processOperation_WithValidBasicAuth_ReturnsOk() throws Exception {
        // given
        when(walletService.processOperation(any(OperationRequestDto.class)))
                .thenReturn(balanceResponse);

        // when/then
        var a = mockMvc.perform(post("/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, createBasicAuthHeader("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(1500));

        verify(walletService, times(1)).processOperation(any(OperationRequestDto.class));
    }

    @Test
    void processOperation_WithoutAuth_ReturnsUnauthorized() throws Exception {
        // when/then
        mockMvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isUnauthorized());

        verify(walletService, never()).processOperation(any());
    }

    @Test
    void processOperation_WithoutRole_ReturnsUnauthorized() throws Exception {
        // when/then
        mockMvc.perform(post("/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, createBasicAuthHeader("user", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isForbidden());

        verify(walletService, never()).processOperation(any());
    }

    @Test
    void processOperation_WithInvalidBasicAuth_ReturnsUnauthorized() throws Exception {
        // when/then
        mockMvc.perform(post("/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, createBasicAuthHeader("user", "wrongpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isUnauthorized());

        verify(walletService, never()).processOperation(any());
    }

    @Test
    void processOperation_WithInvalidBasicAuthFormat_ReturnsUnauthorized() throws Exception {
        // when/then
        mockMvc.perform(post("/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, "Basic invalid_base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBalance_WithValidBasicAuth_ReturnsOk() throws Exception {
        // given
        when(walletService.getBalance(walletId)).thenReturn(balanceResponse);

        // when/then
        mockMvc.perform(get("/v1/wallets/{walletId}", walletId)
                        .header(HttpHeaders.AUTHORIZATION, createBasicAuthHeader("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(1500));
    }

    @Test
    void getBalance_WithoutAuth_ReturnsUnauthorized() throws Exception {
        // when/then
        mockMvc.perform(get("/v1/wallets/{walletId}", walletId))
                .andExpect(status().isUnauthorized());

        verify(walletService, never()).getBalance(any());
    }

    @Test
    void getBalance_WithInvalidAuth_ReturnsUnauthorized() throws Exception {
        // when/then
        mockMvc.perform(get("/v1/wallets/{walletId}", walletId)
                        .header(HttpHeaders.AUTHORIZATION, createBasicAuthHeader("user", "wrongpass")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void processOperation_WithInvalidAmount_ReturnsBadRequest() throws Exception {
        // given - отрицательная сумма
        OperationRequestDto invalidRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(DEPOSIT)
                .amount(BigDecimal.valueOf(-100))
                .build();

        // when/then
        mockMvc.perform(post("/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, createBasicAuthHeader("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processOperation_WithNullWalletId_ReturnsBadRequest() throws Exception {
        // given
        OperationRequestDto invalidRequest = OperationRequestDto.builder()
                .walletId(null)
                .operationType(DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        // when/then
        mockMvc.perform(post("/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, createBasicAuthHeader("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processOperation_WithNullOperationType_ReturnsBadRequest() throws Exception {
        // given
        OperationRequestDto invalidRequest = OperationRequestDto.builder()
                .walletId(walletId)
                .operationType(null)
                .amount(BigDecimal.valueOf(500))
                .build();

        // when/then
        mockMvc.perform(post("/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, createBasicAuthHeader("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
