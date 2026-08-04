CREATE TABLE users (
  user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(160) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portfolios (
  portfolio_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  portfolio_name VARCHAR(160) NOT NULL DEFAULT 'My Portfolio',
  goal_description VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
CREATE INDEX idx_portfolios_user ON portfolios(user_id);

CREATE TABLE asset_classes (
  asset_class_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  display_name VARCHAR(120) NOT NULL,
  purpose VARCHAR(255) NOT NULL,
  is_liability BOOLEAN NOT NULL DEFAULT FALSE,
  is_liquid BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order INT NOT NULL DEFAULT 100
);

CREATE TABLE portfolio_assets (
  asset_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  portfolio_id BIGINT NOT NULL,
  asset_class_id BIGINT NOT NULL,
  asset_name VARCHAR(160) NOT NULL,
  symbol VARCHAR(30),
  quantity DECIMAL(18,4) NOT NULL DEFAULT 1,
  unit_value DECIMAL(18,2) NOT NULL DEFAULT 0,
  current_value DECIMAL(18,2) GENERATED ALWAYS AS (quantity * unit_value),
  annual_income DECIMAL(18,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_asset_quantity CHECK (quantity > 0),
  CONSTRAINT chk_asset_unit_value CHECK (unit_value >= 0),
  CONSTRAINT chk_asset_income CHECK (annual_income >= 0),
  FOREIGN KEY (portfolio_id) REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
  FOREIGN KEY (asset_class_id) REFERENCES asset_classes(asset_class_id)
);
CREATE INDEX idx_assets_portfolio ON portfolio_assets(portfolio_id);
CREATE INDEX idx_assets_class ON portfolio_assets(asset_class_id);

CREATE TABLE instruments (
  instrument_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  symbol VARCHAR(30) NOT NULL UNIQUE,
  instrument_name VARCHAR(180) NOT NULL,
  exchange_code VARCHAR(40),
  asset_type VARCHAR(20) NOT NULL DEFAULT 'stock',
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  is_major BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE watchlists (
  watchlist_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  watchlist_name VARCHAR(120) NOT NULL DEFAULT 'My Watchlist',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
CREATE INDEX idx_watchlists_user ON watchlists(user_id);

CREATE TABLE watchlist_items (
  watchlist_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  watchlist_id BIGINT NOT NULL,
  instrument_id BIGINT NOT NULL,
  added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (watchlist_id) REFERENCES watchlists(watchlist_id) ON DELETE CASCADE,
  FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE,
  UNIQUE (watchlist_id, instrument_id)
);

CREATE TABLE buddy_categories (
  buddy_category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  category_name VARCHAR(80) NOT NULL,
  monthly_budget DECIMAL(12,2) NOT NULL DEFAULT 0,
  color_hex CHAR(7) NOT NULL DEFAULT '#007AFF',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  UNIQUE (user_id, category_name),
  UNIQUE (user_id, buddy_category_id)
);

CREATE TABLE buddy_expenses (
  expense_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  buddy_category_id BIGINT NOT NULL,
  expense_name VARCHAR(160) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  spent_on DATE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_expense_amount CHECK (amount > 0),
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id, buddy_category_id)
    REFERENCES buddy_categories(user_id, buddy_category_id) ON DELETE CASCADE
);
CREATE INDEX idx_expenses_user_date ON buddy_expenses(user_id, spent_on);

INSERT INTO users (email, password_hash, full_name)
VALUES ('student@tangent.local', '{seed}', 'Student User');

INSERT INTO portfolios (user_id, portfolio_name, goal_description)
VALUES (1, 'My Portfolio', 'My retirement portfolio');

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

INSERT INTO instruments (symbol, instrument_name, exchange_code, asset_type, currency, is_major) VALUES
('AAPL', 'Apple Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('MSFT', 'Microsoft Corporation', 'NASDAQ', 'stock', 'USD', TRUE),
('NVDA', 'NVIDIA Corporation', 'NASDAQ', 'stock', 'USD', TRUE),
('GOOGL', 'Alphabet Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('AMZN', 'Amazon.com Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('META', 'Meta Platforms Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('TSLA', 'Tesla Inc.', 'NASDAQ', 'stock', 'USD', TRUE),
('JPM', 'JPMorgan Chase & Co.', 'NYSE', 'stock', 'USD', TRUE);

INSERT INTO buddy_categories (user_id, category_name, monthly_budget, color_hex) VALUES
(1, 'Food', 0, '#007AFF'),
(1, 'Health', 0, '#30D158'),
(1, 'Housing', 0, '#FF9500'),
(1, 'Utilities', 0, '#FF3B30'),
(1, 'Transport', 0, '#64D2FF'),
(1, 'Family', 0, '#5856D6'),
(1, 'Leisure', 0, '#8E8E93');
