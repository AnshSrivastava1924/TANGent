package com.tangent.app;

import com.tangent.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AppDataService {

    private final JdbcTemplate jdbc;

    public AppDataService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> bootstrap(long userId) {
        Map<String, Object> user = jdbc.queryForObject(
                "SELECT user_id, email, full_name FROM users WHERE user_id = ?",
                (rs, row) -> Map.of("id", rs.getLong(1), "email", rs.getString(2), "fullName", rs.getString(3)), userId);

        List<Map<String, Object>> classes = jdbc.query("""
                        SELECT ac.asset_class_id, ac.code, ac.display_name, ac.purpose,
                               ac.is_liability, ac.is_liquid
                        FROM asset_classes ac ORDER BY ac.sort_order
                        """, (rs, row) -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("classId", rs.getLong("asset_class_id"));
                    value.put("id", rs.getString("code"));
                    value.put("name", rs.getString("display_name"));
                    value.put("purpose", rs.getString("purpose"));
                    value.put("isLiability", rs.getBoolean("is_liability"));
                    value.put("isLiquid", rs.getBoolean("is_liquid"));
                    value.put("items", new ArrayList<Map<String, Object>>());
                    return value;
                });

        Long portfolioId = jdbc.query("SELECT portfolio_id FROM portfolios WHERE user_id = ? ORDER BY portfolio_id LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null, userId);
        Map<Long, Map<String, Object>> classById = new LinkedHashMap<>();
        classes.forEach(item -> classById.put((Long) item.get("classId"), item));
        if (portfolioId != null) {
            jdbc.query("""
                            SELECT asset_id, asset_class_id, asset_name, current_value, annual_income, note
                            FROM portfolio_assets WHERE portfolio_id = ? ORDER BY asset_id
                            """, rs -> {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> items = (List<Map<String, Object>>) classById
                                .get(rs.getLong("asset_class_id")).get("items");
                        items.add(Map.of(
                                "id", rs.getLong("asset_id"),
                                "name", rs.getString("asset_name"),
                                "value", rs.getBigDecimal("current_value"),
                                "income", rs.getBigDecimal("annual_income"),
                                "note", rs.getString("note") == null ? "" : rs.getString("note")
                        ));
                    }, portfolioId);
        }

        List<Map<String, Object>> expenses = jdbc.query("""
                        SELECT be.expense_id, be.spent_on, be.expense_name, bc.category_name, be.amount
                        FROM buddy_expenses be
                        JOIN buddy_categories bc ON bc.buddy_category_id = be.buddy_category_id
                        WHERE be.user_id = ? ORDER BY be.spent_on DESC, be.expense_id DESC
                        """, (rs, row) -> Map.of(
                    "id", rs.getLong("expense_id"),
                    "date", rs.getDate("spent_on").toLocalDate().toString(),
                    "name", rs.getString("expense_name"),
                    "category", rs.getString("category_name"),
                    "amount", rs.getBigDecimal("amount")
                ), userId);

        List<String> watchlist = jdbc.query("""
                        SELECT i.symbol FROM watchlists w
                        JOIN watchlist_items wi ON wi.watchlist_id = w.watchlist_id
                        JOIN instruments i ON i.instrument_id = wi.instrument_id
                        WHERE w.user_id = ? ORDER BY wi.added_at, wi.watchlist_item_id
                        """, (rs, row) -> rs.getString(1), userId);

        return Map.of(
                "user", user,
                "portfolioId", portfolioId == null ? 0 : portfolioId,
                "portfolioClasses", classes,
                "expenses", expenses,
                "watchlist", watchlist
        );
    }

    public void updateAsset(long userId, long assetId, BigDecimal value, BigDecimal income) {
        int updated = jdbc.update("""
                UPDATE portfolio_assets pa
                SET unit_value = ? / CASE WHEN quantity = 0 THEN 1 ELSE quantity END,
                    annual_income = ?, valuation_date = CURRENT_DATE, updated_at = CURRENT_TIMESTAMP
                WHERE pa.asset_id = ? AND EXISTS (
                    SELECT 1 FROM portfolios p WHERE p.portfolio_id = pa.portfolio_id AND p.user_id = ?)
                """, value, income, assetId, userId);
        if (updated == 0) throw new ApiException(HttpStatus.NOT_FOUND, "Asset not found");
    }

    @Transactional
    public Map<String, Object> addExpense(long userId, String name, String category,
                                          BigDecimal amount, LocalDate date) {
        Long categoryId = jdbc.query("""
                        SELECT buddy_category_id FROM buddy_categories
                        WHERE user_id = ? AND category_name = ?
                        """, rs -> rs.next() ? rs.getLong(1) : null, userId, category);
        if (categoryId == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown expense category");

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
        return Map.of("id", key == null ? 0 : key.longValue(), "date", date.toString(),
                "name", name, "category", category, "amount", amount);
    }

    public void updateExpense(long userId, long expenseId, BigDecimal amount) {
        int updated = jdbc.update("UPDATE buddy_expenses SET amount = ?, updated_at = CURRENT_TIMESTAMP WHERE expense_id = ? AND user_id = ?",
                amount, expenseId, userId);
        if (updated == 0) throw new ApiException(HttpStatus.NOT_FOUND, "Expense not found");
    }

    @Transactional
    public void addWatchSymbol(long userId, String requestedSymbol) {
        String symbol = requestedSymbol.trim().toUpperCase(Locale.ROOT);
        if (!symbol.matches("[A-Z0-9.-]{1,15}")) throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid symbol");
        Long instrumentId = jdbc.query("SELECT instrument_id FROM instruments WHERE symbol = ?",
                rs -> rs.next() ? rs.getLong(1) : null, symbol);
        if (instrumentId == null) {
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
            instrumentId = keys.getKey().longValue();
        }
        Long watchlistId = jdbc.query("SELECT watchlist_id FROM watchlists WHERE user_id = ? ORDER BY watchlist_id LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null, userId);
        if (watchlistId == null) {
            jdbc.update("INSERT INTO watchlists (user_id, watchlist_name) VALUES (?, 'Default Watchlist')", userId);
            watchlistId = jdbc.queryForObject("SELECT MAX(watchlist_id) FROM watchlists WHERE user_id = ?", Long.class, userId);
        }
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM watchlist_items WHERE watchlist_id = ? AND instrument_id = ?",
                Integer.class, watchlistId, instrumentId);
        if (exists != null && exists == 0) {
            jdbc.update("INSERT INTO watchlist_items (watchlist_id, instrument_id, added_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                    watchlistId, instrumentId);
        }
    }

    public void removeWatchSymbol(long userId, String symbol) {
        jdbc.update("""
                DELETE FROM watchlist_items
                WHERE watchlist_id IN (SELECT watchlist_id FROM watchlists WHERE user_id = ?)
                  AND instrument_id IN (SELECT instrument_id FROM instruments WHERE symbol = ?)
                """, userId, symbol.toUpperCase(Locale.ROOT));
    }
}
