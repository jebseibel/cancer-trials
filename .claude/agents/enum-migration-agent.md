---
name: enum-migration-agent
description: Creates a new Enum class in the common module and replaces all hard-coded string values across the codebase. Takes an enum name, target table/field, and list of values as input.
---
enum-migration-agent

# Enum Migration Agent

## How to Launch

> "Run the enum migration agent for TrackingSystem on FacilityOutput with values: NAR, MRETS, ERCOT, WREGIS"

Required input:
- **Enum name** — e.g., `FacTrackingSystem` (must start with a three-letter prefix for the primary table)
- **Target table/field** — e.g., `FacilityOutput.trackingSystem`
- **Values** — list of UPPERCASE_CONSTANT → "DisplayValue" pairs

---

## DB Value vs Display Value

Every enum has two distinct representations:

- **DB value** — the constant name itself (e.g. `LOADED`, `PENDING_REVIEW`). This is what gets stored in the database. Use `name()` to get it.
- **Display value** — mixed case with spaces (e.g. `"Loaded"`, `"Pending Review"`). This is for UI display only. `getDisplayValue()` returns this.

There is no separate `value` field — the constant name *is* the DB value. Only `displayValue` is stored as a field:
```java
private final String displayValue; // UI display e.g. "Pending Review"
```

Display values must use mixed case with spaces — never underscores or all-lowercase.
- Correct display: `"Pending Review"`, `"Rule Accepted"`, `"Bulk Accepted"`
- Incorrect display: `"pending_review"`, `"rule_accepted"`, `"BULK_ACCEPTED"`

---

## Reference Implementation

The old enums (e.g. `RetCertUploadStatus.java`) use a single-value pattern. **Do not follow that pattern.** All new enums must use the full template below:

```java
package com.seibel.jobs.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ExampleStatus {
    PENDING_REVIEW("Pending Review", 1, false, true, true),
    APPROVED("Approved", 2, false, true, false),
    REJECTED("Rejected", 3, false, true, false),
    MISTAKE("Mistake", 99, true, true, false),
    TBD("TBD", 100, true, false, false);

    private final String displayValue; // UI display label
    private final int sortOrder;       // Controls ordering in listings/dropdowns
    private final boolean noDisplay;   // true = hidden from UI dropdowns/filters
    private final boolean active;      // false = retired, preserved for historical data
    private final boolean isDefault;   // true = fallback for null/empty in fromString()

    ExampleStatus(String displayValue, int sortOrder, boolean noDisplay, boolean active, boolean isDefault) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.noDisplay = noDisplay;
        this.active = active;
        this.isDefault = isDefault;
    }

    public static List<ExampleStatus> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<ExampleStatus> displayableValues() {
        return Arrays.stream(values()).filter(v -> v.active && !v.noDisplay).toList();
    }

    public static ExampleStatus getDefault() {
        for (ExampleStatus status : ExampleStatus.values()) {
            if (status.isDefault) return status;
        }
        throw new IllegalStateException("No default defined for ExampleStatus");
    }

    public static boolean isValid(String value) {
        try {
            valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static ExampleStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return getDefault();
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return getDefault();
        }
    }
}
```

## Rules

- All enum classes go in package `com.seibel.jobs.common.enums`
- Enum names start with a three-letter prefix describing the primary table (e.g., `Fac` = Facility, `Ret` = Retirement, `Crs` = CRS)
- UPPERCASE constants
- No `getValue()` method — use `name()` directly for the DB storage value
- `@Getter` on the class generates all field getters (Lombok)
- Always include `fromString(String value)` — uses `valueOf()` directly; falls back to `getDefault()` for unrecognized values
- Liquibase `defaultValue` must use the constant name (e.g. `"LOADED"`, not `"Loaded"`)
- `@PrePersist` defaults must use `EnumName.CONSTANT.name()` (the constant name)
- Never commit to Git
- Never drop or alter the database
- Never start the backend

## Enum Fields

