# DataFetcher Module

## What It Is

The **datafetcher** module fetches clinical trial data from external sources and
normalizes it into the core schema. Its first (and currently only) source is the
ClinicalTrials.gov v2 REST API. It follows the staging-table-then-normalize pattern
described in `PROJECT_PLAN.md` (sections 4, 6, 10): fetch raw payloads into a staging
table first, then a separate normalization step upserts them into the real schema. This
is the seam a future non-CT.gov source (e.g. a Playwright scraper) would plug into.

## Module Structure

```
datafetcher/
├── src/main/java/com/seibel/cancer/datafetcher/
│   ├── clinicaltrials/
│   │   ├── ClinicalTrialsGovClient.java       REST client for GET /studies (paged)
│   │   └── ClinicalTrialsGovIngestJob.java    fetch -> write StagingRawTrial rows
│   └── normalization/
│       ├── TrialSourceParser.java             interface: raw payload -> NormalizedTrial
│       ├── ClinicalTrialsGovParser.java        implements TrialSourceParser for CT.gov's JSON
│       ├── NormalizedTrial.java                bundles Trial + child records + condition/sponsor names
│       ├── TrialNormalizationService.java      reads pending staging rows, loops, collects results
│       └── TrialRowNormalizer.java             normalizes ONE row in its own transaction
├── src/test/java/com/seibel/cancer/datafetcher/normalization/
│   ├── ClinicalTrialsGovParserTest.java        fixture-based parser test
│   └── TrialRowNormalizerTest.java             mocked-DbService normalizer test
└── build.gradle
```

## Data Flow

```
POST /api/ingestion/clinicaltrials  (root module, IngestionController)
        │  { condition, term, location, maxStudies }
        ▼
ClinicalTrialsGovIngestJob.run(...)
        │  ClinicalTrialsGovClient pages GET /studies (pageSize 100, cursor pageToken)
        │  until maxStudies reached or CT.gov has no more results
        ▼
StagingRawTrialDbService.create(...)   — one row per study, raw JSON preserved verbatim,
                                          normalizedAt left null (pending)
        ▼
TrialNormalizationService.normalizePending(maxRows)
        │  StagingRawTrialDbService.findPending(maxRows)
        ▼
TrialRowNormalizer.normalize(staging)   — per row, own @Transactional
        │  1. Resolve TrialSource by staging.trialSourceId, pick matching TrialSourceParser
        │  2. ClinicalTrialsGovParser.parse(rawPayload) -> NormalizedTrial
        │  3. Upsert Trial by nctId (TrialDbService.findByNctId -> update or create)
        │  4. If trial already existed: delete-and-reinsert all children
        │     (Location, ArmGroup, Intervention, Outcome, OverallOfficial)
        │  5. Insert fresh children from NormalizedTrial
        │  6. Dedup-upsert Condition / Sponsor by name (lookup tables only, not yet
        │     linked to the trial — join tables don't exist yet, see CURRENT_STATE.md)
        │  7. Mark staging row normalizedAt = now(), or leave null + write
        │     normalizationError on failure (so it's retried on the next run)
        ▼
IngestionController combines both results into ResponseIngestionResult
(studiesFetched, stagingRowsWritten, pendingRowsProcessed, trialsNormalized, errors)
```

`TrialNormalizationService` just loops and collects results; `TrialRowNormalizer` (package-private)
does the actual per-row work inside `@Transactional`. They're split into two classes because a
self-invoked `@Transactional` method (i.e. one method on the same bean calling another) bypasses
Spring's transactional proxy entirely and would be silently non-transactional — splitting into a
separate bean forces the proxy to apply.

## Trigger

**On-demand only**, via `POST /api/ingestion/clinicaltrials` (root module,
`IngestionController`) — no `@Scheduled` job. Request body
(`RequestClinicalTrialsIngest`): `condition` (→ `query.cond`), `term` (→ `query.term`),
`location` (→ `query.locn`), `maxStudies` (default 50, min 1, max 500). Frontend trigger:
`frontend/src/pages/Ingestion.tsx` (route `/ingestion`) calls this endpoint and shows the
result counts/errors.

**`maxStudies` is the actual cap on a single call** — it is not a hard limit in the
client. `ClinicalTrialsGovClient.searchStudies` pages in batches of 100 (`PAGE_SIZE`)
via the cursor-based `pageToken`, and keeps paging until either `maxStudies` is reached
or CT.gov runs out of results (`nextPageToken` missing). A default-50 pull only returns
50 because that's what was requested — raising `maxStudies` (up to the DTO's 500 max)
pulls more in one call; pulling more than 500 in one shot means either raising that
`@Max` or calling the endpoint multiple times.

`normalizePending`'s `maxRows` is currently passed the same `maxStudies` value from the
controller — it processes at most that many *pending* staging rows per call, which is
usually fine since ingest and normalize run back-to-back synchronously in the same
request, but if staging ever gets ahead of normalization (e.g. a call fails partway),
pending rows beyond that count wait for the next call.

## Dedup / Re-ingestion Behavior

- **Trials** dedup by `Trial.nctId` — re-running the same search updates existing trials
  in place (extid preserved) rather than duplicating them.
- **Children** (Location, ArmGroup, Intervention, Outcome, OverallOfficial) use
  delete-and-reinsert, not diff-and-merge: on re-normalization of an existing trial, all
  existing child rows for that trial are deleted and replaced fresh from the new
  payload. Simple, avoids diffing, but means any manual edits to a child row would be
  lost on re-ingestion (none are exposed for manual edit today).
