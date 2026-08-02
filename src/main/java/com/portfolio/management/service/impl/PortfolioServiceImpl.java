package com.portfolio.management.service.impl;

import com.portfolio.management.dto.request.PortfolioRequest;
import com.portfolio.management.dto.response.PortfolioResponse;
import com.portfolio.management.exception.ResourceNotFoundException;
import com.portfolio.management.model.Portfolio;
import com.portfolio.management.repository.PortfolioRepository;
import com.portfolio.management.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;

    @Override
    public PortfolioResponse createPortfolio(Long userId, PortfolioRequest request) {

        Portfolio portfolio = Portfolio.builder()
                .userId(userId)
                .portfolioName(request.getPortfolioName())
                .goalDescription(request.getGoalDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        return mapToResponse(savedPortfolio, "Portfolio created successfully");
    }

    @Override
    public List<PortfolioResponse> getAllPortfolios(Long userId) {

        return portfolioRepository.findByUserId(userId)
                .stream()
                .map(portfolio -> mapToResponse(portfolio, null))
                .collect(Collectors.toList());
    }

    @Override
    public PortfolioResponse getPortfolioById(Long portfolioId, Long userId) {

        Portfolio portfolio = portfolioRepository
                .findByPortfolioIdAndUserId(portfolioId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Portfolio not found"));

        return mapToResponse(portfolio, null);
    }

    @Override
    public PortfolioResponse updatePortfolio(Long portfolioId,
                                             Long userId,
                                             PortfolioRequest request) {

        Portfolio portfolio = portfolioRepository
                .findByPortfolioIdAndUserId(portfolioId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Portfolio not found"));

        portfolio.setPortfolioName(request.getPortfolioName());
        portfolio.setGoalDescription(request.getGoalDescription());
        portfolio.setUpdatedAt(LocalDateTime.now());

        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);

        return mapToResponse(updatedPortfolio,
                "Portfolio updated successfully");
    }

    @Override
    public void deletePortfolio(Long portfolioId, Long userId) {

        Portfolio portfolio = portfolioRepository
                .findByPortfolioIdAndUserId(portfolioId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Portfolio not found"));

        portfolioRepository.delete(portfolio);
    }

    private PortfolioResponse mapToResponse(Portfolio portfolio,
                                            String message) {

        return PortfolioResponse.builder()
                .portfolioId(portfolio.getPortfolioId())
                .userId(portfolio.getUserId())
                .portfolioName(portfolio.getPortfolioName())
                .goalDescription(portfolio.getGoalDescription())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .message(message)
                .build();
    }
}