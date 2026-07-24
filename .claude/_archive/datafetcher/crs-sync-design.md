# Green E Loading Process Design

## Overview

Load CRS Listed Facilities data from the Green-e public webpage into our database system.

**Source URL:** https://www.green-e.org/sfdc/reports-data.php

## Data Structure

The webpage contains an HTML table with **15 columns**:

| Column | Description | Data Type |
|--------|-------------|-----------|
| Name of Generation Facility | Facility name | String |
| Tracking System | REC tracking system (M-RETS, WREGIS, etc.) | String |
| Tracking System ID Number | ID in the tracking system | String |
| Status | Approved/Pending/etc. | String |
| Effective Date | When listing became effective | Date |
| Expiration Date | When listing expires | Date |
| Renewable Resource Type | Solar, Wind, Hydro, etc. | String |
| Facility State | US state location | String |
| EIA or QF | EIA plant ID or QF status | String |
| Facility ID Number | Internal facility ID | String |
| Nameplate Capacity (MW) | Generation capacity | Decimal |
| First Operational | Date facility started operating | Date |
| Repowering Date | Date of repowering (if applicable) | Date |
| End Date of Extended Use | Extended use end date | Date |
| Oregon Obligated LSE (HB2021) | Oregon HB2021 compliance flag | String |

## Architecture Decision

> **Note:** The original design wrote directly to `crs_approved`/`crs_pending`. The implemented version uses a two-phase load table approach — importers write to `crs_approved_load`/`crs_pending_load` first (Phase 1), then `GreenELoadingService.processLoadedData()` transfers to the main tables (Phase 2).

**Decision:** Load into existing CRS tables using two separate importers with load table staging

The Green-e webpage contains the same data that currently comes from CSV files. Two importers handle each table:

```
┌─────────────────────────────────────┐
│  GreenEApprovedHtmlImporter         │
│  (filters Status = "Approved")      │
└────────────────┬────────────────────┘
                 │ Phase 1
         ┌───────▼────────────┐
         │ crs_approved_load  │
         └───────┬────────────┘
                 │ Phase 2
         ┌───────▼───────┐
         │ crs_approved  │
         └───────────────┘

┌─────────────────────────────────────┐
│  GreenEPendingHtmlImporter          │
│  (filters Status = "Pending")       │
└────────────────┬────────────────────┘
                 │ Phase 1
         ┌───────▼───────────┐
         │ crs_pending_load  │
         └───────┬───────────┘
                 │ Phase 2
         ┌───────▼───────┐
         │ crs_pending   │
         └───────────────┘
```

**Why two importers:**
- Matches two DB tables
- Future-proof if Green-e splits into separate pages
- Cleaner separation of concerns
- Can configure different URLs later without code changes

### Note on CrsPendingDb

`CrsPendingDb` only has 4 fields while webpage has 15. Options:
1. **Map available fields only** - Pending records will have less data
2. **Expand CrsPendingDb** - Add all fields to match CrsApprovedDb

**Recommendation:** Map available fields for now, expand later if needed.

## Implementation Plan

### Phase 1: Database (No New Entities Needed)

**Using existing entities:**
- `CrsApprovedDb` - Full 13 fields, matches webpage perfectly
- `CrsPendingDb` - 4 fields (facility, trackingSystem, trackingSystemId, previousExpirationDate)

**Existing services to use:**
- `CrsApprovedDbService`
- `CrsPendingDbService`

**No Liquibase migration needed** - tables already exist.

### Version and Status Implementation

**Version Format:** `YYYY-MM-DD-HH-MM` (16 chars, fits column)
- Example: `2025-11-20-06-00` (scheduled 6 AM)
- Example: `2025-11-20-14-30` (on-demand 2:30 PM)

**Status Type Table:** Create using `BaseTypeDb` pattern

**Entity:** `ImportStatusTypeDb extends BaseTypeDb` in database module

**Table:** `import_status_type` with initial data:

| code | name       | description                                        |
|------|------------|----------------------------------------------------|
| NEW  | New        | Newly imported, awaiting processing                |
| PRC  | Processed  | Processed by business logic and matched to facilities |
| SUP  | Superseded | Replaced by a newer import version                 |

**Liquibase migration needed** to create table and populate initial values.

**Usage:** CRS records reference status by code (e.g., `status = 'NEW'`)

**Future optimization:** Cache types on startup to avoid DB hits on every lookup.

**Lifecycle:**
```
Import runs → records created with status = NEW
                    ↓
(Future) Business logic processes → status = PROCESSED
                    ↓
(Future) New import arrives → old version records → status = SUPERSEDED
```

**Initial Implementation:** Import sets status = `NEW` only. PROCESSED/SUPERSEDED transitions to be implemented later.

**Note:** Store enum name as string in DB (not int value) for readability.

