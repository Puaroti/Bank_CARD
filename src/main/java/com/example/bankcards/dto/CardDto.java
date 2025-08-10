package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Card", description = "Card view model with masked number")
/**
 * DTO представление банковской карты с маскированным номером.
 */
public class CardDto {
    /** Уникальный идентификатор карты. */
    @Schema(description = "Card identifier", example = "1")
    private Long id;

    /** Маскированный номер карты. */
    @Schema(description = "Masked card number", example = "**** **** **** 1234")
    private String maskedCardNumber;

    /** Полное имя владельца карты. */
    @Schema(description = "Card owner full name", example = "John Doe")
    private String owner;

    /** Дата окончания срока действия. */
    @Schema(description = "Expiry date (YYYY-MM-DD)", example = "2030-12-31")
    private LocalDate expiryDate;

    /** Статус карты. */
    @Schema(description = "Card status", example = "ACTIVE")
    private CardStatus status;

    /** Текущий баланс карты. */
    @Schema(description = "Current balance", example = "1000.00")
    private BigDecimal balance;

    /** Дата/время регистрации карты. */
    @Schema(description = "Card registration timestamp", example = "2025-08-09T21:23:09")
    private LocalDateTime createdAt;
}
