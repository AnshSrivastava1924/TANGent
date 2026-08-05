-- Migration from database/tangent_schema_seed.sql to the lean application schema.
-- Back up tangent_database before running. This intentionally removes objects proven unused by src/ and public/.
USE tangent_database;

-- Preserve a useful legacy identifier as the optional ticker before removing metadata columns.
ALTER TABLE portfolio_assets ADD COLUMN symbol VARCHAR(30) NULL AFTER asset_name;
UPDATE portfolio_assets
SET symbol = asset_identifier
WHERE asset_identifier REGEXP '^[A-Za-z0-9.-]{1,30}$';

DROP TRIGGER IF EXISTS trg_portfolio_assets_after_insert;
DROP TRIGGER IF EXISTS trg_portfolio_assets_after_update;
DROP PROCEDURE IF EXISTS sp_update_portfolio_asset_value;
DROP PROCEDURE IF EXISTS sp_add_buddy_expense;

DROP VIEW IF EXISTS v_watchlist_latest;
DROP VIEW IF EXISTS v_latest_market_quote;
DROP VIEW IF EXISTS v_buddy_monthly_summary;
DROP VIEW IF EXISTS v_buddy_category_summary;
DROP VIEW IF EXISTS v_portfolio_allocation;
DROP VIEW IF EXISTS v_portfolio_net_worth;
DROP VIEW IF EXISTS v_portfolio_class_summary;

DROP TABLE IF EXISTS news_article_instruments;
DROP TABLE IF EXISTS comparison_set_items;
DROP TABLE IF EXISTS portfolio_asset_value_history;
DROP TABLE IF EXISTS market_quotes;
DROP TABLE IF EXISTS price_bars;
DROP TABLE IF EXISTS news_articles;
DROP TABLE IF EXISTS comparison_sets;
DROP TABLE IF EXISTS order_history;
DROP TABLE IF EXISTS user_sessions;

ALTER TABLE users
  DROP COLUMN date_of_birth,
  DROP COLUMN risk_profile,
  DROP COLUMN base_currency;

ALTER TABLE portfolio_assets
  DROP COLUMN provider_or_location,
  DROP COLUMN asset_identifier,
  DROP COLUMN note,
  DROP COLUMN valuation_date;

ALTER TABLE buddy_expenses DROP COLUMN notes;

ALTER TABLE buddy_expenses DROP FOREIGN KEY fk_expenses_category;
ALTER TABLE buddy_categories ADD UNIQUE KEY uq_user_category_id (user_id, buddy_category_id);
ALTER TABLE buddy_expenses ADD CONSTRAINT fk_expenses_user_category
  FOREIGN KEY (user_id, buddy_category_id)
  REFERENCES buddy_categories(user_id, buddy_category_id) ON DELETE CASCADE;

-- Reject invalid future expenses. Correct any legacy zero/negative rows before this statement.
ALTER TABLE buddy_expenses DROP CHECK chk_expense_amount;
ALTER TABLE buddy_expenses ADD CONSTRAINT chk_expense_amount CHECK (amount > 0);

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
GROUP BY p.portfolio_id, p.user_id, p.portfolio_name, ac.asset_class_id,
         ac.code, ac.display_name, ac.is_liability, ac.is_liquid;

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
