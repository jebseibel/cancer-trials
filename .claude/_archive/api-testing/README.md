# API Snapshot Testing Module

This module provides a framework for capturing and verifying REST API responses through snapshot testing.

## Overview

The `:api-validation` module enables repeatable API-level integration testing by:
1. Capturing API responses as JSON snapshots
2. Verifying future responses match saved baselines
3. Excluding dynamic fields (timestamps, IDs) from comparisons
4. Running tests consistently regardless of backend data state

## Quick Start

### Prerequisites
- Backend running on `http://localhost:8080`
- All dependencies installed

### Capture Initial Snapshots (First Time Only)

```bash
./gradlew :api-validation:captureSnapshots
```

This will:
- Seed deterministic test data into the database
- Call each API endpoint
- Save JSON responses to `api-validation/src/test/resources/snapshots/`

### Verify API Responses (Repeated Testing)

```bash
./gradlew :api-validation:verifySnapshots
```

This will:
- Seed the same test data
- Call each endpoint again
- Compare responses against saved snapshots
- Report mismatches with diffs

## Project Structure

```
api-validation/
├── src/main/java/com/viro/apivalidation/
│   ├── snapshot/
│   │   ├── SnapshotManager.java       # File I/O for snapshot JSON
│   │   ├── SnapshotAssertion.java     # JSON comparison with field exclusions
│   │   └── SnapshotConfig.java        # Endpoint-specific exclusion rules
│   ├── csvvalidation/
│   │   ├── CsvDataValidator.java      # Core CSV vs API validation logic
│   │   ├── CsvEntityConfig.java       # Per-entity CSV validation config
│   │   ├── CsvValidationConfig.java   # Config loading from YAML
│   │   ├── FieldMapper.java           # CSV field to API field mapping
│   │   ├── PaginatedApiFetcher.java   # Fetches all pages from paginated API
│   │   ├── FieldMismatch.java         # Value object for mismatch details
│   │   └── ValidationResult.java     # Validation result aggregate
│   └── fixture/
│       ├── TestDataSeeder.java        # Creates deterministic test data
│       └── DatabaseCleaner.java       # Resets DB to known state
├── src/test/java/com/viro/apivalidation/
│   ├── BaseApiTest.java               # Base class with shared setup
│   ├── FacilityOutputApiTest.java     # Tests for /api/facilities endpoints
│   ├── CompanyApiTest.java            # Tests for /api/accounts endpoints
│   ├── CrsApprovedApiTest.java        # Tests for CRS endpoints
│   ├── CrsChangeApiTest.java          # Tests for CRS change endpoints
│   ├── CrsMistakeApiTest.java         # Tests for CRS mistake endpoints
│   ├── CustomerTransactionApiTest.java # Tests for customer transaction endpoints
│   ├── UserApiTest.java               # Tests for user endpoints
│   ├── TsErcotApiTest.java            # Tests for ERCOT tracking endpoints
│   ├── TsMirecsApiTest.java           # Tests for MIRECS tracking endpoints
│   ├── TsMretsApiTest.java            # Tests for M-RETS tracking endpoints
│   ├── TsNarApiTest.java              # Tests for NAR tracking endpoints
│   ├── TsNcretsApiTest.java           # Tests for NCRETS tracking endpoints
│   ├── TsWregisApiTest.java           # Tests for WREGIS tracking endpoints
│   └── CompanyCsvValidationTest.java  # CSV validation test (@Tag("csvvalidation"))
└── src/test/resources/
    ├── snapshot-config.yaml           # Field exclusions per endpoint
    └── snapshots/                     # Captured API responses (JSON)
```

## Configuration

### snapshot-config.yaml

Define which fields to exclude from comparisons (useful for timestamps, generated IDs, etc.):

```yaml
endpoints:
  /api/facilities:
    exclude:
      - totalElements
      - totalPages

  /api/facilities/{extid}:
    exclude:
      - createdAt
      - updatedAt
```

### Default Exclusions

