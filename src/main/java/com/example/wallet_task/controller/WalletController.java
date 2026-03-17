package com.example.wallet_task.controller;

import com.example.wallet_task.model.BalanceResponseDto;
import com.example.wallet_task.model.OperationRequestDto;
import com.example.wallet_task.model.exception.BaseException;
import com.example.wallet_task.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/wallets")
@Tag(name = "Wallet Controller", description = "Управление кошельками (пополнение, снятие, просмотр баланса)")
@SecurityRequirement(name = "bearerAuth")  // Указываем, что все методы требуют JWT
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(
            summary = "Выполнить операцию с кошельком",
            description = "Позволяет пополнить (DEPOSIT) или снять (WITHDRAW) средства с кошелька"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операция выполнена успешно",
                    content = @Content(schema = @Schema(implementation = BalanceResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Недостаточно средств или неверный запрос",
                    content = @Content(schema = @Schema(implementation = BaseException.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован (отсутствует или недействителен JWT)",
                    content = @Content(schema = @Schema(implementation = BaseException.class))),
            @ApiResponse(responseCode = "404", description = "Кошелек не найден",
                    content = @Content(schema = @Schema(implementation = BaseException.class)))
    })
    public BalanceResponseDto processOperation(
            @Valid @RequestBody OperationRequestDto request) {
        return walletService.processOperation(request);
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Получить баланс кошелька", description = "Возвращает текущий баланс кошелька по его ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Баланс получен",
                    content = @Content(schema = @Schema(implementation = BalanceResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = BaseException.class))),
            @ApiResponse(responseCode = "404", description = "Кошелек не найден",
                    content = @Content(schema = @Schema(implementation = BaseException.class)))
    })
    public BalanceResponseDto getBalance(
            @Parameter(description = "UUID кошелька", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID walletId) {
        return walletService.getBalance(walletId);
    }
}

