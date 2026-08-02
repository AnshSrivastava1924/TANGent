package com.portfolio.management.controller;

import com.portfolio.management.dto.request.WatchlistRequest;
import com.portfolio.management.dto.response.WatchlistResponse;
import com.portfolio.management.service.WatchlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @PostMapping
    public ResponseEntity<WatchlistResponse> addToWatchlist(
            @Valid @RequestBody WatchlistRequest request) {

        return new ResponseEntity<>(
                watchlistService.addToWatchlist(request),
                HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WatchlistResponse>> getWatchlistByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                watchlistService.getWatchlistByUser(userId));
    }

    @GetMapping("/{watchlistId}")
    public ResponseEntity<WatchlistResponse> getWatchlistById(
            @PathVariable Long watchlistId) {

        return ResponseEntity.ok(
                watchlistService.getWatchlistById(watchlistId));
    }

    @DeleteMapping("/{watchlistId}")
    public ResponseEntity<String> removeFromWatchlist(
            @PathVariable Long watchlistId) {

        watchlistService.removeFromWatchlist(watchlistId);

        return ResponseEntity.ok("Removed from watchlist successfully");
    }
}