package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Краткая информация о пользователе с количеством его карт.
 */
@Schema(name = "UserWithCardCount", description = "User summary with number of owned cards")
public record UserWithCardCountDto(
        @Schema(description = "User identifier", example = "1")
        Long id,
        @Schema(description = "Username", example = "jdoe")
        String username,
        @Schema(description = "Full name", example = "Doe John Michael")
        String fullName,
        @Schema(description = "First name", example = "John")
        String firstName,
        @Schema(description = "Last name", example = "Doe")
        String lastName,
        @Schema(description = "Patronymic/Middle name", example = "Michael")
        String patronymic,
        @Schema(description = "Assigned role", example = "USER")
        String role,
        @Schema(description = "Number of cards owned by the user", example = "3")
        long cardCount
) {}
