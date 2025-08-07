package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

@Schema(name = "TransferRequest", description = "Request to transfer money between user's cards")
/**
 * Запрос на перевод средств между картами одного пользователя.
 */
public record TransferRequest(
        @Schema(description = "Sender card number (16 digits)", example = "4111111111111111")
        /** Номер карты-отправителя (16 цифр). */
        @NotBlank
        @Pattern(regexp = "\\d{16}")
        String fromCardNumber,

        @Schema(description = "Recipient card number (16 digits)", example = "5555555555554444")
        /** Номер карты-получателя (16 цифр). */
        @NotBlank
        @Pattern(regexp = "\\d{16}")
        String toCardNumber,

        @Schema(description = "Amount to transfer", example = "100.00")
        /** Сумма перевода (минимум 0.01). */
        @DecimalMin(value = "0.01", inclusive = true)
        BigDecimal amount
) {}
