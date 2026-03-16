package com.example.wallet_task.controller;

import com.example.wallet_task.model.BalanceResponseDto;
import com.example.wallet_task.model.OperationRequestDto;
import com.example.wallet_task.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    @PostMapping()
    public BalanceResponseDto processOperation(
            @Valid @RequestBody OperationRequestDto request) {
        return walletService.processOperation(request);
    }

    @GetMapping("/{walletId}")
    public BalanceResponseDto getBalance(@PathVariable UUID walletId) {
        return walletService.getBalance(walletId);
    }
}

