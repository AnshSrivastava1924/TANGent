package com.tangent.controller;

import com.tangent.dto.AssetUpdateRequest;
import com.tangent.dto.ExpenseCreateRequest;
import com.tangent.dto.ExpenseResponse;
import com.tangent.dto.ExpenseUpdateRequest;
import com.tangent.dto.PortfolioBootstrapResponse;
import com.tangent.dto.WatchSymbolRequest;
import com.tangent.service.PortfolioService;
import com.tangent.wrapper.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/app")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/bootstrap")
    @Operation(summary = "Load the signed-in user's portfolio, Buddy data, and watchlist")
    public ApiResponse<PortfolioBootstrapResponse> bootstrap(Authentication authentication) {
        return ApiResponse.success(portfolioService.bootstrap(userId(authentication)));
    }

    @PutMapping("/assets/{assetId}")
    @Operation(summary = "Update a portfolio asset value and annual income")
    public ResponseEntity<Void> updateAsset(Authentication authentication, @PathVariable long assetId,
                                            @Valid @RequestBody AssetUpdateRequest request) {
        portfolioService.updateAsset(userId(authentication), assetId, request.value(), request.income());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/expenses")
    @Operation(summary = "Add a Buddy expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> addExpense(Authentication authentication,
                                                                   @Valid @RequestBody ExpenseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created", portfolioService.addExpense(userId(authentication), request)));
    }

    @PutMapping("/expenses/{expenseId}")
    @Operation(summary = "Update a Buddy expense amount")
    public ResponseEntity<Void> updateExpense(Authentication authentication, @PathVariable long expenseId,
                                              @Valid @RequestBody ExpenseUpdateRequest request) {
        portfolioService.updateExpense(userId(authentication), expenseId, request.amount());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/watchlist")
    @Operation(summary = "Add a stock symbol to the user's watchlist")
    public ResponseEntity<ApiResponse<Map<String, String>>> addWatchlist(
            Authentication authentication, @Valid @RequestBody WatchSymbolRequest request) {
        String symbol = portfolioService.addWatchSymbol(userId(authentication), request.symbol());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Symbol added", Map.of("symbol", symbol)));
    }

    @DeleteMapping("/watchlist/{symbol}")
    @Operation(summary = "Remove a stock symbol from the user's watchlist")
    public ResponseEntity<Void> removeWatchlist(Authentication authentication, @PathVariable String symbol) {
        portfolioService.removeWatchSymbol(userId(authentication), symbol);
        return ResponseEntity.noContent().build();
    }

    private long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