### Phase 2: HTML Table Importer

**Module:** `:fileloader`

1. Create `AbstractHtmlTableImporter<T>` base class
   - Uses Jsoup (already a dependency)
   - Fetches URL content
   - Parses HTML table by selector
   - Maps rows to entities
   - Batch saves to database

2. Create `GreenEListedHtmlImporter` extending base class
   - Define table selector (CSS selector for the data table)
   - Map column headers to entity fields
   - Handle date parsing and data type conversion

**Base Class Structure:**
```java
public abstract class AbstractHtmlTableImporter<T extends BaseCsvDomain> {

    protected abstract String getTableSelector();
    protected abstract Map<String, String> getHeaderToFieldMapping();
    protected abstract T mapRow(Map<String, String> rowData, String version);

    public ImportResult importFromUrl(String url, String version) {
        // 1. Fetch HTML with Jsoup
        // 2. Select table element
        // 3. Extract headers
        // 4. Iterate rows and map to entities
        // 5. Batch save to database
        // 6. Return ImportResult with stats
    }
}
```

### Phase 3: Service Layer

**Module:** `:fileloader` or main app

Create `GreenELoadingService` to orchestrate:
- URL fetching
- Import execution
- Version tracking
- Error handling and logging
- Import result reporting

```java
@Service
public class GreenELoadingService {

    private final GreenEListedHtmlImporter importer;

    public ImportResult loadGreenEData() {
        String version = LocalDate.now().toString();
        String url = "https://www.green-e.org/sfdc/reports-data.php";
        return importer.importFromUrl(url, version);
    }
}
```

### Phase 4: Trigger Mechanisms

**Both scheduled and on-demand:**

1. **REST Endpoint** - On-demand trigger via website button
   ```java
   @PostMapping("/api/import/green-e")
   public ResponseEntity<ImportResult> triggerGreenEImport() {
       ImportResult result = greenELoadingService.loadGreenEData();
       return ResponseEntity.ok(result);
   }
   ```
   - Frontend button calls this endpoint
   - Returns import stats (records imported, errors, duration)

2. **Scheduled Job** - Daily automatic loading
   ```java
   @Scheduled(cron = "0 0 6 * * *") // Every day at 6 AM
   public void scheduledGreenELoad() {
       greenELoadingService.loadGreenEData();
   }
   ```
   - Runs automatically each day
   - Logs results for monitoring

## New Module: datafetcher

**Purpose:** Fetch data from external web sources (separate from fileloader which handles files coming INTO the system)

**Why new module:**
- Keeps fileloader focused on file processing
- Clean separation of concerns
- Reusable patterns for future web sources
- Can grow independently

## File Structure

```
database/
├── src/main/java/com/seibel/cancer/database/db/entity/
│   └── ImportStatusTypeDb.java               # Status type entity
├── src/main/java/com/seibel/cancer/database/db/repository/
│   └── ImportStatusTypeRepository.java       # Repository
└── src/main/resources/db/changelog/changes/
    └── YYYYMMDD-create-import-status-type.yaml  # Liquibase migration

datafetcher/                                   # NEW MODULE
├── build.gradle
└── src/main/java/com/seibel/cancer/datafetcher/
    ├── importer/
    │   ├── AbstractHtmlTableImporter.java    # Base class for HTML scraping
    │   ├── GreenEApprovedHtmlImporter.java   # Approved facilities importer
    │   └── GreenEPendingHtmlImporter.java    # Pending facilities importer
    ├── service/
    │   └── GreenELoadingService.java         # Orchestration (runs both importers)
    └── config/
        └── DataFetcherProperties.java        # Configuration

src/main/java/com/seibel/cancer/
└── controller/
    └── ImportController.java                 # REST endpoint (or add to existing)
```

## Data Considerations

### Historical Data Strategy

Since we're keeping all historical snapshots:

- **Each import creates new records** with a version date (e.g., "2025-11-20")
- **No deletion of previous imports** - all historical data is preserved
- **Query by version** to get data from a specific date
- **Compare versions** to detect changes over time (new facilities, status changes, etc.)

### Collision Handling

**Old Data vs New Data:**
When a new import runs:
1. Mark existing records (status = `NEW` or `PROCESSED`) → `SUPERSEDED`
2. Insert new records with status = `NEW`
3. Query current data: `WHERE status != 'SUP'`

**Same-day re-imports:**
- Multiple imports on same day get different versions (YYYY-MM-DD-HH-MM)
- Each import supersedes previous data

**Duplicate facilities in source:**
- If same facility appears twice in HTML table, last one wins
- Log warning for duplicates

### Version Tracking

