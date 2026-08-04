package com.tangent.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AssetCreateRequest(
        @NotBlank(message = "assetName is required")
        @Size(max = 160, message = "assetName must be at most 160 characters")
        String assetName,

        @Min(value = 1, message = "assetClassId must be valid")
        long assetClassId,

        @NotNull(message = "quantity is required")
        @DecimalMin(value = "0", inclusive = false, message = "quantity must be greater than 0")
        BigDecimal quantity,

        @NotNull(message = "unitValue is required")
        @DecimalMin(value = "0", inclusive = false, message = "unitValue must be greater than 0")
        BigDecimal unitValue,

        @NotNull(message = "annualIncome is required")
        @DecimalMin(value = "0", inclusive = true, message = "annualIncome must be >= 0")
        BigDecimal annualIncome
) {
}

