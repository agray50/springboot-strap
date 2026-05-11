# Spring Boot Baseline Design

**Date:** 2026-05-11
**Project:** springboot-strap
**Stack:** Spring Boot 4.1.0-RC1 · Java 25 · Maven · PostgreSQL · Liquibase · Spring Security · Lombok · Testcontainers · springdoc-openapi

---

## Goal

Establish a production-ready project baseline starting from the Spring Initializr output (`com.example.demo`), then add a `/api/hello` endpoint to prove the stack end-to-end. No domain logic yet — this is the foundation everything else builds on.

---

## Package layout

Package-by-feature under `com.example.demo`:

```
com.example.demo
├── DemoApplication.java              (existing)
├── config/
│   └── SecurityConfig.java
├── common/
│   └── GlobalExceptionHandler.java
└── hello/
    ├── HelloController.java
    ├── HelloService.java
    └── HelloResponse.java
```

Test classes stay in `com.example.demo` (same package as existing generated tests):

```
DemoApplicationTests.java             (existing — add @ActiveProfiles("test"))
TestcontainersConfiguration.java      (existing — update image to postgres:16-alpine)
TestDemoApplication.java              (existing)
HelloServiceTest.java                 (new)
HelloControllerTest.java              (new)
HelloIntegrationTest.java             (new)
```

---

## pom.xml changes

Add one dependency:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version><!-- latest version compatible with Spring Boot 4 / Spring Framework 7 --></version>
</dependency>
```

Resolve exact version during implementation — springdoc 2.8+ targets SF6/SB3; confirm a SF7/SB4-compatible release exists before adding.

No other dependency changes.

---

## Configuration

### application.yml (replaces application.properties)

```yaml
spring:
  application:
    name: demo
  threads:
    virtual:
      enabled: true
  datasource:
    url: jdbc:postgresql://localhost:5432/yourapp
    username: ${DB_USER:yourapp}
    password: ${DB_PASSWORD:yourapp}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        jdbc.batch_size: 25
        order_inserts: true
        order_updates: true
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml

server:
  shutdown: graceful
  error:
    include-message: never
    include-stacktrace: never

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
```

### application-test.yml

Minimal overrides for the test profile. No datasource URL — `@ServiceConnection` injects it from the container. Hibernate defers to Liquibase for schema ownership.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none
  liquibase:
    enabled: true
```

Slice tests (`@WebMvcTest`) do not load JPA or Liquibase so these overrides are irrelevant to them. The integration test activates this profile via `@ActiveProfiles("test")`.

---

## Liquibase

```
src/main/resources/db/changelog/
├── db.changelog-master.yaml     (includeAll pointing at changes/)
└── changes/
    └── .gitkeep
```

`db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - includeAll:
      path: classpath:db/changelog/changes/
```

No migrations yet — the `changes/` directory is ready for the first real migration.

---

## SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/hello", "/api/hello/**").permitAll()
                .anyRequest().authenticated());
        // Uncomment to enable JWT auth once an IdP is configured:
        // .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

`/api/hello/**` covers both the plain and `/{name}` variants.

---

## GlobalExceptionHandler

Uses Spring's `ProblemDetail` (RFC 7807). Two handlers:

- `handleException(Exception)` → 500, generic message
- `handleValidation(MethodArgumentNotValidException)` → 400, field-level error list extracted from `BindingResult`

No suppression of Spring Security or framework exceptions — those propagate normally before reaching this handler.

---

## Hello feature

### HelloResponse (record)

```java
public record HelloResponse(String message, Instant timestamp) {}
```

### HelloService

- `@Slf4j` + `@RequiredArgsConstructor`
- `greet(String name)` — returns `new HelloResponse("Hello, " + name + "!", Instant.now())`
- Default `greet()` — delegates to `greet("World")`
- Logs at DEBUG on each call

### HelloController

- `@Validated` at class level (required for `@PathVariable` Bean Validation to activate)
- Constructor injection via `@RequiredArgsConstructor`
- `GET /api/hello` → `greet()`
- `GET /api/hello/{name}` → `greet(name)`, where `{name}` is `@NotBlank @Size(max = 50)`

---

## Tests

### HelloServiceTest

Plain JUnit 5. No Spring context. Instantiates `HelloService` directly. Verifies:
- Default `greet()` returns message `"Hello, World!"` and a non-null timestamp
- `greet("Alice")` returns message `"Hello, Alice!"`

### HelloControllerTest

`@WebMvcTest(HelloController.class)` + `@Import(SecurityConfig.class)`.

`SecurityConfig` must be imported explicitly — `@WebMvcTest` does not pick up `@Configuration` classes outside the web-layer slice automatically. Importing it ensures our `permitAll()` rules are active, so no `@WithMockUser` is needed.

Verifies:
- `GET /api/hello` → 200, body contains `"Hello, World!"`
- `GET /api/hello/Alice` → 200, body contains `"Hello, Alice!"`

### DemoApplicationTests (existing — update)

Add `@ActiveProfiles("test")` so it loads `application-test.yml` (`ddl-auto: none`) and the Postgres container URL is injected via `@ServiceConnection`. Without this the test would try to connect to `localhost:5432` with `ddl-auto: validate`.

### HelloIntegrationTest

`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@Import(TestcontainersConfiguration.class)`.

`TestcontainersConfiguration` uses `@ServiceConnection` on a `PostgreSQLContainer("postgres:16-alpine")` — this injects the datasource URL automatically, overriding anything in application-test.yml.

Verifies:
- Full application context loads
- Liquibase runs without errors (changelog applies cleanly against empty schema)
- `GET /api/hello` → 200

---

## README

Sections:
- Prerequisites: Java 25, Docker
- Start Postgres: `docker compose up -d` (user provides their own `compose.yml`)
- Run app: `./mvnw spring-boot:run`
- Run tests: `./mvnw verify`
- Hello endpoint: `http://localhost:8080/api/hello`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Verification

After implementation, run `./mvnw clean verify` and confirm:
- Project compiles with no errors
- All tests pass (unit, slice, integration)
- Application starts cleanly assuming Postgres is available

---

## Constraints

- Java records for all DTOs
- Constructor injection everywhere; no `@Autowired`, no field injection
- Lombok limited to: `@Slf4j`, `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` — no `@Data`
- No new dependencies beyond springdoc-openapi (already agreed)
- Standard Spring Boot naming conventions throughout
