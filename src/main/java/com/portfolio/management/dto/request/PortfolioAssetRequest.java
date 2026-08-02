package com.portfolio.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PortfolioAssetRequest {

    @NotNull(message = "Portfolio ID is required")
    private Long portfolioId;

    @NotNull(message = "Asset Class ID is required")
    private Long assetClassId;

    @NotBlank(message = "Asset Name is required")
    private String assetName;

    private String providerOrLocation;

    private String assetIdentifier;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal quantity;

    @NotNull(message = "Unit Value is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal unitValue;

    @DecimalMin(value = "0.0")
    private BigDecimal annualIncome;

    private String note;

    @NotNull(message = "Valuation Date is required")
    private LocalDate valuationDate;
}