package com.tangent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExpenseUpdateRequest(
        @Schema(example = "90") @NotNull @DecimalMin("0") BigDecimal amount
) {
}
