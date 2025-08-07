package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TokenResponse", description = "JWT token response")
/**
 * Ответ с выданным JWT-токеном.
 */
public record TokenResponse(
        @Schema(description = "JWT token string")
        /** Строковое представление JWT-токена. */
        String token
) {}
