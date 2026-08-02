package com.portfolio.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponse {

    private Long portfolioId;

    private Long userId;

    private String portfolioName;

    private String goalDescription;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String message;
}