package com.portfolio.management.controller;

import com.portfolio.management.dto.request.PortfolioAssetRequest;
import com.portfolio.management.dto.response.PortfolioAssetResponse;
import com.portfolio.management.service.PortfolioAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class PortfolioAssetController {

    private final PortfolioAssetService portfolioAssetService;

    @PostMapping
    public ResponseEntity<PortfolioAssetResponse> createAsset(
            @Valid @RequestBody PortfolioAssetRequest request) {

        PortfolioAssetResponse response =
                portfolioAssetService.createAsset(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<PortfolioAssetResponse>> getAssetsByPortfolio(
            @PathVariable Long portfolioId) {

        return ResponseEntity.ok(
                portfolioAssetService.getAssetsByPortfolio(portfolioId));
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<PortfolioAssetResponse> getAssetById(
            @PathVariable Long assetId) {

        return ResponseEntity.ok(
                portfolioAssetService.getAssetById(assetId));
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<PortfolioAssetResponse> updateAsset(
            @PathVariable Long assetId,
            @Valid @RequestBody PortfolioAssetRequest request) {

        return ResponseEntity.ok(
                portfolioAssetService.updateAsset(assetId, request));
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<String> deleteAsset(
            @PathVariable Long assetId) {

        portfolioAssetService.deleteAsset(assetId);

        return ResponseEntity.ok("Asset deleted successfully");
    }
}