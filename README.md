# TANGent Portfolio Manager

This branch contains the integrated Spring Boot backend and the frontend imported
from `main`. Authentication, portfolio values, Buddy expenses, and watchlists are
database-backed. Market quotes, history, comparison, and news use Massive or Alpha
Vantage when keys are configured and otherwise return deterministic demo data.

## Run locally without MySQL

The `dev` profile uses a temporary in-memory H2 database:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Open Swagger UI at <http://localhost:8080/swagger-ui.html>. The generated OpenAPI
contract is available as JSON at <http://localhost:8080/v3/api-docs> and YAML at
<http://localhost:8080/v3/api-docs.yaml>.

## Run with MySQL

Start MySQL and create the schema:

```bash
mysql -u root -p < database/tangent_schema_seed.sql
```

Create a dedicated application user from a MySQL session (replace the example password):

```sql
CREATE USER IF NOT EXISTS 'tangent_app'@'localhost' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON tangent_db.* TO 'tangent_app'@'localhost';
FLUSH PRIVILEGES;
```

For a local machine, create `config/application.properties` (this path is ignored
by Git) with the matching values:

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/tangent_db
spring.datasource.username=tangent_app
spring.datasource.password=your-password
jwt.secret=a-random-secret-with-at-least-32-characters
market.massive.api-key=your-key
market.alpha-vantage.api-key=your-key
```

Then start the integrated frontend and backend:

```bash
./run.sh
```

Environment variables from `.env.example` remain supported when running Maven
directly. Spring Boot does not automatically load `.env` files.

## Verify

```bash
./mvnw clean install
```

Tests use an isolated H2 database and do not require a running MySQL server.

The seeded development account is:

```text
student@tangent.local
training123
```

The application itself is available at <http://localhost:8080/>.

## Backend architecture

The backend is a traditional layered monolith. It deploys as one Spring Boot
application connected to one MySQL database:

```text
src/main/java/com/tangent
├── TangentApplication.java
├── config
├── constant
├── controller
├── dto
├── exception
├── repository
├── security
├── service
└── wrapper
```

Controllers expose HTTP endpoints and validation, DTOs define API contracts,
services contain business rules and transaction boundaries, repositories own
SQL/database access, constants hold shared immutable values, and wrappers define
the common API envelope. Successful JSON endpoints use `ApiResponse<T>`; failures
use the matching typed error response from the global exception handler.

## Market provider keys

Set one or both keys before starting the application:

```bash
export MASSIVE_API_KEY="your-key"
export ALPHA_VANTAGE_API_KEY="your-key"
```

Massive is preferred for quotes, aggregates, and news. Alpha Vantage is used as a
fallback. The backend never invents stock prices: if configured providers reject
a key, exceed a rate limit, or lack endpoint entitlement, the API returns `503`
with the provider reason. Responses include `provider` and `freshness` fields.

Live exchange data depends on the provider subscription. To request a paid Alpha
Vantage entitlement, set `ALPHA_VANTAGE_ENTITLEMENT=realtime` (or `delayed`).
Without it, Alpha Vantage documents its quote response as end-of-day data.
