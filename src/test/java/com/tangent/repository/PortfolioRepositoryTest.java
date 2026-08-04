package com.tangent.repository;

import com.tangent.dto.ExpenseResponse;
import com.tangent.dto.PortfolioBootstrapResponse;
import com.tangent.dto.UserSummary;
import com.tangent.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PortfolioRepository} with a mocked {@link JdbcTemplate}. No real database
 * connection is used; {@code RowMapper}/{@code RowCallbackHandler}/{@code ResultSetExtractor}
 * callbacks are either bypassed (by stubbing the resulting list/value directly) or invoked
 * manually against a hand-built {@link ResultSet} mock where the production logic lives inside
 * the callback itself.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioRepositoryTest {

    @Mock
    private JdbcTemplate jdbc;

    private PortfolioRepository repository;

    // ---------- findWorkspace ----------

    @Test
    void should_returnEmptyOptional_when_userDoesNotExist() {
        repository = new PortfolioRepository(jdbc);
        stubRowMapperListFor("FROM users", List.of());

        Optional<PortfolioBootstrapResponse> result = repository.findWorkspace(404L);

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnWorkspaceWithZeroPortfolioId_when_userHasNoPortfolio() {
        repository = new PortfolioRepository(jdbc);
        UserSummary user = new UserSummary(1L, "user@tangent.local", "User Name");
        stubRowMapperListFor("FROM users", List.of(user));
        // asset_classes RowCallbackHandler query: no rows -> nothing to populate
        org.mockito.Mockito.doNothing().when(jdbc).query(contains("asset_classes"), any(RowCallbackHandler.class));
        stubResultSetExtractorFor("portfolio_id", null);
        stubRowMapperListFor("buddy_expenses", List.of());
        stubRowMapperListFor("watchlist_items", List.of());

        Optional<PortfolioBootstrapResponse> result = repository.findWorkspace(1L);

        assertThat(result).isPresent();
        PortfolioBootstrapResponse workspace = result.get();
        assertThat(workspace.user()).isEqualTo(user);
        assertThat(workspace.portfolioId()).isZero();
        assertThat(workspace.portfolioClasses()).isEmpty();
        assertThat(workspace.expenses()).isEmpty();
        assertThat(workspace.watchlist()).isEmpty();
    }

    @Test
    void should_populateAssetClassesAssetsExpensesAndWatchlist_when_dataExists() throws SQLException {
        repository = new PortfolioRepository(jdbc);
        UserSummary user = new UserSummary(1L, "user@tangent.local", "User Name");
        stubRowMapperListFor("FROM users", List.of(user));

        // Asset classes: RowCallbackHandler with no args
        ResultSet classRow = mock(ResultSet.class);
        when(classRow.getLong("asset_class_id")).thenReturn(20L);
        when(classRow.getString("code")).thenReturn("EQUITY");
        when(classRow.getString("display_name")).thenReturn("Equities");
        when(classRow.getString("purpose")).thenReturn("Growth");
        when(classRow.getBoolean("is_liability")).thenReturn(false);
        when(classRow.getBoolean("is_liquid")).thenReturn(true);
        org.mockito.Mockito.doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            handler.processRow(classRow);
            return null;
        }).when(jdbc).query(contains("asset_classes"), any(RowCallbackHandler.class));

        stubResultSetExtractorFor("portfolio_id", 500L);

        // Portfolio assets: RowCallbackHandler with args
        ResultSet assetRow = mock(ResultSet.class);
        when(assetRow.getLong("asset_id")).thenReturn(30L);
        when(assetRow.getLong("asset_class_id")).thenReturn(20L);
        when(assetRow.getString("asset_name")).thenReturn("Dividend ETF");
        when(assetRow.getBigDecimal("current_value")).thenReturn(new BigDecimal("10000"));
        when(assetRow.getBigDecimal("annual_income")).thenReturn(new BigDecimal("400"));
        org.mockito.Mockito.doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            handler.processRow(assetRow);
            return null;
        }).when(jdbc).query(contains("portfolio_assets"), any(RowCallbackHandler.class), eq(500L));

        ExpenseResponse expense = new ExpenseResponse(9L, LocalDate.of(2026, 8, 1), "Groceries", "Food", new BigDecimal("85"));
        stubRowMapperListFor("buddy_expenses", List.of(expense));
        stubRowMapperListFor("watchlist_items", List.of("AAPL"));

        Optional<PortfolioBootstrapResponse> result = repository.findWorkspace(1L);

        assertThat(result).isPresent();
        PortfolioBootstrapResponse workspace = result.get();
        assertThat(workspace.portfolioId()).isEqualTo(500L);
        assertThat(workspace.portfolioClasses()).hasSize(1);
        assertThat(workspace.portfolioClasses().get(0).id()).isEqualTo("EQUITY");
        assertThat(workspace.portfolioClasses().get(0).items()).hasSize(1);
        assertThat(workspace.portfolioClasses().get(0).items().get(0).name()).isEqualTo("Dividend ETF");
        assertThat(workspace.expenses()).containsExactly(expense);
        assertThat(workspace.watchlist()).containsExactly("AAPL");
    }

    // ---------- updateAsset ----------

    @Test
    void should_returnAffectedRowCount_when_updatingAsset() {
        repository = new PortfolioRepository(jdbc);
        when(jdbc.update(anyString(), any(), any(), any(), any())).thenReturn(1);

        int rows = repository.updateAsset(1L, 2L, new BigDecimal("100"), new BigDecimal("10"));

        assertThat(rows).isEqualTo(1);
    }

    @Test
    void should_returnZero_when_updatingAssetThatDoesNotBelongToUser() {
        repository = new PortfolioRepository(jdbc);
        when(jdbc.update(anyString(), any(), any(), any(), any())).thenReturn(0);

        int rows = repository.updateAsset(1L, 2L, new BigDecimal("100"), new BigDecimal("10"));

        assertThat(rows).isZero();
    }

    // ---------- findCategoryId ----------

    @Test
    void should_returnCategoryId_when_categoryExists() {
        repository = new PortfolioRepository(jdbc);
        when(jdbc.query(anyString(), any(ResultSetExtractor.class), eq(1L), eq("Food"))).thenReturn(55L);

        Optional<Long> categoryId = repository.findCategoryId(1L, "Food");

        assertThat(categoryId).contains(55L);
    }

    @Test
    void should_returnEmptyOptional_when_categoryDoesNotExist() {
        repository = new PortfolioRepository(jdbc);
        when(jdbc.query(anyString(), any(ResultSetExtractor.class), eq(1L), eq("Unknown"))).thenReturn(null);

        Optional<Long> categoryId = repository.findCategoryId(1L, "Unknown");

        assertThat(categoryId).isEmpty();
    }

    // ---------- createExpense ----------

    @Test
    void should_returnGeneratedExpenseId_when_createExpenseSucceeds() {
        repository = new PortfolioRepository(jdbc);
        stubGeneratedKey(88L);

        long id = repository.createExpense(1L, 2L, "Groceries", new BigDecimal("85"), LocalDate.of(2026, 8, 2));

        assertThat(id).isEqualTo(88L);
    }

    @Test
    void should_throwInternalServerError_when_createExpenseGeneratesNoKey() {
        repository = new PortfolioRepository(jdbc);

        assertThatThrownBy(() -> repository.createExpense(1L, 2L, "Groceries", new BigDecimal("85"), LocalDate.of(2026, 8, 2)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unable to create expense");
    }

    // ---------- updateExpense ----------

    @Test
    void should_returnAffectedRowCount_when_updatingExpense() {
        repository = new PortfolioRepository(jdbc);
        when(jdbc.update(anyString(), any(), any(), any())).thenReturn(1);

        int rows = repository.updateExpense(1L, 5L, new BigDecimal("50"));

        assertThat(rows).isEqualTo(1);
    }

    // ---------- findInstrumentId ----------

    @Test
    void should_returnInstrumentId_when_symbolExists() {
        repository = new PortfolioRepository(jdbc);
        when(jdbc.query(anyString(), any(ResultSetExtractor.class), eq("AAPL"))).thenReturn(77L);

        Long id = repository.findInstrumentId("AAPL");

        assertThat(id).isEqualTo(77L);
    }

    @Test
    void should_returnNull_when_instrumentSymbolDoesNotExist() {
        repository = new PortfolioRepository(jdbc);
        when(jdbc.query(anyString(), any(ResultSetExtractor.class), eq("ZZZZ"))).thenReturn(null);

        Long id = repository.findInstrumentId("ZZZZ");

        assertThat(id).isNull();
    }

    // ---------- createInstrument ----------

    @Test
    void should_returnGeneratedInstrumentId_when_createInstrumentSucceeds() {
        repository = new PortfolioRepository(jdbc);
        stubGeneratedKey(15L);

        long id = repository.createInstrument("AAPL");

        assertThat(id).isEqualTo(15L);
    }

    @Test
    void should_throwInternalServerError_when_createInstrumentGeneratesNoKey() {
        repository = new PortfolioRepository(jdbc);

        assertThatThrownBy(() -> repository.createInstrument("AAPL"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unable to create instrument");
    }

    // ---------- watchlist ----------

    @Test
    void should_returnWatchlistId_when_userHasWatchlist() {
        repository = new PortfolioRepository(jdbc);
        when(jdbc.query(anyString(), any(ResultSetExtractor.class), eq(1L))).thenReturn(200L);

        Long watchlistId = repository.findWatchlistId(1L);

        assertThat(watchlistId).isEqualTo(200L);
    }

    @Test
    void should_returnGeneratedWatchlistId_when_createWatchlistSucceeds() {
        repository = new PortfolioRepository(jdbc);
        stubGeneratedKey(300L);

        long id = repository.createWatchlist(1L);

        assertThat(id).isEqualTo(300L);
    }

    @Test
    void should_throwInternalServerError_when_createWatchlistGeneratesNoKey() {
        repository = new PortfolioRepository(jdbc);

        assertThatThrownBy(() -> repository.createWatchlist(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unable to create watchlist");
    }

    @Test
    void should_returnTrue_when_watchItemAlreadyExists() {
        repository = new PortfolioRepository(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(200L), eq(50L))).thenReturn(1);

        assertThat(repository.watchItemExists(200L, 50L)).isTrue();
    }

    @Test
    void should_returnFalse_when_watchItemDoesNotExist() {
        repository = new PortfolioRepository(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(200L), eq(50L))).thenReturn(0);

        assertThat(repository.watchItemExists(200L, 50L)).isFalse();
    }

    @Test
    void should_insertWatchItem_when_createWatchItemCalled() {
        repository = new PortfolioRepository(jdbc);

        repository.createWatchItem(200L, 50L);

        verify(jdbc).update(contains("INSERT INTO watchlist_items"), eq(200L), eq(50L));
    }

    @Test
    void should_removeWatchItemWithUppercaseSymbol_when_removeWatchItemCalled() {
        repository = new PortfolioRepository(jdbc);

        repository.removeWatchItem(1L, "aapl");

        verify(jdbc).update(contains("DELETE FROM watchlist_items"), eq(1L), eq("AAPL"));
    }

    // ---------- createStarterWorkspace ----------

    @Test
    void should_createPortfolioAssetsAndCategories_when_creatingStarterWorkspace() {
        repository = new PortfolioRepository(jdbc);
        stubGeneratedKey(999L);

        repository.getOrCreatePortfolio(1L);

        verify(jdbc).query(contains("SELECT portfolio_id FROM portfolios"),
                any(ResultSetExtractor.class), eq(1L));
    }

    @Test
    void should_throwInternalServerError_when_starterWorkspacePortfolioCreationGeneratesNoKey() {
        repository = new PortfolioRepository(jdbc);

        assertThatThrownBy(() -> repository.getOrCreatePortfolio(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unable to create portfolio");
    }

    // ---------- helpers ----------

    @SuppressWarnings("unchecked")
    private void stubRowMapperListFor(String sqlFragment, List<?> results) {
        when(jdbc.query(contains(sqlFragment), any(RowMapper.class), any(Object[].class)))
                .thenReturn((List) results);
    }

    @SuppressWarnings("unchecked")
    private void stubResultSetExtractorFor(String sqlFragment, Long result) {
        when(jdbc.query(contains(sqlFragment), any(ResultSetExtractor.class), any(Object[].class)))
                .thenReturn(result);
    }

    @SuppressWarnings("unchecked")
    private void stubGeneratedKey(long generatedKey) {
        when(jdbc.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(invocation -> {
            KeyHolder holder = invocation.getArgument(1);
            holder.getKeyList().add(Map.of("id", generatedKey));
            return 1;
        });
    }
}




