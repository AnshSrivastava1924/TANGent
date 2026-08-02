package com.tangent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AssetUpdateRequest(
        @Schema(example = "50000") @NotNull @DecimalMin("0") BigDecimal value,
        @Schema(example = "1800") @NotNull @DecimalMin("0") BigDecimal income
) {
}
