# How API Snapshot Testing Works

## Overview

API Snapshot Testing is a technique for verifying REST API responses by comparing them against saved baseline responses. This document explains how the implementation works, the design decisions, and how to extend it.

## Table of Contents

1. [Core Concept](#core-concept)
2. [Architecture](#architecture)
3. [The Test Flow](#the-test-flow)
4. [Snapshot Management](#snapshot-management)
5. [JSON Comparison](#json-comparison)
6. [Field Exclusions](#field-exclusions)
7. [Test Data](#test-data)
8. [Gradle Integration](#gradle-integration)
9. [Design Patterns](#design-patterns)
10. [Troubleshooting](#troubleshooting)

---

## Core Concept

### What Problem Does It Solve?

Testing a REST API typically requires:
1. Making HTTP requests to endpoints
2. Parsing JSON responses
3. Asserting on response structure and data

Traditional assertion-based testing can be verbose:

```java
// Old way - assertion hell
response.statusCode(200);
assertThat(response.jsonPath().getString("name")).isEqualTo("Facility A");
assertThat(response.jsonPath().getInt("capacity")).isEqualTo(100);
assertThat(response.jsonPath().getString("state")).isEqualTo("CA");
// ... 50 more assertions
```

**Snapshot testing simplifies this** by treating the entire response as the source of truth:

```java
// New way - snapshot testing
String response = RestAssured.get("/facilities/FAC-001").asString();
snapshotAssertion.assertEquals(expectedSnapshot, response, Set.of("createdAt", "updatedAt"));
```

### Key Benefits

1. **Less Code** - No need for field-by-field assertions
2. **Complete Coverage** - Every field in the response is tested
3. **Catch Unexpected Changes** - Any deviation from baseline fails the test
4. **Easy to Review** - Git diffs show exactly what changed
5. **Maintainable** - When API changes, update snapshot once, verify in PR

### When to Use Snapshots

✅ **Good For:**
- REST API contract testing
- Verifying complete response structure
- Detecting breaking changes
- Regression testing across multiple endpoints
- API versioning/compatibility

❌ **Not Good For:**
- Unit testing small functions
- Testing with highly variable data (random IDs, timestamps)
- Golden master testing without proper setup
- Testing without deterministic test data

---

## Architecture

### Module Organization

```
:api-validation
├── src/main/java/com/seibel/cancer/apivalidation/
│   ├── snapshot/                    ← Core framework
│   │   ├── SnapshotManager.java
│   │   ├── SnapshotAssertion.java
│   │   └── SnapshotConfig.java
│   └── fixture/                     ← Test support
│       ├── DatabaseCleaner.java
│       └── TestDataSeeder.java
└── src/test/java/com/seibel/cancer/apivalidation/
    ├── BaseApiTest.java             ← Common test setup
    └── *ApiTest.java                ← Endpoint-specific tests (15 classes)
```

### Class Responsibilities

#### SnapshotManager
**Purpose:** File I/O for snapshot persistence

```java
public class SnapshotManager {
    // Save pretty-printed JSON to disk
    public void save(String name, String json) { }

    // Load JSON from disk
    public String load(String name) { }

    // Check if snapshot exists
    public boolean exists(String name) { }
}
```

**How It Works:**
1. Receives raw JSON response from API
2. Parses JSON with Jackson ObjectMapper
3. Pretty-prints for readability
4. Writes to `api-validation/src/test/resources/snapshots/{name}.json`

**Example File Location:**
```
api-validation/src/test/resources/snapshots/
├── GET_api_facilities_page0.json
├── GET_api_facilities_.json
└── GET_api_accounts_page0.json
```

#### SnapshotAssertion
**Purpose:** JSON comparison with field exclusion

```java
public class SnapshotAssertion {
    // Compare two JSON responses
    public void assertEquals(String expected, String actual,
                             Set<String> additionalExclusions) { }
}
```

**How It Works:**

1. **Parse Both Responses**
   ```
   Expected: {"id": 1, "name": "FAC-001", "createdAt": "2024-01-01T10:00:00Z"}
   Actual:   {"id": 1, "name": "FAC-001", "createdAt": "2024-01-02T10:00:00Z"}
   ```

2. **Collect Exclusion Fields**
   - Default exclusions: `createdAt`, `updatedAt`, `id`, `timestamp`
   - Plus fields specified in `snapshot-config.yaml`
   - Plus additional fields passed to `assertEquals()`

3. **Recursively Remove Excluded Fields**
   ```java
   private JsonNode removeFields(JsonNode node, Set<String> fieldsToExclude) {
       if (node.isObject()) {
           ObjectNode copy = deepCopy(node);
           for (String field : fieldsToExclude) {
               copy.remove(field);  // Remove top-level
           }
           // Recursively process nested objects/arrays
           for (String fieldName : copy.fieldNames()) {
               copy.set(fieldName, removeFields(copy.get(fieldName), fieldsToExclude));
           }
       }
       // ... handle arrays and primitives
   }
   ```

4. **Compare Normalized JSON**
   ```
   Expected: {"name": "FAC-001"}
   Actual:   {"name": "FAC-001"}
   → PASS
   ```

5. **Generate Diff on Mismatch**
   ```
   Expected:
   {"name": "FAC-001"}

   Actual:
   {"name": "FAC-002"}
   ```

#### SnapshotConfig
**Purpose:** Centralized field exclusion rules

```java
public class SnapshotConfig {
    // Get exclusions for a specific endpoint
    public Set<String> getExclusions(String endpoint) { }

    // Get default exclusions (always excluded)
    public Set<String> getDefaultExclusions() { }
}
```

**How It Works:**

1. **Load YAML at Startup**
   ```yaml
   endpoints:
     /api/facilities:
       exclude:
         - totalElements
         - totalPages
   ```

2. **Parse with Jackson YAMLFactory**
   ```java
   ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
   Map<String, Object> config = mapper.readValue(yamlContent, Map.class);
   ```

3. **Store in Memory**
   ```java
   endpointExclusions = {
       "/api/facilities" -> {"totalElements", "totalPages"},
       ...
   }
   ```

4. **Merge with Defaults**
   ```java
   public Set<String> getExclusions(String endpoint) {
       Set<String> exclusions = new HashSet<>(DEFAULT_EXCLUSIONS);
       exclusions.addAll(endpointExclusions.getOrDefault(endpoint, Set.of()));
       return exclusions;
   }
   ```

#### DatabaseCleaner
**Purpose:** Reset database to known state

```java
public class DatabaseCleaner {
    public void cleanDatabase() { }
}
```

**How It Works:**

1. **Disable Foreign Key Constraints**
   ```sql
   SET FOREIGN_KEY_CHECKS = 0;
   ```

2. **Truncate All Tables**
   ```sql
   TRUNCATE TABLE facility_output;
   TRUNCATE TABLE company;
   TRUNCATE TABLE crs_approved;
   -- ... all tables
   ```

3. **Re-enable Foreign Key Constraints**
   ```sql
   SET FOREIGN_KEY_CHECKS = 1;
   ```

**Why This Approach:**
- Faster than DELETE (no row-by-row processing)
- Simpler than dropping/recreating tables
- Disabling FK checks allows truncating in any order
- Leaves schema intact (only clears data)

#### TestDataSeeder
**Purpose:** Create deterministic test data

```java
public class TestDataSeeder {
    public void seedAll() { }
}
```

**How It Works:**

1. **Call cleanDatabase() First**
   - Ensures consistent starting state

2. **Seed Each Domain Entity**
   ```java
   private void seedFacilities() {
       FacilityOutputDb facility1 = new FacilityOutputDb();
       facility1.setExtid("FAC-001");
       facility1.setState("CA");
       // ... set all fields
       facilityService.save(facility1);
   }
   ```

3. **Create Predictable IDs**
   - Use fixed external IDs (FAC-001, FAC-002, etc.)
   - Avoid auto-generated UUIDs when possible
   - This makes snapshot names consistent across runs

**Current State:**
- ✅ Cleans database successfully
- ⚠️ Seeding methods are stubbed (TODO: implement with repositories)
- Once backend is running with seeding, tests will be repeatable

---

## The Test Flow

### Capture Mode (Initial Run)

```
Test Execution → captureSnapshots gradle task
                      ↓
             @BeforeAll setupOnce()
                      ↓
             Create SnapshotManager
             Create SnapshotAssertion
             Create TestDataSeeder
             Create DatabaseCleaner
                      ↓
             Verify backend is running (HTTP health check)
                      ↓
             @BeforeEach seedTestData()
                      ↓
             DatabaseCleaner.cleanDatabase()
             TestDataSeeder.seedAll()
                      ↓
             @Test method executes
                      ↓
             RestAssured.get("/facilities") → JSON response
                      ↓
             isCaptureMode() == true
                      ↓
             SnapshotManager.save("GET_api_facilities_page0", response)
                      ↓
             Write to api-validation/src/test/resources/snapshots/
                      ↓
             Test completes (always passes in capture mode)
                      ↓
             All tests complete
                      ↓
             Snapshots saved to disk, ready for verification
```

### Verify Mode (Regression Testing)

```
Test Execution → verifySnapshots gradle task
                      ↓
             @BeforeAll setupOnce()
                      ↓
             Setup same as capture mode
                      ↓
             @BeforeEach seedTestData()
                      ↓
             Clean and reseed database to identical state
                      ↓
             @Test method executes
                      ↓
             RestAssured.get("/facilities") → JSON response
                      ↓
             isCaptureMode() == false
                      ↓
             Load expected snapshot from disk
             SnapshotManager.load("GET_api_facilities_page0")
                      ↓
             SnapshotAssertion.assertEquals(expected, actual, exclusions)
                      ↓
             Parse both JSON with Jackson
                      ↓
             Remove excluded fields recursively
                      ↓
             Compare normalized JSON trees
                      ↓
             If equal: TEST PASSES ✓
             If different: throw AssertionError with diff ✗
                      ↓
             All tests complete
```

### Example: Testing /api/facilities Endpoint

**Test Code:**
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
        System.out.println("✓ Captured: " + snapshotName);
    } else {
        String expected = snapshotManager.load(snapshotName);
        snapshotAssertion.assertEquals(expected, response,
            snapshotConfig.getExclusions("/api/facilities"));
    }
}
```

**First Run (Capture):**
```
$ ./gradlew :api-validation:captureSnapshots

Clean database
Seed test facilities
Make GET /api/facilities?page=0&size=20 request
Receive JSON response with 20 facilities
Pretty-print JSON
Save to api-validation/src/test/resources/snapshots/GET_api_facilities_page0.json
✓ Captured: GET_api_facilities_page0

BUILD SUCCESSFUL
```

**Subsequent Runs (Verify):**
```
$ ./gradlew :api-validation:verifySnapshots

Clean database (identical state)
Seed test facilities (same 20 facilities)
Make GET /api/facilities?page=0&size=20 request
Receive JSON response with 20 facilities
Load expected snapshot from disk
Remove pagination fields (totalElements, totalPages)
Compare JSON structures
✓ MATCH - Test passes

BUILD SUCCESSFUL
```

**If API Changes (Breaking Change):**
```
$ ./gradlew :api-validation:verifySnapshots

Clean database
Seed test facilities
Make GET /api/facilities?page=0&size=20 request
Receive JSON with different field names (e.g., "facilityName" instead of "name")
Load expected snapshot (still has "name" field)
Compare: expected has "name", actual has "facilityName"
✗ MISMATCH

Snapshot mismatch:
Expected:
{"name": "FAC-001", ...}

Actual:
{"facilityName": "FAC-001", ...}

BUILD FAILED
```

---

## Snapshot Management

### Naming Convention

Snapshots are named using a pattern that makes them self-documenting:

```
{METHOD}_{PATH}_{VARIANT}.json

Examples:
GET_api_facilities_page0.json          ← /api/facilities with page=0 param
GET_api_facilities_.json               ← /api/facilities/{extid}
GET_api_accounts_page0.json            ← /api/accounts with page=0 param
POST_api_facilities_create.json        ← POST /api/facilities (create)
```

**How names are generated:**

```java
protected String getSnapshotName(String method, String endpoint, String variant) {
    String normalized = endpoint
        .replaceAll("^/", "")                    // Remove leading /
        .replaceAll("/", "_")                    // Replace / with _
        .replaceAll("\\{.*?\\}", "")             // Remove {param} placeholders
        .replaceAll("_+", "_")                   // Collapse multiple _
        .replaceAll("_$", "");                   // Remove trailing _

    if (variant != null && !variant.isEmpty()) {
        return method + "_api_" + normalized + "_" + variant;
    }
    return method + "_api_" + normalized;
}
```

**Examples:**

| Method | Endpoint | Variant | Result |
|--------|----------|---------|--------|
| GET | /facilities | page0 | GET_api_facilities_page0 |
| GET | /facilities/{extid} | (empty) | GET_api_facilities_ |
| POST | /facilities | create | POST_api_facilities_create |
| PUT | /facilities/{extid} | update | PUT_api_facilities_update |

### Storage Location

```
api-validation/
└── src/test/resources/
    └── snapshots/
        ├── GET_api_facilities_page0.json
        ├── GET_api_facilities_.json
        ├── GET_api_accounts_page0.json
        └── ... (one file per endpoint variant)
```

### File Format

Snapshots are pretty-printed JSON for readability:

```json
{
  "content" : [ {
    "id" : 1,
    "extid" : "FAC-001",
    "uniqueId" : "WREGIS-12345",
    "name" : "Solar Farm A",
    "state" : "CA",
    "capacity" : 100.5,
    "fuelType" : "Solar",
    "createdAt" : "2024-01-01T10:00:00Z",
    "updatedAt" : "2024-01-02T15:30:00Z"
  }, {
    "id" : 2,
    "extid" : "FAC-002",
    ...
  } ],
  "pageable" : {
    "pageNumber" : 0,
    "pageSize" : 20,
    "offset" : 0,
    "paged" : true,
    "unpaged" : false
  },
  "totalElements" : 2,
  "totalPages" : 1,
  "numberOfElements" : 2
}
```

### Git Tracking

Snapshots should be committed to version control:

```bash
$ git add api-validation/src/test/resources/snapshots/
$ git commit -m "Add initial API snapshots for regression testing"
```

**Benefits:**
- Review API changes in PRs via git diff
- Track API evolution over time
- Catch unintended breaking changes

**Example PR Diff:**

```diff
diff --git a/api-validation/src/test/resources/snapshots/GET_api_facilities_page0.json b/api-validation/src/test/resources/snapshots/GET_api_facilities_page0.json

{
   "content" : [ {
     "id" : 1,
     "extid" : "FAC-001",
-    "name" : "Solar Farm A",
+    "facilityName" : "Solar Farm A",
     "state" : "CA"
   } ]
}
```

Reviewers can see exactly what changed and decide if it's intentional.

---

## JSON Comparison

### The Challenge

Direct string comparison of JSON won't work because:
1. Fields might be in different order
2. Dynamic fields (timestamps, IDs) change each run
3. Whitespace varies

### The Solution: Normalized Comparison

**Step 1: Parse JSON**
```java
JsonNode expectedNode = objectMapper.readTree(expected);
JsonNode actualNode = objectMapper.readTree(actual);
```

**Step 2: Remove Excluded Fields**
```java
private JsonNode removeFields(JsonNode node, Set<String> fieldsToExclude) {
    if (node.isObject()) {
        ObjectNode copy = ((ObjectNode) node).deepCopy();
        for (String field : fieldsToExclude) {
            copy.remove(field);
        }
        // Recursively process nested objects
        Iterator<String> fieldNames = copy.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            copy.set(fieldName, removeFields(copy.get(fieldName), fieldsToExclude));
        }
        return copy;
    } else if (node.isArray()) {
        // Recursively process array elements
        ArrayNode copy = ((ArrayNode) node).deepCopy();
        for (int i = 0; i < copy.size(); i++) {
            copy.set(i, removeFields(copy.get(i), fieldsToExclude));
        }
        return copy;
    }
    return node;  // Primitive values unchanged
}
```

**Step 3: Compare with Jackson's equals()**
```java
if (!normalizedExpected.equals(normalizedActual)) {
    throw new AssertionError("Snapshot mismatch:\n" + diff);
}
```

Jackson's `JsonNode.equals()` does semantic comparison:
- Ignores key order
- Compares values, not string representation
- Handles nested structures

### Example Walkthrough

**Expected Snapshot:**
```json
{
  "id": 1,
  "name": "FAC-001",
  "createdAt": "2024-01-01T10:00:00Z",
  "nested": {
    "id": 10,
    "value": "test"
  }
}
```

**Actual Response:**
```json
{
  "id": 1,
  "name": "FAC-001",
  "createdAt": "2024-01-02T15:00:00Z",
  "nested": {
    "id": 10,
    "value": "test"
  }
}
```

**Exclusion Set:** `{id, createdAt}`

**After Removing Fields:**

Expected:
```json
{
  "name": "FAC-001",
  "nested": {
    "value": "test"
  }
}
```

Actual:
```json
{
  "name": "FAC-001",
  "nested": {
    "value": "test"
  }
}
```

**Result:** ✅ EQUAL (timestamp and nested id were excluded)

---

## Field Exclusions

### Why Exclusions Are Necessary

Some fields change on every run:
- **Timestamps**: `createdAt`, `updatedAt` change with each API call
- **IDs**: Auto-generated primary keys or UUIDs differ across runs
- **Counts**: `totalElements`, `totalPages` depend on test data count
- **Derived Fields**: Computed values that depend on external state

**Without exclusions:** Every test would fail because timestamps differ.

**With exclusions:** Only structural changes are detected (breaking changes).

### Default Exclusions

Always excluded from all endpoints:

```java
private static final Set<String> DEFAULT_EXCLUSIONS = Set.of(
    "createdAt", "updatedAt",
    "createdBy", "updatedBy",
    "id", "timestamp",
    "lastModifiedDate", "creationDate",
    "hibernateLazyInitializer", "__proxy__"
);
```

**Rationale:**
- **createdAt/updatedAt**: Audit fields that change per transaction
- **createdBy/updatedBy**: User tracking fields
- **id**: Auto-generated primary keys
- **timestamp**: Any timestamp field
- **lastModifiedDate/creationDate**: Alternative timestamp names
- **hibernateLazyInitializer**: Proxy artifacts from ORM

### Per-Endpoint Exclusions

Some endpoints have field-specific exclusions:

**snapshot-config.yaml:**
```yaml
endpoints:
  /api/facilities:
    exclude:
      - totalElements
      - totalPages
      - numberOfElements

  /api/facilities/{extid}:
    exclude:
      - createdAt        # Will be merged with defaults
      - updatedAt
```

**Why totalElements/totalPages:**
- Depend on how many test records exist
- Don't reflect API contract, just pagination metadata
- Better to test pagination structure separately

### Adding Custom Exclusions at Runtime

Tests can add additional exclusions when needed:

```java
@Test
void testEndpointWithCustomData() {
    String response = RestAssured.get("/endpoint").asString();

    // These are merged with defaults + config exclusions
    snapshotAssertion.assertEquals(expected, response,
        Set.of("customField1", "customField2", "dynamicValue"));
}
```

### How Exclusions Are Merged

```java
public void assertEquals(String expected, String actual,
                         Set<String> additionalExclusions) {
    // Combine all exclusion sources
    Set<String> allExclusions = new HashSet<>(config.getDefaultExclusions());
    allExclusions.addAll(config.getExclusions(endpoint));  // From YAML
    allExclusions.addAll(additionalExclusions);             // From test

    // Now apply to both expected and actual
    JsonNode normalizedExpected = removeFields(expectedNode, allExclusions);
    JsonNode normalizedActual = removeFields(actualNode, allExclusions);
}
```

**Priority (highest to lowest):**
1. Runtime exclusions (passed to assertEquals)
2. Endpoint-specific exclusions (snapshot-config.yaml)
3. Default exclusions (hardcoded)

---

## Test Data

### The Problem: Repeatability

For snapshot testing to work reliably, the API must return identical data on every run.

❌ **Without deterministic data:**
```
Run 1: GET /facilities → Returns FAC-001, FAC-002, FAC-003
Run 2: GET /facilities → Returns FAC-001, FAC-002 (FAC-003 was deleted!)
→ Snapshot comparison fails even though API works correctly
```

✅ **With deterministic data:**
```
Run 1: Seed FAC-001, FAC-002, FAC-003 → GET /facilities → Always returns same 3
Run 2: Seed FAC-001, FAC-002, FAC-003 → GET /facilities → Always returns same 3
→ Snapshot comparison reliable
```

### DatabaseCleaner Strategy

Before each test, the database is reset to a blank state:

```
@BeforeEach seedTestData()
    ↓
DatabaseCleaner.cleanDatabase()
    ↓
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE facility_output;
TRUNCATE TABLE company;
TRUNCATE TABLE crs_approved;
... (all tables)
SET FOREIGN_KEY_CHECKS = 1;
    ↓
Database is now empty
```

**Why TRUNCATE instead of DELETE:**
- TRUNCATE is much faster (no row-by-row processing)
- DELETE would require handling FK constraints
- TRUNCATE resets identity seeds (auto-increment)

### TestDataSeeder Strategy

After cleaning, known test data is inserted:

```
@BeforeEach seedTestData()
    ↓
TestDataSeeder.seedAll()
    ↓
Create 5-10 test facilities with fixed external IDs
    FAC-001, FAC-002, FAC-003, etc.

Create 3-5 test companies
    COMP-001, COMP-002, etc.

Create test tracking systems, CRS records, etc.
    ↓
Database now has predictable, consistent data
```

**Current Implementation Status:**
- ✅ DatabaseCleaner fully implemented
- ⚠️ TestDataSeeder methods are stubs (TODO: implement with repositories)

**What's Needed for Full Implementation:**

```java
private void seedFacilities() {
    FacilityOutputDb facility1 = new FacilityOutputDb();
    facility1.setExtid("FAC-001");
    facility1.setUniqueId("WREGIS-12345");
    facility1.setState("CA");
    facility1.setFuelType("Solar");
    facility1.setCapacity(100.0);
    facility1.setName("Test Facility 1");
    // ... set all required fields
    facilityOutputRepository.save(facility1);

    FacilityOutputDb facility2 = new FacilityOutputDb();
    facility2.setExtid("FAC-002");
    // ... same pattern for other facilities
    facilityOutputRepository.save(facility2);
}
```

Once repositories are autowired into TestDataSeeder, this will work seamlessly.

---

## Gradle Integration

### How Gradle Tasks Work

Custom Gradle tasks control which tests run and how:

```gradle
tasks.register('captureSnapshots', Test) {
    group = 'verification'
    description = 'Capture API responses and save as snapshot files'
    systemProperty 'snapshot.mode', 'capture'
    useJUnitPlatform {
        includeTags 'snapshot'
    }
}

tasks.register('verifySnapshots', Test) {
    group = 'verification'
    description = 'Verify API responses match saved snapshots'
    systemProperty 'snapshot.mode', 'verify'
    useJUnitPlatform {
        includeTags 'snapshot'
    }
}
```

### What Each Task Does

#### captureSnapshots

```bash
$ ./gradlew :api-validation:captureSnapshots
```

1. **Runs JUnit tests** with tag `@Tag("snapshot")`
2. **Sets system property** `snapshot.mode=capture`
3. **Tests check:** `if (isCaptureMode()) { snapshotManager.save(...) }`
4. **Result:** Snapshots saved to disk (test always passes)

#### verifySnapshots

```bash
$ ./gradlew :api-validation:verifySnapshots
```

1. **Runs same JUnit tests** with tag `@Tag("snapshot")`
2. **Sets system property** `snapshot.mode=verify`
3. **Tests check:** `else { snapshotAssertion.assertEquals(...) }`
4. **Result:** Tests pass/fail based on comparison

### Tag Filtering

Only tests tagged with `@Tag("snapshot")` run:

```gradle
useJUnitPlatform {
    includeTags 'snapshot'    // Include tests with @Tag("snapshot")
}
```

This prevents running tests during normal build (which would fail without backend):

```bash
$ ./gradlew :api-validation:build
# Tests are skipped (no snapshot.mode system property)

$ ./gradlew :api-validation:captureSnapshots
# Only snapshot tests run (tagged + snapshot.mode=capture)
```

### Default Test Behavior

Normal `./gradlew test` is skipped:

```gradle
tasks.named('test') {
    // Skip tests by default since backend must be running
    onlyIf {
        System.getProperty('snapshot.mode') != null
    }
}
```

Tests only run when explicitly requested via captureSnapshots or verifySnapshots tasks.

---

## Design Patterns

### 1. Dual-Mode Testing

The same test code works in two modes:

```java
@Test
void testGetFacilities() {
    String response = RestAssured.get("/facilities").asString();

    if (isCaptureMode()) {
        // Mode 1: Save response
        snapshotManager.save(name, response);
    } else {
        // Mode 2: Compare response
        String expected = snapshotManager.load(name);
        snapshotAssertion.assertEquals(expected, response, exclusions);
    }
}
```

**Advantages:**
- Single test implementation for two purposes
- No code duplication
- Easy to switch between modes
- Clear intent in test code

**Disadvantages:**
- Test logic is slightly more complex
- Could be confused with test that does both (capture and verify)

**Alternative: Separate Test Classes**
```
FacilityApiCaptureTest extends BaseApiTest {
    // Capture-only tests
}

FacilityApiVerifyTest extends BaseApiTest {
    // Verify-only tests
}
```
(Not implemented, but possible)

### 2. Template Method Pattern (BaseApiTest)

BaseApiTest defines the template:

```java
abstract public class BaseApiTest {
    @BeforeAll
    static void setupOnce() {
        // Common setup for all tests
        // Configure REST Assured
        // Verify backend running
    }

    @BeforeEach
    void seedTestData() {
        // Common setup before each test
        // Clean and seed database
    }

    protected boolean isCaptureMode() { }
    protected String getSnapshotName(...) { }
}
```

Subclasses fill in the specifics:

```java
class FacilityOutputApiTest extends BaseApiTest {
    @Test
    void testGetFacilities_page0() {
        // Test-specific implementation
    }
}
```

**Benefits:**
- Avoid duplicating @BeforeAll, @BeforeEach
- Centralize common test infrastructure
- Consistent setup across all tests

### 3. Configuration Object Pattern (SnapshotConfig)

Instead of passing parameters everywhere, use a config object:

```java
// Bad: Too many parameters
assertEquals(expected, actual, endpoint, defaultExclusions,
             customExclusions, useYamlConfig);

// Good: Config object encapsulates all configuration
public class SnapshotConfig {
    public Set<String> getExclusions(String endpoint) { }
}
snapshotAssertion = new SnapshotAssertion(config);
snapshotAssertion.assertEquals(expected, actual, additionalExclusions);
```

**Benefits:**
- Easier to add new config options
- Config is loaded once, reused everywhere
- Clear separation between framework and test data

### 4. Recursive Field Removal

Exclusions apply at all nesting levels:

```java
private JsonNode removeFields(JsonNode node, Set<String> fieldsToExclude) {
    if (node.isObject()) {
        ObjectNode copy = deepCopy(node);
        // Remove from this level
        for (String field : fieldsToExclude) {
            copy.remove(field);
        }
        // Recursively remove from nested objects/arrays
        for (String fieldName : copy.fieldNames()) {
            copy.set(fieldName, removeFields(copy.get(fieldName), fieldsToExclude));
        }
        return copy;
    } else if (node.isArray()) {
        ArrayNode copy = deepCopy(node);
        for (int i = 0; i < copy.size(); i++) {
            copy.set(i, removeFields(copy.get(i), fieldsToExclude));
        }
        return copy;
    }
    return node;
}
```

**Example:**
```json
{
  "createdAt": "2024-01-01",  ← Removed
  "data": {
    "createdAt": "2024-01-01", ← Also removed (nested)
    "name": "Test"
  },
  "items": [
    {
      "createdAt": "2024-01-01", ← Also removed (in array)
      "value": 1
    }
  ]
}
```

---

## Troubleshooting

### Backend Not Running

**Error:**
```
✗ Backend not running on http://localhost:8080
  Please start the backend first: ./gradlew bootRun
```

**Root Cause:**
The @BeforeAll setup tries to connect to backend:

```java
io.restassured.response.Response response = RestAssured.given()
    .get("/facilities")
    .andReturn();
```

This fails if backend isn't listening on port 8080.

**Solution:**
```bash
# Terminal 1
./gradlew bootRun

# Terminal 2 (wait for "started" message)
./gradlew :api-validation:captureSnapshots
```

### Snapshot Not Found

**Error:**
```
⚠ Snapshot not found: GET_api_facilities_page0
  Run with -Dsnapshot.mode=capture to create it
```

**Root Cause:**
Verify mode tries to load snapshot that doesn't exist:

```java
String expected = snapshotManager.load(snapshotName);  ← File not found
```

**Solution:**
Run capture mode first:
```bash
./gradlew :api-validation:captureSnapshots
```

### Unexpected Snapshot Mismatch

**Error:**
```
Snapshot mismatch:
Expected:
{"name": "FAC-001", "state": "CA"}

Actual:
{"name": "FAC-001", "state": "NY"}
```

**Possible Causes:**
1. **API changed** - Response format or logic changed
2. **Test data differs** - Database wasn't cleaned, old data mixed with new
3. **Configuration issue** - Exclusions aren't applied correctly
4. **Backend bug** - Same API call returns different data

**Debugging Steps:**
1. Check database state:
   ```sql
   SELECT * FROM facility_output WHERE extid = 'FAC-001';
   ```

2. Verify exclusions in snapshot-config.yaml:
   ```yaml
   /api/facilities/{extid}:
     exclude:
       - state  # If state shouldn't be compared
   ```

3. Test data seeding:
   - Check TestDataSeeder is setting all fields
   - Verify database cleanup worked

4. Direct API test:
   ```bash
   curl http://localhost:8080/api/facilities/FAC-001
   ```

### Tests Pass But Snapshot Looks Wrong

**Issue:**
Snapshot file contains unexpected data, but test passes.

**Possible Cause:**
Fields being excluded that shouldn't be.

**Example:**
```json
{
  // createdAt is excluded, so not in snapshot
  // but this might be important for API contract
}
```

**Solution:**
Review snapshot-config.yaml. Remove fields that should be part of API contract.

**Key Principle:**
Only exclude fields that are **legitimately variable and unimportant**:
- Timestamps (change on every run)
- Auto-generated IDs (not useful in snapshot)
- Counts (depend on test data amount)

**Don't exclude:**
- Core business fields (name, state, etc.)
- Type information
- Nested structure
- Anything that's part of the API contract

---

## Summary

The API Snapshot Testing framework works by:

1. **Capturing** - Save API responses as JSON baseline
2. **Cleaning** - Reset database to known state before each test
3. **Seeding** - Insert deterministic test data
4. **Comparing** - Fetch fresh response and compare against baseline
5. **Normalizing** - Remove dynamic fields before comparison
6. **Reporting** - Show clear diffs on mismatches

This enables reliable, comprehensive API contract testing without writing assertion for every field.

The framework is designed to be:
- **Simple** - Two gradle commands: capture and verify
- **Maintainable** - Snapshots tracked in git, reviewable in PRs
- **Extensible** - Easy to add tests for new endpoints
- **Repeatable** - Same results every run with deterministic data
