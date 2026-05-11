# demo

Spring Boot 4 baseline with virtual threads, PostgreSQL, Liquibase, Spring Security, and springdoc-openapi.

## Prerequisites

- Java 25
- Docker (for local Postgres and integration tests)

## Start Postgres

```bash
docker compose up -d
```

This starts a Postgres 16 instance on port 5432 with database `yourapp`, user `yourapp`, password `yourapp` (matches the defaults in `application.yml`).

## Run the application

```bash
./mvnw spring-boot:run
```

## Run all tests

```bash
./mvnw verify
```

Integration tests use Testcontainers — Docker must be running, but the app does not need to be started separately.

## Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `http://localhost:8080/api/hello` | Returns `Hello, World!` |
| GET | `http://localhost:8080/api/hello/{name}` | Returns `Hello, {name}!` (name: 1–50 chars) |

## API docs

Swagger UI: `http://localhost:8080/swagger-ui.html`

OpenAPI JSON: `http://localhost:8080/v3/api-docs`
