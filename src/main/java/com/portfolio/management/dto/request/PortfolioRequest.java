package com.portfolio.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortfolioRequest {

    @NotBlank(message = "Portfolio name is required")
    @Size(max = 160, message = "Portfolio name cannot exceed 160 characters")
    private String portfolioName;

    @Size(max = 255, message = "Goal description cannot exceed 255 characters")
    private String goalDescription;
}