- **Condition / Sponsor** dedup by name (`findByName` then create-if-absent) — these are
  lookup tables only; nothing links them to a trial yet (see "Known gaps" below).
- **Staging rows** also dedup, by `(trial_source_id, source_trial_id)` — enforced with a
  DB-level unique constraint (`uq_staging_raw_trial_source` in
  `010-staging-raw-trial.yaml`). `ClinicalTrialsGovIngestJob.run()` checks for an
  existing row before staging each study: a pending (unnormalized) duplicate is skipped;
  an already-normalized one is refreshed in place (new payload, `normalizedAt`/
  `normalizationError` cleared so it's re-processed) instead of inserted as a new row.

## Known Gaps

- **Condition/Sponsor linkage.** The join tables (`trial_condition`, `trial_sponsor`,
  `trial_phase`, `trial_std_age`, `trial_keyword`) don't exist yet. The normalizer
  populates the standalone `Condition`/`Sponsor` lookup tables so the data isn't lost,
  but nothing associates them with the trial they came from. See `CURRENT_STATE.md`.
- **EligibilityRule is not populated.** Only raw narrative text lands in
  `trial.eligibility_criteria`; parsing into the structured rule tree is manual, by
  design, per `_archive/database/clinical-trials-tables.md`.
- **No rate-limit/backoff logic.** CT.gov has no documented hard limit; the client pages
  conservatively (100/page) but does not back off or retry on failure.
- **Not yet pulling the full available result set.** Verified working end-to-end for a
  50-study pull; CT.gov has thousands of matching studies for real conditions. Next step
  is deciding how to pull the full set (raise `maxStudies` per call vs. multiple calls
  vs. raising the `@Max(500)` cap) — see Suggested next steps in `CURRENT_STATE.md`.

## Dependencies

### Internal Modules
```gradle
implementation project(':common')   // Trial, StagingRawTrial, TrialSource, etc. domain objects
implementation project(':database') // *DbService classes, called directly (see below)
```

### External Libraries
```gradle
implementation 'org.springframework.boot:spring-boot-starter'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'org.springframework.boot:spring-boot-starter-web'   // RestClient for the CT.gov HTTP calls
implementation 'org.jsoup:jsoup:1.18.1'                              // unused by CT.gov ingestion; reserved for a future HTML-scraping source
implementation 'org.apache.commons:commons-lang3:3.17.0'
```

## Architectural Note: Why `datafetcher` Bypasses Root's Service Layer

`datafetcher` depends on `:common`/`:database` only; the root module depends on
`datafetcher`. Root's `TrialService`/`ConditionService`/`SponsorService` (in
`com.seibel.cancer.service`) live in the root module, so `datafetcher` calling into them
would be a circular dependency. Resolution: `TrialNormalizationService`/
`TrialRowNormalizer` call `:database`'s `*DbService` classes
(`TrialDbService`, `ConditionDbService`, `SponsorDbService`, etc.) directly, bypassing
root's REST-facing Service layer entirely for ingestion. Root's Service-layer
upsert/dedup methods added alongside this feature are harmless but unused by ingestion —
they only serve the REST API's own CRUD endpoints.

## Testing

```bash
./gradlew :datafetcher:test
./gradlew :datafetcher:test --tests "*ClinicalTrialsGovParserTest"
./gradlew :datafetcher:test --tests "*TrialRowNormalizerTest"
```

- **`ClinicalTrialsGovParserTest`** — fixture-based (`src/test/resources/sample-clinicaltrials-study.json`),
  asserts a captured-shape CT.gov payload parses into the expected `NormalizedTrial` fields.
- **`TrialRowNormalizerTest`** — mocks the `*DbService` collaborators, covers the
  new-trial-vs-existing-trial branch and the delete-and-reinsert-children behavior.
- No live-network test against the real CT.gov API in the automated suite — verified
  manually instead (a 50-study pull has been run successfully; see Known Gaps above for
  what's still outstanding at larger volumes).

## Module Type

**java-library** — plain library module, no main class or embedded server. Components
discovered via Spring scanning when the root app starts (root depends on `datafetcher`
via `implementation project(':datafetcher')`).

## Integration with Other Modules

### Used By
- **Root module** — `IngestionController` (`POST /api/ingestion/clinicaltrials`)

### Uses
- **`:common`** — `Trial`, `StagingRawTrial`, `TrialSource`, `Location`, `ArmGroup`,
  `Intervention`, `Outcome`, `OverallOfficial`, `Condition` domain objects
- **`:database`** — `*DbService` classes, called directly (see architectural note above)

## Related Documentation

- `PROJECT_PLAN.md` — sections 4 (CT.gov API), 6 (package layout), 10 (phased roadmap)
- `INGESTION_PLAN.md` — the original build plan for this feature, with the
  verification checklist
- `.claude/CURRENT_STATE.md` — current overall project status, including join-table gaps
- `_archive/database/clinical-trials-tables.md` — schema design + CT.gov field-mapping reference

## History

This module previously handled Green-e CRS facility HTML scraping and other
energy-tracking data sources, from this project's earlier life as an energy-data
tracking app (before the pivot to clinical trials). That code and its design docs
(`green-e-loading-design.md`, `crs-facility-sync.md`, `crs-sync-design.md`) have been
removed; `jsoup` remains a dependency, unused today, reserved for a possible future
HTML-scraping source under the same `TrialSourceParser` seam.
