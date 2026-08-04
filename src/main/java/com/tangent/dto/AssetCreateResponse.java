package com.tangent.dto;

import java.math.BigDecimal;

public record AssetCreateResponse(
        long assetId,
        String assetName,
        String assetClassName,
        BigDecimal quantity,
        BigDecimal unitValue,
        BigDecimal currentValue,
        BigDecimal annualIncome
) {
}

