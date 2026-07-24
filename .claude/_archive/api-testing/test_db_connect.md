# Database Connection - Environment Variables Setup

How this project sets and propagates database environment variables through Spring Boot and Gradle.

---

## Required Environment Variables

| Variable | Purpose | Example |
|----------|---------|---------|
| `RDS_HOSTNAME` | MySQL host | `localhost` |
| `RDS_PORT` | MySQL port | `3306` |
| `RDS_DB_NAME` | Database name | `cancer` |
| `RDS_USERNAME` | MySQL username | `cancer_user` |
| `RDS_PASSWORD` | MySQL password | *(your password)* |

---

## How Variables Are Set: The `.env` File

A `.env` file in the project root defines all environment variables:

**File:** `.env` (project root)
```
RDS_HOSTNAME=localhost
RDS_PORT=3306
RDS_DB_NAME=cancer
RDS_USERNAME=cancer_user
RDS_PASSWORD=your_password_here
```

This file is NOT committed to git. Each developer creates their own.

---

## How Variables Are Loaded

### Runtime (Spring Boot Application)

The `spring-dotenv` library automatically reads the `.env` file and makes the variables available to Spring's property resolver.

**Dependency in root `build.gradle`:**
```gradle
implementation "me.paulschwarz:spring-dotenv:4.0.0"
```

Spring Boot's YAML configs reference the variables using `${}` syntax:

**File:** `src/main/resources/application.yaml`
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${RDS_HOSTNAME}:${RDS_PORT}/${RDS_DB_NAME}
    username: ${RDS_USERNAME}
    password: ${RDS_PASSWORD}
```

`spring-dotenv` loads `.env` → Spring resolves `${RDS_HOSTNAME}` → HikariCP connects to MySQL.

No code changes needed. Just having the dependency on the classpath and a `.env` file is enough.

---

### Gradle Test Tasks

Gradle test JVMs do NOT automatically inherit `.env` files. The variables must be explicitly passed.

#### Root `build.gradle` — All Subproject Tests

The root `build.gradle` applies env var passthrough to ALL subproject test tasks:

```gradle
subprojects {
    tasks.withType(Test).configureEach {
        useJUnitPlatform()
        jvmArgs '-XX:+EnableDynamicAgentLoading'

        // Pass through environment variables to all subproject tests
        if (System.getenv("RDS_HOSTNAME")) environment "RDS_HOSTNAME", System.getenv("RDS_HOSTNAME")
        if (System.getenv("RDS_PORT")) environment "RDS_PORT", System.getenv("RDS_PORT")
        if (System.getenv("RDS_DB_NAME")) environment "RDS_DB_NAME", System.getenv("RDS_DB_NAME")
        if (System.getenv("RDS_USERNAME")) environment "RDS_USERNAME", System.getenv("RDS_USERNAME")
        if (System.getenv("RDS_PASSWORD")) environment "RDS_PASSWORD", System.getenv("RDS_PASSWORD")
    }
}
```

This conditionally passes each variable only if it exists in the shell. Variables must be exported in the shell (`export RDS_HOSTNAME=localhost`) or loaded via IntelliJ's EnvFile plugin.

#### Database Module `build.gradle` — Simpler Approach

The database module passes ALL environment variables at once:

```gradle
tasks.named('test') {
    useJUnitPlatform()
    environment System.getenv()    // passes ALL shell env vars to test JVM
}

tasks.register('testConnection', Test) {
    useJUnitPlatform()
    environment System.getenv()    // same for connection tests
    filter {
        includeTestsMatching "*Connection*"
    }
}
```

`System.getenv()` returns the entire environment map. This is simpler but passes everything.

---

### Test Profile YAML Files

Each module with tests has its own profile YAML that references the same `RDS_*` variables:

| Module | Profile | File |
|--------|---------|------|
| root (app) | `test` | `src/test/resources/application-test.yml` |
| database | `test-database` | `database/src/test/resources/application-test-database.yml` |
| fileloader | `test-fileloader` | `fileloader/src/test/resources/application-test-fileloader.yml` |

All use the same pattern:
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${RDS_HOSTNAME}:${RDS_PORT}/${RDS_DB_NAME}
    username: ${RDS_USERNAME}
    password: ${RDS_PASSWORD}
```

Tests activate their profile with:
```java
@ActiveProfiles("test-database")
```

---

## Full Flow Diagram

### Application Runtime
```
.env file
  → spring-dotenv reads it automatically
    → Spring resolves ${RDS_*} in application.yaml
      → HikariCP connects to MySQL
```

### Gradle Tests
```
Shell env vars (exported or via IntelliJ EnvFile)
  → build.gradle: environment System.getenv() passes to test JVM
    → Spring resolves ${RDS_*} in application-test-*.yml
      → HikariCP connects to MySQL
```

---

## Setting Up a New Module With DB Tests

To add database test support to a new module:

### 1. Create the test profile YAML

**File:** `{module}/src/test/resources/application-test-{module}.yml`
```yaml
spring:
  application:
    name: cancer-{module}-test
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${RDS_HOSTNAME}:${RDS_PORT}/${RDS_DB_NAME}
    username: ${RDS_USERNAME}
    password: ${RDS_PASSWORD}
    hikari:
      initialization-fail-timeout: 0
      connection-timeout: 3000
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: none
  main:
    allow-bean-definition-overriding: true
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

### 2. Add env var passthrough in module `build.gradle`

```gradle
tasks.named('test') {
    useJUnitPlatform()
    environment System.getenv()
}
```

Or rely on the root `build.gradle` subprojects block which already passes `RDS_*` to all subproject test tasks.

### 3. Annotate test classes

```java
@SpringBootTest(classes = YourTestApplication.class)
@ActiveProfiles("test-{module}")
class YourTest {
    // ...
}
```

### 4. If the module needs `spring-dotenv` at runtime

Add to the module's `build.gradle`:
```gradle
implementation "me.paulschwarz:spring-dotenv:4.0.0"
```

---

## Troubleshooting

### "Could not resolve placeholder 'RDS_HOSTNAME'"
- Variables not in the shell environment
- `build.gradle` missing `environment System.getenv()`
- Run: `echo $RDS_HOSTNAME` to check

### Tests work in IntelliJ but fail from command line
- IntelliJ may load `.env` via EnvFile plugin
- Command line needs: `source .env && export RDS_HOSTNAME RDS_PORT RDS_DB_NAME RDS_USERNAME RDS_PASSWORD`
- Or: `set -a && source .env && set +a && ./gradlew :database:test`

### Connection refused
- MySQL not running
- Run: `./gradlew :database:testConnection` for fast feedback
