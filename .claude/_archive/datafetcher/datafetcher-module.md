# DataFetcher Module

## What It Is

The **datafetcher** module fetches data from external web sources (HTML pages, APIs) and imports it into the database. Unlike the fileloader module which processes files uploaded INTO the system, datafetcher reaches OUT to external sources to pull data.

## Why

Following separation of concerns:
- **fileloader** - Handles files coming into the system (uploads, CSV imports)
- **datafetcher** - Fetches data from external URLs (web scraping, API calls)

This keeps each module focused and allows them to grow independently.

## Module Structure

```
datafetcher/
├── src/main/java/com/seibel/cancer/datafetcher/
│   ├── config/
│   │   ├── DataFetcherProperties.java      # Configuration properties
│   │   ├── TsNarProperties.java            # NAR configuration properties
│   │   └── TsMretsProperties.java          # M-RETS configuration properties
│   ├── importer/
│   │   ├── AbstractHtmlTableImporter.java  # Base class for HTML scraping
│   │   ├── GreenEApprovedHtmlImporter.java # Green-e approved facilities
│   │   ├── GreenEPendingHtmlImporter.java  # Green-e pending facilities
│   │   ├── TsNarHtmlImporter.java          # NAR HTML importer
│   │   └── TsMretsApiImporter.java         # M-RETS API importer
│   └── service/
│       ├── GreenELoadingService.java       # Orchestration service
│       ├── TsNarLoadingService.java        # NAR loading service
│       └── TsMretsLoadingService.java      # M-RETS loading service
├── src/test/java/com/seibel/cancer/datafetcher/
│   ├── importer/
│   │   ├── GreenEApprovedHtmlImporterTest.java
│   │   └── GreenEPendingHtmlImporterTest.java
│   └── service/
│       └── GreenELoadingServiceTest.java
└── build.gradle
```

## What It Does

### 1. HTML Table Scraping

**AbstractHtmlTableImporter** provides base functionality for scraping HTML tables:

**Features:**
- Fetches HTML from URL using Jsoup
- Parses tables by CSS selector
- Extracts headers automatically
- Maps rows to domain entities
- Row filtering (e.g., by status)
- Error tracking per row
- Batch saves to database

**Abstract Methods to Implement:**
```java
protected abstract String getTableSelector();      // CSS selector for table
protected abstract String getTableName();          // Table name for logging/identification
protected abstract T mapRow(Map<String, String> rowData, String version);
```

**Optional Override:**
```java
protected boolean shouldImportRow(Map<String, String> rowData) {
    // Return false to skip rows (default: import all)
}
```

### 2. Green-e CRS Data Import

Imports CRS Listed Facilities from https://www.green-e.org/sfdc/reports-data.php

**Two importers handle the same HTML table:**

| Importer | Filter | Target Load Table |
|----------|--------|------------------|
| `GreenEApprovedHtmlImporter` | Status = "Approved" | `crs_approved_load` |
| `GreenEPendingHtmlImporter` | Status = "Pending" | `crs_pending_load` |

**Two-Phase Process:**
- **Phase 1:** Importers write to load tables (`crs_approved_load`, `crs_pending_load`)
- **Phase 2:** `GreenELoadingService.processLoadedData()` clears main tables then transfers from load tables to `crs_approved` / `crs_pending` in batches

**HTML Table Columns (15 fields):**
- Name of Generation Facility
- Tracking System
- Tracking System ID Number
- Status
- Effective Date / Expiration Date
- Renewable Resource Type
- Facility State
- EIA or QF
- Facility ID Number
- Nameplate Capacity (MW)
- First Operational
- Repowering Date
- End Date of Extended Use
- Oregon Obligated LSE HB2021

### 3. Service Orchestration

**GreenELoadingService** coordinates both importers with a two-phase process:

- **Phase 1:** Deletes existing load data, then runs both importers to populate `crs_approved_load` and `crs_pending_load`
- **Phase 2:** Calls `processLoadedData()` which clears the main tables and transfers records from load tables to `crs_approved` / `crs_pending` in batches, then flags duplicate uniqueIds

**GreenELoadResult** provides:
- `getApprovedCount()` / `getPendingCount()`
- `getTotalImported()`
- `getTotalErrors()` / `getAllErrors()`
- `getSummary()` - Human-readable summary

## Architecture

