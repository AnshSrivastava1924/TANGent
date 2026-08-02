CREATE TABLE users (
  user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(160) NOT NULL,
  date_of_birth DATE,
  risk_profile VARCHAR(32) NOT NULL DEFAULT 'moderate',
  base_currency CHAR(3) NOT NULL DEFAULT 'USD',
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portfolios (
  portfolio_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  portfolio_name VARCHAR(160) NOT NULL,
  goal_description VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

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
  provider_or_location VARCHAR(160),
  asset_identifier VARCHAR(80),
  quantity DECIMAL(18,4) NOT NULL DEFAULT 1,
  unit_value DECIMAL(18,2) NOT NULL DEFAULT 0,
  current_value DECIMAL(18,2) GENERATED ALWAYS AS (quantity * unit_value),
  annual_income DECIMAL(18,2) NOT NULL DEFAULT 0,
  note VARCHAR(255),
  valuation_date DATE NOT NULL DEFAULT CURRENT_DATE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (portfolio_id) REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
  FOREIGN KEY (asset_class_id) REFERENCES asset_classes(asset_class_id)
);

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
  watchlist_name VARCHAR(120) NOT NULL DEFAULT 'Default Watchlist',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE watchlist_items (
  watchlist_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  watchlist_id BIGINT NOT NULL,
  instrument_id BIGINT NOT NULL,
  added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (watchlist_id) REFERENCES watchlists(watchlist_id) ON DELETE CASCADE,
  FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id),
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
  UNIQUE (user_id, category_name)
);

CREATE TABLE buddy_expenses (
  expense_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  buddy_category_id BIGINT NOT NULL,
  expense_name VARCHAR(160) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  spent_on DATE NOT NULL,
  notes VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (buddy_category_id) REFERENCES buddy_categories(buddy_category_id)
);

INSERT INTO users (email, password_hash, full_name, date_of_birth, risk_profile)
VALUES ('student@tangent.local', '{seed}', 'Anita Sharma', '1956-04-18', 'conservative');

INSERT INTO portfolios (user_id, portfolio_name, goal_description)
VALUES (1, 'Retirement household portfolio', 'Balance income, safety, housing, and long-term family wealth');

INSERT INTO asset_classes (code, display_name, purpose, is_liability, is_liquid, sort_order) VALUES
('cash', 'Cash and Bank Accounts', 'Ready money for bills, emergencies, and near-term care needs.', FALSE, TRUE, 10),
('securities', 'Listed Securities', 'Stocks and ETFs that provide growth and dividend potential.', FALSE, TRUE, 20),
('fixedIncome', 'Bonds and Fixed Income', 'Stability, predictable coupons, and lower volatility.', FALSE, TRUE, 30),
('funds', 'Mutual Funds and ETFs', 'Managed diversification across markets and sectors.', FALSE, TRUE, 40),
('pension', 'Pension Sources', 'Expected yearly income from retirement plans.', FALSE, FALSE, 50),
('annuities', 'Annuities', 'Contracted income that can support regular expenses.', FALSE, FALSE, 60),
('housing', 'Housing and Real Estate', 'Home equity and rental property value.', FALSE, FALSE, 70),
('commodities', 'Gold and Commodities', 'Inflation hedge and alternative asset exposure.', FALSE, FALSE, 80),
('insurance', 'Insurance Cash Value', 'Policies with accessible value or estate-planning support.', FALSE, FALSE, 90),
('liabilities', 'Loans and Debts', 'Amounts owed that reduce household net worth.', TRUE, FALSE, 100);

