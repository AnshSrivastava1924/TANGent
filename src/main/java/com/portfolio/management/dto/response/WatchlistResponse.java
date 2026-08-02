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
public class WatchlistResponse {

    private Long watchlistId;

    private Long userId;

    private Long assetId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String message;
}