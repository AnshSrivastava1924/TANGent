package com.tangent.dto;

import java.math.BigDecimal;

public record PortfolioAssetResponse(long id, String name, BigDecimal value,
                                     BigDecimal income, String note) {
}
