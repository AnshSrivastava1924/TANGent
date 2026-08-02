package com.portfolio.management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("portfolio_assets")
public class PortfolioAsset {

    @Id
    private Long assetId;

    private Long portfolioId;

    private Long assetClassId;

    private String assetName;

    private String providerOrLocation;

    private String assetIdentifier;

    private BigDecimal quantity;

    private BigDecimal unitValue;

    @ReadOnlyProperty
    private BigDecimal currentValue;

    private BigDecimal annualIncome;

    private String note;

    private LocalDate valuationDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}