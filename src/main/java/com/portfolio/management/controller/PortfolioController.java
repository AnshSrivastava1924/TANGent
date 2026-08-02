package com.portfolio.management.controller;

import com.portfolio.management.dto.request.PortfolioRequest;
import com.portfolio.management.dto.response.PortfolioResponse;
import com.portfolio.management.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @RequestParam Long userId,
            @Valid @RequestBody PortfolioRequest request) {

        PortfolioResponse response = portfolioService.createPortfolio(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPortfolios(
            @RequestParam Long userId) {

        return ResponseEntity.ok(portfolioService.getAllPortfolios(userId));
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(
            @PathVariable Long portfolioId,
            @RequestParam Long userId) {

        return ResponseEntity.ok(
                portfolioService.getPortfolioById(portfolioId, userId));
    }

    @PutMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @PathVariable Long portfolioId,
            @RequestParam Long userId,
            @Valid @RequestBody PortfolioRequest request) {

        return ResponseEntity.ok(
                portfolioService.updatePortfolio(portfolioId, userId, request));
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<String> deletePortfolio(
            @PathVariable Long portfolioId,
            @RequestParam Long userId) {

        portfolioService.deletePortfolio(portfolioId, userId);

        return ResponseEntity.ok("Portfolio deleted successfully");
    }
}