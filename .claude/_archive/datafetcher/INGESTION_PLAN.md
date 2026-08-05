# ClinicalTrials.gov Ingestion — Implementation Plan

Design for the `datafetcher` module's first real feature: pulling trial data from the
ClinicalTrials.gov v2 API and normalizing it into the core schema. Companion to
`../../PROJECT_PLAN.md` (section 4, 6, 10) and `_archive/database/clinical-trials-tables.md` (schema +
field-mapping reference). This doc is the concrete build plan; execute it top to bottom.

## Decisions made this session

- **Trigger:** on-demand only, via a REST endpoint. No `@Scheduled` job yet — matches
  `../../PROJECT_PLAN.md` section 11's recommendation ("trial data doesn't change fast"). The
  frontend button that calls this endpoint is a later task — for now, "a call triggers
  it" (e.g. via Swagger or curl) is sufficient.
- **Search parameters:** supplied per-call in the trigger endpoint's request body
  (condition, location, etc.), not hardcoded in `application.yml`. Avoids baking any
  personal medical detail into a config file that might get committed.
- **Sync execution:** one endpoint call does fetch → stage → normalize, synchronously,
  and returns a summary (counts, errors). No separate "stage now, normalize later" step
  for this pass — simplest thing that works, matches personal-scale data volumes.
- **`TrialSourceParser` seam:** build the interface now (not deferred), with
  `ClinicalTrialsGovParser` as the sole implementation, per `../../PROJECT_PLAN.md` section 6's
  explicit design intent — this is exactly the seam Phase 2 scrapers need.
- **Dedup key:** `Trial.nctId`. Add `findByNctId` + an upsert path to `TrialService`
  (repository method `TrialRepository.findByNctId` already exists, just needs wiring
  through the service/dbservice layers — same shape as the `trialId` filter work from
  last session).
- **Re-normalization of children:** delete-and-reinsert, not diff-and-merge. When a
  trial that already exists gets re-normalized, soft-delete all its existing
  Location/ArmGroup/Intervention/Outcome/OverallOfficial rows (via each entity's
  existing `findByTrialId` + `delete(extid)`) and insert fresh rows from the new
  payload. Simple, avoids complex diffing — a "clean slate" refresh rather than a diff.
- **Extid-only rule applies.** Everything built here follows the session's standing
  rule ([[feedback_extid_only]] in memory / `../../CURRENT_STATE.md`) — no internal numeric
  ids cross the API boundary. Internally, the normalizer works with numeric ids freely
  (it's not the wire boundary), but the trigger endpoint's request/response DTOs must
  not expose any.
- **Correction found mid-build: dependency direction.** `TrialService`,
  `StagingRawTrialService`, `TrialSourceService`, `ConditionService`, `SponsorService`
  all live in the **root module**'s `service` package. `datafetcher` depends on
  `:common`/`:database` only, and root depends on `datafetcher` — so `datafetcher`
  calling into root's `service` package would be circular. Resolution: `datafetcher`
  works directly against the `*DbService` classes in `:database` (already a dependency),
  bypassing root's Service layer entirely for ingestion/normalization. This means the
  `upsertByNctId`/`findOrCreateByName` business logic originally added to
  `TrialService`/`ConditionService`/`SponsorService` (steps below) is **not reachable
  from `datafetcher`** — those methods stay as harmless, unused-by-ingestion additions
  to root's REST-facing services (fine to leave, may be useful later), and the
  equivalent upsert/dedup logic is re-implemented inside `TrialNormalizationService`
  itself, calling `TrialDbService.findByNctId`/`create`/`update`,
  `ConditionDbService.findByName`/`create`, `SponsorDbService.findByName`/`create`
  directly.

## Explicitly out of scope for this pass

- **EligibilityRule population.** Per `_archive/database/clinical-trials-tables.md`, eligibility rules are
  populated *manually* by reading the free-text criteria — the normalizer only writes
  `trial.eligibility_criteria` as raw narrative text. No parsing into the rule tree.
- **Condition/Sponsor linkage.** The join tables (`trial_condition`, `trial_sponsor`,
  `trial_phase`, `trial_std_age`, `trial_keyword`) don't exist yet (see
  `../../CURRENT_STATE.md`). The normalizer should still populate the standalone `Condition`
  and `Sponsor` lookup tables (dedup by name) so that data isn't lost, but it cannot
  link them to a trial yet — add that linkage when the join tables land.
- **Frontend trigger button.** Only the backend endpoint is being built now.
- **Scheduled/automatic re-ingestion.** On-demand only.
- **Rate limiting / backoff tuning.** CT.gov has no documented hard limit; use a
  reasonable page size and don't hammer it, but no sophisticated backoff logic this
  pass.

## Module wiring