INSERT INTO portfolio_assets
(portfolio_id, asset_class_id, asset_name, provider_or_location, asset_identifier, quantity, unit_value, annual_income, note, valuation_date) VALUES
(1,1,'Checking account','Community Bank',NULL,1,24500,0,'Monthly spending account','2026-07-31'),
(1,1,'Savings account','Community Bank',NULL,1,78000,1800,'Emergency reserve','2026-07-31'),
(1,1,'Fixed deposit ladder','National Bank',NULL,1,135000,6400,'Low-risk income','2026-07-31'),
(1,2,'Dividend stock basket','Brokerage','DIV-BASKET',1,162000,6200,'Blue-chip shares','2026-07-31'),
(1,2,'Broad market ETF','Brokerage','SPY',420,280.95,2100,'Diversified equity exposure','2026-07-31'),
(1,3,'Government bonds','Treasury Direct','GOV-BOND',1,220000,10500,'Core retirement income','2026-07-31'),
(1,3,'Municipal bond fund','Brokerage','MUNI-FUND',1,94000,3900,'Tax-aware income','2026-07-31'),
(1,4,'Balanced mutual fund','Brokerage','BAL-FUND',1,86000,2600,'Moderate risk','2026-07-31'),
(1,4,'Healthcare ETF','Brokerage','XLV',300,140,700,'Sector allocation','2026-07-31'),
(1,5,'Company pension','Former employer',NULL,1,0,42000,'Annual pension income','2026-07-31'),
(1,5,'Social security','Government',NULL,1,0,31800,'Annual benefit estimate','2026-07-31'),
(1,6,'Lifetime annuity','Secure Life',NULL,1,175000,15600,'Guaranteed yearly payout','2026-07-31'),
(1,7,'Primary home','Springfield',NULL,1,485000,0,'Mortgage-free residence','2026-07-31'),
(1,7,'Rental apartment','Lakeside',NULL,1,265000,18000,'Rental income property','2026-07-31'),
(1,8,'Gold holdings','Home safe',NULL,1,36000,0,'Long-term reserve','2026-07-31'),
(1,9,'Whole life cash value','Secure Life',NULL,1,58000,0,'Policy cash value','2026-07-31'),
(1,10,'Home equity line','Community Bank',NULL,1,41000,0,'Outstanding balance','2026-07-31'),
(1,10,'Car loan','Auto Finance',NULL,1,12500,0,'Remaining balance','2026-07-31');

INSERT INTO instruments (symbol, instrument_name, exchange_code, asset_type, currency, is_major) VALUES
('AAPL','Apple Inc.','NASDAQ','stock','USD',TRUE),
('MSFT','Microsoft Corporation','NASDAQ','stock','USD',TRUE),
('NVDA','NVIDIA Corporation','NASDAQ','stock','USD',TRUE),
('GOOGL','Alphabet Inc.','NASDAQ','stock','USD',TRUE),
('AMZN','Amazon.com Inc.','NASDAQ','stock','USD',TRUE),
('META','Meta Platforms Inc.','NASDAQ','stock','USD',TRUE),
('TSLA','Tesla Inc.','NASDAQ','stock','USD',TRUE),
('JPM','JPMorgan Chase & Co.','NYSE','stock','USD',TRUE);

INSERT INTO watchlists (user_id, watchlist_name) VALUES (1, 'Default Watchlist');
INSERT INTO watchlist_items (watchlist_id, instrument_id) VALUES (1,1),(1,2),(1,3);

INSERT INTO buddy_categories (user_id, category_name, monthly_budget, color_hex) VALUES
(1,'Food',900,'#007AFF'),(1,'Health',650,'#30D158'),(1,'Housing',1200,'#FF9500'),
(1,'Utilities',420,'#FF3B30'),(1,'Transport',360,'#64D2FF'),
(1,'Family',500,'#5856D6'),(1,'Leisure',300,'#8E8E93');

INSERT INTO buddy_expenses (user_id, buddy_category_id, expense_name, amount, spent_on, notes) VALUES
(1,1,'Groceries',84,'2026-07-31','Weekly food shopping'),
(1,2,'Prescription refill',42,'2026-07-31','Monthly medication'),
(1,4,'Electric bill',126,'2026-07-30','Utility payment'),
(1,5,'Taxi to clinic',28,'2026-07-30','Medical transport'),
(1,6,'Dinner with family',96,'2026-07-29','Family meal'),
(1,3,'Gardening supplies',64,'2026-07-28','Home maintenance'),
(1,7,'Movie tickets',34,'2026-07-27','Leisure outing'),
(1,1,'Bakery and fruit',31,'2026-07-26','Daily food purchase'),
(1,2,'Doctor copay',55,'2026-07-25','Clinic visit'),
(1,4,'Water bill',48,'2026-07-24','Utility payment');
