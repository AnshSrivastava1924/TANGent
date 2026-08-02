package com.tangent.service;

import com.tangent.dto.ExpenseCreateRequest;
import com.tangent.dto.ExpenseResponse;
import com.tangent.dto.PortfolioBootstrapResponse;
import com.tangent.exception.ApiException;
import com.tangent.repository.PortfolioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

import static com.tangent.constant.ApplicationConstants.SYMBOL_PATTERN;

@Service
public class PortfolioService {

    private final PortfolioRepository repository;
    private final MarketDataService marketDataService;

    public PortfolioService(PortfolioRepository repository, MarketDataService marketDataService) {
        this.repository = repository;
        this.marketDataService = marketDataService;
    }

    @Transactional(readOnly = true)
    public PortfolioBootstrapResponse bootstrap(long userId) {
        return repository.findWorkspace(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User workspace not found"));
    }

    @Transactional
    public void updateAsset(long userId, long assetId, BigDecimal value, BigDecimal income) {
        if (repository.updateAsset(userId, assetId, value, income) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Asset not found");
        }
    }

    @Transactional
    public ExpenseResponse addExpense(long userId, ExpenseCreateRequest request) {
        long categoryId = repository.findCategoryId(userId, request.category())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown expense category"));
        long expenseId = repository.createExpense(userId, categoryId, request.name(),
                request.amount(), request.date());
        return new ExpenseResponse(expenseId, request.date(), request.name(),
                request.category(), request.amount());
    }

    @Transactional
    public void updateExpense(long userId, long expenseId, BigDecimal amount) {
        if (repository.updateExpense(userId, expenseId, amount) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Expense not found");
        }
    }

    @Transactional
    public String addWatchSymbol(long userId, String requestedSymbol) {
        String symbol = normalizeSymbol(requestedSymbol);
        if (marketDataService.isConfigured()) {
            marketDataService.validateSymbol(symbol);
        }
        Long instrumentId = repository.findInstrumentId(symbol);
        if (instrumentId == null) instrumentId = repository.createInstrument(symbol);
        Long watchlistId = repository.findWatchlistId(userId);
        if (watchlistId == null) watchlistId = repository.createWatchlist(userId);
        if (!repository.watchItemExists(watchlistId, instrumentId)) {
            repository.createWatchItem(watchlistId, instrumentId);
        }
        return symbol;
    }

    @Transactional
    public void removeWatchSymbol(long userId, String requestedSymbol) {
        repository.removeWatchItem(userId, normalizeSymbol(requestedSymbol));
    }

    private String normalizeSymbol(String value) {
        String symbol = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!symbol.matches(SYMBOL_PATTERN)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid symbol");
        }
        return symbol;
    }
}
