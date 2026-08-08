# DataFetcher Module

## What It Is

The **datafetcher** module fetches data from external sources and normalizes it into the
core schema. It has **two** sources: the ClinicalTrials.gov v2 REST API (trials) and
UCHealth's Epic FHIR R4 API (patient clinical data). Both follow the
staging-table-then-normalize pattern described in `PROJECT_PLAN.md` (sections 4, 6, 10):
fetch raw payloads into a staging table first, then a separate normalization step upserts
them into the real schema. That seam is where a future source (e.g. a Playwright scraper)
would plug in.

> **Verified 2026-08-08.** Structure below reflects the 20 source files actually present.

## Module Structure

```
datafetcher/
├── src/main/java/com/seibel/cancer/datafetcher/
│   ├── clinicaltrials/
│   │   ├── ClinicalTrialsGovClient.java       REST client for GET /studies (paged)
│   │   └── ClinicalTrialsGovIngestJob.java    fetch -> write StagingRawTrial rows
│   ├── config/
│   │   ├── ClinicalTrialsIngestProperties.java  cancer.ingestion.clinicaltrials.* defaults
│   │   └── DatafetcherConfig.java
│   ├── uchealth/                              Epic FHIR R4 source
│   │   ├── UcHealthOAuthProperties.java       config from .env
│   │   ├── UcHealthOAuthClient.java           authorize URL, code exchange, refresh, ensureValidToken()
│   │   ├── PkceChallengeStore.java            in-memory state->verifier, 15-min TTL, single use
│   │   ├── UcHealthFhirClient.java            Bearer auth, Bundle next-link pagination
│   │   └── UcHealthIngestJob.java             fetch -> write StagingRawFhirResource rows
│   └── normalization/
│       ├── TrialSourceParser.java             interface: raw payload -> NormalizedTrial
│       ├── ClinicalTrialsGovParser.java       implements TrialSourceParser for CT.gov's JSON
│       ├── NormalizedTrial.java               bundles Trial + child records + condition/sponsor names
│       ├── TrialNormalizationService.java     reads pending staging rows, loops, collects results
│       ├── TrialRowNormalizer.java            normalizes ONE row in its own transaction
│       ├── NormalizationCache.java            per-run cache for TrialSource + condition/sponsor lookups
│       ├── FhirSourceParser.java              interface (deliberately NOT TrialSourceParser)
│       ├── EpicObservationParser.java         Epic Observation -> NormalizedLabResult
│       ├── NormalizedLabResult.java
│       ├── FhirNormalizationService.java
│       └── FhirRowNormalizer.java             per-row transaction, upsert by fhirResourceId
├── src/test/java/...                          6 test classes:
│   ClinicalTrialsGovParserTest, TrialRowNormalizerTest, EpicObservationParserTest,
│   UcHealthFhirClientTest, UcHealthOAuthClientTest, PkceChallengeStoreTest
└── build.gradle
```

Note `FhirSourceParser` is a **separate interface** from `TrialSourceParser` — FHIR
resources are not trials and forcing them through one interface would have distorted both.

## Data Flow

