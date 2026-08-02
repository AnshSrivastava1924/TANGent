package com.portfolio.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioAssetResponse {

    private Long assetId;
    private Long portfolioId;
    private Long assetClassId;

    private String assetName;
    private String providerOrLocation;
    private String assetIdentifier;

    private BigDecimal quantity;
    private BigDecimal unitValue;
    private BigDecimal currentValue;
    private BigDecimal annualIncome;

    private String note;

    private LocalDate valuationDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String message;
}