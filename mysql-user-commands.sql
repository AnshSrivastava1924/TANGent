-- ============================================
-- MYSQL COMMANDS TO RUN
-- ============================================
-- Copy and paste these into MySQL after logging in as root
--
-- INSTRUCTIONS:
-- 1. Open Command Prompt or PowerShell
-- 2. Run: mysql -u root -p
-- 3. Enter your MySQL root password
-- 4. Copy and paste the commands below
-- 5. Replace 'tangent123' with YOUR chosen password
-- ============================================

-- Create the application user
-- ⬇️⬇️⬇️ CHANGE 'tangent123' to your password ⬇️⬇️⬇️
CREATE USER IF NOT EXISTS 'tangent_app'@'localhost' IDENTIFIED BY 'tangent123';

-- Grant permissions
GRANT ALL PRIVILEGES ON tangent_db.* TO 'tangent_app'@'localhost';

-- Apply changes
FLUSH PRIVILEGES;

-- Verify the user was created
SELECT User, Host FROM mysql.user WHERE User = 'tangent_app';

-- Exit MySQL
EXIT;

-- ============================================
-- AFTER RUNNING THESE COMMANDS:
-- ============================================
-- 1. Open: config\application.properties
-- 2. Find line: spring.datasource.password=PUT_YOUR_MYSQL_PASSWORD_HERE
-- 3. Replace PUT_YOUR_MYSQL_PASSWORD_HERE with: tangent123
--    (or whatever password you chose above)
-- 4. Find line: jwt.secret=PUT_ANY_RANDOM_TEXT_HERE_MINIMUM_32_CHARS
-- 5. Replace with any random text (32+ characters)
-- 6. Save the file
-- ============================================