```
POST /api/ingestion/clinicaltrials  (root module, IngestionController)
        │  { condition, term, location, overallStatus, maxStudies }
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

**On-demand only** — no `@Scheduled` job. Three endpoints on `IngestionController` (root
module):

| Endpoint | Source |
| --- | --- |
| `POST /api/ingestion/clinicaltrials` | CT.gov — fetch, stage, normalize |
| `POST /api/ingestion/uchealth/observation` | Epic Observation → `lab_result` |
| `POST /api/ingestion/uchealth/medicationrequest` | Epic MedicationRequest (blocked, see below) |

Request body for the CT.gov call (`RequestClinicalTrialsIngest`): `condition`
(→ `query.cond`), `term` (→ `query.term`), `location` (→ `query.locn`), `overallStatus`
(→ `filter.overallStatus`; send `"ALL"` to clear it), `maxStudies` (min 1, **max 50000**).
Frontend trigger: `frontend/src/pages/Ingestion.tsx` (route `/ingestion`).

Defaults come from `cancer.ingestion.clinicaltrials.*` in `application.yml`
(`ClinicalTrialsIngestProperties`) and are overridable per request:

```yaml
condition: cancer
overall-status: RECRUITING
max-studies: 1000
max-normalize-rows: 5000
```

**`maxStudies` is the cap on a single call, not a client limit.**
`ClinicalTrialsGovClient.searchStudies` pages in batches of 100 via the cursor
`pageToken` until `maxStudies` is reached or CT.gov runs out (`nextPageToken` missing).
`countStudies()` uses `countTotal=true` to size a query before committing to the pull.

**Normalization is bounded independently** by `max-normalize-rows`. This was a real bug:
the controller previously passed `maxStudies` as the normalization limit, but staging
rows accumulate across runs, so the two numbers drift and rows get silently left pending.

⚠️ **Normalization is the bottleneck, not fetching.** Measured on 736 staging rows: fetch
~1.1s (0.4%), staging ~0.7s (0.3%), **normalization ~257s (99.3%)** — about 349ms and ~38
DB round trips per trial. Extrapolated, a full recruiting-only cancer pull (~18,773
trials) is ~110 minutes, which **will not survive an HTTP request timeout**. Batching the
~25 child INSERTs is blocked by `BaseDb`'s `GenerationType.IDENTITY`, which disables
Hibernate JDBC batching outright.

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
  design, per `_archive/clinical-trials/clinical-trials-tables.md`.
- **No rate-limit/backoff logic.** CT.gov has no documented hard limit; the client pages
  conservatively (100/page) but does not back off or retry on failure.
- **Memory on large pulls.** `ClinicalTrialsGovClient` accumulates every study in a
  `List<JsonNode>` before staging any of them, so an 18,773-trial pull holds ~280MB of
  parsed JSON in heap at once. The fix is streaming (stage each page as it arrives), not
  a bigger page size.
- **Ingestion does not update the vector store.** `POST /api/rag/backfill` is a separate,
  deliberate second step — newly ingested trials are not semantically searchable until it
  runs. See `../clinical-trials-rag/RAG_SESSION_STATE.md`.

### Epic / UCHealth specific

- **No refresh token.** Epic granted `patient/*.read fhirUser launch/patient openid` but
  silently dropped `offline_access`. The access token dies in ~1 hour with no way to renew
  it — every run past that needs a fresh interactive browser login. The refresh code path
  exists and is unit-tested but has **never executed against Epic**.
- **MedicationRequest fetch is blocked.** Epic rejects the patient search with *"Combination
  of parameters is not valid for any authorized sub-resource."* The app was registered for
  `MedicationRequest.*(Order Template Medication)` — a formulary catalog, not patient
  prescriptions. Re-registered for `Signed Medication Order` (R4); grant had not propagated.
  The whole `patient_medication` stack sits unused until this clears.
- **`DiagnosticReport` returns 403** despite `.Read/.Search (Results) (R4)` being registered.
  Not investigated — not needed for the current slice.
- **Panel handling is untested against real data.** `lab_result_component` and the parser's
  component logic are exercised only by a hand-built CBC payload; the sandbox patient has one
  lab (an A1C) and no panels.
- **`UcHealthOAuthTokenController` exposes full CRUD over the token table**, including reading
  refresh tokens back over HTTP. Restrict or remove before any deployment.

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

Six test classes, 34 tests, all passing:

- **`ClinicalTrialsGovParserTest`** — fixture-based (`src/test/resources/sample-clinicaltrials-study.json`),
  asserts a captured-shape CT.gov payload parses into the expected `NormalizedTrial` fields.
- **`TrialRowNormalizerTest`** — mocks the `*DbService` collaborators, covers the
  new-trial-vs-existing-trial branch and the delete-and-reinsert-children behavior.
- **`EpicObservationParserTest`** — 11 tests against a real captured Epic payload
  (`src/test/resources/sample-epic-observation.json`).
- **`UcHealthFhirClientTest`**, **`UcHealthOAuthClientTest`**, **`PkceChallengeStoreTest`**.

⚠️ **The CT.gov fixture is misleading about real data.** Measured across 50 real trials:
escaped markdown (`\*`, `\<`) survives ingestion in 23/50; bullet markers are `*` (632
lines) and numbered (163) with **hyphen never used**; 2/50 have no eligibility headers at
all; eligibility text runs to 13,771 chars at the longest. A parser written against the
hand-built fixture alone matches little in production — see
`../clinical-trials-rag/RAG_SESSION_STATE.md`.

No live-network test against either API in the automated suite. Both are verified
manually: CT.gov end-to-end repeatedly (most recently 49/50 normalized), Epic against the
sandbox patient.

## Module Type

**java-library** — plain library module, no main class or embedded server. Components
discovered via Spring scanning when the root app starts (root depends on `datafetcher`
via `implementation project(':datafetcher')`).

## Integration with Other Modules

### Used By
- **Root module** — `IngestionController` (all three `/api/ingestion/*` endpoints) and
  `UcHealthAuthController` (`GET /api/uchealth/authorize`, `GET /api/uchealth/callback`)

### Uses
- **`:common`** — `Trial`, `StagingRawTrial`, `TrialSource`, `Location`, `ArmGroup`,
  `Intervention`, `Outcome`, `OverallOfficial`, `Condition`, `LabResult`,
  `StagingRawFhirResource`, `UcHealthOAuthToken` domain objects
- **`:database`** — `*DbService` classes, called directly (see architectural note above)

## Related Documentation

- `PROJECT_PLAN.md` — sections 4 (CT.gov API), 6 (package layout), 10 (phased roadmap)
- `.claude/CURRENT_STATE.md` — current overall project status, including join-table gaps
- `_archive/clinical-trials/clinical-trials-tables.md` — schema design + CT.gov field-mapping reference

## History

This module previously handled Green-e CRS facility HTML scraping and other
energy-tracking data sources, from this project's earlier life as an energy-data
tracking app (before the pivot to clinical trials). That code and its design docs
(`green-e-loading-design.md`, `crs-facility-sync.md`, `crs-sync-design.md`) have been
removed; `jsoup` remains a dependency, unused today, reserved for a possible future
HTML-scraping source under the same `TrialSourceParser` seam.
