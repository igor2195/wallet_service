package com.example.wallet_task.service;

import com.example.wallet_task.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public String authenticate(String authHeader) {
        // 1. Проверка формата заголовка
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        // 2. Декодирование Base64
        String base64Credentials = authHeader.substring("Basic ".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);


        // 3. Разделение username:password
        String[] parts = credentials.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Basic Auth format");
        }

        String username = parts[0];
        String password = parts[1];

        try {
            // 1. Аутентификация через Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // 2. Генерация JWT токена
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtTokenUtil.generateToken(userDetails.getUsername());

            log.info("Successfully authenticated user: {}", username);

            return token;

        } catch (BadCredentialsException e) {
            log.warn("Invalid credentials for user: {}", username);
            throw new BadCredentialsException("Invalid username or password");
        } catch (IllegalArgumentException e) {
            log.error("Invalid Basic Auth format: {}", e.getMessage());
            throw new IllegalArgumentException();
        }
    }
}
