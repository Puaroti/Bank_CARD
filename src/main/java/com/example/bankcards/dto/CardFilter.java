package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "CardFilter", description = "Filter parameters for cards list")
/**
 * Параметры фильтрации списка карт.
 */
public class CardFilter {
    /** Фильтр по владельцу (подстрока). */
    @Schema(description = "Owner filter", example = "John")
    private String owner;
    /** Фильтр по статусу карты. */
    @Schema(description = "Status filter", example = "ACTIVE")
    private CardStatus status;
    /** Произвольный текстовый запрос. */
    @Schema(description = "Free-text query", example = "Visa 1234")
    private String query;
}
