# Apply the Minimal MySQL Schema

The canonical schema contains nine base tables and three portfolio summary views.

For a clean installation (this recreates `tangent_db`):

```powershell
Get-Content .\database\tangent_schema_minimal.sql | mysql -u root -p
```

For an existing database:

1. Create a verified backup: `mysqldump -u root -p --routines --triggers tangent_db > tangent_db_before_minimal.sql`.
2. Check for invalid expenses: `SELECT * FROM tangent_db.buddy_expenses WHERE amount <= 0;`.
3. Review `database/migrate_to_minimal.sql`, especially the documented column and table removals.
4. Apply it: `Get-Content .\database\migrate_to_minimal.sql | mysql -u root -p`.
5. Run every query in `DATABASE-VERIFICATION-GUIDE.md`.

Create a least-privilege local application account from a MySQL session, using your own password:

```sql
CREATE USER IF NOT EXISTS 'tangent_app'@'localhost' IDENTIFIED BY 'replace-with-a-strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON tangent_db.* TO 'tangent_app'@'localhost';
FLUSH PRIVILEGES;
```

Place the matching connection values in the ignored `config/application.properties`; never commit credentials.
