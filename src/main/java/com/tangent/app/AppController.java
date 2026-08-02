package com.tangent.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/app")
public class AppController {

    private final AppDataService data;

    public AppController(AppDataService data) {
        this.data = data;
    }

    @GetMapping("/bootstrap")
    @Operation(summary = "Load the signed-in user's portfolio, Buddy data, and watchlist")
    public Map<String, Object> bootstrap(Authentication authentication) {
        return data.bootstrap(userId(authentication));
    }

    @PutMapping("/assets/{assetId}")
    @Operation(summary = "Update a portfolio asset value and annual income")
    public ResponseEntity<Void> updateAsset(Authentication authentication, @PathVariable long assetId,
                                            @Valid @RequestBody AssetUpdate request) {
        data.updateAsset(userId(authentication), assetId, request.value(), request.income());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/expenses")
    @Operation(summary = "Add a Buddy expense")
    public ResponseEntity<Map<String, Object>> addExpense(Authentication authentication,
                                                           @Valid @RequestBody ExpenseCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(data.addExpense(
                userId(authentication), request.name(), request.category(), request.amount(), request.date()));
    }

    @PutMapping("/expenses/{expenseId}")
    @Operation(summary = "Update a Buddy expense amount")
    public ResponseEntity<Void> updateExpense(Authentication authentication, @PathVariable long expenseId,
                                              @Valid @RequestBody ExpenseUpdate request) {
        data.updateExpense(userId(authentication), expenseId, request.amount());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/watchlist")
    @Operation(summary = "Add a stock symbol to the user's watchlist")
    public ResponseEntity<Void> addWatchlist(Authentication authentication,
                                             @Valid @RequestBody WatchSymbol request) {
        data.addWatchSymbol(userId(authentication), request.symbol());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/watchlist/{symbol}")
    @Operation(summary = "Remove a stock symbol from the user's watchlist")
    public ResponseEntity<Void> removeWatchlist(Authentication authentication, @PathVariable String symbol) {
        data.removeWatchSymbol(userId(authentication), symbol);
        return ResponseEntity.noContent().build();
    }

    private long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    public record AssetUpdate(
            @Schema(example = "50000") @NotNull @DecimalMin("0") BigDecimal value,
            @Schema(example = "1800") @NotNull @DecimalMin("0") BigDecimal income
    ) {}

    public record ExpenseCreate(
            @Schema(example = "Groceries") @NotBlank String name,
            @Schema(example = "Food") @NotBlank String category,
            @Schema(example = "85") @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
            @Schema(example = "2026-08-02") @NotNull LocalDate date
    ) {}

    public record ExpenseUpdate(
            @Schema(example = "90") @NotNull @DecimalMin("0") BigDecimal amount
    ) {}

    public record WatchSymbol(@Schema(example = "AAPL") @NotBlank String symbol) {}
}
