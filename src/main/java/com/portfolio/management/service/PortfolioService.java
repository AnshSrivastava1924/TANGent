package com.portfolio.management.service;

import com.portfolio.management.dto.request.PortfolioRequest;
import com.portfolio.management.dto.response.PortfolioResponse;

import java.util.List;

public interface PortfolioService {

    /**
     * Create a new portfolio
     */
    PortfolioResponse createPortfolio(Long userId, PortfolioRequest request);

    /**
     * Get all portfolios of a user
     */
    List<PortfolioResponse> getAllPortfolios(Long userId);

    /**
     * Get portfolio by ID
     */
    PortfolioResponse getPortfolioById(Long portfolioId, Long userId);

    /**
     * Update portfolio
     */
    PortfolioResponse updatePortfolio(Long portfolioId,
                                      Long userId,
                                      PortfolioRequest request);

    /**
     * Delete portfolio
     */
    void deletePortfolio(Long portfolioId, Long userId);
}