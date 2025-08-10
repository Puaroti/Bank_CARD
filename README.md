# Bank_CARD

User and Admin card management API built with Spring Boot 3 (Java 21), PostgreSQL, Liquibase, springdoc-openapi.

## Tech stack
- Java 21, Spring Boot 3.3.x
- Spring Web, Spring Security (JWT), Spring Data JPA
- PostgreSQL + Liquibase
- springdoc-openapi (Swagger UI)
- Lombok

## Prerequisites
- Java 21 (JDK)
- Maven 3.9+
- PostgreSQL 14+ running and accessible

## Configuration
The app is configured via `application.yaml` (and profiles). Important properties:

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
      # Minimum 32 bytes. If absent/too short, a random key will be generated at runtime (tokens become ephemeral).
      secret: "please-change-me-to-32-bytes-minimum-secret-key........"
```

Notes:
- If you leave `app.security.jwt.secret` unset or shorter than 32 bytes, the app will log a warning and generate a random key at startup (useful for local dev, tokens won’t persist across restarts).
- Liquibase will auto-apply migrations at startup.

## Database
Create a database and a user before the first run, for example:

```sql
CREATE DATABASE bankcards;
CREATE USER bankcards WITH ENCRYPTED PASSWORD 'bankcards';
GRANT ALL PRIVILEGES ON DATABASE bankcards TO bankcards;
```

## Build & Run
- Run tests:

```bash
mvn test
```

- Run the application (dev):

```bash
mvn spring-boot:run
```

- Or build an executable JAR and run:

```bash
mvn clean package
java -jar target/bankcards-0.0.1-SNAPSHOT.jar
```

## API Docs (Swagger)
After startup, open:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Health
- GET http://localhost:8080/api/health (public if allowed in security config)

## Authentication
- JWT-based. Obtain/attach token according to your auth endpoints (e.g., `Authorization: Bearer <token>`).
- Endpoints are secured by roles (`ROLE_ADMIN`, `ROLE_USER`). See controllers for details.

## Key Endpoints (selection)
- Admin:
  - `GET /api/admin/users` — users with card counts
  - `POST /api/admin/cards/{cardId}/block|unblock` — manage card status
  - `POST /api/admin/users/{userId}/cards` — issue card for user
- User:
  - `GET /api/user/users/{userId}/cards` — list own cards
  - `POST /api/user/cards/{cardId}/block|unblock` — manage own card
  - `GET /api/user/cards/{cardId}/balance` — card balance
  - `POST /api/user/users/{userId}/transfers` — transfer between own cards using body:

```json
{
  "fromCardNumber": "4111111111111111",
  "toCardNumber":   "5555555555554444",
  "amount":         100.00
}
```

## Development Tips
- Prefer setting a stable `app.security.jwt.secret` locally.
- Avoid committing build artifacts. Add to `.gitignore`:

```
/target/
*.iml
.idea/
```

## Testing
- Controller tests use MockMvc with security filter mocked to delegate to the chain (see `UserControllerTest`, `AdminControllerTest`).
- Service tests verify business rules (e.g., cannot activate EXPIRED).

## License
MIT (or your preferred license).
