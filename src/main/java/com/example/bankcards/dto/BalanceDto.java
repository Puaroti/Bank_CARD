package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "Balance", description = "Card balance response")
public record BalanceDto(
        @Schema(description = "Current balance", example = "1234.56") BigDecimal balance
) {}
