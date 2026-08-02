package com.portfolio.management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchlistRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Asset ID is required")
    private Long assetId;
}