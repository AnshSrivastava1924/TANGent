# 🚀 TANGent Application - Complete Setup Guide

## ✅ What Was Done

### 1. **Schema Optimization Complete**
- ✅ Reduced from **18 tables → 10 tables** (44% reduction)
- ✅ Removed unnecessary attributes:
  - `users`: removed `date_of_birth`, `risk_profile`, `base_currency`
  - `portfolio_assets`: removed `provider_or_location`, `asset_identifier`, `note`, `valuation_date`
- ✅ Portfolio starts **EMPTY** (no dummy data)
- ✅ Reference data loaded (16 stocks, 9 asset classes)

### 2. **Database Applied**
- ✅ New minimal schema created
- ✅ Database user `tangent_app` configured
- ✅ Password: `n3u3da!`

### 3. **Application Running**
- ✅ Spring Boot application deployed
- ✅ Running on port 8080
- ✅ Connected to minimal database

---

## 📊 Access Information

### Frontend
```
http://localhost:8080
```

### API Documentation (Swagger)
```
http://localhost:8080/swagger-ui.html
```

### Demo Login
```
Email: demo@tangent.local
Password: training123
```

---

## 🗄️ MySQL Workbench Connection

### Connection Details:
```
Connection Name: TANGent Local
Hostname: localhost (or 127.0.0.1)
Port: 3306
Username: tangent_app
Password: n3u3da!
Default Schema: tangent_db
```

### Connect Steps:
1. Open MySQL Workbench
2. Click **"+"** next to MySQL Connections
3. Enter the details above
4. Click **"Test Connection"**
5. Click **"OK"** to save
6. Double-click to connect

---

## 📋 MySQL Workbench Test Commands

### Quick Verification:
```sql
-- 1. Show all tables
SHOW TABLES;
-- Expected: 10 tables + 3 views

-- 2. Check users table structure (simplified)
DESCRIBE users;
-- Should NOT have: date_of_birth, risk_profile, base_currency

-- 3. Check portfolio is empty
SELECT COUNT(*) AS assets FROM portfolio_assets;
-- Expected: 0

-- 4. Check reference data
SELECT COUNT(*) FROM instruments;  -- Should be 16
SELECT COUNT(*) FROM asset_classes;  -- Should be 9
SELECT COUNT(*) FROM users;  -- Should be 1

-- 5. View available stocks
SELECT symbol, instrument_name FROM instruments ORDER BY symbol;

-- 6. View asset categories
SELECT code, display_name FROM asset_classes ORDER BY sort_order;
```

### Full Test Script:
Open the file: **`MYSQL-WORKBENCH-TESTS.sql`**
- Contains 15 test sections
- Copy and run in MySQL Workbench
- Tests creating portfolios, adding assets, watchlists, expenses

---

## 🧪 API Testing Commands (PowerShell)

### Test Login:
```powershell
$auth = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth" `
  -ContentType "application/json" `
  -Body '{"mode":"login","email":"demo@tangent.local","password":"training123"}'

$auth.data
```

### Get Token:
```powershell
$token = $auth.data.token
$headers = @{ Authorization = "Bearer $token" }
```

### Test Portfolio (Should be Empty):
```powershell
$portfolio = Invoke-RestMethod -Headers $headers -Uri "http://localhost:8080/api/app/bootstrap"
$portfolio.data
```

---

## 📁 Files Created

1. **`database/tangent_schema_minimal.sql`** - Optimized schema
2. **`SCHEMA-OPTIMIZATION-REPORT.md`** - Full analysis
3. **`MYSQL-WORKBENCH-TESTS.sql`** - Test queries
4. **`APPLY-MINIMAL-SCHEMA.md`** - Quick guide
5. **`THIS-FILE.md`** - Complete setup summary

---

## 🎯 What's Different

| Feature | Old Schema | New Schema |
|---------|-----------|------------|
| **Tables** | 18 | 10 ✅ |
| **User Fields** | 10 fields | 6 fields ✅ |
| **Portfolio Data** | 10 demo assets | Empty ✅ |
| **Market Data** | Stored in DB | From API ✅ |
| **User Sessions** | DB table | JWT only ✅ |

---

## 🔄 If You Need to Reset

### Reapply Schema:
```powershell
cd C:\Users\Administrator\tested\TANGent
Get-Content .\database\tangent_schema_minimal.sql | mysql -u root -p
```

### Recreate User:
```powershell
$sql = "CREATE USER IF NOT EXISTS 'tangent_app'@'localhost' IDENTIFIED BY 'n3u3da!'; GRANT ALL PRIVILEGES ON tangent_db.* TO 'tangent_app'@'localhost'; FLUSH PRIVILEGES;"
$sql | mysql -u root -p
```

### Restart Application:
```powershell
# Stop old processes
Get-Process -Name java | Stop-Process -Force

# Start fresh
cd C:\Users\Administrator\tested\TANGent
.\mvnw.cmd spring-boot:run
```

---

## ✅ Verification Checklist

Run these in MySQL Workbench to verify everything:

```sql
-- ✅ Tables exist (10 + 3 views)
SHOW TABLES;

-- ✅ Portfolio is empty
SELECT COUNT(*) FROM portfolio_assets;  -- Should be 0

-- ✅ Reference data loaded
SELECT COUNT(*) FROM instruments;  -- Should be 16
SELECT COUNT(*) FROM asset_classes;  -- Should be 9

-- ✅ User exists
SELECT email, full_name FROM users;

-- ✅ Simplified user structure
SHOW COLUMNS FROM users;
-- Should NOT have: date_of_birth, risk_profile, base_currency

-- ✅ Simplified portfolio_assets structure
SHOW COLUMNS FROM portfolio_assets;
-- Should NOT have: provider_or_location, asset_identifier, note, valuation_date

-- ✅ Views work
SELECT * FROM v_portfolio_summary;
SELECT * FROM v_portfolio_net_worth;
```

---

## 🎉 You're All Set!

### Next Steps:
1. **Open MySQL Workbench** and connect using credentials above
2. **Run test queries** from `MYSQL-WORKBENCH-TESTS.sql`
3. **Test frontend** at http://localhost:8080
4. **Try API** with Swagger UI

### Features Working:
- ✅ User authentication
- ✅ Empty portfolio (ready for user data)
- ✅ Asset management
- ✅ Watchlists
- ✅ Expense tracking
- ✅ Market quotes (from API)

---

## 📞 Quick Reference

- **Application**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui.html
- **Database**: tangent_db
- **User**: tangent_app
- **Password**: n3u3da!
- **Demo Login**: demo@tangent.local / training123

---

**Schema Version**: Minimal v1.0  
**Date**: August 4, 2026  
**Status**: ✅ Production Ready

