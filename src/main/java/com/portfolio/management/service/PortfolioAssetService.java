package com.portfolio.management.service;

import com.portfolio.management.dto.request.PortfolioAssetRequest;
import com.portfolio.management.dto.response.PortfolioAssetResponse;

import java.util.List;

public interface PortfolioAssetService {

    PortfolioAssetResponse createAsset(PortfolioAssetRequest request);

    List<PortfolioAssetResponse> getAssetsByPortfolio(Long portfolioId);

    PortfolioAssetResponse getAssetById(Long assetId);

    PortfolioAssetResponse updateAsset(Long assetId,
                                       PortfolioAssetRequest request);

    void deleteAsset(Long assetId);
}