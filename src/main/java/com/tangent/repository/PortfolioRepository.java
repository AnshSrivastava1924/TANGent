package com.tangent.repository;

import com.tangent.dto.ExpenseResponse;
import com.tangent.dto.PortfolioAssetResponse;
import com.tangent.dto.PortfolioBootstrapResponse;
import com.tangent.dto.PortfolioClassResponse;
import com.tangent.dto.UserSummary;
import com.tangent.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.tangent.constant.ApplicationConstants.EXPENSE_CATEGORIES;

@Repository
public class PortfolioRepository {

    private final JdbcTemplate jdbc;

    public PortfolioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<PortfolioBootstrapResponse> findWorkspace(long userId) {
        Optional<UserSummary> user = jdbc.query(
                "SELECT user_id, email, full_name FROM users WHERE user_id = ?",
                (rs, row) -> new UserSummary(rs.getLong(1), rs.getString(2), rs.getString(3)), userId)
                .stream().findFirst();
        if (user.isEmpty()) return Optional.empty();

        Map<Long, MutableClass> classes = new LinkedHashMap<>();
        jdbc.query("""
                SELECT asset_class_id, code, display_name, purpose, is_liability, is_liquid
                FROM asset_classes ORDER BY sort_order
                """, rs -> {
            long classId = rs.getLong("asset_class_id");
            classes.put(classId, new MutableClass(classId, rs.getString("code"),
                    rs.getString("display_name"), rs.getString("purpose"),
                    rs.getBoolean("is_liability"), rs.getBoolean("is_liquid")));
        });

        Long portfolioId = jdbc.query("SELECT portfolio_id FROM portfolios WHERE user_id = ? ORDER BY portfolio_id LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null, userId);
        if (portfolioId != null) {
            jdbc.query("""
                    SELECT asset_id, asset_class_id, asset_name, current_value, annual_income
                    FROM portfolio_assets WHERE portfolio_id = ? ORDER BY asset_id
                    """, rs -> {
                MutableClass assetClass = classes.get(rs.getLong("asset_class_id"));
                if (assetClass != null) {
                    assetClass.items.add(new PortfolioAssetResponse(rs.getLong("asset_id"),
                            rs.getString("asset_name"), rs.getBigDecimal("current_value"),
                            rs.getBigDecimal("annual_income"), ""));
                }
            }, portfolioId);
        }

        List<ExpenseResponse> expenses = jdbc.query("""
                SELECT be.expense_id, be.spent_on, be.expense_name, bc.category_name, be.amount
                FROM buddy_expenses be
                JOIN buddy_categories bc ON bc.buddy_category_id = be.buddy_category_id
                WHERE be.user_id = ? ORDER BY be.spent_on DESC, be.expense_id DESC
                """, (rs, row) -> new ExpenseResponse(rs.getLong("expense_id"),
                rs.getDate("spent_on").toLocalDate(), rs.getString("expense_name"),
                rs.getString("category_name"), rs.getBigDecimal("amount")), userId);

        List<String> watchlist = jdbc.query("""
                SELECT i.symbol FROM watchlists w
                JOIN watchlist_items wi ON wi.watchlist_id = w.watchlist_id
                JOIN instruments i ON i.instrument_id = wi.instrument_id
                WHERE w.user_id = ? ORDER BY wi.added_at, wi.watchlist_item_id
                """, (rs, row) -> rs.getString(1), userId);

        List<PortfolioClassResponse> responses = classes.values().stream()
                .map(MutableClass::toResponse).toList();
        return Optional.of(new PortfolioBootstrapResponse(user.get(), portfolioId == null ? 0 : portfolioId,
                responses, expenses, watchlist));
    }

    public int updateAsset(long userId, long assetId, BigDecimal value, BigDecimal income) {
        return jdbc.update("""
                UPDATE portfolio_assets pa
                SET unit_value = ? / CASE WHEN quantity = 0 THEN 1 ELSE quantity END,
                    annual_income = ?, updated_at = CURRENT_TIMESTAMP
                WHERE pa.asset_id = ? AND EXISTS (
                    SELECT 1 FROM portfolios p WHERE p.portfolio_id = pa.portfolio_id AND p.user_id = ?)
                """, value, income, assetId, userId);
    }

    public Optional<Long> findCategoryId(long userId, String category) {
        return Optional.ofNullable(jdbc.query("""
                SELECT buddy_category_id FROM buddy_categories WHERE user_id = ? AND category_name = ?
                """, rs -> rs.next() ? rs.getLong(1) : null, userId, category));
    }

    public long createExpense(long userId, long categoryId, String name, BigDecimal amount, LocalDate date) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO buddy_expenses
                        (user_id, buddy_category_id, expense_name, amount, spent_on, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, new String[]{"expense_id"});
            statement.setLong(1, userId);
            statement.setLong(2, categoryId);
            statement.setString(3, name);
            statement.setBigDecimal(4, amount);
            statement.setObject(5, date);
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create expense");
        return key.longValue();
    }

    public int updateExpense(long userId, long expenseId, BigDecimal amount) {
        return jdbc.update("""
                UPDATE buddy_expenses SET amount = ?, updated_at = CURRENT_TIMESTAMP
                WHERE expense_id = ? AND user_id = ?
                """, amount, expenseId, userId);
    }