```
┌─────────────────────────────────────────┐
│  REST API / Scheduler                   │
│  (FacilityReconService, GreenEImportScheduler)│
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│  GreenELoadingService                   │
│  (orchestration)                        │
└────────────────┬────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
┌───────▼──────┐  ┌──────▼───────────────┐
│ GreenE       │  │ GreenE               │
│ Approved     │  │ Pending              │
│ Importer     │  │ Importer             │
└───────┬──────┘  └──────┬───────────────┘
        │                │
        └────────┬───────┘
                 │
┌────────────────▼────────────────────────┐
│  AbstractHtmlTableImporter              │
│  (Jsoup fetch + parse)                  │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│  Database Module                        │
│  (CrsApprovedLoadDbService,             │
│   CrsPendingLoadDbService)              │
└────────────────┬────────────────────────┘
                 │ Phase 1: write to load tables
┌────────────────▼────────────────────────┐
│  MySQL Database                         │
│  (crs_approved_load, crs_pending_load)  │
└────────────────┬────────────────────────┘
                 │ Phase 2: transfer to main tables
┌────────────────▼────────────────────────┐
│  MySQL Database                         │
│  (crs_approved, crs_pending tables)     │
└─────────────────────────────────────────┘
```

## Dependencies

### Internal Modules
```gradle
implementation project(':common')   // Shared DTOs, ImportResult, ImportError
implementation project(':database') // DB services for persistence
```

### External Libraries
```gradle
// Spring Framework
implementation 'org.springframework.boot:spring-boot-starter'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'

// Web Scraping
implementation 'org.jsoup:jsoup:1.18.1'

// Utilities
implementation 'org.apache.commons:commons-lang3:3.17.0'

// Development
compileOnly 'org.projectlombok:lombok:1.18.34'
```

## Configuration

**DataFetcherProperties** (`application.yaml`):

```yaml
cancer:
  datafetcher:
    green-e:
      approved-enabled: true
      approved-url: https://www.green-e.org/sfdc/reports-data.php
      pending-enabled: true
      pending-url: https://www.green-e.org/sfdc/reports-data.php
      schedule-enabled: true
      schedule-cron: "0 0 6 * * *"  # Daily at 6 AM
```

**Note:** Both URLs point to the same page currently. If Green-e splits into separate pages later, just update the URLs - no code changes needed.

## Trigger Mechanisms

### 1. REST Endpoint (On-Demand)

The green-e load is triggered via the facility sync endpoint (`FacilityReconService`). `ImportController.java` has been removed.

The green-e load runs as part of: `POST /api/facility-recon/sync` or `POST /api/facility-recon/recon`

### 2. Scheduled Job (Automatic)

Located in main app: `src/main/java/com/seibel/cancer/scheduler/GreenEImportScheduler.java`

```java
@Scheduled(cron = "${cancer.datafetcher.green-e.schedule-cron:0 0 6 * * *}")
public void scheduledGreenELoad() {
    greenELoadingService.loadGreenEData();
}
```

Default: Daily at 6 AM

## Data Import Flow

```
1. Fetch HTML from URL (Jsoup)
        ↓
2. Parse table by CSS selector ("table#myTable")
        ↓
3. Extract headers from <thead> or first <tr>
        ↓
4. For each data row:
    a. Build Map<String, String> of header → cell value
    b. Check shouldImportRow() filter
    c. Call mapRow() to create entity
    d. Apply standard fields (version, status)
    e. Save via dbService.create()
    f. Database generates unique_id automatically
        ↓
5. Return ImportResult with successful + errors
```

## Version and Status Tracking

**Version Format:** `YYYY-MM-DD-HH-MM` (e.g., `2025-11-20-06-00`)

**Status Values** (in `import_status_type` table):

| Code | Name | Description |
|------|------|-------------|
| NEW | New | Newly imported, awaiting processing |
| PRC | Processed | Processed by business logic |
| SUP | Superseded | Replaced by newer import |

**Lifecycle:**
```
Import runs → records created with status = NEW
                    ↓
(Future) Business logic processes → status = PROCESSED
                    ↓
(Future) New import arrives → old records → status = SUPERSEDED
```

**Unique ID:**

The `unique_id` field is a MySQL generated column, automatically computed as:
```sql
CONCAT(tracking_system, ' ', tracking_system_id)
```

**Examples:** `MRETS 123456`, `WREGIS ABC-789`, `ERCOT TX-001`

This ensures consistency between:
- Data loaded via Liquibase CSV on startup
- Data fetched via datafetcher at runtime