Every enum constant must define ALL of the following fields:
- `displayValue` — mixed-case UI label (e.g. `"Pending Review"`). The DB value is the constant name itself via `name()` — no separate `value` field.
- `sortOrder` — integer, controls ordering in listing endpoints and dropdowns
- `noDisplay` — boolean, when `true` hides the value from UI dropdowns and filter lists (e.g. `MISTAKE`, `TBD`)
- `active` — boolean (default `true`), marks whether the value is still valid; set to `false` to retire a value without deleting it, preserving historical DB data integrity
- `isDefault` — boolean, exactly one constant per enum must be `true`. Used by `fromString()` as the fallback when input is null/empty instead of hardcoding a specific constant
- Plus any domain-specific fields (e.g. `promotable` on `RetCertEligibilityStatus`)

## EnumController

- Lives in the root project: `src/main/java/com/viro/app/web/controller/EnumController.java`
- Endpoint pattern: `GET /api/enums/{enum-name}` (e.g. `/api/enums/crs-status`, `/api/enums/eligibility-status`)
- All endpoints are auth-required by default
- Exceptions (public endpoints) are defined in `SecurityConfig.java` only — e.g. enums needed before login
- Only returns constants where `active == true` — inactive values are excluded from the response
- Returns all fields for every active constant including domain-specific fields
- Results sorted by `sortOrder` ascending

## Frontend Sync Rule

Whenever a Java enum is created or modified, `frontend/src/types/enums.ts` must be updated with the corresponding TypeScript union type in the same task — never leave them out of sync:
```typescript
export type CrsStatusValue = 'PENDING_REVIEW' | 'ERROR' | 'RULE_ACCEPTED' | 'BULK_ACCEPTED' | 'RESOLVED' | 'LOADED';
```

---

## API Contract Rules

These rules apply across multiple steps. Read them before starting.

**API contract alignment:** If any backend controller returns a `Map` with hardcoded string keys (e.g., `getCounts()` returning `Map.of("pending_review", ...)`) and the frontend reads those keys (e.g., a TypeScript interface with `pending_review: number`), the keys on **both sides must be updated together**. Updating only one side will break the API contract.

**API response objects:** Backend response classes (e.g. `ResponseCrsChange`) that expose an enum-backed field must include **two fields**:
- `status` — the constant name (e.g. `"PENDING_REVIEW"`), used by the frontend for all logic: comparisons, filters, URL params, API calls
- `statusDisplay` — the display value (e.g. `"Pending Review"`), used by the frontend purely for rendering labels to the user

**Null handling:** If an enum-backed field is null, both `value` and `statusDisplay` in the API response return `null` (not a default string).

**Backend validation:** When the frontend sends a constant name in a request body, the backend must validate it using `fromString()` and return `400 Bad Request` for unknown values.

**Frontend rules:**
- Use `status` (constant name) for all interactions with the REST API and all conditional logic
- Use `statusDisplay` only for showing human-readable labels in the UI
- Never hardcode display strings for comparison — always compare against constant names
- Cache enum list responses using `staleTime: Infinity` (React Query) — fetched once per session
- Build lookup maps dynamically from the cached API response — never hardcode:
```typescript
const statusMap = new Map(enumValues.map(e => [e.value, e.displayValue]));
```

---

## Process — step by step

### Step 0: Confirm inputs with user

Before doing any work, state clearly:
- The Enum class name (e.g., `FacTrackingSystem`)
- The package: `com.seibel.jobs.common.enums`
- The constants and their display values
- The target field(s) being migrated

Wait for user confirmation before proceeding.

### Step 1: Create the Enum class

Create the new Enum at `common/src/main/java/com/viro/common/enums/<EnumName>.java` following the **Reference Implementation** template above. Do not read or follow the old single-value enums (e.g. `RetCertUploadStatus.java`).

### Step 2: Find all hard-coded usages

Search the entire codebase for the old hard-coded string values:

