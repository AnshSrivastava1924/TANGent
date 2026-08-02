package com.tangent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseCreateRequest(
        @Schema(example = "Groceries") @NotBlank String name,
        @Schema(example = "Food") @NotBlank String category,
        @Schema(example = "85") @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        @Schema(example = "2026-08-02") @NotNull LocalDate date
) {
}
