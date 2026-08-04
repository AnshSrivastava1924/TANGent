-- ============================================
-- TANGent MySQL Workbench Test Queries
-- Minimal Schema (10 Tables)
-- ============================================
-- Connection Details:
-- Host: localhost
-- Port: 3306
-- Database: tangent_db
-- Username: tangent_app
-- Password: n3u3da!
-- ============================================

-- ============================================
-- 1. VERIFY SCHEMA STRUCTURE
-- ============================================

-- Show all tables (Should show 10 tables + 3 views)
SHOW TABLES;

-- Show table counts
SELECT
  'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'portfolios', COUNT(*) FROM portfolios
UNION ALL SELECT 'portfolio_assets', COUNT(*) FROM portfolio_assets
UNION ALL SELECT 'asset_classes', COUNT(*) FROM asset_classes
UNION ALL SELECT 'instruments', COUNT(*) FROM instruments
UNION ALL SELECT 'watchlists', COUNT(*) FROM watchlists
UNION ALL SELECT 'watchlist_items', COUNT(*) FROM watchlist_items
UNION ALL SELECT 'buddy_categories', COUNT(*) FROM buddy_categories
UNION ALL SELECT 'buddy_expenses', COUNT(*) FROM buddy_expenses;

-- ============================================
-- 2. CHECK USERS TABLE (SIMPLIFIED)
-- ============================================

-- View all users
SELECT * FROM users;

-- Check user structure (should NOT have date_of_birth, risk_profile, base_currency)
DESCRIBE users;

-- ============================================
-- 3. CHECK ASSET CLASSES (Reference Data)
-- ============================================

-- View all asset classes
SELECT
  asset_class_id,
  code,
  display_name,
  is_liability,
  is_liquid,
  sort_order
FROM asset_classes
ORDER BY sort_order;

-- Expected: 9 categories
-- cash, stocks, bonds, funds, real_estate, crypto, commodities, other_assets, liabilities

-- ============================================
-- 4. CHECK INSTRUMENTS (Stocks List)
-- ============================================

-- View all available stocks
SELECT
  symbol,
  instrument_name,
  exchange_code,
  asset_type,
  is_major
FROM instruments
ORDER BY symbol;

-- Expected: 16 major stocks (AAPL, MSFT, GOOGL, etc.)

-- Major tech stocks only
SELECT symbol, instrument_name
FROM instruments
WHERE asset_type = 'stock' AND is_major = TRUE
ORDER BY symbol;

-- ============================================
-- 5. CHECK PORTFOLIO (Should be EMPTY)
-- ============================================

-- Check portfolios table
SELECT * FROM portfolios;
-- Expected: Empty (no portfolios created yet)

-- Check portfolio assets (Should be 0)
SELECT COUNT(*) AS total_assets FROM portfolio_assets;
-- Expected: 0

-- Verify portfolio is truly empty
SELECT
  p.portfolio_id,
  p.portfolio_name,
  COUNT(pa.asset_id) AS asset_count,
  COALESCE(SUM(pa.current_value), 0) AS total_value
FROM portfolios p
LEFT JOIN portfolio_assets pa ON pa.portfolio_id = p.portfolio_id
GROUP BY p.portfolio_id, p.portfolio_name;
-- Expected: Empty result or 0 assets if portfolio exists

-- ============================================
-- 6. CHECK VIEWS (Portfolio Summary)
-- ============================================

-- Portfolio summary view
SELECT * FROM v_portfolio_summary;
-- Expected: Empty or all zeros

-- Net worth view
SELECT * FROM v_portfolio_net_worth;
-- Expected: Empty or all zeros

-- Allocation view
SELECT * FROM v_portfolio_allocation;
-- Expected: Empty or 0% allocations

-- ============================================
-- 7. TEST CREATING A PORTFOLIO
-- ============================================

-- Insert a test portfolio for user 1
INSERT INTO portfolios (user_id, portfolio_name, goal_description)
VALUES (1, 'Test Portfolio', 'Testing portfolio creation');

-- Verify portfolio created
SELECT * FROM portfolios;

-- Get the portfolio ID
SELECT portfolio_id, portfolio_name FROM portfolios ORDER BY created_at DESC LIMIT 1;

-- ============================================
-- 8. TEST ADDING ASSETS TO PORTFOLIO
-- ============================================

-- Add a cash asset (use portfolio_id from step 7)
INSERT INTO portfolio_assets
  (portfolio_id, asset_class_id, asset_name, symbol, quantity, unit_value, annual_income)
VALUES
  (1, 1, 'Checking Account', NULL, 1, 5000.00, 0),
  (1, 2, 'Apple Stock', 'AAPL', 10, 213.42, 0),
  (1, 2, 'Microsoft Stock', 'MSFT', 5, 518.66, 0);

-- View all assets in portfolio
SELECT
  a.asset_id,
  ac.display_name AS asset_class,
  a.asset_name,
  a.symbol,
  a.quantity,
  a.unit_value,
  a.current_value,
  a.annual_income
FROM portfolio_assets a
JOIN asset_classes ac ON ac.asset_class_id = a.asset_class_id
ORDER BY a.asset_id;

-- ============================================
-- 9. CHECK PORTFOLIO SUMMARY AFTER ADDING ASSETS
-- ============================================

