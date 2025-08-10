package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CardUpdateStatusRequest", description = "Request to update card status")
/**
 * Запрос на изменение статуса карты.
 */
public record CardUpdateStatusRequest(
        @Schema(description = "New card status", example = "BLOCKED")
        /** Новый статус карты. */
        @NotNull
        CardStatus status
) {}
