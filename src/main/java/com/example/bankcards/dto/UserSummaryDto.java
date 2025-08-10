package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Краткое представление пользователя для административных операций.
 */
@Schema(name = "UserSummary", description = "Brief user view for admin operations")
public record UserSummaryDto(
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
        String role
) {}
