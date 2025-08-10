package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(name = "CardCreateRequest", description = "Request to create a new card")
/**
 * Запрос на создание новой банковской карты.
 */
public record CardCreateRequest(
        @Schema(description = "Raw card number (16 digits)", example = "4111111111111111")
        /** Номер карты из 16 цифр без пробелов. */
        @NotBlank
        @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
        String cardNumber,

        @Schema(description = "Owner full name", example = "John Doe")
        /** Полное имя владельца карты. */
        @NotBlank
        @Size(max = 100)
        String owner,

        @Schema(description = "Expiry date (YYYY-MM-DD)", example = "2030-12-31")
        /** Дата окончания срока действия карты. Должна быть в будущем. */
        @NotNull
        @Future(message = "Expiry date must be in the future")
        LocalDate expiryDate
) {}