-- Portfolio by class
SELECT
  class_name,
  class_value,
  asset_count
FROM v_portfolio_summary
WHERE portfolio_id = 1
ORDER BY class_code;

-- Net worth calculation
SELECT
  total_assets,
  total_liabilities,
  net_worth,
  liquid_assets,
  annual_income
FROM v_portfolio_net_worth
WHERE portfolio_id = 1;

-- Asset allocation
SELECT
  class_name,
  CONCAT('$', FORMAT(class_value, 2)) AS value,
  CONCAT(allocation_percent, '%') AS allocation
FROM v_portfolio_allocation
WHERE portfolio_id = 1 AND class_value > 0
ORDER BY allocation_percent DESC;

-- ============================================
-- 10. TEST WATCHLIST FUNCTIONALITY
-- ============================================

-- Create a watchlist
INSERT INTO watchlists (user_id, watchlist_name)
VALUES (1, 'My Tech Stocks');

-- Add stocks to watchlist
INSERT INTO watchlist_items (watchlist_id, instrument_id)
SELECT 1, instrument_id
FROM instruments
WHERE symbol IN ('AAPL', 'MSFT', 'GOOGL', 'NVDA')
LIMIT 4;

-- View watchlist
SELECT
  w.watchlist_name,
  i.symbol,
  i.instrument_name,
  i.exchange_code
FROM watchlists w
JOIN watchlist_items wi ON wi.watchlist_id = w.watchlist_id
JOIN instruments i ON i.instrument_id = wi.instrument_id
WHERE w.user_id = 1
ORDER BY i.symbol;

-- ============================================
-- 11. TEST EXPENSE TRACKING
-- ============================================

-- Create expense categories
INSERT INTO buddy_categories (user_id, category_name, monthly_budget, color_hex)
VALUES
  (1, 'Food', 500.00, '#FF6B6B'),
  (1, 'Transport', 200.00, '#4ECDC4'),
  (1, 'Entertainment', 150.00, '#95E1D3');

-- Add some expenses
INSERT INTO buddy_expenses (user_id, buddy_category_id, expense_name, amount, spent_on)
SELECT 1, bc.buddy_category_id, 'Test Expense', 50.00, CURRENT_DATE
FROM buddy_categories bc
WHERE bc.category_name = 'Food' LIMIT 1;

-- View expenses
SELECT
  bc.category_name,
  bc.monthly_budget,
  be.expense_name,
  be.amount,
  be.spent_on
FROM buddy_expenses be
JOIN buddy_categories bc ON bc.buddy_category_id = be.buddy_category_id
ORDER BY be.spent_on DESC;

-- ============================================
-- 12. VERIFY TABLE STRUCTURE CHANGES
-- ============================================

-- Users table should NOT have these columns:
-- - date_of_birth (REMOVED)
-- - risk_profile (REMOVED)
-- - base_currency (REMOVED)
SHOW COLUMNS FROM users;

-- Portfolio_assets should NOT have these columns:
-- - provider_or_location (REMOVED)
-- - asset_identifier (REMOVED, now just 'symbol')
-- - note (REMOVED)
-- - valuation_date (REMOVED)
SHOW COLUMNS FROM portfolio_assets;

-- ============================================
-- 13. CLEANUP TEST DATA (Optional)
-- ============================================

-- Delete test expenses
-- DELETE FROM buddy_expenses WHERE user_id = 1;

-- Delete test categories
-- DELETE FROM buddy_categories WHERE user_id = 1;

-- Delete test watchlist items
-- DELETE FROM watchlist_items WHERE watchlist_id = 1;

-- Delete test watchlist
-- DELETE FROM watchlists WHERE user_id = 1;

-- Delete test assets
-- DELETE FROM portfolio_assets WHERE portfolio_id = 1;

-- Delete test portfolio
-- DELETE FROM portfolios WHERE user_id = 1;

-- ============================================
-- 14. PERFORMANCE CHECKS
-- ============================================

-- Check for indexes
SHOW INDEX FROM users;
SHOW INDEX FROM portfolio_assets;
SHOW INDEX FROM portfolios;

-- Verify foreign key relationships
SELECT
  TABLE_NAME,
  CONSTRAINT_NAME,
  REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'tangent_db'
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME;

-- ============================================
-- 15. DATABASE STATISTICS
-- ============================================

-- Table sizes
SELECT
  TABLE_NAME AS 'Table',
  ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) AS 'Size (MB)',
  TABLE_ROWS AS 'Rows'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'tangent_db'
  AND TABLE_TYPE = 'BASE TABLE'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;

-- ============================================
-- EXPECTED RESULTS SUMMARY
-- ============================================
/*
Tables: 10 (+ 3 views)
- users: 1 row (demo user)
- portfolios: 0 rows initially (empty)
- portfolio_assets: 0 rows initially (empty)
- asset_classes: 9 rows (reference data)
- instruments: 16 rows (major stocks)
- watchlists: 0 rows initially
- watchlist_items: 0 rows initially
- buddy_categories: 0 rows initially
- buddy_expenses: 0 rows initially

Removed Columns:
- users: date_of_birth, risk_profile, base_currency
- portfolio_assets: provider_or_location, asset_identifier, note, valuation_date

Portfolio starts EMPTY - users add their own assets!
*/

-- ============================================
-- END OF TEST QUERIES
-- ============================================