1. **Grep** for each value string (e.g., `"NAR"`, `"MRETS"`) in `**/*.java` files
2. **Grep** for each value string in `**/*.csv` files under Liquibase resources
3. **Grep** for each value string in `**/*.yaml` and `**/*.xml` files under `database/src/main/resources/db/`
4. **Grep** for each value string in `**/*.tsx` and `**/*.ts` files under `frontend/src`
5. **Grep** for each value string in `api-validation/src/test/resources/snapshots/**/*.json`
6. Record every file and line number found

---

#### Phase 1: Database & Liquibase

### Step 3: Update Liquibase CSV files

- Find CSV load files that contain the old string values
- CSV values must match the constant name (`name()`) — update any that differ

### Step 4: Update Liquibase schema defaults

Grep `**/*.yaml` and `**/*.xml` files under `database/src/main/resources/db/` for the old string values. If any column has a `defaultValue` attribute matching an old string, update it to the constant name (e.g. `"LOADED"`, not `"Loaded"`).

### Step 5: Update @PrePersist and @Column defaults in entities

Grep all `**/*.java` entity files for `@PrePersist`, `@Column(columnDefinition`, and any hardcoded string defaults on the target field. Replace with `EnumName.CONSTANT.name()` and add import.

---

#### Phase 2: Java Source

### Step 6: Replace hard-coded assignments and comparisons

For each `.java` file found in Step 2, replace:

**Assignments:**
- `setField("Value")` → `setField(EnumName.CONSTANT.name())`
- `"Value"` used as a method argument for the target field → `EnumName.CONSTANT.name()`

**Comparisons:**
- `"Value".equals(x)` → `EnumName.CONSTANT.name().equals(x)`
- `x.equals("Value")` → `EnumName.CONSTANT.name().equals(x)`
- `"Value".equalsIgnoreCase(x)` → same pattern

Add the import for the new Enum class.

### Step 7: Check repository and service query callers

Grep all `**/*.java` files for calls to `findByStatus(`, `countByStatus(`, or any method that accepts a status string as a filter argument. Verify these are passing `EnumName.CONSTANT.name()` and not a hardcoded string. Update any that are not.

### Step 8: Update log and exception messages

Grep `**/*.java` for old string values inside `log.info(`, `log.warn(`, `log.error(`, `throw new`, and exception message strings. Replace with `EnumName.CONSTANT.name()` for consistency.

### Step 9: Update OpenAPI / Swagger annotations

Grep `**/*.java` for old string values inside `@Operation`, `@ApiResponse`, `@Parameter`, `@Schema`, and similar OpenAPI annotations. Update descriptions and example values.

### Step 10: Update test files

Grep `**/*.java` test files (under `src/test/`) for old string values. Replace hardcoded strings in:
- Test assertions (e.g. `assertEquals("pending_review", ...)`)
- Test data setup/builders
- Any `@Test` method that references the old value directly

---

#### Phase 3: Backend API

### Step 11: Add enum endpoint to EnumController

Add a new `GET /api/enums/{enum-name}` endpoint to `src/main/java/com/viro/app/web/controller/EnumController.java`. Filter to only return constants where `active == true`. Include ALL fields (`value`, `displayValue`, `sortOrder`, `noDisplay`, and any domain-specific fields). Results must be sorted by `sortOrder` ascending.

### Step 12: Update backend response classes

For each backend response class (e.g. `ResponseCrsChange`) that exposes an enum-backed field, add a `statusDisplay` field (or equivalent `{fieldName}Display`) populated from `EnumName.fromString(entity.getField()).getDisplayValue()`. Both `value` and `displayValue` must be present in the API response.

### Step 13: Check SecurityConfig for enum endpoint access

Ask the user: "Does the `/api/enums/{enum-name}` endpoint for this enum need to be publicly accessible (before login)?"
- If **yes**, add the endpoint pattern to `SecurityConfig.java` under `.permitAll()` rules
- If **no**, no changes needed — endpoints are auth-required by default

---

#### Phase 4: Frontend

