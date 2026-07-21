# CSV Data Validation — api-validation Module

## Context

Data is pulled from external sources into the database. Known-good CSV files exist with expected values. Need to validate that data accessible via REST API matches these CSV baselines. No existing tool fits the architecture (REST API only, no direct DB access), so we build a lightweight custom solution inside the existing `api-validation` module.

Starting with Company (CSV: `company.csv`, endpoint: `GET /api/accounts`). Designed to be reusable for any entity.

## Solution

Add a `csvvalidation` package to `api-validation` alongside the existing `snapshot` package. Same pattern: core classes in `src/main`, tests + config in `src/test`, new Gradle task.

**Run command:** `./gradlew :api-validation:validateCsv`

## New Files (10 total)

### Core Classes — `api-validation/src/main/java/com/viro/apivalidation/csvvalidation/`

| File | Purpose |
|------|---------|
| `CsvEntityConfig.java` | POJO: csvPath, apiEndpoint, keyField, pageSize, excludeFields, fieldOverrides |
| `CsvValidationConfig.java` | Loads `csv-validation-config.yaml`, returns `CsvEntityConfig` by name |
| `FieldMapper.java` | snake_case→camelCase conversion, value normalization (empty/whitespace→null, trim) |
| `PaginatedApiFetcher.java` | Fetches all pages from paginated endpoint via REST Assured, returns `List<Map<String,Object>>` |
| `CsvDataValidator.java` | Core engine: parses CSV, fetches API data, matches rows by key, compares field-by-field |
| `ValidationResult.java` | Holds: csvRowCount, apiRowCount, missingInApi, extraInApi, `List<FieldMismatch>`, `toReport()` |
| `FieldMismatch.java` | Holds: keyValue, fieldName, csvValue, apiValue |

### Config + Test — `api-validation/src/test/`

| File | Purpose |
|------|---------|
| `resources/csv-validation-config.yaml` | Entity configs (company first) |
| `java/.../apivalidation/CompanyCsvValidationTest.java` | JUnit test tagged `@Tag("csvvalidation")` |

### Modified File

| File | Change |
|------|--------|
| `api-validation/build.gradle` | Add `commons-csv:1.11.0` dependency + `validateCsv` Gradle task |

## Comparison Logic (CsvDataValidator)

1. Parse CSV via Apache Commons CSV (`CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)`)
2. Build field map: for each CSV header, convert snake_case→camelCase (apply overrides)
3. Fetch all API pages via `PaginatedApiFetcher` (uses `last` boolean to stop)
4. Index both datasets by `keyField` (e.g. `code`)
5. For each CSV row key:
   - Missing from API → add to `missingInApi`
   - Present → compare each mapped field with normalization (empty=""→null equivalence)
6. API keys not in CSV → add to `extraInApi`
7. Return `ValidationResult`

## YAML Config Example

```yaml
entities:
  company:
    csvPath: "database/src/main/resources/db/data/company.csv"
    apiEndpoint: "/accounts"
    keyField: "code"
    pageSize: 100
    excludeFields:
      - extid
      - createdAt
      - updatedAt
    fieldOverrides: {}
```

## Gradle Task Addition (build.gradle line ~90)

```groovy
tasks.register('validateCsv', Test) {
    group = 'verification'
    description = 'Validate API data matches known-good CSV baselines'
    systemProperty 'snapshot.mode', 'verify'
    useJUnitPlatform {
        includeTags 'csvvalidation'
    }
}
```

## Implementation Order

1. `FieldMapper.java` + `FieldMismatch.java` + `ValidationResult.java` (no dependencies)
2. `CsvEntityConfig.java` + `CsvValidationConfig.java` (config layer)
3. `PaginatedApiFetcher.java` (REST Assured pagination)
4. `CsvDataValidator.java` (ties everything together)
5. `csv-validation-config.yaml` (company config)
6. `build.gradle` changes (dependency + task)
7. `CompanyCsvValidationTest.java` (the test)
