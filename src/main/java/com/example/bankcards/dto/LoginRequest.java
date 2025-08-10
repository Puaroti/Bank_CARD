package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "User credentials for authentication")
/**
 * Запрос на аутентификацию пользователя.
 */
public record LoginRequest(
        @Schema(description = "Username", example = "admin")
        /** Имя пользователя. */
        @NotBlank String username,
        @Schema(description = "Password", example = "P@ssw0rd")
        /** Пароль пользователя. */
        @NotBlank String password
) {}
