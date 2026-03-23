### Basicspring — Project-Specific Development Guidelines

This document captures repo-specific knowledge for advanced contributors. It focuses on configuration, testing, and practical development patterns observed in this codebase.

#### Build and Runtime Configuration
- Java: 21 via Gradle Toolchains (`build.gradle`). No local JDK juggling required; wrapper manages it.
- Build system: Gradle Wrapper + Spring Boot 3.5.7.
- Key dependencies: Liquibase (DB migrations), Spring Data JPA, ModelMapper, SpringDoc OpenAPI, Lombok.
- Commands:
  - Clean build without tests (useful when local DB is down):
    ```bash
    ./gradlew clean build -x test
    ```
  - Run the app (dev):
    ```bash
    ./gradlew bootRun
    ```
- Database and migrations:
  - MySQL with driver `com.mysql.cj.jdbc.Driver`.
  - Liquibase changelog: `src/main/resources/db/changelog/db.changelog-master.yaml` (includes initial dataset under `changes/sql/`).
  - Liquibase is enabled for both app and the `test-database` profile.
- Configuration sources used by the app:
  - Main config: `src/main/resources/application.yml`.
    - Uses env vars: `CPSS_USERNAME` and `CPSS_PASSWORD` (defaults are `default_user` / `default_pass`).
    - JDBC URL: `jdbc:mysql://localhost:3306/cpss`.
  - Note: `developer-documents/setup-env-and-database.md` mentions `DB_USERNAME/DB_PASSWORD`; the running app and tests actually use `CPSS_USERNAME/CPSS_PASSWORD`. Prefer the `CPSS_*` names to avoid confusion.
  - The repo includes `me.paulschwarz:spring-dotenv`, so variables can be provided via a `.env` file at project root or via OS environment.

#### Testing — How It Works Here
- Framework: JUnit 5; Gradle uses JUnit Platform (`tasks.test.useJUnitPlatform()` in `build.gradle`).
- Two styles of tests exist:
  1) Pure unit tests (no Spring, no DB) — e.g., `src/test/java/com/seibel/cpss/sample/SamplePureUnitTest.java`.
  2) Spring Boot tests that touch the database — e.g., `database/connection/*` tests.
- Test profiles and config:
  - DB tests use `@ActiveProfiles("test-database")` and pick up `src/test/resources/application-test-database.yml`.
  - That profile enables Liquibase and reads credentials from env with the same names as the app: `CPSS_USERNAME` / `CPSS_PASSWORD`.
- Typical commands (verified):
  - Run a single pure unit test (no DB needed):
    ```bash
    ./gradlew test --tests 'com.seibel.basic.sample.SamplePureUnitTest'
    ```
  - Run by simple pattern:
    ```bash
    ./gradlew test --tests '*Sample*'
    ```
  - Run a specific DB connection test (requires local MySQL and credentials exported):
    ```bash
    export CPSS_USERNAME=cpss_username
    export CPSS_PASSWORD=your_password
    ./gradlew test --tests 'com.seibel.basic.database.connection.DbConnectionTest'
    # `@ActiveProfiles("test-database")` is already on the test; no need to pass -Dspring.profiles.active
    ```
  - Run full test suite:
    ```bash
    ./gradlew test
    ```

#### Adding New Tests
- Prefer pure unit tests: keep them deterministic and fast; avoid bringing up the Spring context unless necessary.
- For DB/Spring tests:
  - Annotate with `@ActiveProfiles("test-database")`.
  - Assume Liquibase manages schema; add changesets rather than ad-hoc SQL in tests.
  - Expect credentials in env: `CPSS_USERNAME` / `CPSS_PASSWORD`.
- Minimal pure unit test template (this pattern was validated locally by running existing tests):
  ```java
  package com.seibel.basic.sample;

  import org.junit.jupiter.api.Test;
  import static org.junit.jupiter.api.Assertions.*;

  class ExampleUnitTest {
      @Test
      void computesSum() {
          assertEquals(5, 2 + 3);
      }
  }
  ```
  Run it selectively:
  ```bash
  ./gradlew test --tests 'com.seibel.basic.sample.ExampleUnitTest'
  ```

#### Additional Development Notes (Codebase-Specific)
- Exception layering:
  - DB layer exceptions live in `com.seibel.basic.database.database.exception`.
  - Service layer exceptions live under `com.seibel.basic.common.exceptions` and `ServiceException` wrappers. Keep this separation intact and import from the correct package.
- Persistence model:
  - Entities implement base types (`BaseDb`, `BaseTypeDb`) and track activity state via `ActiveEnum` and `deletedAt`. Avoid hard deletes in services; set inactive + timestamp instead.
- Mapping:
  - ModelMapper is used (`...database/db/mapper/*`). Mirror existing mapping patterns when adding DTOs/entities.
- Lombok:
  - Present as `compileOnly` + `annotationProcessor`. Ensure IDE annotation processing is enabled to avoid phantom errors.
- OpenAPI:
  - SpringDoc starter is present. Swagger UI typically at `/swagger-ui.html` or `/swagger-ui/index.html` when the app is running.
- When DB is unavailable but you need a build:
  - Use `./gradlew build -x test` to skip DB-bound tests.

#### Verified During Preparation
- Confirmed Gradle config and JUnit platform setup from `build.gradle`.
- Successfully ran an existing pure unit test:
  ```bash
  ./gradlew test --tests 'com.seibel.basic.sample.SamplePureUnitTest'
  ```
- DB tests require a reachable local MySQL and the `CPSS_*` env vars.

#### Quick Reference
- Build (skip tests): `./gradlew clean build -x test`
- Run app: `./gradlew bootRun`
- Run pure unit test: `./gradlew test --tests 'com.seibel.basic.sample.SamplePureUnitTest'`
- Run DB test (with env): `./gradlew test --tests 'com.seibel.basic.database.connection.DbConnectionTest'`
