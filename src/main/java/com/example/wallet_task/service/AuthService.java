package com.example.wallet_task.service;

/**
 * Сервис для Аутентификации
 */
public interface AuthService {

    /**
     * @param authHeader Заголовок авторизации
     * @return jwt токен
     */
    String authenticate(String authHeader);
}
