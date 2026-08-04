# Database Verification Guide

These queries target the final MySQL 8 schema in `database/tangent_schema_minimal.sql`.
Set `@user_id` to the account being tested. Never paste a database password into this file or a shell command.

```sql
USE tangent_db;
SET @user_id = 1;
```

## Schema and constraints

```sql
SHOW FULL TABLES;
DESCRIBE users;
DESCRIBE portfolio_assets;
DESCRIBE buddy_expenses;

SELECT table_name, column_name, column_type, is_nullable, column_key
FROM information_schema.columns
WHERE table_schema = 'tangent_db'
ORDER BY table_name, ordinal_position;

SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = 'tangent_db'
ORDER BY table_name, constraint_type, constraint_name;

SELECT table_name, index_name, non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_used
FROM information_schema.statistics
WHERE table_schema = 'tangent_db'
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;
```

The application schema should contain these nine base tables: `users`, `portfolios`,
`asset_classes`, `portfolio_assets`, `instruments`, `watchlists`, `watchlist_items`,
`buddy_categories`, and `buddy_expenses`. It also contains three portfolio summary views.

## User and retirement allocations

```sql
SELECT user_id, email, full_name, is_active, created_at
FROM users
WHERE user_id = @user_id;

SELECT pa.asset_id, ac.code, ac.display_name AS asset_class,
       pa.asset_name, pa.quantity, pa.unit_value, pa.current_value, pa.annual_income
FROM portfolio_assets pa
JOIN portfolios p ON p.portfolio_id = pa.portfolio_id
JOIN asset_classes ac ON ac.asset_class_id = pa.asset_class_id
WHERE p.user_id = @user_id
ORDER BY ac.sort_order, pa.asset_id;

SELECT ac.code, ac.display_name AS asset_class,
       SUM(pa.current_value) AS total_amount,
       SUM(pa.annual_income) AS total_annual_income
FROM portfolio_assets pa
JOIN portfolios p ON p.portfolio_id = pa.portfolio_id
JOIN asset_classes ac ON ac.asset_class_id = pa.asset_class_id
WHERE p.user_id = @user_id
GROUP BY ac.asset_class_id, ac.code, ac.display_name, ac.sort_order
ORDER BY ac.sort_order;

SELECT COALESCE(SUM(CASE WHEN ac.is_liability THEN -pa.current_value ELSE pa.current_value END), 0) AS net_worth
FROM portfolio_assets pa
JOIN portfolios p ON p.portfolio_id = pa.portfolio_id
JOIN asset_classes ac ON ac.asset_class_id = pa.asset_class_id
WHERE p.user_id = @user_id;
```

## Daily expenses

```sql
SELECT be.expense_id, be.spent_on, bc.category_name, be.expense_name, be.amount
FROM buddy_expenses be
JOIN buddy_categories bc ON bc.buddy_category_id = be.buddy_category_id
WHERE be.user_id = @user_id
ORDER BY be.spent_on DESC, be.expense_id DESC;

SELECT bc.category_name, SUM(be.amount) AS total_amount
FROM buddy_expenses be
JOIN buddy_categories bc ON bc.buddy_category_id = be.buddy_category_id
WHERE be.user_id = @user_id
GROUP BY bc.buddy_category_id, bc.category_name
ORDER BY total_amount DESC;

SET @from_date = '2026-08-01';
SET @to_date = '2026-08-31';
SELECT COALESCE(SUM(amount), 0) AS period_expenses
FROM buddy_expenses
WHERE user_id = @user_id
  AND spent_on BETWEEN @from_date AND @to_date;
```

## Isolation and integrity checks

Every query below should return zero rows.

```sql
SELECT pa.asset_id
FROM portfolio_assets pa
LEFT JOIN portfolios p ON p.portfolio_id = pa.portfolio_id
WHERE p.portfolio_id IS NULL;

SELECT be.expense_id
FROM buddy_expenses be
LEFT JOIN users u ON u.user_id = be.user_id
LEFT JOIN buddy_categories bc ON bc.buddy_category_id = be.buddy_category_id
WHERE u.user_id IS NULL OR bc.buddy_category_id IS NULL OR bc.user_id <> be.user_id;

SELECT expense_id, amount
FROM buddy_expenses
WHERE amount <= 0;

SELECT p.user_id, pa.asset_id, other_user.user_id AS unexpected_owner
FROM portfolio_assets pa
JOIN portfolios p ON p.portfolio_id = pa.portfolio_id
JOIN portfolios other_user ON other_user.portfolio_id = pa.portfolio_id
WHERE p.user_id <> other_user.user_id;
```

To verify isolation manually, create two accounts through the UI, set `@user_id` to each user in turn,
and confirm that the allocation and expense queries only return that user’s records.
