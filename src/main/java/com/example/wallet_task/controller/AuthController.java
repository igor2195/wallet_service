package com.example.wallet_task.controller;

import com.example.wallet_task.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Аутентификация и получение JWT токена")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Вход в систему через Basic Auth",
            description = "Принимает Basic Auth заголовок, проверяет учетные данные и возвращает JWT токен. " +
                    "Доступны тестовые пользователи: user/password и admin/admin"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация, возвращен JWT токен",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Неверный формат Basic Auth"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    public String login(
            @Parameter(description = "Basic Auth заголовок. Формат: 'Basic base64(username:password)'",
                    required = true,
                    example = "Basic dXNlcjpwYXNzd29yZA==")
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
            return authService.authenticate(authHeader);
    }
}
