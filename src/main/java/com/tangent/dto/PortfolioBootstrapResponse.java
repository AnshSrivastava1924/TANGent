package com.tangent.dto;

import java.util.List;

public record PortfolioBootstrapResponse(UserSummary user, long portfolioId,
                                         List<PortfolioClassResponse> portfolioClasses,
                                         List<ExpenseResponse> expenses,
                                         List<String> watchlist) {
}
