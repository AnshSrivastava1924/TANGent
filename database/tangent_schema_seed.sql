-- TANGent MySQL schema and seed data
-- Run with: mysql -u <user> -p < database/tangent_schema_seed.sql

DROP DATABASE IF EXISTS tangent_db;
CREATE DATABASE tangent_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE tangent_db;

CREATE TABLE users (
  user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(160) NOT NULL,
  date_of_birth DATE NULL,
  risk_profile ENUM('conservative', 'moderate', 'growth') NOT NULL DEFAULT 'conservative',
  base_currency CHAR(3) NOT NULL DEFAULT 'USD',
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_sessions (
  session_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE portfolios (
  portfolio_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  portfolio_name VARCHAR(160) NOT NULL,
  goal_description VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_portfolios_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

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
  provider_or_location VARCHAR(160) NULL,
  asset_identifier VARCHAR(80) NULL,
  quantity DECIMAL(18, 4) NOT NULL DEFAULT 1.0000,
  unit_value DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
  current_value DECIMAL(18, 2) AS (quantity * unit_value) STORED,
  annual_income DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
  note VARCHAR(255) NULL,
  valuation_date DATE NOT NULL DEFAULT (CURRENT_DATE),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_assets_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
  CONSTRAINT fk_assets_class FOREIGN KEY (asset_class_id) REFERENCES asset_classes(asset_class_id),
  INDEX idx_assets_portfolio_class (portfolio_id, asset_class_id),
  INDEX idx_assets_identifier (asset_identifier)
);

CREATE TABLE portfolio_asset_value_history (
  value_history_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  asset_id BIGINT UNSIGNED NOT NULL,
  valuation_date DATE NOT NULL,
  quantity DECIMAL(18, 4) NOT NULL,
  unit_value DECIMAL(18, 2) NOT NULL,
  total_value DECIMAL(18, 2) AS (quantity * unit_value) STORED,
  annual_income DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
  source VARCHAR(80) NOT NULL DEFAULT 'manual',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_asset_history_asset FOREIGN KEY (asset_id) REFERENCES portfolio_assets(asset_id) ON DELETE CASCADE,
  UNIQUE KEY uq_asset_history_day (asset_id, valuation_date)
);

CREATE TABLE instruments (
  instrument_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  symbol VARCHAR(30) NOT NULL UNIQUE,
  instrument_name VARCHAR(180) NOT NULL,
  exchange_code VARCHAR(40) NULL,
  asset_type ENUM('stock', 'etf', 'fund', 'bond', 'index', 'crypto', 'other') NOT NULL DEFAULT 'stock',
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  is_major BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE watchlists (
  watchlist_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  watchlist_name VARCHAR(120) NOT NULL DEFAULT 'Default Watchlist',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_watchlists_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE watchlist_items (
  watchlist_item_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  watchlist_id BIGINT UNSIGNED NOT NULL,
  instrument_id BIGINT UNSIGNED NOT NULL,
  added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_watch_items_watchlist FOREIGN KEY (watchlist_id) REFERENCES watchlists(watchlist_id) ON DELETE CASCADE,
  CONSTRAINT fk_watch_items_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id),
  UNIQUE KEY uq_watchlist_symbol (watchlist_id, instrument_id)
);

CREATE TABLE market_quotes (
  quote_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  instrument_id BIGINT UNSIGNED NOT NULL,
  provider_source VARCHAR(80) NOT NULL,
  as_of_time DATETIME NOT NULL,
  last_price DECIMAL(18, 4) NOT NULL,
  open_price DECIMAL(18, 4) NULL,
  high_price DECIMAL(18, 4) NULL,
  low_price DECIMAL(18, 4) NULL,
  close_price DECIMAL(18, 4) NULL,
  volume BIGINT UNSIGNED NULL,
  change_amount DECIMAL(18, 4) NULL,
  change_percent DECIMAL(10, 4) NULL,
  CONSTRAINT fk_quotes_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE,
  INDEX idx_quotes_latest (instrument_id, as_of_time DESC)
);

CREATE TABLE price_bars (
  price_bar_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  instrument_id BIGINT UNSIGNED NOT NULL,
  provider_source VARCHAR(80) NOT NULL,
  interval_code ENUM('1m', '5m', '15m', '1h', '1d', '1wk', '1mo') NOT NULL DEFAULT '1d',
  bar_time DATETIME NOT NULL,
  open_price DECIMAL(18, 4) NOT NULL,
  high_price DECIMAL(18, 4) NOT NULL,
  low_price DECIMAL(18, 4) NOT NULL,
  close_price DECIMAL(18, 4) NOT NULL,
  volume BIGINT UNSIGNED NULL,
  CONSTRAINT fk_price_bars_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE,
  UNIQUE KEY uq_price_bar (instrument_id, interval_code, bar_time)
);

CREATE TABLE news_articles (
  news_article_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  provider_source VARCHAR(80) NOT NULL,
  headline VARCHAR(255) NOT NULL,
  summary TEXT NULL,
  source_name VARCHAR(160) NULL,
  article_url VARCHAR(500) NOT NULL,
  published_at DATETIME NOT NULL,
  sentiment_score DECIMAL(8, 4) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_news_url (article_url)
);

CREATE TABLE news_article_instruments (
  news_article_id BIGINT UNSIGNED NOT NULL,
  instrument_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (news_article_id, instrument_id),
  CONSTRAINT fk_news_map_article FOREIGN KEY (news_article_id) REFERENCES news_articles(news_article_id) ON DELETE CASCADE,
  CONSTRAINT fk_news_map_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE
);

CREATE TABLE comparison_sets (
  comparison_set_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  set_name VARCHAR(120) NOT NULL,
  range_code ENUM('1mo', '3mo', '6mo', '1y') NOT NULL DEFAULT '3mo',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_compare_sets_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE comparison_set_items (
  comparison_set_id BIGINT UNSIGNED NOT NULL,
  instrument_id BIGINT UNSIGNED NOT NULL,
  sort_order INT NOT NULL DEFAULT 1,
  PRIMARY KEY (comparison_set_id, instrument_id),
  CONSTRAINT fk_compare_items_set FOREIGN KEY (comparison_set_id) REFERENCES comparison_sets(comparison_set_id) ON DELETE CASCADE,
  CONSTRAINT fk_compare_items_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE
);

CREATE TABLE buddy_categories (
  buddy_category_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  category_name VARCHAR(80) NOT NULL,
  monthly_budget DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
  color_hex CHAR(7) NOT NULL DEFAULT '#007AFF',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_buddy_categories_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  UNIQUE KEY uq_user_category (user_id, category_name)
);

CREATE TABLE buddy_expenses (
  expense_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  buddy_category_id BIGINT UNSIGNED NOT NULL,
  expense_name VARCHAR(160) NOT NULL,
  amount DECIMAL(12, 2) NOT NULL,
  spent_on DATE NOT NULL,
  notes VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_expense_amount CHECK (amount >= 0),
  CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_expenses_category FOREIGN KEY (buddy_category_id) REFERENCES buddy_categories(buddy_category_id),
  INDEX idx_expenses_user_date (user_id, spent_on DESC),
  INDEX idx_expenses_category_date (buddy_category_id, spent_on DESC)
);

CREATE TABLE order_history (
  order_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  portfolio_id BIGINT UNSIGNED NOT NULL,
  instrument_id BIGINT UNSIGNED NOT NULL,
  side ENUM('buy', 'sell') NOT NULL,
  quantity DECIMAL(18, 4) NOT NULL,
  price DECIMAL(18, 4) NOT NULL,
  gross_amount DECIMAL(18, 2) AS (quantity * price) STORED,
  fees DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
  executed_at DATETIME NOT NULL,
  status ENUM('filled', 'cancelled', 'pending') NOT NULL DEFAULT 'filled',
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_orders_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
  CONSTRAINT fk_orders_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id)
);

DELIMITER //

CREATE TRIGGER trg_portfolio_assets_after_insert
AFTER INSERT ON portfolio_assets
FOR EACH ROW
BEGIN
  INSERT INTO portfolio_asset_value_history
    (asset_id, valuation_date, quantity, unit_value, annual_income, source)
  VALUES
    (NEW.asset_id, NEW.valuation_date, NEW.quantity, NEW.unit_value, NEW.annual_income, 'asset insert')
  ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    unit_value = VALUES(unit_value),
    annual_income = VALUES(annual_income),
    source = VALUES(source);
END//

CREATE TRIGGER trg_portfolio_assets_after_update
AFTER UPDATE ON portfolio_assets
FOR EACH ROW
BEGIN
  INSERT INTO portfolio_asset_value_history
    (asset_id, valuation_date, quantity, unit_value, annual_income, source)
  VALUES
    (NEW.asset_id, NEW.valuation_date, NEW.quantity, NEW.unit_value, NEW.annual_income, 'asset update')
  ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    unit_value = VALUES(unit_value),
    annual_income = VALUES(annual_income),
    source = VALUES(source);
END//

CREATE PROCEDURE sp_update_portfolio_asset_value(
  IN p_asset_id BIGINT UNSIGNED,
  IN p_quantity DECIMAL(18, 4),
  IN p_unit_value DECIMAL(18, 2),
  IN p_annual_income DECIMAL(18, 2),
  IN p_valuation_date DATE
)
BEGIN
  UPDATE portfolio_assets
  SET quantity = p_quantity,
      unit_value = p_unit_value,
      annual_income = p_annual_income,
      valuation_date = COALESCE(p_valuation_date, CURRENT_DATE)
  WHERE asset_id = p_asset_id;
END//

CREATE PROCEDURE sp_add_buddy_expense(
  IN p_user_id BIGINT UNSIGNED,
  IN p_category_name VARCHAR(80),
  IN p_expense_name VARCHAR(160),
  IN p_amount DECIMAL(12, 2),
  IN p_spent_on DATE,
  IN p_notes VARCHAR(255)
)
BEGIN
  DECLARE v_category_id BIGINT UNSIGNED;

  SELECT buddy_category_id
    INTO v_category_id
  FROM buddy_categories
  WHERE user_id = p_user_id
    AND category_name = p_category_name
  LIMIT 1;

  IF v_category_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Buddy category does not exist for this user';
  END IF;

  INSERT INTO buddy_expenses
    (user_id, buddy_category_id, expense_name, amount, spent_on, notes)
  VALUES
    (p_user_id, v_category_id, p_expense_name, p_amount, COALESCE(p_spent_on, CURRENT_DATE), p_notes);
END//

DELIMITER ;

CREATE VIEW v_portfolio_class_summary AS
SELECT
  p.portfolio_id,
  p.user_id,
  ac.asset_class_id,
  ac.code,
  ac.display_name,
  ac.is_liability,
  ac.is_liquid,
  COALESCE(SUM(pa.current_value), 0) AS class_value,
  COALESCE(SUM(pa.annual_income), 0) AS annual_income
FROM portfolios p
CROSS JOIN asset_classes ac
LEFT JOIN portfolio_assets pa
  ON pa.portfolio_id = p.portfolio_id
 AND pa.asset_class_id = ac.asset_class_id
GROUP BY p.portfolio_id, p.user_id, ac.asset_class_id, ac.code, ac.display_name, ac.is_liability, ac.is_liquid;

CREATE VIEW v_portfolio_net_worth AS
SELECT
  portfolio_id,
  user_id,
  SUM(CASE WHEN is_liability = FALSE THEN class_value ELSE 0 END) AS total_assets,
  SUM(CASE WHEN is_liability = TRUE THEN class_value ELSE 0 END) AS total_liabilities,
  SUM(CASE WHEN is_liability = FALSE THEN class_value ELSE -class_value END) AS net_worth,
  SUM(CASE WHEN is_liquid = TRUE AND is_liability = FALSE THEN class_value ELSE 0 END) AS liquid_assets,
  SUM(annual_income) AS annual_income
FROM v_portfolio_class_summary
GROUP BY portfolio_id, user_id;

CREATE VIEW v_portfolio_allocation AS
SELECT
  pcs.*,
  CASE
    WHEN nw.total_assets = 0 OR pcs.is_liability = TRUE THEN 0
    ELSE ROUND((pcs.class_value / nw.total_assets) * 100, 2)
  END AS allocation_percent
FROM v_portfolio_class_summary pcs
JOIN v_portfolio_net_worth nw ON nw.portfolio_id = pcs.portfolio_id;

CREATE VIEW v_latest_market_quote AS
SELECT q.*
FROM market_quotes q
JOIN (
  SELECT instrument_id, MAX(as_of_time) AS latest_time
  FROM market_quotes
  GROUP BY instrument_id
) latest
  ON latest.instrument_id = q.instrument_id
 AND latest.latest_time = q.as_of_time;

CREATE VIEW v_watchlist_latest AS
SELECT
  w.watchlist_id,
  w.user_id,
  w.watchlist_name,
  i.symbol,
  i.instrument_name,
  q.provider_source,
  q.as_of_time,
  q.last_price,
  q.change_percent,
  q.volume
FROM watchlists w
JOIN watchlist_items wi ON wi.watchlist_id = w.watchlist_id
JOIN instruments i ON i.instrument_id = wi.instrument_id
LEFT JOIN v_latest_market_quote q ON q.instrument_id = i.instrument_id;

CREATE VIEW v_buddy_monthly_summary AS
SELECT
  be.user_id,
  DATE_FORMAT(be.spent_on, '%Y-%m') AS expense_month,
  SUM(be.amount) AS month_spend,
  COUNT(*) AS expense_count,
  ROUND(AVG(be.amount), 2) AS average_expense
FROM buddy_expenses be
GROUP BY be.user_id, DATE_FORMAT(be.spent_on, '%Y-%m');

CREATE VIEW v_buddy_category_summary AS
SELECT
  be.user_id,
  DATE_FORMAT(be.spent_on, '%Y-%m') AS expense_month,
  bc.category_name,
  bc.monthly_budget,
  SUM(be.amount) AS category_spend,
  bc.monthly_budget - SUM(be.amount) AS budget_remaining
FROM buddy_expenses be
JOIN buddy_categories bc ON bc.buddy_category_id = be.buddy_category_id
GROUP BY be.user_id, DATE_FORMAT(be.spent_on, '%Y-%m'), bc.category_name, bc.monthly_budget;

INSERT INTO users (email, password_hash, full_name, date_of_birth, risk_profile) VALUES
('student@tangent.local', '{seed}', 'Anita Sharma', '1956-04-18', 'conservative');

INSERT INTO portfolios (user_id, portfolio_name, goal_description) VALUES
(1, 'Retirement household portfolio', 'Balance income, safety, housing, and long-term family wealth');

INSERT INTO asset_classes (code, display_name, purpose, is_liability, is_liquid, sort_order) VALUES
('cash', 'Cash and Bank Accounts', 'Ready money for bills, emergencies, and near-term care needs.', FALSE, TRUE, 10),
('securities', 'Listed Securities', 'Stocks and ETFs that provide growth and dividend potential.', FALSE, TRUE, 20),
('fixed_income', 'Bonds and Fixed Income', 'Stability, predictable coupons, and lower volatility.', FALSE, TRUE, 30),
('funds', 'Mutual Funds and ETFs', 'Managed diversification across markets and sectors.', FALSE, TRUE, 40),
('pension', 'Pension Sources', 'Expected yearly income from retirement plans.', FALSE, FALSE, 50),
('annuities', 'Annuities', 'Contracted income that can support regular expenses.', FALSE, FALSE, 60),
('housing', 'Housing and Real Estate', 'Home equity and rental property value.', FALSE, FALSE, 70),
('commodities', 'Gold and Commodities', 'Inflation hedge and alternative asset exposure.', FALSE, FALSE, 80),
('insurance', 'Insurance Cash Value', 'Policies with accessible value or estate-planning support.', FALSE, FALSE, 90),
('liabilities', 'Loans and Debts', 'Amounts owed that reduce household net worth.', TRUE, FALSE, 100);

INSERT INTO portfolio_assets
(portfolio_id, asset_class_id, asset_name, provider_or_location, asset_identifier, quantity, unit_value, annual_income, note, valuation_date)
VALUES
(1, 1, 'Checking account', 'Community Bank', NULL, 1, 24500.00, 0.00, 'Monthly spending account', '2026-07-31'),
(1, 1, 'Savings account', 'Community Bank', NULL, 1, 78000.00, 1800.00, 'Emergency reserve', '2026-07-31'),
(1, 1, 'Fixed deposit ladder', 'National Bank', NULL, 1, 135000.00, 6400.00, 'Low-risk income', '2026-07-31'),
(1, 2, 'Dividend stock basket', 'Brokerage', 'DIV-BASKET', 1, 162000.00, 6200.00, 'Blue-chip shares', '2026-07-31'),
(1, 2, 'Broad market ETF', 'Brokerage', 'SPY', 420, 280.95, 2100.00, 'Diversified equity exposure', '2026-07-31'),
(1, 3, 'Government bonds', 'Treasury Direct', 'GOV-BOND', 1, 220000.00, 10500.00, 'Core retirement income', '2026-07-31'),
(1, 3, 'Municipal bond fund', 'Brokerage', 'MUNI-FUND', 1, 94000.00, 3900.00, 'Tax-aware income', '2026-07-31'),
(1, 4, 'Balanced mutual fund', 'Brokerage', 'BAL-FUND', 1, 86000.00, 2600.00, 'Moderate risk', '2026-07-31'),
(1, 4, 'Healthcare ETF', 'Brokerage', 'XLV', 300, 140.00, 700.00, 'Sector allocation', '2026-07-31'),
(1, 5, 'Company pension', 'Former employer', NULL, 1, 0.00, 42000.00, 'Annual pension income', '2026-07-31'),
(1, 5, 'Social security', 'Government', NULL, 1, 0.00, 31800.00, 'Annual benefit estimate', '2026-07-31'),
(1, 6, 'Lifetime annuity', 'Secure Life', NULL, 1, 175000.00, 15600.00, 'Guaranteed yearly payout', '2026-07-31'),
(1, 7, 'Primary home', 'Springfield', NULL, 1, 485000.00, 0.00, 'Mortgage-free residence', '2026-07-31'),
(1, 7, 'Rental apartment', 'Lakeside', NULL, 1, 265000.00, 18000.00, 'Rental income property', '2026-07-31'),
(1, 8, 'Gold holdings', 'Home safe and vault', NULL, 1, 36000.00, 0.00, 'Long-term reserve', '2026-07-31'),
(1, 9, 'Whole life cash value', 'Secure Life', NULL, 1, 58000.00, 0.00, 'Policy cash value', '2026-07-31'),
(1, 10, 'Home equity line', 'Community Bank', NULL, 1, 41000.00, 0.00, 'Outstanding balance', '2026-07-31'),
(1, 10, 'Car loan', 'Auto Finance', NULL, 1, 12500.00, 0.00, 'Remaining balance', '2026-07-31');

INSERT INTO instruments (symbol, instrument_name, exchange_code, asset_type, currency, is_major) VALUES
('AAPL', 'Apple Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('MSFT', 'Microsoft Corporation', 'NASDAQ', 'stock', 'USD', TRUE),
('NVDA', 'NVIDIA Corporation', 'NASDAQ', 'stock', 'USD', TRUE),
('GOOGL', 'Alphabet Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('AMZN', 'Amazon.com Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('META', 'Meta Platforms Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('TSLA', 'Tesla Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('JPM', 'JPMorgan Chase & Co.', 'NYSE', 'stock', 'USD', TRUE),
('SPY', 'SPDR S&P 500 ETF Trust', 'NYSEARCA', 'etf', 'USD', FALSE),
('XLV', 'Health Care Select Sector SPDR Fund', 'NYSEARCA', 'etf', 'USD', FALSE);

INSERT INTO watchlists (user_id, watchlist_name) VALUES
(1, 'Default Watchlist');

INSERT INTO watchlist_items (watchlist_id, instrument_id) VALUES
(1, 1), (1, 2), (1, 3);

INSERT INTO market_quotes
(instrument_id, provider_source, as_of_time, last_price, open_price, high_price, low_price, close_price, volume, change_amount, change_percent)
VALUES
(1, 'seed', '2026-07-31 16:00:00', 213.42, 211.80, 214.20, 209.95, 210.14, 54320000, 3.28, 1.56),
(2, 'seed', '2026-07-31 16:00:00', 518.66, 514.25, 520.10, 511.90, 512.40, 31240000, 6.26, 1.22),
(3, 'seed', '2026-07-31 16:00:00', 181.15, 178.10, 183.44, 177.60, 177.98, 62890000, 3.17, 1.78),
(4, 'seed', '2026-07-31 16:00:00', 196.72, 195.00, 198.10, 193.84, 194.50, 21800000, 2.22, 1.14),
(5, 'seed', '2026-07-31 16:00:00', 232.08, 229.40, 233.50, 228.25, 229.86, 40120000, 2.22, 0.97),
(6, 'seed', '2026-07-31 16:00:00', 745.30, 738.20, 749.00, 734.50, 739.10, 16730000, 6.20, 0.84),
(7, 'seed', '2026-07-31 16:00:00', 318.44, 314.70, 320.15, 312.40, 315.18, 75500000, 3.26, 1.03),
(8, 'seed', '2026-07-31 16:00:00', 286.90, 284.10, 288.50, 282.60, 284.75, 10280000, 2.15, 0.76),
(9, 'seed', '2026-07-31 16:00:00', 602.35, 599.00, 604.10, 596.40, 598.70, 84400000, 3.65, 0.61),
(10, 'seed', '2026-07-31 16:00:00', 151.20, 149.90, 152.00, 148.80, 149.60, 11700000, 1.60, 1.07);

INSERT INTO price_bars
(instrument_id, provider_source, interval_code, bar_time, open_price, high_price, low_price, close_price, volume)
VALUES
(1, 'seed', '1d', '2026-07-29 16:00:00', 207.20, 210.90, 205.80, 209.60, 50100000),
(1, 'seed', '1d', '2026-07-30 16:00:00', 209.80, 212.10, 208.50, 210.14, 48800000),
(1, 'seed', '1d', '2026-07-31 16:00:00', 211.80, 214.20, 209.95, 213.42, 54320000),
(2, 'seed', '1d', '2026-07-29 16:00:00', 507.30, 513.00, 505.40, 510.25, 28500000),
(2, 'seed', '1d', '2026-07-30 16:00:00', 511.10, 515.20, 508.90, 512.40, 29100000),
(2, 'seed', '1d', '2026-07-31 16:00:00', 514.25, 520.10, 511.90, 518.66, 31240000),
(3, 'seed', '1d', '2026-07-29 16:00:00', 172.20, 176.00, 171.85, 175.40, 59000000),
(3, 'seed', '1d', '2026-07-30 16:00:00', 176.30, 179.20, 175.70, 177.98, 60800000),
(3, 'seed', '1d', '2026-07-31 16:00:00', 178.10, 183.44, 177.60, 181.15, 62890000),
(8, 'seed', '1d', '2026-07-31 16:00:00', 284.10, 288.50, 282.60, 286.90, 10280000);

INSERT INTO news_articles
(provider_source, headline, summary, source_name, article_url, published_at, sentiment_score)
VALUES
('seed', 'Markets rise as earnings and rate outlook improve', 'Broad market indexes moved higher after stronger earnings and calmer rate expectations.', 'TANGent Market Desk', 'https://example.com/markets-rise-rate-outlook', '2026-07-31 09:30:00', 0.42),
('seed', 'Healthcare and dividend sectors attract retirement investors', 'Analysts noted renewed interest in lower-volatility sectors and income-producing securities.', 'TANGent Market Desk', 'https://example.com/healthcare-dividend-retirement', '2026-07-31 10:15:00', 0.36),
('seed', 'Bond yields steady as investors seek predictable income', 'Fixed income flows remained resilient as retirees emphasized stable cash flow.', 'TANGent Market Desk', 'https://example.com/bond-yields-steady-income', '2026-07-31 11:00:00', 0.18);

INSERT INTO news_article_instruments (news_article_id, instrument_id) VALUES
(1, 1), (1, 2), (2, 10), (3, 9);

INSERT INTO comparison_sets (user_id, set_name, range_code) VALUES
(1, 'Mega-cap technology comparison', '3mo');

INSERT INTO comparison_set_items (comparison_set_id, instrument_id, sort_order) VALUES
(1, 1, 1), (1, 2, 2), (1, 3, 3);

INSERT INTO buddy_categories (user_id, category_name, monthly_budget, color_hex) VALUES
(1, 'Food', 900.00, '#007AFF'),
(1, 'Health', 650.00, '#30D158'),
(1, 'Housing', 1200.00, '#FF9500'),
(1, 'Utilities', 420.00, '#FF3B30'),
(1, 'Transport', 360.00, '#64D2FF'),
(1, 'Family', 500.00, '#5856D6'),
(1, 'Leisure', 300.00, '#8E8E93');

INSERT INTO buddy_expenses (user_id, buddy_category_id, expense_name, amount, spent_on, notes) VALUES
(1, 1, 'Groceries', 84.00, '2026-07-31', 'Weekly food shopping'),
(1, 2, 'Prescription refill', 42.00, '2026-07-31', 'Monthly medication'),
(1, 4, 'Electric bill', 126.00, '2026-07-30', 'Utility payment'),
(1, 5, 'Taxi to clinic', 28.00, '2026-07-30', 'Medical appointment transport'),
(1, 6, 'Dinner with family', 96.00, '2026-07-29', 'Family meal'),
(1, 3, 'Gardening supplies', 64.00, '2026-07-28', 'Home maintenance'),
(1, 7, 'Movie tickets', 34.00, '2026-07-27', 'Leisure outing'),
(1, 1, 'Bakery and fruit', 31.00, '2026-07-26', 'Daily food purchase'),
(1, 2, 'Doctor copay', 55.00, '2026-07-25', 'Clinic visit'),
(1, 4, 'Water bill', 48.00, '2026-07-24', 'Utility payment');

INSERT INTO order_history
(user_id, portfolio_id, instrument_id, side, quantity, price, fees, executed_at, status)
VALUES
(1, 1, 9, 'buy', 12.0000, 598.70, 4.95, '2026-07-20 10:05:00', 'filled'),
(1, 1, 10, 'buy', 18.0000, 149.60, 4.95, '2026-07-22 11:40:00', 'filled');

-- Quick sanity checks after running the script:
-- SELECT * FROM v_portfolio_net_worth;
-- SELECT display_name, class_value, allocation_percent FROM v_portfolio_allocation ORDER BY asset_class_id;
-- SELECT * FROM v_buddy_monthly_summary;
-- SELECT * FROM v_watchlist_latest;
