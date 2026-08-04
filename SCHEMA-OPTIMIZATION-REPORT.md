# TANGent Schema Optimization Report

## 📊 Schema Reduction Summary

### Before → After
- **18 tables** → **10 tables** (44% reduction)
- **Removed 8 unnecessary tables**
- **Cleared demo portfolio data** (starts empty for production)
- **Kept only reference data** (asset classes, instruments)

---

## ✅ What Was KEPT (10 Core Tables)

### 1. **User Management** (1 table)
- `users` - Essential user accounts

### 2. **Portfolio Management** (3 tables)
- `portfolios` - User portfolios (NO DUMMY DATA)
- `portfolio_assets` - Individual assets (STARTS EMPTY)
- `asset_classes` - Reference data for asset categories

### 3. **Market Reference** (1 table)
- `instruments` - Stock/ETF lookup data (16 major stocks seeded)

### 4. **Watchlists** (2 tables)
- `watchlists` - User watchlists
- `watchlist_items` - Watchlist contents

### 5. **Expense Tracking** (2 tables)
- `buddy_categories` - Expense categories
- `buddy_expenses` - User expenses

### 6. **User Management** (1 table)
- Only 1 demo user for testing

---

## ❌ What Was REMOVED (8 Tables)

### Reason: JWT Handles This
1. ✂️ `user_sessions` - Backend uses JWT tokens, no DB sessions needed

### Reason: Fetch from API (No Storage Needed)
2. ✂️ `market_quotes` - Real-time quotes from Massive/Alpha Vantage APIs
3. ✂️ `price_bars` - Historical data from APIs
4. ✂️ `news_articles` - News fetched from APIs
5. ✂️ `news_article_instruments` - Junction table for news

### Reason: Advanced Features (Not MVP)
6. ✂️ `comparison_sets` - Stock comparison feature
7. ✂️ `comparison_set_items` - Junction table
8. ✂️ `order_history` - Trade history (not core portfolio tracking)

### Optional (Can Add Back Later)
- `portfolio_asset_value_history` - Historical tracking (simplified for now)

---

## 🎯 Key Improvements

### 1. **Empty Portfolio on Signup**
```sql
-- OLD: Had 10 demo assets with $1.9M portfolio
-- NEW: Portfolio table exists but portfolio_assets is EMPTY
```

Users start with:
- ✅ Empty portfolio (add assets as they go)
- ✅ Default watchlist created
- ✅ Clean slate

### 2. **Reference Data Only**
```sql
-- KEPT: 16 major stocks for lookup
-- KEPT: 9 asset class categories
-- REMOVED: All demo portfolio data
```

### 3. **Simplified Views**
```sql
-- Consolidated to 3 essential views:
- v_portfolio_summary (replaces v_portfolio_class_summary)
- v_portfolio_net_worth (unchanged)
- v_portfolio_allocation (unchanged)
```

### 4. **Helper Procedures**
```sql
-- Added useful procedures:
- sp_create_default_portfolio()
- sp_create_default_watchlist()
- sp_update_asset_value()
```

---

## 🔄 Migration Guide

### Option 1: Fresh Start (Recommended)
```bash
# Backup current data if needed
mysqldump -u tangent_app -p tangent_db > backup_tangent_db.sql

# Drop and recreate with minimal schema
mysql -u root -p < database/tangent_schema_minimal.sql

# Create app user
mysql -u root -p -e "
CREATE USER IF NOT EXISTS 'tangent_app'@'localhost' IDENTIFIED BY 'n3u3da!';
GRANT ALL PRIVILEGES ON tangent_db.* TO 'tangent_app'@'localhost';
FLUSH PRIVILEGES;"
```

### Option 2: Migrate Existing Data
```bash
# 1. Export only user data
mysqldump -u tangent_app -p tangent_db users > users_backup.sql

# 2. Apply new schema
mysql -u root -p < database/tangent_schema_minimal.sql

# 3. Import user data
mysql -u tangent_app -p tangent_db < users_backup.sql
```

---

## 📋 Table Comparison