The importer code does NOT set `unique_id` - the database generates it automatically.

## Adding a New HTML Importer

### Step 1: Create the Importer

```java
@Component
public class NewSourceHtmlImporter extends AbstractHtmlTableImporter<MyEntity> {

    public NewSourceHtmlImporter(MyEntityDbService dbService) {
        super(dbService);
    }

    @Override
    protected String getTableSelector() {
        return "table.data-table";  // CSS selector
    }

    @Override
    protected String getTableName() {
        return "my_entity";
    }

    @Override
    protected boolean shouldImportRow(Map<String, String> rowData) {
        // Optional: filter rows
        return true;
    }

    @Override
    protected MyEntity mapRow(Map<String, String> rowData, String version) {
        return MyEntity.builder()
            .field1(rowData.get("Column Header 1"))
            .field2(rowData.get("Column Header 2"))
            .version(version)
            .build();
    }
}
```

### Step 2: Create Service (if orchestrating multiple importers)

```java
@Service
public class NewSourceLoadingService {
    private final NewSourceHtmlImporter importer;

    public ImportResult<MyEntity> loadData() {
        String version = LocalDateTime.now().format(VERSION_FORMATTER);
        return importer.importFromUrl("https://example.com/data", version);
    }
}
```

### Step 3: Add Configuration

```java
@Data
@ConfigurationProperties(prefix = "cancer.datafetcher.new-source")
public class NewSourceProperties {
    private String url;
    private boolean enabled = true;
}
```

### Step 4: Add REST Endpoint / Scheduler (in main app)

## Testing

```bash
# Run all datafetcher tests
./gradlew :datafetcher:test

# Run specific test
./gradlew :datafetcher:test --tests "*GreenEApprovedHtmlImporterTest"
```

**Test Types:**
- **Unit Tests** - HTML parsing with sample HTML fixtures
- **Service Tests** - Orchestration logic with mocked importers
- **Integration Tests** - REST endpoint response format (in main app)

## Key Features

- **Jsoup Integration** - Robust HTML parsing and scraping
- **CSS Selectors** - Flexible table selection
- **Row Filtering** - Import only matching rows
- **Error Tracking** - Per-row error collection with ImportResult
- **Version Tracking** - Each import gets a unique version
- **Status Lifecycle** - NEW → PROCESSED → SUPERSEDED
- **Configurable URLs** - Change sources without code changes
- **Dual Triggers** - REST endpoint + scheduled job
- **Extensible** - Easy to add new HTML importers

## DataFetcher vs FileLoader

| Aspect | FileLoader | DataFetcher |
|--------|------------|-------------|
| **Direction** | Files coming IN | Fetches from external URLs |
| **Input** | CSV, PDF, ZIP uploads | HTML pages, APIs |
| **Parser** | Apache Commons CSV | Jsoup HTML parser |
| **Trigger** | User upload | Scheduled / on-demand |
| **Base Class** | `AbstractCsvImporter` | `AbstractHtmlTableImporter` |

Both modules:
- Use `ImportResult` / `ImportError` from common module
- Save via database module services
- Support version tracking
- Return detailed error information

## Module Type

**java-library** - Plain library module
- Used by main app for REST endpoints and scheduling
- No main class or embedded server
- Components discovered via Spring scanning

## Integration with Other Modules

### Used By:
- **Root (main application)** - REST endpoints, scheduler

### Uses:
- **:common** - ImportResult, ImportError, domain classes
- **:database** - CrsApprovedDbService, CrsPendingDbService

## Build Status

- **BUILD SUCCESSFUL** - All classes compile
- **Tests passing** - 15 tests (unit + integration)
- **Module integrated** - Added to settings.gradle
- **Ready to use** - Production-ready

## Planned Direction

This module is planned to expand beyond Green-e to handle all external data fetching:

**Current:**
- Green-e CRS Listed Facilities (Approved + Pending)

**Planned:**
- EIA data (Generator Operable, Generator Proposed, Plant, Utility)
- Tracking Systems (ERCOT, M-RETS, WREGIS, MIRECS, NAR, NC-RETS, NE Pool GIS, NYGATS, PJM-GATS)
- FacilityOutput data

This will replace the CSV importers currently in the `fileloader` module, pulling data directly from external sources rather than requiring manual CSV uploads.

## Related Documentation

- **Design Doc:** `.claude/green-e-loading-design.md` - Detailed design decisions and implementation history for Green-e loading feature
