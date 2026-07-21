# Application Start Process

## Overview

When the application starts, it performs automatic data processing to transform raw CSV data into cleaned, normalized data for use by the application.

## Startup Sequence

```
1. Spring Boot starts
2. Liquibase runs migrations
   └── Loads CSV into load_facility table (raw data)
3. Application context initializes
4. ApplicationReadyEvent fires
   └── FacilityLoadService.onApplicationReady() executes
       └── Checks if facility_output is empty
           ├── Empty → runs process()
           └── Has data → skips
```

## FacilityLoad → FacilityOutput Process

### What It Does

Transforms raw CSV data from `load_facility` into cleaned data in `facility_output`:

| Step | Description |
|------|-------------|
| 1. Clear | Deletes all existing `facility_output` records |
| 2. Read | Fetches all `load_facility` records |
| 3. Transform | For each record: clean strings, normalize dates |
| 4. Save | Creates new `facility_output` records |

### Transformations Applied

**String Cleaning (all text fields):**
- Trims whitespace
- Converts empty strings to NULL

**Date Normalization (5 fields):**
- `trackingAttestationStartDate`
- `trackingAttestationExpirationDate`
- `firstOperational`
- `repoweringDate`
- `endDateOfExtendedUse`

| Input Format | Output Format |
|--------------|---------------|
| `1/1/2019` | `2019-01-01` |
| `12/31/2025` | `2025-12-31` |
| `02/01/25` | `2025-02-01` |

## Startup Behavior

### First Deploy (Empty Database)

```
Liquibase: Loads 2,605 records into load_facility
App Ready: facility_output is empty
Action: Runs process()
Result: 2,605 cleaned records in facility_output
```

**Log Output:**
```
onApplicationReady(): facility_output is empty, running process()
process(): Starting FacilityLoad → FacilityOutput transformation
process(): Cleared existing facility_output data
process(): Found 2605 load_facility records to process
process(): Processed 100 records
process(): Processed 200 records
...
process(): Completed. Processed 2605 records
```

### Subsequent Restarts

```
Liquibase: No changes (already applied)
App Ready: facility_output has 2,605 records
Action: Skips processing
Result: No changes
```

**Log Output:**
```
onApplicationReady(): facility_output has 2605 records, skipping process()
```

## Manual Trigger

### REST Endpoint

```
POST /api/facility-loads/process?force=false
```

| Parameter | Default | Description |
|-----------|---------|-------------|
| `force` | `false` | Force reprocessing even if data exists |

### Examples

**Check without reprocessing:**
```bash
curl -X POST http://localhost:8080/api/facility-loads/process
```
Response (if data exists):
```json
{
  "processed": 0,
  "message": "Skipped: facility_output has 2605 records. Use force=true to reprocess."
}
```

**Force reprocessing:**
```bash
curl -X POST "http://localhost:8080/api/facility-loads/process?force=true"
```
Response:
```json
{
  "processed": 2605,
  "message": "Processed 2605 records from facility_load to facility_output."
}
```

## AWS Deployment Considerations

| Scenario | Behavior |
|----------|----------|
| Fresh deploy | Auto-processes (table empty) |
| App restart | Skips (data exists) |
| New CSV data | Use endpoint with `force=true` |
| Scale out (new instance) | Skips (data exists) |

## Key Files

| File | Purpose |
|------|---------|
| `FacilityLoadService.java` | Contains `onApplicationReady()` and `process()` |
| `FacilityLoadController.java` | REST endpoint `/api/facility-loads/process` |
| `FacilityOutputDbService.java` | Database operations for facility_output |
| `103-import-facility-data.yaml` | Liquibase changeset that loads CSV |

## Troubleshooting

### Process didn't run on startup

Check logs for:
```
onApplicationReady(): facility_output has X records, skipping process()
```
This means data already exists. Use the REST endpoint with `force=true` to reprocess.

### Need to reload CSV data

1. Update the CSV file
2. Create new Liquibase changeset (or manually truncate `facility_load`)
3. Truncate `facility_output` table
4. Restart application

Or use the REST endpoint:
```bash
curl -X POST "http://localhost:8080/api/facility-loads/process?force=true"
```