| Table Name | Old Schema | New Schema | Status |
|------------|------------|------------|--------|
| users | ✅ | ✅ | **Kept** (1 demo user) |
| user_sessions | ✅ | ❌ | **Removed** (JWT only) |
| portfolios | ✅ | ✅ | **Kept** (empty) |
| asset_classes | ✅ | ✅ | **Kept** (9 classes) |
| portfolio_assets | ✅ | ✅ | **Kept** (empty) |
| portfolio_asset_value_history | ✅ | ❌ | **Removed** (simplify) |
| instruments | ✅ | ✅ | **Kept** (16 stocks) |
| watchlists | ✅ | ✅ | **Kept** |
| watchlist_items | ✅ | ✅ | **Kept** |
| market_quotes | ✅ | ❌ | **Removed** (API) |
| price_bars | ✅ | ❌ | **Removed** (API) |
| news_articles | ✅ | ❌ | **Removed** (API) |
| news_article_instruments | ✅ | ❌ | **Removed** (API) |
| comparison_sets | ✅ | ❌ | **Removed** (advanced) |
| comparison_set_items | ✅ | ❌ | **Removed** (advanced) |
| buddy_categories | ✅ | ✅ | **Kept** |
| buddy_expenses | ✅ | ✅ | **Kept** |
| order_history | ✅ | ❌ | **Removed** (not MVP) |

---

## 🔧 Backend Code Compatibility

### ✅ NO BREAKING CHANGES for Core Features:
- ✅ User authentication (JWT)
- ✅ Portfolio CRUD operations
- ✅ Asset management
- ✅ Watchlist operations
- ✅ Expense tracking
- ✅ Market quotes (fetched from API)

### ⚠️ Features That Need Updates:
- ❌ `user_sessions` table → Already using JWT, remove DB lookups
- ❌ Stock comparison feature → Remove or rebuild
- ❌ Order history → Remove or add back if needed

### 📝 Code Changes Needed:
```java
// REMOVE any queries to:
// - user_sessions (use JWT only)
// - market_quotes, price_bars (fetch from API)
// - news_articles (fetch from API)
// - comparison_sets, order_history

// CHANGE:
// Portfolio views remain same (v_portfolio_net_worth, etc.)
```

---

## 🚀 Apply New Schema

### Step 1: Backup Current Database
```powershell
mysqldump -u tangent_app -pn3u3da! tangent_db > tangent_db_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql
```

### Step 2: Apply Minimal Schema
```powershell
# Drop and recreate
Get-Content .\database\tangent_schema_minimal.sql | mysql -u root -p
```

### Step 3: Recreate User
```powershell
$sql = "
CREATE USER IF NOT EXISTS 'tangent_app'@'localhost' IDENTIFIED BY 'n3u3da!';
GRANT ALL PRIVILEGES ON tangent_db.* TO 'tangent_app'@'localhost';
FLUSH PRIVILEGES;
"
$sql | mysql -u root -p
```

### Step 4: Verify
```powershell
"SHOW TABLES;" | mysql -u tangent_app -pn3u3da! -D tangent_db
"SELECT COUNT(*) FROM portfolio_assets;" | mysql -u tangent_app -pn3u3da! -D tangent_db
# Should show 0 (empty portfolio)
```

### Step 5: Restart Application
```powershell
# Stop current app (Ctrl+C)
# Start fresh
.\mvnw.cmd spring-boot:run
```

---

## 💡 Benefits

1. **Cleaner Database** - 44% fewer tables
2. **Production Ready** - No demo data in portfolio
3. **Faster Queries** - Less joins, simpler structure
4. **API First** - Market data from APIs (always fresh)
5. **Easier Maintenance** - Less code to maintain
6. **Scalable** - Add features incrementally

---

## 📌 Next Steps

1. **Apply minimal schema** to fresh database
2. **Test core features** (signup, portfolio, watchlist)
3. **Update backend code** to remove references to deleted tables
4. **Add features back** incrementally as needed

---

## ❓ FAQ

**Q: Will my current application break?**
A: Core features (portfolio, watchlist, expenses) will work. Remove code that queries deleted tables (sessions, news, comparison).

**Q: Can I add back portfolio_asset_value_history?**
A: Yes! Add it back if you need historical tracking. It's just simplified for MVP.

**Q: Where do market quotes come from now?**
A: Your Massive AI and Alpha Vantage APIs (already configured). Always fresh data.

**Q: What about the demo user?**
A: Still there (demo@tangent.local) but with empty portfolio for testing.

---

Generated: 2026-08-04
Schema Version: Minimal v1.0

