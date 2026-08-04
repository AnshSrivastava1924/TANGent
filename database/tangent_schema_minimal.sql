-- TANGent canonical schema for a clean MySQL 8 installation.
-- WARNING: this bootstrap recreates tangent_db. Use migrate_to_minimal.sql for an existing database.

DROP DATABASE IF EXISTS tangent_db;
CREATE DATABASE tangent_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE tangent_db;

-- ============================================
-- CORE USER MANAGEMENT
-- ============================================

CREATE TABLE users (
  user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(160) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_users_email (email)
);

-- ============================================
-- PORTFOLIO MANAGEMENT
-- ============================================

CREATE TABLE portfolios (
  portfolio_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  portfolio_name VARCHAR(160) NOT NULL DEFAULT 'My Portfolio',
  goal_description VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_portfolios_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX idx_portfolios_user (user_id)
);

-- Reference data: Asset classification
CREATE TABLE asset_classes (
  asset_class_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  display_name VARCHAR(120) NOT NULL,
  purpose VARCHAR(255) NOT NULL,
  is_liability BOOLEAN NOT NULL DEFAULT FALSE,
  is_liquid BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order INT NOT NULL DEFAULT 100
);

CREATE TABLE portfolio_assets (
  asset_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  portfolio_id BIGINT UNSIGNED NOT NULL,
  asset_class_id BIGINT UNSIGNED NOT NULL,
  asset_name VARCHAR(160) NOT NULL,
  symbol VARCHAR(30) NULL COMMENT 'Stock ticker symbol if applicable',
  quantity DECIMAL(18, 4) NOT NULL DEFAULT 1.0000,
  unit_value DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
  current_value DECIMAL(18, 2) AS (quantity * unit_value) STORED,
  annual_income DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_asset_quantity CHECK (quantity > 0),
  CONSTRAINT chk_asset_unit_value CHECK (unit_value >= 0),
  CONSTRAINT chk_asset_income CHECK (annual_income >= 0),
  CONSTRAINT fk_assets_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
  CONSTRAINT fk_assets_class FOREIGN KEY (asset_class_id) REFERENCES asset_classes(asset_class_id),
  INDEX idx_assets_portfolio (portfolio_id),
  INDEX idx_assets_class (asset_class_id)
);

-- ============================================
-- MARKET DATA (Reference/Lookup Only)
-- ============================================

-- Reference data: Available stocks/instruments for lookup
CREATE TABLE instruments (
  instrument_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  symbol VARCHAR(30) NOT NULL UNIQUE,
  instrument_name VARCHAR(180) NOT NULL,
  exchange_code VARCHAR(40) NULL,
  asset_type ENUM('stock', 'etf', 'fund', 'bond', 'index', 'crypto', 'other') NOT NULL DEFAULT 'stock',
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  is_major BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Popular/major instruments',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_instruments_symbol (symbol),
  INDEX idx_instruments_type (asset_type)
);

-- ============================================
-- WATCHLIST MANAGEMENT
-- ============================================

CREATE TABLE watchlists (
  watchlist_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  watchlist_name VARCHAR(120) NOT NULL DEFAULT 'My Watchlist',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_watchlists_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX idx_watchlists_user (user_id)
);

CREATE TABLE watchlist_items (
  watchlist_item_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  watchlist_id BIGINT UNSIGNED NOT NULL,
  instrument_id BIGINT UNSIGNED NOT NULL,
  added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_watch_items_watchlist FOREIGN KEY (watchlist_id) REFERENCES watchlists(watchlist_id) ON DELETE CASCADE,
  CONSTRAINT fk_watch_items_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE,
  UNIQUE KEY uq_watchlist_symbol (watchlist_id, instrument_id)
);

-- ============================================
-- EXPENSE TRACKING (Optional Feature)
-- ============================================

CREATE TABLE buddy_categories (
  buddy_category_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  category_name VARCHAR(80) NOT NULL,
  monthly_budget DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
  color_hex CHAR(7) NOT NULL DEFAULT '#007AFF',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_buddy_categories_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  UNIQUE KEY uq_user_category (user_id, category_name),
  UNIQUE KEY uq_user_category_id (user_id, buddy_category_id)
);

CREATE TABLE buddy_expenses (
  expense_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  buddy_category_id BIGINT UNSIGNED NOT NULL,
  expense_name VARCHAR(160) NOT NULL,
  amount DECIMAL(12, 2) NOT NULL,
  spent_on DATE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_expense_amount CHECK (amount > 0),
  CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_expenses_user_category FOREIGN KEY (user_id, buddy_category_id)
    REFERENCES buddy_categories(user_id, buddy_category_id) ON DELETE CASCADE,
  INDEX idx_expenses_user_date (user_id, spent_on DESC)
);

-- ============================================
-- VIEWS FOR PORTFOLIO SUMMARY
-- ============================================

CREATE VIEW v_portfolio_summary AS
SELECT
  p.portfolio_id,
  p.user_id,
  p.portfolio_name,
  ac.asset_class_id,
  ac.code AS class_code,
  ac.display_name AS class_name,
  ac.is_liability,
  ac.is_liquid,
  COALESCE(SUM(pa.current_value), 0) AS class_value,
  COALESCE(SUM(pa.annual_income), 0) AS class_income,
  COUNT(pa.asset_id) AS asset_count
FROM portfolios p
CROSS JOIN asset_classes ac
LEFT JOIN portfolio_assets pa
  ON pa.portfolio_id = p.portfolio_id
 AND pa.asset_class_id = ac.asset_class_id
