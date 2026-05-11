# Spring Boot Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish a production-ready Spring Boot 4.1.0-RC1 baseline — package-by-feature layout, full configuration, and a `/api/hello` endpoint verified end-to-end by unit, slice, and integration tests.

**Architecture:** Package-by-feature under `com.example.demo` with `config`, `common`, and `hello` sub-packages. Security is stateless with the JWT/OAuth2 resource server block present but commented out for one-line activation later. Liquibase owns all schema changes; Testcontainers supplies a real Postgres instance for integration tests.

**Tech Stack:** Java 25, Spring Boot 4.1.0-RC1, Spring MVC, Spring Security, Liquibase, PostgreSQL, Testcontainers, Lombok, springdoc-openapi 3.0.3, JUnit 5, MockMvc

---

## File Map

| Action | Path |
|--------|------|
| Modify | `pom.xml` |
| Delete | `src/main/resources/application.properties` |
| Create | `src/main/resources/application.yml` |
| Create | `src/main/resources/application-test.yml` |
| Create | `src/main/resources/db/changelog/db.changelog-master.yaml` |
| Create | `src/main/resources/db/changelog/changes/.gitkeep` |
| Create | `compose.yml` |
| Create | `src/main/java/com/example/demo/config/SecurityConfig.java` |
| Create | `src/main/java/com/example/demo/common/GlobalExceptionHandler.java` |
| Create | `src/main/java/com/example/demo/hello/HelloResponse.java` |
| Create | `src/main/java/com/example/demo/hello/HelloService.java` |
| Create | `src/main/java/com/example/demo/hello/HelloController.java` |
| Modify | `src/test/java/com/example/demo/TestcontainersConfiguration.java` |
| Modify | `src/test/java/com/example/demo/DemoApplicationTests.java` |
| Create | `src/test/java/com/example/demo/HelloServiceTest.java` |
| Create | `src/test/java/com/example/demo/HelloControllerTest.java` |
| Create | `src/test/java/com/example/demo/HelloIntegrationTest.java` |
| Create | `README.md` |

---

## Task 1: Add springdoc-openapi to pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add the springdoc-openapi 3.0.3 dependency**

Open `pom.xml` and add this block inside `<dependencies>`, after the last runtime dependency and before the test dependencies:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.3</version>
</dependency>
```

> **Note:** 3.0.3 targets Spring Boot 4.0.5. If it fails to resolve or throws a Jackson-related error at startup, check https://github.com/springdoc/springdoc-openapi/releases for a newer 3.x release and update the version number.

- [ ] **Step 2: Verify the dependency resolves**

```bash
./mvnw dependency:resolve -q
```

Expected: BUILD SUCCESS with no resolution errors. If Jackson conflicts appear, set this property in `pom.xml` `<properties>` to force Jackson 3:

```xml
<jackson-bom.version>3.0.0</jackson-bom.version>
```

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: add springdoc-openapi 3.0.3 for Spring Boot 4"
```

---

## Task 2: Configuration files

**Files:**
- Delete: `src/main/resources/application.properties`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-test.yml`
- Create: `compose.yml`

- [ ] **Step 1: Delete application.properties**

```bash
rm src/main/resources/application.properties
```

- [ ] **Step 2: Create application.yml**

Create `src/main/resources/application.yml`:

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
        jdbc:
          batch_size: 25
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

- [ ] **Step 3: Create application-test.yml**

Create `src/main/resources/application-test.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none
  liquibase:
    enabled: true
```

`ddl-auto: none` because Liquibase owns the schema in tests — Hibernate must not try to validate or create it.
No datasource URL here; `@ServiceConnection` in `TestcontainersConfiguration` injects the real container URL at runtime.

- [ ] **Step 4: Create compose.yml for local development**

Create `compose.yml` at the project root:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: yourapp
      POSTGRES_PASSWORD: yourapp
      POSTGRES_DB: yourapp
    ports:
      - "5432:5432"
```

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/application.yml \
        src/main/resources/application-test.yml \
        compose.yml
git commit -m "chore: replace application.properties with application.yml, add test profile and compose"
```

---

## Task 3: Liquibase changelog structure

**Files:**
- Create: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Create: `src/main/resources/db/changelog/changes/.gitkeep`

- [ ] **Step 1: Create the master changelog**

Create `src/main/resources/db/changelog/db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - includeAll:
      path: classpath:db/changelog/changes/
      errorIfMissingOrEmpty: false
```

`errorIfMissingOrEmpty: false` prevents Liquibase from failing when `changes/` is empty (which it is until the first real migration is added).

- [ ] **Step 2: Add .gitkeep so the changes/ directory is tracked**

```bash
touch src/main/resources/db/changelog/changes/.gitkeep
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/changelog/
git commit -m "chore: add Liquibase changelog structure"
```

---

## Task 4: SecurityConfig

**Files:**
- Create: `src/main/java/com/example/demo/config/SecurityConfig.java`

No dedicated unit test — correctness is verified by `HelloControllerTest` (Task 7) which imports this class.

- [ ] **Step 1: Create SecurityConfig**

Create `src/main/java/com/example/demo/config/SecurityConfig.java`:

```java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

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

