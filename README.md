# Bank_CARD

API для управления банковскими картами (пользователь и администратор) на Spring Boot 3 (Java 21) с PostgreSQL, Liquibase и Swagger (springdoc-openapi).

## Стек технологий
- Java 21, Spring Boot 3.3.x
- Spring Web, Spring Security (JWT), Spring Data JPA
- PostgreSQL + Liquibase
- springdoc-openapi (Swagger UI)
- Lombok

## Предварительные требования
- Java 21 (JDK)
- Maven 3.9+
- PostgreSQL 14+
- Docker / Docker Compose (опционально, для контейнерного запуска)

## Конфигурация
Приложение настраивается через `application.yaml` (и профили). Важные свойства:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bankcards
    username: bankcards
    password: bankcards
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
  liquibase:
    change-log: classpath:db/migration/db.migration-master.yaml

app:
  security:
    jwt:
      # Минимум 32 байта. Если пусто/короче — при старте сгенерируется случайный ключ (токены не переживут рестарт).
      secret: "please-change-me-to-32-bytes-minimum-secret-key........"
```

Примечания:
- Если `app.security.jwt.secret` отсутствует или короче 32 байт, при запуске будет сгенерирован случайный ключ (удобно для локальной разработки, но токены не будут валидны после рестарта).
- Liquibase применит миграции автоматически при старте.

## База данных
Создайте БД и пользователя перед первым запуском, например:

```sql
CREATE DATABASE bankcards;
CREATE USER bankcards WITH ENCRYPTED PASSWORD 'bankcards';
GRANT ALL PRIVILEGES ON DATABASE bankcards TO bankcards;
```

## Сборка и запуск (локально)
- Запуск тестов:

```bash
mvn test
```

- Запуск приложения (dev):

```bash
mvn spring-boot:run
```

- Или собрать исполняемый JAR и запустить:

```bash
mvn clean package
java -jar target/bankcards-0.0.1-SNAPSHOT.jar
```

## Документация API (Swagger)
После старта доступны:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Health
- GET http://localhost:8080/api/health (публичный, если разрешено в настройках безопасности)

## Аутентификация и роли
- JWT. Токен передаётся в заголовке `Authorization: Bearer <token>`.
- Доступ ограничен ролями (`ROLE_ADMIN`, `ROLE_USER`). Подробности — в контроллерах.

### Пример получения JWT токена (Auth)
Эндпоинты (могут отличаться в вашей версии):
- `POST /api/auth/register` — регистрация пользователя
- `POST /api/auth/login` — получение токена по логину/паролю

Пример запроса входа:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
        "username": "user1",
        "password": "password"
      }'
```

Пример ответа:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

Использование токена в запросах:
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/user/users/1/cards
```

## Основные эндпоинты (выборка)
- Админ:
  - `GET /api/admin/users` — список пользователей с количеством карт
  - `GET /api/admin/cards` — список всех карт с фильтрами
  - `PATCH /api/admin/cards/{cardId}/status` — смена статуса карты (ACTIVE/BLOCKED/…)
  - `POST /api/admin/cards/{cardId}/block` — блокировка карты
  - `POST /api/admin/cards/{cardId}/unblock` — разблокировка карты
  - `POST /api/admin/users/{userId}/cards` — выпуск карты пользователю (опц. тело `{ "owner": "..." }`)
- Пользователь:
  - `GET /api/user/users/{userId}/cards` — список своих карт
  - `POST /api/user/cards/{cardId}/block` — блокировка своей карты
  - `POST /api/user/cards/{cardId}/unblock` — разблокировка своей карты
  - `GET /api/user/cards/{cardId}/balance` — баланс своей карты
  - `POST /api/user/users/{userId}/transfers` — перевод между своими картами. Пример тела:

```json
{
  "fromCardNumber": "4111111111111111",
  "toCardNumber":   "5555555555554444",
  "amount":         100.00
}
```

## Запуск в Docker Compose
Ниже пример docker-compose для PostgreSQL и приложения. Требуется Dockerfile в корне проекта (см. пример далее).

docker-compose.yml:
```yaml
version: "3.9"
services:
  db:
    image: postgres:14
    container_name: bankcards-postgres
    environment:
      POSTGRES_DB: bankcards
      POSTGRES_USER: bankcards
      POSTGRES_PASSWORD: bankcards
    ports:
      - "5432:5432"
    volumes:
      - dbdata:/var/lib/postgresql/data

  app:
    build: .
    container_name: bankcards-app
    depends_on:
      - db
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/bankcards
      SPRING_DATASOURCE_USERNAME: bankcards
      SPRING_DATASOURCE_PASSWORD: bankcards
      APP_SECURITY_JWT_SECRET: please-change-me-to-32-bytes-minimum-secret-key........
    ports:
      - "8080:8080"

volumes:
  dbdata:
```

Пример Dockerfile (multi-stage):
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS=""
COPY --from=build /app/target/*SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

Запуск:
```bash
docker compose up --build
```

После старта:
- Приложение: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html

## Полезные советы
- Задайте стабильный `app.security.jwt.secret` локально/в контейнере.
- Не коммитьте артефакты сборки. Добавьте в `.gitignore`:

```
/target/
*.iml
.idea/
```

## Тестирование
- Контроллеры тестируются через MockMvc с замоканным security-фильтром (см. `UserControllerTest`, `AdminControllerTest`).
- Сервисные тесты проверяют бизнес-правила (например, запрет разблокировки EXPIRED).

## Изменения API
- Группа `/api/cards/**` удалена.
  Используйте актуальные эндпоинты:
  - вместо `POST /api/cards/{userId}` →
    - Админ: `POST /api/admin/users/{userId}/cards` (опц. тело `{ "owner": "..." }`)
    - Пользователь: `POST /api/user/users/{userId}/cards` (создание своей карты)
  - вместо `GET /api/cards/{userId}` → `GET /api/user/users/{userId}/cards`
  - вместо `PATCH /api/cards/{cardId}/status` → `PATCH /api/admin/cards/{cardId}/status`
  - вместо `POST /api/cards/{cardId}/block-request` →
    - `POST /api/user/cards/{cardId}/block` (пользователь)
    - `POST /api/admin/cards/{cardId}/block` (админ)
  - вместо `POST /api/cards/{userId}/additional` → `POST /api/admin/users/{userId}/cards` с телом `{ "owner": "..." }`

## License
MIT (or your preferred license).