GROUP BY p.portfolio_id, p.user_id, p.portfolio_name, ac.asset_class_id, ac.code, ac.display_name, ac.is_liability, ac.is_liquid;

CREATE VIEW v_portfolio_net_worth AS
SELECT
  portfolio_id,
  user_id,
  SUM(CASE WHEN is_liability = FALSE THEN class_value ELSE 0 END) AS total_assets,
  SUM(CASE WHEN is_liability = TRUE THEN class_value ELSE 0 END) AS total_liabilities,
  SUM(CASE WHEN is_liability = FALSE THEN class_value ELSE -class_value END) AS net_worth,
  SUM(CASE WHEN is_liquid = TRUE AND is_liability = FALSE THEN class_value ELSE 0 END) AS liquid_assets,
  SUM(class_income) AS annual_income
FROM v_portfolio_summary
GROUP BY portfolio_id, user_id;

CREATE VIEW v_portfolio_allocation AS
SELECT
  ps.*,
  CASE
    WHEN nw.total_assets = 0 OR ps.is_liability = TRUE THEN 0
    ELSE ROUND((ps.class_value / nw.total_assets) * 100, 2)
  END AS allocation_percent
FROM v_portfolio_summary ps
JOIN v_portfolio_net_worth nw ON nw.portfolio_id = ps.portfolio_id;

-- ============================================
-- SEED DATA: Reference Tables Only
-- ============================================

-- Asset Classes (Reference Data - Required)
INSERT INTO asset_classes (code, display_name, purpose, is_liability, is_liquid, sort_order) VALUES
('cash', 'Cash & Bank Accounts', 'Liquid money for daily needs and emergencies', FALSE, TRUE, 10),
('stocks', 'Stocks & Equities', 'Individual company stocks and equity investments', FALSE, TRUE, 20),
('bonds', 'Bonds & Fixed Income', 'Government and corporate bonds for stable income', FALSE, TRUE, 30),
('funds', 'Mutual Funds & ETFs', 'Diversified investment funds', FALSE, TRUE, 40),
('real_estate', 'Real Estate', 'Property and real estate investments', FALSE, FALSE, 50),
('crypto', 'Cryptocurrency', 'Digital assets and crypto investments', FALSE, TRUE, 60),
('commodities', 'Commodities', 'Gold, silver, and other commodities', FALSE, FALSE, 70),
('other_assets', 'Other Assets', 'Miscellaneous investments', FALSE, FALSE, 80),
('liabilities', 'Loans & Debts', 'Outstanding debts and liabilities', TRUE, FALSE, 90);

-- Major Stocks/Instruments (Reference Data - For search/lookup)
INSERT INTO instruments (symbol, instrument_name, exchange_code, asset_type, currency, is_major) VALUES
-- Major Tech Stocks
('AAPL', 'Apple Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('MSFT', 'Microsoft Corporation', 'NASDAQ', 'stock', 'USD', TRUE),
('GOOGL', 'Alphabet Inc. (Class A)', 'NASDAQ', 'stock', 'USD', TRUE),
('AMZN', 'Amazon.com Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('META', 'Meta Platforms Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('TSLA', 'Tesla Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('NVDA', 'NVIDIA Corporation', 'NASDAQ', 'stock', 'USD', TRUE),
-- Financial
('JPM', 'JPMorgan Chase & Co.', 'NYSE', 'stock', 'USD', TRUE),
('BAC', 'Bank of America Corp.', 'NYSE', 'stock', 'USD', TRUE),
('WFC', 'Wells Fargo & Company', 'NYSE', 'stock', 'USD', TRUE),
-- Major ETFs
('SPY', 'SPDR S&P 500 ETF Trust', 'NYSEARCA', 'etf', 'USD', TRUE),
('QQQ', 'Invesco QQQ Trust', 'NASDAQ', 'etf', 'USD', TRUE),
('VTI', 'Vanguard Total Stock Market ETF', 'NYSEARCA', 'etf', 'USD', TRUE),
('VOO', 'Vanguard S&P 500 ETF', 'NYSEARCA', 'etf', 'USD', TRUE),
-- Healthcare
('JNJ', 'Johnson & Johnson', 'NYSE', 'stock', 'USD', TRUE),
('UNH', 'UnitedHealth Group Inc.', 'NYSE', 'stock', 'USD', TRUE);

-- Development login. SeedPasswordInitializer replaces {seed} with a BCrypt hash at startup.
INSERT INTO users (email, password_hash, full_name) VALUES
('student@tangent.local', '{seed}', 'Student User');

INSERT INTO portfolios (user_id, portfolio_name, goal_description)
VALUES (1, 'My Portfolio', 'My retirement portfolio');

INSERT INTO buddy_categories (user_id, category_name, monthly_budget, color_hex) VALUES
(1, 'Food', 0, '#007AFF'),
(1, 'Health', 0, '#30D158'),
(1, 'Housing', 0, '#FF9500'),
(1, 'Utilities', 0, '#FF3B30'),
(1, 'Transport', 0, '#64D2FF'),
(1, 'Family', 0, '#5856D6'),
(1, 'Leisure', 0, '#8E8E93');

-- ============================================
-- SCHEMA STATISTICS
-- ============================================
-- Total base tables: 9 (reduced from 18)
-- Core: users, portfolios, portfolio_assets, asset_classes
-- Market: instruments
-- Features: watchlists, watchlist_items, buddy_categories, buddy_expenses
-- Views: 3 (portfolio_summary, net_worth, allocation)
-- Stored Procedures: none (all writes are authenticated in the application service layer)
-- ============================================

