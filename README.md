# TANGent Portfolio Manager

TANGent is a full-stack portfolio management application built with a Spring Boot backend and an integrated frontend. It provides a clean experience for tracking holdings, monitoring watchlists, and viewing market-linked insights.

## Quick Start

Run the application in development mode:

```bash
./mvnw spring-boot:run
```

Or on Windows PowerShell:

```powershell
.\run-dev.ps1
```

Open the app at <http://localhost:8080/>.

## Features

- User authentication and secure session handling
- Portfolio tracking with current value insights
- Watchlist management for selected symbols
- Buddy expense and shared tracking support
- Market quote, history, comparison, and news integration
- Swagger/OpenAPI documentation for backend APIs

## API Docs

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>

## Build and Test

```bash
./mvnw clean install
```

Tests run on an isolated in-memory setup and do not require external database configuration.

## Project Structure

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

## Notes

- Environment variables can be used for secrets and external provider keys.
- Keep local runtime configuration and secrets out of version control.
