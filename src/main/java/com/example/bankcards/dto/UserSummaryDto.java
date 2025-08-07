package com.example.bankcards.dto;

/**
 * Краткое представление пользователя для административных операций.
 */
public record UserSummaryDto(
        Long id,
        String username,
        String fullName,
        String firstName,
        String lastName,
        String patronymic,
        String role
) {}
