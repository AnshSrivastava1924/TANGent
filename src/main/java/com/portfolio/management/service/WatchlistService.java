package com.portfolio.management.service;

import com.portfolio.management.dto.request.WatchlistRequest;
import com.portfolio.management.dto.response.WatchlistResponse;

import java.util.List;

public interface WatchlistService {

    WatchlistResponse addToWatchlist(WatchlistRequest request);

    List<WatchlistResponse> getWatchlistByUser(Long userId);

    WatchlistResponse getWatchlistById(Long watchlistId);

    void removeFromWatchlist(Long watchlistId);
}