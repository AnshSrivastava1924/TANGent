package com.portfolio.management.service.impl;

import com.portfolio.management.dto.request.WatchlistRequest;
import com.portfolio.management.dto.response.WatchlistResponse;
import com.portfolio.management.model.Watchlist;
import com.portfolio.management.repository.WatchlistRepository;
import com.portfolio.management.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistRepository watchlistRepository;

    @Override
    public WatchlistResponse addToWatchlist(WatchlistRequest request) {

        Watchlist watchlist = Watchlist.builder()
                .userId(request.getUserId())
                .assetId(request.getAssetId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Watchlist saved = watchlistRepository.save(watchlist);

        return mapToResponse(saved);
    }

    @Override
    public List<WatchlistResponse> getWatchlistByUser(Long userId) {

        return watchlistRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public WatchlistResponse getWatchlistById(Long watchlistId) {

        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new RuntimeException("Watchlist not found"));

        return mapToResponse(watchlist);
    }

    @Override
    public void removeFromWatchlist(Long watchlistId) {

        watchlistRepository.deleteById(watchlistId);
    }

    private WatchlistResponse mapToResponse(Watchlist watchlist) {

        return WatchlistResponse.builder()
                .watchlistId(watchlist.getWatchlistId())
                .userId(watchlist.getUserId())
                .assetId(watchlist.getAssetId())
                .createdAt(watchlist.getCreatedAt())
                .updatedAt(watchlist.getUpdatedAt())
                .build();
    }
}