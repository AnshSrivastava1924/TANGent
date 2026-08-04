package com.tangent.service;

import com.tangent.dto.AssetCreateRequest;
import com.tangent.dto.AssetCreateResponse;
import com.tangent.dto.ExpenseCreateRequest;
import com.tangent.dto.ExpenseResponse;
import com.tangent.dto.PortfolioBootstrapResponse;
import com.tangent.exception.ApiException;
import com.tangent.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PortfolioService}. Repository and market-data collaborators are mocked.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository repository;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private PortfolioService portfolioService;

    private static final long USER_ID = 42L;

    @Test
    void should_returnWorkspace_when_userExists() {
        PortfolioBootstrapResponse workspace = new PortfolioBootstrapResponse(null, 1L, List.of(), List.of(), List.of());
        when(repository.findWorkspace(USER_ID)).thenReturn(Optional.of(workspace));

        PortfolioBootstrapResponse result = portfolioService.bootstrap(USER_ID);

        assertThat(result).isSameAs(workspace);
        verify(repository).findWorkspace(USER_ID);
    }

    @Test
    void should_throwNotFound_when_userWorkspaceDoesNotExist() {
        when(repository.findWorkspace(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.bootstrap(USER_ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User workspace not found")
                .satisfies(exception -> assertThat(((ApiException) exception).status()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void should_updateAsset_when_repositoryUpdatesOneRow() {
        BigDecimal value = new BigDecimal("1000");
        BigDecimal income = new BigDecimal("50");
        when(repository.updateAsset(USER_ID, 1L, value, income)).thenReturn(1);

        portfolioService.updateAsset(USER_ID, 1L, value, income);

        verify(repository, times(1)).updateAsset(USER_ID, 1L, value, income);
    }

    @Test
    void should_throwNotFound_when_updateAssetAffectsNoRows() {
        BigDecimal value = new BigDecimal("1000");
        BigDecimal income = new BigDecimal("50");
        when(repository.updateAsset(USER_ID, 1L, value, income)).thenReturn(0);

        assertThatThrownBy(() -> portfolioService.updateAsset(USER_ID, 1L, value, income))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Asset not found")
                .satisfies(exception -> assertThat(((ApiException) exception).status()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void should_createAsset_when_assetClassExists() {
        AssetCreateRequest request = new AssetCreateRequest("Retirement cash", 3L,
                BigDecimal.ONE, new BigDecimal("2500.50"), new BigDecimal("100"));
        when(repository.assetClassExists(3L)).thenReturn(true);
        when(repository.getOrCreatePortfolio(USER_ID)).thenReturn(8L);
        when(repository.createAsset(8L, 3L, "Retirement cash", BigDecimal.ONE,
                new BigDecimal("2500.50"), new BigDecimal("100"))).thenReturn(15L);
        when(repository.getAssetClassName(3L)).thenReturn("Cash");

        AssetCreateResponse response = portfolioService.createAsset(USER_ID, request);

        assertThat(response.assetId()).isEqualTo(15L);
        assertThat(response.currentValue()).isEqualByComparingTo("2500.50");
    }

    @Test
    void should_rejectAsset_when_assetClassDoesNotExist() {
        AssetCreateRequest request = new AssetCreateRequest("Unknown", 999L,
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO);
        when(repository.assetClassExists(999L)).thenReturn(false);

        assertThatThrownBy(() -> portfolioService.createAsset(USER_ID, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown asset class");

        verify(repository, never()).createAsset(anyLong(), anyLong(), anyString(), any(), any(), any());
    }

    @Test
    void should_addExpense_when_categoryExists() {
        ExpenseCreateRequest request = new ExpenseCreateRequest("Groceries", "Food",
                new BigDecimal("85"), LocalDate.of(2026, 8, 2));
        when(repository.findCategoryId(USER_ID, "Food")).thenReturn(Optional.of(7L));
        when(repository.createExpense(USER_ID, 7L, "Groceries", new BigDecimal("85"), LocalDate.of(2026, 8, 2)))
                .thenReturn(99L);

        ExpenseResponse response = portfolioService.addExpense(USER_ID, request);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.name()).isEqualTo("Groceries");
        assertThat(response.category()).isEqualTo("Food");
        assertThat(response.amount()).isEqualByComparingTo("85");
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 8, 2));
    }

    @Test
    void should_throwBadRequest_when_expenseCategoryIsUnknown() {
        ExpenseCreateRequest request = new ExpenseCreateRequest("Groceries", "UnknownCategory",
                new BigDecimal("85"), LocalDate.of(2026, 8, 2));
        when(repository.findCategoryId(USER_ID, "UnknownCategory")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.addExpense(USER_ID, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown expense category")
                .satisfies(exception -> assertThat(((ApiException) exception).status()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(repository, never()).createExpense(anyLong(), anyLong(), anyString(), any(), any());
    }

    @Test
    void should_updateExpense_when_repositoryUpdatesOneRow() {
        when(repository.updateExpense(USER_ID, 3L, new BigDecimal("90"))).thenReturn(1);

        portfolioService.updateExpense(USER_ID, 3L, new BigDecimal("90"));

        verify(repository).updateExpense(USER_ID, 3L, new BigDecimal("90"));
    }

    @Test
    void should_throwNotFound_when_updateExpenseAffectsNoRows() {
        when(repository.updateExpense(USER_ID, 3L, new BigDecimal("90"))).thenReturn(0);

        assertThatThrownBy(() -> portfolioService.updateExpense(USER_ID, 3L, new BigDecimal("90")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Expense not found");
    }

    @Test
    void should_addWatchSymbol_when_symbolIsNewAndMarketNotConfigured() {
        when(marketDataService.isConfigured()).thenReturn(false);
        when(repository.findInstrumentId("AAPL")).thenReturn(null);
        when(repository.createInstrument("AAPL")).thenReturn(10L);
        when(repository.findWatchlistId(USER_ID)).thenReturn(null);
        when(repository.createWatchlist(USER_ID)).thenReturn(20L);
        when(repository.watchItemExists(20L, 10L)).thenReturn(false);

        String symbol = portfolioService.addWatchSymbol(USER_ID, " aapl ");

        assertThat(symbol).isEqualTo("AAPL");
        verify(marketDataService, never()).validateSymbol(anyString());
        verify(repository).createInstrument("AAPL");
        verify(repository).createWatchlist(USER_ID);
        verify(repository).createWatchItem(20L, 10L);
    }

    @Test
    void should_validateSymbolWithMarketData_when_marketDataIsConfigured() {
        when(marketDataService.isConfigured()).thenReturn(true);
        when(repository.findInstrumentId("AAPL")).thenReturn(5L);
        when(repository.findWatchlistId(USER_ID)).thenReturn(20L);
        when(repository.watchItemExists(20L, 5L)).thenReturn(false);

        portfolioService.addWatchSymbol(USER_ID, "AAPL");

        verify(marketDataService).validateSymbol("AAPL");
        verify(repository, never()).createInstrument(anyString());
        verify(repository, never()).createWatchlist(anyLong());
        verify(repository).createWatchItem(20L, 5L);
    }

    @Test
    void should_notCreateWatchItem_when_itAlreadyExists() {
        when(marketDataService.isConfigured()).thenReturn(false);
        when(repository.findInstrumentId("AAPL")).thenReturn(5L);
        when(repository.findWatchlistId(USER_ID)).thenReturn(20L);
        when(repository.watchItemExists(20L, 5L)).thenReturn(true);

        portfolioService.addWatchSymbol(USER_ID, "AAPL");

        verify(repository, never()).createWatchItem(anyLong(), anyLong());
    }

    @Test
    void should_propagateException_when_marketDataValidationFails() {
        when(marketDataService.isConfigured()).thenReturn(true);
        org.mockito.Mockito.doThrow(new ApiException(HttpStatus.BAD_REQUEST, "Ticker symbol was not found by the market provider"))
                .when(marketDataService).validateSymbol("BADSYM");

        assertThatThrownBy(() -> portfolioService.addWatchSymbol(USER_ID, "BADSYM"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Ticker symbol was not found");

        verify(repository, never()).findInstrumentId(anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "INVALID SYMBOL!", "TOO-LONG-SYMBOL-1234567890"})
    void should_throwBadRequest_when_watchSymbolIsInvalid(String invalidSymbol) {
        assertThatThrownBy(() -> portfolioService.addWatchSymbol(USER_ID, invalidSymbol))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid symbol")
                .satisfies(exception -> assertThat(((ApiException) exception).status()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(marketDataService);
    }

    @Test
    void should_removeWatchSymbol_when_symbolIsValid() {
        portfolioService.removeWatchSymbol(USER_ID, "aapl");

        verify(repository).removeWatchItem(USER_ID, "AAPL");
    }

    @Test
    void should_throwBadRequest_when_removeWatchSymbolIsInvalid() {
        assertThatThrownBy(() -> portfolioService.removeWatchSymbol(USER_ID, "bad symbol!"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid symbol");

        verify(repository, never()).removeWatchItem(anyLong(), anyString());
    }

    @BeforeEach
    void setUp() {
        // no-op; kept for future common stubbing needs
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}



