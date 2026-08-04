# End-to-End Verification

## Automated verification

From the repository root:

```powershell
mvn "-Dmaven.repo.local=$PWD\.m2\repository" clean verify
```

This compiles the application, runs the unit and Spring/H2 integration tests, packages the frontend, and builds the executable JAR.

## Manual lifecycle

1. Run `.\run-dev.ps1` and open `http://localhost:8080`.
2. Sign up as User A with a unique email and confirm that the dashboard opens.
3. Log out, log back in, and confirm the session-protected dashboard still opens.
4. Open Markets and verify stock quotes, history, and comparison. Without API keys, confirm the documented provider fallback/error state appears without crashing the application.
5. Open Portfolio. Confirm the charts show an empty state for a new account.
6. In “Add money to an asset class,” select a class, enter a name and positive amount, and submit.
7. Confirm the allocation appears immediately and the allocation, class contribution, and summary values update without a browser reload.
8. Refresh the browser and confirm the allocation remains.
9. Change the allocation value inline, refresh, and confirm the new value remains.
10. Try zero, negative, missing, and malformed amounts. Confirm they are rejected and charts do not change.
11. Open Buddy, enter a category, positive amount, and date, then submit.
12. Confirm the expense history, monthly summary, category pie chart, and daily chart update without a browser reload.
13. Refresh the browser and confirm the expense remains.
14. Change an expense amount inline and confirm the chart updates only after the server accepts the change.
15. Sign up as User B. Confirm User A’s allocations and expenses are absent.
16. Using User B’s token, attempt `PUT /api/app/assets/{User-A-asset-id}` and `PUT /api/app/expenses/{User-A-expense-id}`. Both must return `404` and leave User A’s records unchanged.
17. For MySQL, run the queries in `DATABASE-VERIFICATION-GUIDE.md` for both user IDs and compare the SQL totals with the chart totals.
18. Log out and request `/api/app/bootstrap` without a token. Confirm the API returns `401`.
19. Run the automated verification command again after the manual test.
