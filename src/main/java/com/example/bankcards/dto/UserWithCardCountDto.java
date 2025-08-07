package com.example.bankcards.dto;

/**
 * Краткая информация о пользователе с количеством его карт.
 */
public record UserWithCardCountDto(
        Long id,
        String username,
        String fullName,
        String firstName,
        String lastName,
        String patronymic,
        String role,
        long cardCount
) {
}