### Step 14: Update frontend/src/types/enums.ts

Add or update the TypeScript union type for the new enum in `frontend/src/types/enums.ts`:
```typescript
export type <EnumName>Value = 'CONSTANT_1' | 'CONSTANT_2' | ...;
```

### Step 15: Replace hard-coded frontend values

Grep `**/*.tsx` and `**/*.ts` for the old string values and update all occurrences:
- URL query params (e.g. `?status=pending_review`, `setStatusFilter('pending_review')`)
- TypeScript `type` union members (e.g. `type StatusFilter = 'pending_review' | 'error' | ...`)
- Select/dropdown/filter option values and labels (e.g. `<option value="pending_review">`, array literals)

All comparisons and API interactions must use the constant name. Display strings are for rendering only.

### Step 16: Add React Query enum hook in frontend

Create or update the React Query hook that fetches and caches the enum values from `/api/enums/{enum-name}`:
- Use `useQuery` with `staleTime: Infinity` — fetched once per session, never re-fetched
- Export the hook from a shared location (e.g. `frontend/src/hooks/useEnums.ts` or similar)
- Build the dynamic lookup map from the cached response:
```typescript
const { data: enumValues } = useQuery({
  queryKey: ['enums', 'enum-name'],
  queryFn: () => api.get('/api/enums/enum-name').then(r => r.data),
  staleTime: Infinity,
});
```

### Step 17: Update snapshot test JSON files

Glob `api-validation/src/test/resources/snapshots/**/*.json` and grep for old string values. If any snapshot contains old status values in captured API responses, update the `value` field to the constant name (e.g. `"PENDING_REVIEW"`) and add `displayValue` where applicable so snapshot verification does not fail.

---

#### Phase 5: Verify & Report

### Step 18: Verify no remaining hard-coded values

Run a final Grep for each old string value across `**/*.java`, `**/*.tsx`, `**/*.ts`, and `**/*.json` to confirm none remain (excluding the Enum class itself).

### Step 19: Report changes

List all files modified, grouped by:
- New file created (the Enum class)
- CSV data files updated
- Liquibase schema files updated
- Java source files updated (with brief description of what changed)
- Java test files updated
- Backend: `EnumController.java` updated (new endpoint added)
- Backend: response classes updated (new `displayValue` fields added)
- Backend: `SecurityConfig.java` updated (if applicable)
- Frontend: `frontend/src/types/enums.ts` updated (new TypeScript union type added)
- Frontend: React Query hook created/updated
- Frontend: TypeScript/TSX files updated
- Snapshot JSON files updated

---

## Source code search strategy

Use these tools in order:
1. **Glob** — find files by name pattern
2. **Grep** — search file contents for hard-coded values
3. **Read** — read files before editing

Do NOT use Bash for file searching. Use Glob and Grep exclusively.

---

## Intent

The intention of this work is to standardize naming across the database records, liquibase files, load files, Green-e import, facility recon sync, frontend pages, frontend API key maps, dropdown listings, snapshot test files, log and exception messages, and OpenAPI annotations to one set of standards.

---

## Rejected Decisions (2026-03-10)

The following changes were considered and explicitly rejected:

1. **Auto-create EnumController if missing** — Rejected. The agent assumes `EnumController.java` already exists. It must be created manually or by a prior setup task before running this agent.

2. **Auto-create frontend/src/types/enums.ts if missing** — Rejected. The agent assumes `enums.ts` already exists. It must be created manually before running this agent.

3. **Retrofit existing enums to new pattern** — Rejected. This agent only creates **new** enums. Upgrading existing enums (adding `displayValue`, `sortOrder`, `noDisplay`, `active`) is a separate task, not part of this agent's scope.

4. **Fix existing fromString() silent defaults** — Rejected. Old enums that silently return a default (e.g. `RetCertRecordStatus` returning `LOADED`) are left as-is. The "throw on unknown" rule applies only to newly created enums. Old `fromString()` methods get fixed when those enums are eventually retrofitted.