1. Add `implementation project(':datafetcher')` to root `../../../build.gradle` (currently
   missing — `datafetcher` exists in `../../../settings.gradle` and depends on `:common`/
   `:database`, but nothing depends on *it* yet).
2. `datafetcher`'s `../../../build.gradle` already has what's needed (Spring Boot starters,
   `:common`, `:database`) — no dependency changes expected. `jsoup` stays unused for
   this pass (it's for future HTML-scraping sources, not the CT.gov JSON API).

## Package layout (mirrors `../../PROJECT_PLAN.md` section 6)

```
datafetcher/src/main/java/com/seibel/cancer/datafetcher/
├── clinicaltrials/
│   ├── ClinicalTrialsGovClient.java       REST client for GET /studies (paged)
│   └── ClinicalTrialsGovIngestJob.java    orchestrates fetch → write staging rows
└── normalization/
    ├── TrialSourceParser.java             interface: staging row JSON → normalized writes
    ├── ClinicalTrialsGovParser.java        implements TrialSourceParser for CT.gov's JSON shape
    └── TrialNormalizationService.java      reads pending staging rows, dispatches by
                                             trial_source.code, upserts core tables

src/main/java/com/seibel/cancer/web/
└── controller/
    └── IngestionController.java            POST /api/ingestion/clinicaltrials
└── request/
    └── RequestClinicalTrialsIngest.java     condition, location, term, maxStudies, etc.
└── response/
    └── ResponseIngestionResult.java         counts fetched/staged/normalized/errors, error list
```

## Step-by-step build order

### 1. `TrialService` upsert-by-nctId support
- `TrialService.findByNctId(String nctId): Optional<Trial>` — thin passthrough, mirrors
  existing `findByExtid` pattern (add at DbService then Service layer).
- `TrialService.upsertByNctId(Trial incoming): Trial` — if `findByNctId` finds an
  existing row, update it in place (extid preserved); else create a new one. Business
  logic lives at the Service layer, following existing layering.

### 2. `TrialSourceParser` interface (`datafetcher/normalization/`)
```
interface TrialSourceParser {
    boolean supports(String trialSourceCode);
    NormalizedTrial parse(String rawPayloadJson);
}
```
`NormalizedTrial` is a plain result object bundling the `Trial` domain object plus its
child records (locations, arm groups, interventions, outcomes, overall officials,
conditions, sponsors) parsed from one payload — the shape `TrialNormalizationService`
needs to persist in one pass. Exact field breakdown mirrors the "Source field mapping
reference" table in `_archive/database/clinical-trials-tables.md`.

### 3. `ClinicalTrialsGovClient` (`datafetcher/clinicaltrials/`)
- Confirmed: no HTTP client dependency exists in `datafetcher`/root yet. Spring's
  `RestClient` (`org.springframework.web.client.RestClient`) ships as part of
  `spring-web`, already pulled in transitively by `spring-boot-starter-web` (which
  `datafetcher` already depends on) — no new Gradle dependency needed.
- Wraps `GET https://clinicaltrials.gov/api/v2/studies` using `RestClient`.
- Params: `query.cond`, `query.term`, `query.locn`, `filter.overallStatus`, `pageSize`
  (cap at e.g. 100 per page for this pass), `pageToken` for cursor pagination,
  `countTotal=true`.
- Pages through results up to a caller-supplied max (avoid unbounded pulls on a whim).
- Returns raw JSON per study (as `String` or parsed `JsonNode`) — the staging table
  wants the raw payload preserved as-is.

### 4. `ClinicalTrialsGovIngestJob` (`datafetcher/clinicaltrials/`)
- Takes search params + max studies, calls `ClinicalTrialsGovClient`, and for each study
  writes a `StagingRawTrial` row via `StagingRawTrialDbService.create(...)` directly
  (`:database` dependency, not root's `StagingRawTrialService` — see dependency-direction
  correction above). `trialSourceId` resolved from
  `TrialSourceDbService`/`TrialSourceRepository.findByCode("CLINICALTRIALS_GOV")`.
  `sourceTrialId` = the study's `nctId`. `normalizedAt` left null (pending).
- Confirmed: `100-load-init-data.yaml` only seeds old Customer/User/Purchase template
  data — no `trial_source` rows exist. Add a seed insert for
  `CLINICALTRIALS_GOV` / "ClinicalTrials.gov" / `https://clinicaltrials.gov/api/v2/`
  to `100-load-init-data.yaml` (direct edit is fine per this project's Liquibase
  conventions — not in production, no new changeset needed).
- Returns a simple result: studies fetched, staging rows written, any per-study errors.

### 5. `ClinicalTrialsGovParser implements TrialSourceParser` (`datafetcher/normalization/`)
- Parses one `StagingRawTrial.rawPayload` JSON string into a `NormalizedTrial` per the
  field-mapping table in `_archive/database/clinical-trials-tables.md`.
- Also extracts `Condition` names (`conditionsModule.conditions[]`) and `Sponsor`
  names+orgClass (`sponsorCollaboratorsModule.leadSponsor`/`.collaborators[]`) as
  separate lists on `NormalizedTrial`, even though nothing links them to the trial yet
  (see "out of scope" above) — so `TrialNormalizationService` can dedup-and-upsert them
  into the lookup tables without losing that data.

### 6. `TrialNormalizationService` (`datafetcher/normalization/`)
- Lives in `datafetcher`, so it calls `*DbService` classes from `:database` directly —
  `TrialDbService`, `LocationDbService`, `ArmGroupDbService`, `InterventionDbService`,
  `OutcomeDbService`, `OverallOfficialDbService`, `ConditionDbService`, `SponsorDbService`,
  `StagingRawTrialDbService` — not root's Service layer (see dependency-direction
  correction above). `StagingRawTrialDbService.findPending(maxRows)` and
  `TrialDbService.findByNctId(nctId)` already exist (added this session at the DbService
  layer directly — verify they're reachable without going through root's Service
  wrappers). `StagingRawTrialDbService.update(extid, trialSourceId, sourceTrialId,
  rawPayload, fetchedAt, normalizedAt, normalizationError)` (explicit-params overload)
  marks a row normalized/failed directly.
