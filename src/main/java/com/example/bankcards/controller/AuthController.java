package com.example.bankcards.controller;

import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtTokenService;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.service.CardService;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Контроллер аутентификации и регистрации пользователей.
 */
/**
 * Тело запроса для входа в систему.
 * @param username имя пользователя
 * @param password пароль
 */
record LoginRequest(@NotBlank String username, @NotBlank String password) {}
/**
 * Ответ на запрос входа (JWT-токен).
 * @param token JWT-токен
 */
record LoginResponse(String token) {}
/**
 * Тело запроса для регистрации нового пользователя.
 * @param username имя пользователя
 * @param password пароль
 * @param fullName полное имя пользователя
 */
/**
 * Требования:
 * - username: 3..30 символов, латинские буквы, цифры, точка, дефис, подчёркивание
 * - password: сложность по паттерну "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$"
 */
record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username can contain letters, digits, dot, dash, underscore")
        String username,

        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$",
                message = "Password must contain lower/upper case letters, digits and special characters")
        String password,
        @NotBlank @Size(min = 1, max = 100) String fullName) {}
/**
 * Ответ на регистрацию с данными пользователя и токеном.
 * @param userId идентификатор пользователя
 * @param username имя пользователя
 * @param fullName полное имя
 * @param token JWT-токен
 */
record RegisterResponse(Long userId, String username, String fullName, String token) {}

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Авторизация и регистрация пользователей")
public class AuthController {

    /**
     * Репозиторий пользователей.
     */
    private final UserRepository userRepository;
    /**
     * Сервис кодирования паролей.
     */
    private final PasswordEncoder passwordEncoder;
    /**
     * Сервис генерации JWT-токенов.
     */
    private final JwtTokenService jwtTokenService;
    /**
     * Сервис карт для авто-выпуска карты при регистрации.
     */
    private final CardService cardService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenService jwtTokenService,
                          CardService cardService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.cardService = cardService;
    }

    @PostMapping("/login")
    @Operation(summary = "Вход пользователя", responses = {
            @ApiResponse(responseCode = "200", description = "Успешный вход",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    /**
     * Аутентифицирует пользователя и возвращает JWT-токен при успешном входе.
     *
     * @param request логин и пароль
     * @return 200 OK с токеном или 401 при неверных учетных данных
     */
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .<ResponseEntity<?>>map(u -> {
                    String token = jwtTokenService.generateToken(u.getUsername(), u.getRole());
                    return ResponseEntity.ok(new LoginResponse(token));
                })
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя",
            description = "Требования: username 3..30 (латиница, цифры, .-_), пароль минимум 8 символов и соответствует политике сложности; fullName: строго три компонента — Фамилия Имя Отчество, каждый компонент минимум 2 символа",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь зарегистрирован",
                            content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибки валидации"),
                    @ApiResponse(responseCode = "409", description = "Имя пользователя занято")
            })
    /**
     * Регистрирует нового пользователя, автоматически выпускает ему карту и возвращает JWT-токен.
     *
     * @param request данные регистрации (username, password, fullName)
     * @return 200 OK с данными созданного пользователя и токеном либо 409, если имя занято
     */
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(409).body(Map.of("error", "Username already taken"));
        }
        // Разбор ФИО: ожидается строго "Фамилия Имя Отчество"
        String[] parts = request.fullName().trim().replaceAll("\\s+", " ").split(" ");
        if (parts.length != 3) {
            return ResponseEntity.badRequest().body(Map.of("error", "Full name must contain exactly 3 parts: Lastname Firstname Patronymic"));
        }
        String lastName = parts[0];
        String firstName = parts[1];
        String patronymic = parts[2];
        if (lastName.length() < 2 || firstName.length() < 2 || patronymic.length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Each full name part must be at least 2 characters"));
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(String.join(" ", lastName, firstName, patronymic));
        user.setLastName(lastName);
        user.setFirstName(firstName);
        user.setPatronymic(patronymic);
        user.setRole("USER");
        UserEntity saved = userRepository.save(user);

        // Auto-issue a default card for the new user (owner = fullName, expiry +4 years)
        cardService.issueAutoCard(saved.getId(), saved.getFullName());

        String token = jwtTokenService.generateToken(saved.getUsername(), saved.getRole());
        return ResponseEntity.ok(new RegisterResponse(saved.getId(), saved.getUsername(), saved.getFullName(), token));
    }
}
