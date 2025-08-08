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
- PostgreSQL 17+
- Docker / Docker Compose (опционально, для контейнерного запуска)

## Конфигурация
Приложение настраивается через `application.yml` (и профили). Важные свойства (по умолчанию):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bankdb
    username: admincard
    password: admincard
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
      secret: "change-me-in-prod"
```

Примечания:
- Секрет JWT задавайте через переменную окружения `JWT_SECRET`. Значение по умолчанию в `application.yml` — только для локальной разработки.
- Liquibase применит миграции автоматически при старте.

## База данных
Создайте БД и пользователя перед первым запуском, например:

```sql
CREATE DATABASE bankdb;
CREATE USER admincard WITH ENCRYPTED PASSWORD 'admincard';
GRANT ALL PRIVILEGES ON DATABASE bankdb TO admincard;
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
- Swagger UI: http://localhost:8080/swagger-ui.html (или /swagger-ui/index.html)
- OpenAPI JSON (автогенерация springdoc): http://localhost:8080/v3/api-docs
- Статическая OpenAPI YAML: http://localhost:8080/openapi.yaml (Swagger UI использует её по умолчанию)

## Health
- GET http://localhost:8080/api/health (публичный, если разрешено в настройках безопасности)

## Аутентификация и роли
- JWT. Токен передаётся в заголовке `Authorization: Bearer <token>`.
- Доступ ограничен ролями (`ROLE_ADMIN`, `ROLE_USER`). Подробности — в контроллерах.

### Автосоздание пользователя ADMIN при старте (вариант A)
Реализован идемпотентный механизм создания администратора при первом запуске:
- Если пользователя с указанным именем нет — создаётся пользователь с ролью `ADMIN` и захэшированным паролем.
- Механизм управляется свойствами (можно задавать через ENV):
  - `app.bootstrap.admin.enabled` / `ADMIN_BOOTSTRAP_ENABLED` — по умолчанию `true` (в проде рекомендовано выключить)
  - `app.bootstrap.admin.username` / `ADMIN_USERNAME` — имя пользователя (по умолчанию `admin`)
  - `app.bootstrap.admin.password` / `ADMIN_PASSWORD` — пароль (по умолчанию `changeMe123`)
  - `app.bootstrap.admin.full-name` / `ADMIN_FULL_NAME`
  - `app.bootstrap.admin.first-name` / `ADMIN_FIRST_NAME`
  - `app.bootstrap.admin.last-name` / `ADMIN_LAST_NAME`
  - `app.bootstrap.admin.patronymic` / `ADMIN_PATRONYMIC`

Рекомендации безопасности:
- Для production установите `ADMIN_BOOTSTRAP_ENABLED=false` либо включайте механизм только на первый запуск с сильным паролем, затем выключайте.
- Секрет JWT храните в `JWT_SECRET`.

## Быстрый чеклист «первого запуска»
1) Подготовьте БД PostgreSQL (локально или в Docker):
   - DB: `bankdb`, USER/PASSWORD: `admincard/admincard` (или свои значения)
2) Установите переменные окружения (как минимум):
   - `JWT_SECRET` — надёжный секрет
   - Опционально для первого запуска: `ADMIN_BOOTSTRAP_ENABLED=true`, `ADMIN_USERNAME=admin`, `ADMIN_PASSWORD=<сильный_пароль>`
3) Запустите приложение:
   - Локально: `mvn spring-boot:run`
   - Или Docker Compose: `docker compose up --build`
4) Убедитесь, что сервис поднялся:
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - OpenAPI JSON: http://localhost:8080/v3/api-docs
5) Проверьте, что создан администратор (по логам будет предупреждение о создании):
   - Выполните логин `POST /api/auth/login` с `ADMIN_USERNAME/ADMIN_PASSWORD`
6) Сразу смените пароль администратора (см. следующий раздел) и выключите автосоздание в проде.

## Смена пароля администратора после первичного старта
В текущей версии API отдельного эндпоинта для смены пароля админа нет. Используйте один из вариантов:

Вариант A (рекомендуется): обновить пароль напрямую в базе с использованием BCrypt-хэша
- Сгенерируйте BCrypt-хэш для нового пароля. Способы:
  1. Небольшой Java-скрипт (запустите как отдельный класс/JShell):
     ```java
     import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
     public class GenHash { public static void main(String[] args){
         System.out.println(new BCryptPasswordEncoder().encode("<NEW_PASSWORD>"));
     }}
     ```
  2. Любой надёжный генератор BCrypt (офлайн), убедитесь в корректности.
- Выполните SQL для обновления (замените `<BCRYPT_HASH>` и `<ADMIN_USERNAME>`):
  ```sql
  UPDATE users SET password_hash = '<BCRYPT_HASH>' WHERE username = '<ADMIN_USERNAME>';
  ```

Вариант B (временное решение через окружение):
- Если база пустая и вы настраиваете новый стенд, можно задать `ADMIN_PASSWORD=<новый>` и запустить приложение впервые — админ будет создан сразу с нужным паролем. После этого отключите `ADMIN_BOOTSTRAP_ENABLED`.

Примечание:
- Не храните хэши/пароли в репозитории. Меняйте пароль сразу после первого логина.

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
  - `POST /api/user/users/{userId}/cards` — создание новой карты пользователем для своего аккаунта
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
    image: postgres:17
    container_name: bankcards-postgres
    environment:
      POSTGRES_DB: bankdb
      POSTGRES_USER: admincard
      POSTGRES_PASSWORD: admincard
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
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/bankdb
      SPRING_DATASOURCE_USERNAME: admincard
      SPRING_DATASOURCE_PASSWORD: admincard
      JWT_SECRET: please-change-me-to-strong-secret
      # Автосоздание администратора (для первого запуска dev/staging)
      ADMIN_BOOTSTRAP_ENABLED: "true"
      ADMIN_USERNAME: admin
      ADMIN_PASSWORD: changeMe123
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
- Swagger: http://localhost:8080/swagger-ui.html

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
