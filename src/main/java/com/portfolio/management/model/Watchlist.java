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
@Table("watchlists")
public class Watchlist {

    @Id
    private Long watchlistId;

    private Long userId;

    private Long assetId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}