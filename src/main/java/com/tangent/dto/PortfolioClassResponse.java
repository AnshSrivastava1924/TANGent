package com.tangent.dto;

import java.util.List;

public record PortfolioClassResponse(long classId, String id, String name, String purpose,
                                     boolean isLiability, boolean isLiquid,
                                     List<PortfolioAssetResponse> items) {
}
