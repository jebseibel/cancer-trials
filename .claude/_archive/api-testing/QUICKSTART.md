# API Snapshot Testing - Quick Start

## 1. Capture Initial Snapshots (First Time Only)

```bash
# Terminal 1: Start the backend
cd /home/jeb/projects/personal/cancer
./gradlew bootRun

# Terminal 2: Capture snapshots
cd /home/jeb/projects/personal/cancer
./gradlew :api-validation:captureSnapshots
```

**Expected Output:**
```
✓ Captured: GET_api_facilities_page0
✓ Captured: GET_api_facilities_
✓ Captured: GET_api_facilities_by_unique_id_
✓ Captured: GET_api_accounts_page0
✓ Captured: GET_api_accounts_
...
```

Snapshots are saved to: `api-validation/src/test/resources/snapshots/`

## 2. Verify API Responses (Regular Testing)

```bash
# Backend must be running (see step 1, terminal 1)
./gradlew :api-validation:verifySnapshots
```

**Success Output:**
```
BUILD SUCCESSFUL
```

**Failure Output:**
```
Snapshot mismatch:
Expected:
{...}
Actual:
{...}
```

## 3. After API Changes

If you intentionally change an endpoint:

```bash
# 1. Verify it fails with expected diff
./gradlew :api-validation:verifySnapshots

# 2. Re-capture new baseline
./gradlew :api-validation:captureSnapshots

# 3. Review and commit changes
git diff api-validation/src/test/resources/snapshots/
git add api-validation/src/test/resources/snapshots/
git commit -m "Update API snapshots after [change description]"
```

## Configuration

### API Endpoint

Default: `http://localhost:8080/api`

Override with:
```bash
./gradlew :api-validation:captureSnapshots \
  -Dapi.baseUri=http://your-host \
  -Dapi.port=9000 \
  -Dapi.basePath=/api
```

### Field Exclusions

Edit `api-validation/src/test/resources/snapshot-config.yaml`:
```yaml
endpoints:
  /api/your-endpoint:
    exclude:
      - field1
      - field2
      - dynamicField
```

## Troubleshooting

### "Backend not running"
```
✗ Backend not running on http://localhost:8080
```
→ Start the backend: `./gradlew bootRun`

### "Snapshot not found"
```
⚠ Snapshot not found: GET_api_facilities_page0
  Run with -Dsnapshot.mode=capture to create it
```
→ Run capture mode: `./gradlew :api-validation:captureSnapshots`

### Tests fail unexpectedly
- Check if you modified the API response format
- Verify test data is correct in the database
- Run with backend logs enabled for debugging
- Review the diff to see what changed

## Files Created

```
api-validation/src/test/resources/snapshots/
├── GET_api_accounts_page0.json
├── GET_api_crs-mistakes_page0.json
├── GET_api_customer-transactions_page0.json
├── GET_api_fac-crs-approved_page0.json
├── GET_api_fac-crs-change_page0.json
├── GET_api_fac-crs-pending_page0.json
├── GET_api_facilities_page0.json
├── GET_api_ts-ercot_page0.json
├── GET_api_ts-mirecs_page0.json
├── GET_api_ts-mrets_page0.json
├── GET_api_ts-nar_page0.json
├── GET_api_ts-ncrets_page0.json
├── GET_api_ts-wregis_page0.json
└── GET_api_users_page0.json
```

## Adding Tests for New Endpoints

1. Add test method to appropriate test class:
```java
@Test
void testNewEndpoint() {
    String snapshotName = getSnapshotName("GET", "/new-endpoint", "");

    String response = RestAssured.given()
        .when()
        .get("/new-endpoint")
        .then()
        .statusCode(200)
        .extract().asString();

    if (isCaptureMode()) {
        snapshotManager.save(snapshotName, response);
    } else {
        String expected = snapshotManager.load(snapshotName);
        snapshotAssertion.assertEquals(expected, response, Set.of());
    }
}
```

2. Add field exclusions to `snapshot-config.yaml`:
```yaml
endpoints:
  /api/new-endpoint:
    exclude:
      - totalElements
      - totalPages
```

3. Capture: `./gradlew :api-validation:captureSnapshots`
4. Test: `./gradlew :api-validation:verifySnapshots`

## See Also

- **Full Documentation**: `api-validation/README.md`
- **Implementation Details**: `.claude/_archive/api-testing/api-test-implementation.md`
