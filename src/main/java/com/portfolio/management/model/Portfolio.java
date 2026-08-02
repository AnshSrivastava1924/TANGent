package com.portfolio.management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("portfolios")
public class Portfolio {

    @Id
    private Long portfolioId;

    private Long userId;

    private String portfolioName;

    private String goalDescription;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}