- [ ] **Step 2: Verify it compiles**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/demo/config/
git commit -m "feat: add stateless SecurityConfig with permit-all for hello and actuator"
```

---

## Task 5: GlobalExceptionHandler

**Files:**
- Create: `src/main/java/com/example/demo/common/GlobalExceptionHandler.java`

No dedicated unit test — `@WebMvcTest` auto-discovers `@RestControllerAdvice` beans, so `HelloControllerTest` (Task 7) covers it.

> **Why three handlers:** `MethodArgumentNotValidException` fires for `@RequestBody @Valid` failures. `HandlerMethodValidationException` fires for `@PathVariable` / `@RequestParam` constraint violations when the controller has `@Validated`. Without an explicit handler for `HandlerMethodValidationException`, the generic `Exception` handler would catch it and return 500 instead of 400.

- [ ] **Step 1: Create GlobalExceptionHandler**

Create `src/main/java/com/example/demo/common/GlobalExceptionHandler.java`:

```java
package com.example.demo.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Validation failed");
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(HandlerMethodValidationException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Validation failed");
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        detail.setTitle("Internal server error");
        return detail;
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/demo/common/
git commit -m "feat: add GlobalExceptionHandler with ProblemDetail (RFC 7807)"
```

---

## Task 6: HelloService — unit test (TDD)

**Files:**
- Create: `src/main/java/com/example/demo/hello/HelloResponse.java`
- Create: `src/main/java/com/example/demo/hello/HelloService.java`
- Create: `src/test/java/com/example/demo/HelloServiceTest.java`

- [ ] **Step 1: Create HelloResponse record**

Create `src/main/java/com/example/demo/hello/HelloResponse.java`:

```java
package com.example.demo.hello;

import java.time.Instant;

public record HelloResponse(String message, Instant timestamp) {}
```

- [ ] **Step 2: Write the failing unit test**

Create `src/test/java/com/example/demo/HelloServiceTest.java`:

```java
package com.example.demo;

import com.example.demo.hello.HelloService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HelloServiceTest {

    private final HelloService service = new HelloService();

    @Test
    void greet_returnsHelloWorld() {
        var response = service.greet();
        assertThat(response.message()).isEqualTo("Hello, World!");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void greet_withName_returnsPersonalisedMessage() {
        var response = service.greet("Alice");
        assertThat(response.message()).isEqualTo("Hello, Alice!");
    }
}
```

- [ ] **Step 3: Run the test — expect FAIL (class not found)**

```bash
./mvnw test -pl . -Dtest=HelloServiceTest -q 2>&1 | tail -20
```

Expected: compilation error — `HelloService` does not exist yet.

- [ ] **Step 4: Implement HelloService**

Create `src/main/java/com/example/demo/hello/HelloService.java`:

```java
package com.example.demo.hello;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class HelloService {

    public HelloResponse greet() {
        return greet("World");
    }

    public HelloResponse greet(String name) {
        log.debug("greet called with name={}", name);
        return new HelloResponse("Hello, " + name + "!", Instant.now());
    }
}
```

- [ ] **Step 5: Run the test — expect PASS**

```bash
./mvnw test -pl . -Dtest=HelloServiceTest -q 2>&1 | tail -5
```

Expected:
```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/demo/hello/HelloResponse.java \
        src/main/java/com/example/demo/hello/HelloService.java \
        src/test/java/com/example/demo/HelloServiceTest.java
git commit -m "feat: add HelloResponse record and HelloService with unit tests"
```

---

## Task 7: HelloController — web slice test (TDD)

**Files:**
- Create: `src/test/java/com/example/demo/HelloControllerTest.java`
- Create: `src/main/java/com/example/demo/hello/HelloController.java`

> **Why `@Import(SecurityConfig.class)`:** `@WebMvcTest` loads only the web layer — it does NOT auto-pick custom `@Configuration` classes outside that slice. Without the import, Spring applies a default security config that blocks every request with 401. Importing our `SecurityConfig` activates the `permitAll()` rule so `/api/hello` is accessible without mocking authentication.
>
> **Why `@MockitoBean` not `@MockBean`:** Spring Boot 4 removed `@MockBean` in favour of Spring Framework's `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`).

- [ ] **Step 1: Write the failing web slice test**

Create `src/test/java/com/example/demo/HelloControllerTest.java`:

```java
package com.example.demo;

import com.example.demo.config.SecurityConfig;
import com.example.demo.hello.HelloController;
import com.example.demo.hello.HelloResponse;
import com.example.demo.hello.HelloService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
@Import(SecurityConfig.class)
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HelloService helloService;

    @Test
    void getHello_returns200WithHelloWorld() throws Exception {
        when(helloService.greet()).thenReturn(new HelloResponse("Hello, World!", Instant.now()));

        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, World!"));
    }

    @Test
    void getHelloWithName_returns200WithHelloAlice() throws Exception {
        when(helloService.greet("Alice")).thenReturn(new HelloResponse("Hello, Alice!", Instant.now()));

        mockMvc.perform(get("/api/hello/Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Alice!"));
    }

    @Test
    void getHelloWithNameTooLong_returns400() throws Exception {
        mockMvc.perform(get("/api/hello/" + "a".repeat(51)))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL (controller not found)**

```bash
./mvnw test -pl . -Dtest=HelloControllerTest -q 2>&1 | tail -20
```

Expected: compilation error or `NoSuchBeanDefinitionException` — `HelloController` does not exist yet.

- [ ] **Step 3: Implement HelloController**

Create `src/main/java/com/example/demo/hello/HelloController.java`:

```java
package com.example.demo.hello;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
@RequiredArgsConstructor
@Validated
public class HelloController {

    private final HelloService helloService;

    @GetMapping
    public ResponseEntity<HelloResponse> hello() {
        return ResponseEntity.ok(helloService.greet());
    }

    @GetMapping("/{name}")
    public ResponseEntity<HelloResponse> helloName(
            @PathVariable @NotBlank @Size(max = 50) String name) {
        return ResponseEntity.ok(helloService.greet(name));
    }
}
```

- [ ] **Step 4: Run the tests — expect all 3 PASS**

```bash
./mvnw test -pl . -Dtest=HelloControllerTest -q 2>&1 | tail -5
```

Expected:
```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/demo/hello/HelloController.java \
        src/test/java/com/example/demo/HelloControllerTest.java
git commit -m "feat: add HelloController with GET /api/hello and /{name}, with web slice tests"
```

---

## Task 8: Update existing tests and add HelloIntegrationTest

**Files:**
- Modify: `src/test/java/com/example/demo/TestcontainersConfiguration.java`
- Modify: `src/test/java/com/example/demo/DemoApplicationTests.java`
- Create: `src/test/java/com/example/demo/HelloIntegrationTest.java`

- [ ] **Step 1: Update TestcontainersConfiguration — use postgres:16-alpine**

Replace the full content of `src/test/java/com/example/demo/TestcontainersConfiguration.java`:

```java
package com.example.demo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }
}
```

Changes from the generated version: image changed from `postgres:latest` to `postgres:16-alpine`; added `<?>` generic wildcard for type safety.

- [ ] **Step 2: Add @ActiveProfiles("test") to DemoApplicationTests**

Replace the full content of `src/test/java/com/example/demo/DemoApplicationTests.java`:

```java
package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

`@ActiveProfiles("test")` activates `application-test.yml` so `ddl-auto: none` is used and the datasource URL comes from `@ServiceConnection` rather than `application.yml`.

- [ ] **Step 3: Create HelloIntegrationTest**

Create `src/test/java/com/example/demo/HelloIntegrationTest.java`:

```java
package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class HelloIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void getHello_returns200() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, World!"));
    }
}
```

- [ ] **Step 4: Run all three integration/context tests**

```bash
./mvnw test -pl . -Dtest="DemoApplicationTests,HelloIntegrationTest" 2>&1 | tail -10
```

Expected:
```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

If Docker is not running, start it first: `docker compose up -d` then retry.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/example/demo/TestcontainersConfiguration.java \
        src/test/java/com/example/demo/DemoApplicationTests.java \
        src/test/java/com/example/demo/HelloIntegrationTest.java
git commit -m "test: update Testcontainers image to postgres:16-alpine, add HelloIntegrationTest"
```

---

## Task 9: README

**Files:**
- Create: `README.md`

- [ ] **Step 1: Create README.md**

Create `README.md` at the project root with the following content (write verbatim — the inner bash fences are part of the README, not this plan):

    # demo

    Spring Boot 4 baseline with virtual threads, PostgreSQL, Liquibase, Spring Security, and springdoc-openapi.

    ## Prerequisites

    - Java 25
    - Docker (for local Postgres and integration tests)

    ## Start Postgres

    ```bash
    docker compose up -d
    ```

    This starts a Postgres 16 instance on port 5432 with database `yourapp`, user `yourapp`,
    password `yourapp` (matches the defaults in `application.yml`).

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

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add README with prerequisites, run instructions, and endpoint reference"
```

---

## Task 10: Final verification

- [ ] **Step 1: Run the full build and test suite**

```bash
./mvnw clean verify 2>&1 | tail -20
```

Expected output (all tests pass):
```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0  (HelloServiceTest)
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0  (HelloControllerTest)
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0  (DemoApplicationTests)
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0  (HelloIntegrationTest)
[INFO] BUILD SUCCESS
```

- [ ] **Step 2: Verify the app starts (requires Postgres running)**

```bash
./mvnw spring-boot:run &
sleep 15
curl -s http://localhost:8080/api/hello | python3 -m json.tool
curl -s http://localhost:8080/api/hello/World | python3 -m json.tool
curl -o /dev/null -s -w "%{http_code}" http://localhost:8080/actuator/health
```

Expected:
- First two curls: JSON with `"message"` and `"timestamp"` fields
- Health check: `200`

Stop the app:
```bash
kill %1
```

- [ ] **Step 3: Final commit (if any leftover unstaged changes)**

```bash
git status
# If clean: nothing to do
# If there are changes: git add <files> && git commit -m "chore: final cleanup"
```
