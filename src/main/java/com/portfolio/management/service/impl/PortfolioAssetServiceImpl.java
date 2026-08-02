
package com.portfolio.management.service.impl;

import com.portfolio.management.dto.request.PortfolioAssetRequest;
import com.portfolio.management.dto.response.PortfolioAssetResponse;
import com.portfolio.management.exception.ResourceNotFoundException;
import com.portfolio.management.model.PortfolioAsset;
import com.portfolio.management.repository.PortfolioAssetRepository;
import com.portfolio.management.service.PortfolioAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioAssetServiceImpl implements PortfolioAssetService {

    private final PortfolioAssetRepository portfolioAssetRepository;

    @Override
    public PortfolioAssetResponse createAsset(PortfolioAssetRequest request) {

        PortfolioAsset asset = PortfolioAsset.builder()
                .portfolioId(request.getPortfolioId())
                .assetClassId(request.getAssetClassId())
                .assetName(request.getAssetName())
                .providerOrLocation(request.getProviderOrLocation())
                .assetIdentifier(request.getAssetIdentifier())
                .quantity(request.getQuantity())
                .unitValue(request.getUnitValue())
                .annualIncome(request.getAnnualIncome())
                .note(request.getNote())
                .valuationDate(request.getValuationDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PortfolioAsset savedAsset =
                portfolioAssetRepository.save(asset);

        return mapToResponse(savedAsset,
                "Asset created successfully");
    }

    @Override
    public List<PortfolioAssetResponse> getAssetsByPortfolio(Long portfolioId) {

        return portfolioAssetRepository.findByPortfolioId(portfolioId)
                .stream()
                .map(asset -> mapToResponse(asset, null))
                .collect(Collectors.toList());
    }

    @Override
    public PortfolioAssetResponse getAssetById(Long assetId) {

        PortfolioAsset asset = portfolioAssetRepository
                .findByAssetId(assetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        return mapToResponse(asset, null);
    }

    @Override
    public PortfolioAssetResponse updateAsset(Long assetId,
                                              PortfolioAssetRequest request) {

        PortfolioAsset asset = portfolioAssetRepository
                .findByAssetId(assetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        asset.setAssetClassId(request.getAssetClassId());
        asset.setAssetName(request.getAssetName());
        asset.setProviderOrLocation(request.getProviderOrLocation());
        asset.setAssetIdentifier(request.getAssetIdentifier());
        asset.setQuantity(request.getQuantity());
        asset.setUnitValue(request.getUnitValue());
        asset.setAnnualIncome(request.getAnnualIncome());
        asset.setNote(request.getNote());
        asset.setValuationDate(request.getValuationDate());
        asset.setUpdatedAt(LocalDateTime.now());

        PortfolioAsset updatedAsset =
                portfolioAssetRepository.save(asset);

        return mapToResponse(updatedAsset,
                "Asset updated successfully");
    }
    @Override
    public void deleteAsset(Long assetId) {

        PortfolioAsset asset = portfolioAssetRepository
                .findByAssetId(assetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Asset not found"));

        portfolioAssetRepository.delete(asset);
    }

    private PortfolioAssetResponse mapToResponse(PortfolioAsset asset,
                                                 String message) {

        return PortfolioAssetResponse.builder()
                .assetId(asset.getAssetId())
                .portfolioId(asset.getPortfolioId())
                .assetClassId(asset.getAssetClassId())
                .assetName(asset.getAssetName())
                .providerOrLocation(asset.getProviderOrLocation())
                .assetIdentifier(asset.getAssetIdentifier())
                .quantity(asset.getQuantity())
                .unitValue(asset.getUnitValue())
                .currentValue(asset.getCurrentValue())
                .annualIncome(asset.getAnnualIncome())
                .note(asset.getNote())
                .valuationDate(asset.getValuationDate())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .message(message)
                .build();
    }
}