- `normalizePending(int maxRows)`: reads pending staging rows, dispatches each to the
  matching `TrialSourceParser` by `trial_source.code` (resolve the row's
  `trialSourceId` → `TrialSourceDbService.findByExtid`/a `findById`-style lookup → code),
  and per row:
  1. Parse payload → `NormalizedTrial`.
  2. Upsert-by-nctId inline: `TrialDbService.findByNctId(nctId)` — if found, call
     `TrialDbService.update(existing.getExtid(), normalized.trial)`; else
     `TrialDbService.create(normalized.trial)`. Get back the persisted `Trial` (internal
     id + extid).
  3. If the trial already existed: for each child type (Location, ArmGroup,
     Intervention, Outcome, OverallOfficial), call the DbService's `findByTrialId(trial
     .getId())` and `delete(extid)` on each existing row.
  4. Insert fresh child rows from `NormalizedTrial` via each DbService's `create(...)`,
     all pointing at the resolved trial's internal id.
  5. Dedup-upsert `Condition`/`Sponsor` by name: `ConditionDbService.findByName(name)` /
     `SponsorDbService.findByName(name)` (both added this session) — if null, call
     `create(name)` / `create(name, orgClass)`.
  6. Mark the staging row `normalizedAt = now()` on success, or write
     `normalizationError` and leave `normalizedAt` null on failure (so it's retried
     next run, per the schema's own comment in `_archive/database/clinical-trials-tables.md`).
- Wrap each row's processing in its own try/catch so one bad payload doesn't abort the
  batch.

### 7. `IngestionController` (root module, `web/controller/`)
- `POST /api/ingestion/clinicaltrials`
- Request body (`RequestClinicalTrialsIngest`): `condition` (maps to `query.cond`),
  `term` (optional, `query.term`), `location` (optional, `query.locn`), `maxStudies`
  (cap, e.g. default 50).
- Calls `ClinicalTrialsGovIngestJob.run(...)` then `TrialNormalizationService
  .normalizePending(...)` synchronously, combines both results into
  `ResponseIngestionResult` (studiesFetched, stagingRowsWritten, trialsNormalized,
  errors: List<String>).
- No auth changes needed — existing JWT-protected `/api/**` covers it.

### 8. Tests
- `ClinicalTrialsGovParserTest` — parse a captured sample CT.gov JSON payload (save a
  fixture file), assert `NormalizedTrial` fields match the mapping table.
- `TrialNormalizationServiceTest` — mock `TrialService`/child services, verify
  create-vs-update branching and delete-and-reinsert behavior for children.
- Skip a live-network test against the real CT.gov API in the automated suite; a manual
  end-to-end run (per `../../PROJECT_PLAN.md` phase 1 step 8) covers that.

## Verification checklist before calling this done

- [ ] `./gradlew build` succeeds with `:datafetcher` wired into root.
- [ ] Manual call to `POST /api/ingestion/clinicaltrials` with a real condition/term
      against the live CT.gov API returns a sane summary and actually populates `trial`,
      `location`, `arm_group`, `intervention`, `outcome`, `overall_official`,
      `condition`, `sponsor` rows.
- [ ] Re-running the same call updates the same trials (by nctId) rather than
      duplicating them, and children are refreshed (old ones soft-deleted, not left as
      orphaned duplicates).
- [ ] `staging_raw_trial.normalization_error` is populated (and `normalized_at` stays
      null) for any row that fails to parse, without aborting the rest of the batch.
- [ ] Trial Detail page in the frontend renders a trial ingested this way correctly
      (spot check against what's already built).