- Track import date as version string (e.g., "2025-11-20")
- Allows historical comparison, trend analysis, and rollback
- Query patterns:
  - Latest: `WHERE version = (SELECT MAX(version) FROM ts_green_e_listed)`
  - Specific date: `WHERE version = '2025-11-20'`
  - Date range: `WHERE version BETWEEN '2025-01-01' AND '2025-11-20'`

### Data Validation

- Validate required fields (facility name, tracking system, status)
- Parse dates with multiple format support
- Handle null/empty values gracefully
- Log validation errors without failing entire import

### Error Handling

- Network timeout handling for URL fetch
- HTML structure change detection
- Partial import support (continue on row errors)
- Detailed error logging with row numbers

## Relationship to Existing CRS Data

The fileloader module already has:
- `CrsApprovedCsvImporter` - CRS approved facilities from CSV
- `CrsPendingCsvImporter` - CRS pending facilities from CSV

**Decision:** Load Green E webpage data into the same tables (`crs_approved`, `crs_pending`).

This means:
- Web scraping becomes an alternative data source to CSV files
- Same tables, different import method
- Version field distinguishes import batches
- Can run both CSV and web imports (they'll have different versions)

## Testing Plan

1. **Unit Tests**
   - HTML parsing with sample HTML fixtures
   - Date parsing edge cases
   - Field mapping accuracy

2. **Integration Tests**
   - Full import from test URL or local HTML file
   - Database persistence verification
   - Duplicate handling

3. **Manual Testing**
   - Run against live URL
   - Verify data accuracy against source
   - Check import metrics

## Configuration

Add to `application.yaml`:

```yaml
cancer:
  datafetcher:
    green-e:
      approved-url: https://www.green-e.org/sfdc/reports-data.php
      pending-url: https://www.green-e.org/sfdc/reports-data.php   # same URL for now
      schedule-enabled: true
      schedule-cron: "0 0 6 * * *"  # Daily at 6 AM
```

**Note:** Both URLs point to same page currently. If Green-e splits into separate pages later, just update the URLs - no code changes needed.

## Decisions

1. **Update frequency** - **Daily scheduled job + On-demand via website button**

2. **Historical data** - **Keep all historical snapshots** (each import creates new records with version date)

3. **Data matching** - **TBD** - May link Green E facilities to existing facility data later

## Resolved Questions

1. **Notifications** - Yes, alert on import failures (implementation TBD)

2. **Oregon HB2021 field** - Yes, include it (maps to `oregonObligation` in CrsApprovedDb)

3. **Daily schedule time** - 6 AM confirmed

## Implementation Order

1. Create `datafetcher` module (build.gradle, settings.gradle)
2. ImportStatusTypeDb entity + repository + Liquibase migration
3. Abstract HTML table importer base class
4. Green E specific importer (routes to CrsApproved/CrsPending)
5. Service layer (orchestration)
6. REST endpoint for website button
7. Scheduled job for daily runs
8. Tests

## Estimated Effort

- Phase 0 (New module): 2 files (build.gradle, settings.gradle update)
- Phase 1 (Status Type): 3 files (entity, repository, migration)
- Phase 2 (Importer): 3 files (base + approved + pending)
- Phase 3 (Service + Config): 2 files
- Phase 4 (Endpoint + Schedule): 1-2 files
- Tests: 3-4 files

Total: ~14-16 files

## Next Steps

1. ~~Review and approve this design~~
2. ~~Answer open questions~~
3. ~~Begin implementation with Phase 1 (database entity)~~

---

## Implementation Status

**Status: COMPLETED** (2025-11-20)

All core implementation steps have been completed. The system is ready for testing against the live Green-e website.

### Completed Implementation

#### Step 1: Created `datafetcher` Module ✅
- `datafetcher/build.gradle` - Module configuration with Jsoup, Spring Boot, JPA dependencies
- `settings.gradle` - Updated to include `datafetcher` module

#### Step 2: ImportStatusTypeDb Entity ✅
- `database/src/main/java/com/seibel/cancer/database/db/entity/ImportStatusTypeDb.java`
- `database/src/main/java/com/seibel/cancer/database/db/repository/ImportStatusTypeRepository.java`
- `database/src/main/resources/db/changelog/changes/011-import-status-type.yaml`
  - Creates `import_status_type` table with NEW, PRC, SUP statuses

#### Step 3: Abstract HTML Table Importer ✅
- Refactored `ImportResult` and `ImportError` to `common` module for shared use
- `datafetcher/src/main/java/com/seibel/cancer/datafetcher/importer/AbstractHtmlTableImporter.java`
  - Uses Jsoup for HTML fetching and parsing
  - Configurable table selector
  - Row filtering support via `shouldImportRow()`
  - Returns `ImportResult<T>` with success/error tracking

#### Step 4: Green-e Specific Importers ✅
- `datafetcher/src/main/java/com/seibel/cancer/datafetcher/importer/GreenEApprovedHtmlImporter.java`
  - Filters rows where Status = "Approved"
  - Maps all 15 HTML columns to `CrsApproved` domain
- `datafetcher/src/main/java/com/seibel/cancer/datafetcher/importer/GreenEPendingHtmlImporter.java`
  - Filters rows where Status = "Pending"
  - Maps 4 fields to `CrsPending` domain

#### Step 5: Service Layer ✅
- `datafetcher/src/main/java/com/seibel/cancer/datafetcher/config/DataFetcherProperties.java`
  - Configuration for URLs and schedule settings
- `datafetcher/src/main/java/com/seibel/cancer/datafetcher/service/GreenELoadingService.java`
  - Orchestrates both importers
  - Generates version in `YYYY-MM-DD-HH-MM` format
  - Returns `GreenELoadResult` with combined statistics

#### Step 6: REST Endpoint + Scheduler ✅
- `ImportController.java` (removed — green-e import is triggered via the facility sync endpoint and `FacilityReconService`)
- `src/main/java/com/seibel/cancer/scheduler/GreenEImportScheduler.java`
  - Daily scheduled job (default 6 AM)
  - Configurable via `cancer.datafetcher.green-e.schedule-cron`
- `CancerApplication.java` updated with:
  - `@EnableScheduling`
  - `com.seibel.cancer.datafetcher` package scanning
  - `DataFetcherProperties` configuration

#### Step 7: Tests ✅
**Unit Tests (11 tests):**
- `datafetcher/src/test/java/com/seibel/cancer/datafetcher/importer/GreenEApprovedHtmlImporterTest.java` (4 tests)
- `datafetcher/src/test/java/com/seibel/cancer/datafetcher/importer/GreenEPendingHtmlImporterTest.java` (3 tests)
- `datafetcher/src/test/java/com/seibel/cancer/datafetcher/service/GreenELoadingServiceTest.java` (4 tests)

**Integration Tests (4 tests):**
- `src/test/java/com/seibel/cancer/web/controller/ImportControllerTest.java` (4 tests)

### Actual File Structure

```
database/
├── src/main/java/com/seibel/cancer/database/db/entity/
│   └── ImportStatusTypeDb.java
├── src/main/java/com/seibel/cancer/database/db/repository/
│   └── ImportStatusTypeRepository.java
└── src/main/resources/db/changelog/changes/
    └── 011-import-status-type.yaml

common/
└── src/main/java/com/seibel/cancer/common/model/
    ├── ImportResult.java          # Moved from fileloader
    └── ImportError.java           # Moved from fileloader

datafetcher/
├── build.gradle
└── src/
    ├── main/java/com/seibel/cancer/datafetcher/
    │   ├── importer/
    │   │   ├── AbstractHtmlTableImporter.java
    │   │   ├── GreenEApprovedHtmlImporter.java
    │   │   └── GreenEPendingHtmlImporter.java
    │   ├── service/
    │   │   └── GreenELoadingService.java
    │   └── config/
    │       └── DataFetcherProperties.java
    └── test/java/com/seibel/cancer/datafetcher/
        ├── importer/
        │   ├── GreenEApprovedHtmlImporterTest.java
        │   └── GreenEPendingHtmlImporterTest.java
        └── service/
            └── GreenELoadingServiceTest.java

src/main/java/com/seibel/cancer/
└── scheduler/
    └── GreenEImportScheduler.java
```

### Configuration Required

Add to `application.yaml`:

```yaml
cancer:
  datafetcher:
    green-e:
      approved-url: https://www.green-e.org/sfdc/reports-data.php
      pending-url: https://www.green-e.org/sfdc/reports-data.php
      schedule-enabled: true
      schedule-cron: "0 0 6 * * *"
```

### API Response Format

`POST /api/import/green-e` returns:

```json
{
  "version": "2025-11-20-08-04",
  "approvedCount": 150,
  "pendingCount": 12,
  "totalImported": 162,
  "errorCount": 0,
  "status": "Success",
  "summary": "Green-e Load [version=2025-11-20-08-04]: 150 approved, 12 pending imported. 0 total errors."
}
```

### Remaining Work

1. **Run Liquibase migration** - Apply `011-import-status-type.yaml` to database
2. **Add configuration** - Add `cancer.datafetcher.green-e.*` properties to `application.yaml`
3. **Test against live URL** - Verify HTML parsing works with actual Green-e website
4. **Implement notifications** - Alert on import failures (TODO in scheduler)
5. **Implement status transitions** - PROCESSED/SUPERSEDED status updates (future)

### Test Summary

All **15 tests passing**:
- Unit tests verify HTML parsing, filtering, and field mapping
- Integration tests verify REST endpoint response format

```
BUILD SUCCESSFUL
```
