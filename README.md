# demo

Spring Boot 4 baseline with virtual threads, PostgreSQL, Liquibase, Spring Security, and springdoc-openapi.

## Generating the project from Spring Initializr

Go to [start.spring.io](https://start.spring.io) and configure the project as follows:

| Setting | Value |
|---------|-------|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.1.0 |
| Packaging | Jar |
| Java | 25 |

Under **Project Metadata**, set your Group, Artifact, and Package name as desired.

Under **Dependencies**, add:

| Dependency | Category |
|------------|----------|
| Spring Web | Web |
| Spring Boot Actuator | Ops |
| Spring Data JPA | SQL |
| Spring Security | Security |
| OAuth2 Resource Server | Security |
| Validation | I/O |
| PostgreSQL Driver | SQL |
| Liquibase Migration | SQL |
| Lombok | Developer Tools |
| Testcontainers | Testing |
| Spring Boot DevTools | Developer Tools |

Click **Generate**, unzip the download, and place the contents at your project root.

> **Note:** springdoc-openapi is not available on Spring Initializr — add it manually to `pom.xml` after generation (see Task 1 in the implementation plan).

---

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