    public Long findInstrumentId(String symbol) {
        return jdbc.query("SELECT instrument_id FROM instruments WHERE symbol = ?",
                rs -> rs.next() ? rs.getLong(1) : null, symbol);
    }

    public long createInstrument(String symbol) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO instruments (symbol, instrument_name, asset_type, currency, is_major, created_at)
                    VALUES (?, ?, 'stock', 'USD', FALSE, CURRENT_TIMESTAMP)
                    """, new String[]{"instrument_id"});
            statement.setString(1, symbol);
            statement.setString(2, symbol);
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create instrument");
        return key.longValue();
    }

    public Long findWatchlistId(long userId) {
        return jdbc.query("SELECT watchlist_id FROM watchlists WHERE user_id = ? ORDER BY watchlist_id LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null, userId);
    }

    public long createWatchlist(long userId) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO watchlists (user_id, watchlist_name) VALUES (?, 'Default Watchlist')",
                    new String[]{"watchlist_id"});
            statement.setLong(1, userId);
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create watchlist");
        return key.longValue();
    }

    public boolean watchItemExists(long watchlistId, long instrumentId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM watchlist_items WHERE watchlist_id = ? AND instrument_id = ?
                """, Integer.class, watchlistId, instrumentId);
        return count != null && count > 0;
    }

    public void createWatchItem(long watchlistId, long instrumentId) {
        jdbc.update("""
                INSERT INTO watchlist_items (watchlist_id, instrument_id, added_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """, watchlistId, instrumentId);
    }

    public void removeWatchItem(long userId, String symbol) {
        jdbc.update("""
                DELETE FROM watchlist_items
                WHERE watchlist_id IN (SELECT watchlist_id FROM watchlists WHERE user_id = ?)
                  AND instrument_id IN (SELECT instrument_id FROM instruments WHERE symbol = ?)
                """, userId, symbol.toUpperCase(Locale.ROOT));
    }

    public long getOrCreatePortfolio(long userId) {
        Long portfolioId = jdbc.query("SELECT portfolio_id FROM portfolios WHERE user_id = ? ORDER BY portfolio_id LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null, userId);
        if (portfolioId != null) return portfolioId;

        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO portfolios (user_id, portfolio_name, goal_description) VALUES (?, ?, ?)",
                    new String[]{"portfolio_id"});
            statement.setLong(1, userId);
            statement.setString(2, "My Portfolio");
            statement.setString(3, "My retirement portfolio");
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create portfolio");
        return key.longValue();
    }

    public void createDefaultExpenseCategories(long userId) {
        for (int index = 0; index < EXPENSE_CATEGORIES.size(); index++) {
            String category = EXPENSE_CATEGORIES.get(index);
            String color = List.of("#007AFF", "#30D158", "#FF9500", "#FF3B30",
                    "#64D2FF", "#5856D6", "#8E8E93").get(index);
            jdbc.update("""
                    INSERT INTO buddy_categories
                        (user_id, category_name, monthly_budget, color_hex, created_at)
                    VALUES (?, ?, 0, ?, CURRENT_TIMESTAMP)
                    """, userId, category, color);
        }
    }

    public boolean assetClassExists(long assetClassId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM asset_classes WHERE asset_class_id = ?",
                Integer.class, assetClassId);
        return count != null && count > 0;
    }

    public long createAsset(long portfolioId, long assetClassId, String assetName,
                           BigDecimal quantity, BigDecimal unitValue, BigDecimal annualIncome) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO portfolio_assets
                        (portfolio_id, asset_class_id, asset_name, quantity, unit_value, annual_income, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, new String[]{"asset_id"});
            statement.setLong(1, portfolioId);
            statement.setLong(2, assetClassId);
            statement.setString(3, assetName);
            statement.setBigDecimal(4, quantity);
            statement.setBigDecimal(5, unitValue);
            statement.setBigDecimal(6, annualIncome);
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create asset");
        return key.longValue();
    }

    public int deleteAsset(long userId, long assetId) {
        return jdbc.update("""
                DELETE FROM portfolio_assets
                WHERE asset_id = ? AND EXISTS (
                    SELECT 1 FROM portfolios p WHERE p.portfolio_id = portfolio_assets.portfolio_id AND p.user_id = ?)
                """, assetId, userId);
    }

    public String getAssetClassName(long assetClassId) {
        return jdbc.query("SELECT display_name FROM asset_classes WHERE asset_class_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, assetClassId);
    }

    private static final class MutableClass {
        private final long classId;
        private final String id;
        private final String name;
        private final String purpose;
        private final boolean liability;
        private final boolean liquid;
        private final List<PortfolioAssetResponse> items = new ArrayList<>();

        private MutableClass(long classId, String id, String name, String purpose,
                             boolean liability, boolean liquid) {
            this.classId = classId;
            this.id = id;
            this.name = name;
            this.purpose = purpose;
            this.liability = liability;
            this.liquid = liquid;
        }

        private PortfolioClassResponse toResponse() {
            return new PortfolioClassResponse(classId, id, name, purpose, liability, liquid, List.copyOf(items));
        }
    }

}
