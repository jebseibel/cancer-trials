# ClinicalTrials.gov Ingestion

Specific to the ClinicalTrials.gov v2 API source. `datafetcher-module.md` (same
directory) covers the module's generic architecture (staging → normalize pattern,
`TrialSourceParser` seam, why `datafetcher` bypasses root's Service layer, etc.) — read
that first if you haven't. This doc is the "everything about this one source" reference:
the API itself, exact field mappings, what's been verified, and what's still open.

## Status (verified 2026-08-08)

Pipeline is built, working, and has been run many times end-to-end against the live API —
including multi-page pulls of 1,000+ studies and repeat runs proving dedup. See "What's
verified" below for specifics and the one remaining untested path.

## The API

- Base URL: `https://clinicaltrials.gov/api/v2/`
- No API key required. No documented hard rate limit — `ClinicalTrialsGovClient` pages
  conservatively (100 studies/page) but does no backoff/retry.
- `GET /studies` — search/list endpoint, the only one this pipeline uses today.
  - `query.cond` — condition/disease search term
  - `query.term` — free-text search term
  - `query.locn` — location search term
  - `pageSize` — max 1000 per CT.gov; this client requests 100/page
  - `pageToken` / response's `nextPageToken` — cursor pagination; client loops until
    either the caller's `maxStudies` is reached or `nextPageToken` is absent
  - `filter.overallStatus` — **not currently wired up** by `ClinicalTrialsGovClient`,
    though the field exists in CT.gov's API. Add if you need to restrict ingestion to
    e.g. only RECRUITING studies.
  - `countTotal=true` — **not currently requested**. Adding it would let the response
    report the true total match count, useful for knowing how many studies exist for a
    condition before deciding how many pages to pull.
- `GET /studies/{nctId}` — single-study fetch. Not used; this pipeline only does bulk
  search ingestion.
- Each study JSON has a `protocolSection` (everything this pipeline reads),
  `derivedSection` (unused), `hasResults` / `resultsSection` (unused — trial *results*
  are out of scope, only trial *metadata* is ingested).

## Stable identifier

`nctId` (format `NCT########`) at `protocolSection.identificationModule.nctId`. This is
the dedup key for the `trial` table — `TrialDbService.findByNctId` decides
create-vs-update on every normalize pass. `ClinicalTrialsGovClient.extractNctId(study)`
pulls it straight off the raw JSON node during staging (before normalization even runs),
so a study missing this field is skipped at staging time with an error, never even
reaching the staging table.

## Field mapping (raw JSON → core schema)

Full authoritative mapping table lives in
`_archive/clinical-trials/clinical-trials-tables.md` ("Source field mapping reference"
section) — this is a pointer, not a duplicate, so it doesn't drift out of sync. Key
points specific to how `ClinicalTrialsGovParser` implements that mapping:

- **Eligibility criteria is free text.** One string field under
  `protocolSection.eligibilityModule.eligibilityCriteria`, formatted with
  "Inclusion Criteria:" / "Exclusion Criteria:" headers and bullet items. Stored as-is
  in `trial.eligibility_criteria`. This pipeline does **not** parse it into the
  `EligibilityRule` tree — that's a manual step by design (see
  `_archive/clinical-trials/clinical-trials-tables.md`).
- Only `minimumAge`, `maximumAge` (free text like `"18 Years"`), `sex`,
  `healthyVolunteers`, and `stdAges[]` are structured eligibility fields; everything
  else in that module is narrative.
- **Conditions** — `protocolSection.conditionsModule.conditions[]`, a flat list of
  strings. Parsed into `NormalizedTrial.getConditionNames()`, then dedup-upserted into
  the `condition` lookup table by name (`ConditionDbService.findByName`/`create`).
- **Sponsors** — `protocolSection.sponsorCollaboratorsModule.leadSponsor` (single) plus
  `.collaborators[]` (list), each with a name and `orgClass`. Parsed into
  `NormalizedTrial.getSponsors()` (as `NormalizedSponsor` records), dedup-upserted into
  the `sponsor` lookup table by name (`SponsorDbService.findByName`/`create`).
- **Locations, arm groups, interventions, outcomes, overall officials** — each mapped to
  their own domain object inside `NormalizedTrial` and inserted as `Trial` children.
  See `ClinicalTrialsGovParser` for the exact JSON paths per field; not duplicated here
  to avoid drift as CT.gov's schema or the parser evolves.

## What's verified

Verified repeatedly against the live API, most recently 2026-08-08 (49 of 50 normalized;
the one failure was a `LocationDb` insert hitting the `decimal(9,6)` latitude overflow,
not a parser problem):

- Fetch → stage → normalize round-trip, including the `pageToken` loop on multi-page pulls.
- **Re-running the same search dedups correctly** — trials upsert by `nctId`, children are
  delete-and-reinserted, staging rows skip or refresh in place.
- Trials and their Location/ArmGroup/Intervention/Outcome/OverallOfficial children render
  in the frontend's Trial Detail page.
- A 1,000-trial pull completed in ~82 seconds; a 736-row normalization run was measured in
  detail (see the performance note in `datafetcher-module.md`).

**Still not verified:** failure handling on a genuinely malformed payload in a live pull.
The fixture-based `ClinicalTrialsGovParserTest` uses one captured-shape sample, and real
CT.gov data varies far more than it suggests.

## Resolved constraints

Recorded because the reasoning still matters, not because action is needed:

1. ~~**`maxStudies` capped at 500.**~~ **Fixed** — `@Max` is now 50000. The cap was never a
   client limitation; the paging loop already followed `nextPageToken` correctly.
2. ~~**Staging never dedups.**~~ **Fixed** — `staging_raw_trial` has a composite unique
   constraint on `(trial_source_id, source_trial_id)` (`uq_staging_raw_trial_source` in
   `010-staging-raw-trial.yaml`). `ClinicalTrialsGovIngestJob.run()` checks
   `findByTrialSourceIdAndSourceTrialId` before staging: a pending duplicate is skipped
   (counted as `stagingRowsSkipped`); an already-normalized one is refreshed in place via
   `refreshForRenormalization`, which clears `normalizedAt`/`normalizationError` so the
   next `normalizePending` pass reprocesses it.
3. ~~**No `filter.overallStatus` support.**~~ **Fixed** — the client had not been sending
   the parameter at all. Now supported and defaulting to `RECRUITING`, which matters: a
   corpus of mostly COMPLETED/TERMINATED trials is the wrong corpus for a discovery tool.
4. ~~**No way to size a query first.**~~ **Fixed** — `countStudies()` uses `countTotal=true`.
   Live counts as of 2026-08-07: cancer+RECRUITING 18,773; cancer any status 122,393;
   breast cancer+RECRUITING 2,456.

**Still open:** one synchronous request does fetch+stage+normalize together. At the measured
normalization rate a full 18,773-trial pull is ~110 minutes and **will not survive an HTTP
timeout** — an async job endpoint becomes necessary at that scale, not merely nice to have.

## Related documentation

- `datafetcher-module.md` (same directory) — module architecture, data flow, dedup
  behaviour, performance measurements, and the Epic/FHIR source
- `.claude/CURRENT_STATE.md` — overall project status, including the join-table gaps
  that limit what's linked once trials are normalized
- `_archive/clinical-trials/clinical-trials-tables.md` — schema design and the authoritative
  field-mapping table