These fields are always excluded from all endpoints:
- `createdAt`, `updatedAt`
- `createdBy`, `updatedBy`
- `id`, `timestamp`
- `lastModifiedDate`, `creationDate`
- Hibernate lazy proxies

## Workflow

### Adding Tests for a New Endpoint

1. Create a test method in the appropriate test class (or new class if needed)
2. Use `RestAssured` to call the endpoint
3. Extract the JSON response
4. In capture mode, save it; in verify mode, compare it

Example:

```java
@Test
void testGetFacilities_page0() {
    String snapshotName = getSnapshotName("GET", "/facilities", "page0");

    String response = RestAssured
        .given()
            .queryParam("page", 0)
            .queryParam("size", 20)
        .when()
            .get("/facilities")
        .then()
            .statusCode(200)
            .extract().asString();

    if (isCaptureMode()) {
        snapshotManager.save(snapshotName, response);
    } else {
        String expected = snapshotManager.load(snapshotName);
        snapshotAssertion.assertEquals(expected, response,
            snapshotConfig.getExclusions("/api/facilities"));
    }
}
```

### After API Changes

When you intentionally change an endpoint:

1. Run verification to confirm it fails with expected diff:
   ```bash
   ./gradlew :api-validation:verifySnapshots
   ```

2. Re-capture with the new response:
   ```bash
   ./gradlew :api-validation:captureSnapshots
   ```

3. Review the changes:
   ```bash
   git diff api-validation/src/test/resources/snapshots/
   ```

4. Commit the updated snapshots:
   ```bash
   git add api-validation/src/test/resources/snapshots/
   git commit -m "Update API snapshots after [endpoint] changes"
   ```

## Testing Modes

### System Properties

Control test behavior via system properties:

```bash
# Capture mode (save responses)
./gradlew :api-validation:captureSnapshots -Dsnapshot.mode=capture

# Verify mode (compare responses)
./gradlew :api-validation:verifySnapshots -Dsnapshot.mode=verify

# Configure API endpoint
./gradlew :api-validation:captureSnapshots \
  -Dapi.baseUri=http://localhost \
  -Dapi.port=8080 \
  -Dapi.basePath=/api
```

## Troubleshooting

### Backend Not Running

```
✗ Backend not running on http://localhost:8080
  Please start the backend first: ./gradlew bootRun
```

**Fix:** Start the backend in another terminal before running tests.

### Snapshot Not Found

```
⚠ Snapshot not found: GET_api_facilities_page0
  Run with -Dsnapshot.mode=capture to create it
```

**Fix:** Run capture mode first to create initial snapshots.

### Test Fails with Diff

Expected vs Actual differences are shown. Common causes:
- API behavior changed (intentional)
- Backend data is different
- New field added to response
- Field value changed

**Fix:** If intentional, re-capture; if bug, fix the code.

## Best Practices

1. **Seed Deterministic Data**: Use `TestDataSeeder` to create consistent test data
2. **Exclude Dynamic Fields**: Add timestamps, IDs, and counts to `snapshot-config.yaml`
3. **One Snapshot per Scenario**: Create separate tests for different parameter values
4. **Review Diffs**: Always review `git diff` before committing snapshot changes
5. **Keep Snapshots Small**: Focus on key response fields, not entire responses
6. **Document Expectations**: Add comments explaining what each test validates

## Limitations & Future Enhancements

- Tests require backend to be running (not unit tests)
- Test data seeding is basic (TODO: implement actual repository usage)
- No request/response logging yet
- Snapshot diffs could be more sophisticated (JSON Patch format)

## Integration with CI/CD

To add to CI pipeline:

```bash
# Start backend in background
./gradlew bootRun &
BACKEND_PID=$!

# Wait for startup
sleep 10

# Run verification
./gradlew :api-validation:verifySnapshots

# Capture exit code
TEST_RESULT=$?

# Cleanup
kill $BACKEND_PID

exit $TEST_RESULT
```
