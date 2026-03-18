# Wallet Service API

RESTful сервис для управления кошельками с поддержкой операций депозита и снятия средств.

## Требования

- Java 17 или выше
- Docker и Docker Compose (для запуска в контейнерах)
- Maven (опционально, можно использовать ./mvnw)
- PostgreSQL (если запуск без Docker)

## Быстрый старт

### Запуск с Docker Compose

1. Клонируйте репозиторий:
```bash
git clone <https://github.com/igor2195/wallet_service>
cd wallet-service
```

2. Соберите и запустите приложение:
```bash
# Сборка проекта
./mvnw clean package

# Запуск c Docker Compose
docker-compose up --build -d
```

3. Проверьте, что все работает:
```bash
# Проверка статуса контейнеров
docker-compose ps

# Просмотр логов
# Все логи
docker-compose logs -f

# Логи только приложения
docker-compose logs -f wallet-app

# Логи только базы данных
docker-compose logs -f postgres
```
4. Остановка приложения
```bash
docker-compose down

#Перезапустить
docker-compose up -d
```

## После запуска приложения документация Swagger доступна по адресу:

Swagger UI: http://localhost:8080/wallet-service/api/swagger-ui/index.html


##  Аутентификация

API защищено **Basic Authentication**. Используйте следующие учетные данные:


| Роль | Логин | Пароль |
|------|-------|--------|
| **Пользователь** | `user` | `password` |
| **Администратор** | `admin` | `admin` |

Базовый урл методов: http://localhost:8080/wallet-service/api/v1/

| URL               | Описание                         | Доступ |
|-------------------|----------------------------------|--------|
| **POST /wallets** | `Выполнить операцию с кошельком` | `user, admin` |
| **GET /wallets/{id}**          | `Получить баланс кошелька`       | `user, admin` |
