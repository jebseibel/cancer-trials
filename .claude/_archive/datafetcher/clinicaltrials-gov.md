# ClinicalTrials.gov Ingestion

Specific to the ClinicalTrials.gov v2 API source. `datafetcher-module.md` (same
directory) covers the module's generic architecture (staging → normalize pattern,
`TrialSourceParser` seam, why `datafetcher` bypasses root's Service layer, etc.) — read
that first if you haven't. This doc is the "everything about this one source" reference:
the API itself, exact field mappings, what's been verified, and what's still open.

## Status as of this writing

Pipeline is built and has been run successfully end-to-end: a call to
`POST /api/ingestion/clinicaltrials` with `maxStudies=50` fetched real studies from the
live API, staged them, normalized them into `trial` + child tables, and they render
correctly in the frontend's Trial Detail page. That's the only verified run so far — see
"Open work" below for what a full-volume pull still needs.

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
`_archive/database/clinical-trials-tables.md` ("Source field mapping reference"
section) — this is a pointer, not a duplicate, so it doesn't drift out of sync. Key
points specific to how `ClinicalTrialsGovParser` implements that mapping:

- **Eligibility criteria is free text.** One string field under
  `protocolSection.eligibilityModule.eligibilityCriteria`, formatted with
  "Inclusion Criteria:" / "Exclusion Criteria:" headers and bullet items. Stored as-is
  in `trial.eligibility_criteria`. This pipeline does **not** parse it into the
  `EligibilityRule` tree — that's a manual step by design (see
  `_archive/database/clinical-trials-tables.md`).
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

## What's verified vs. not

**Verified** (one successful run, `maxStudies=50`):
- Fetch → stage → normalize round-trip works against the live API.
- Trials, and their Location/ArmGroup/Intervention/Outcome/OverallOfficial children,
  land correctly and render in the frontend's Trial Detail page.

**Not yet verified:**
- **Re-running the same search dedups correctly.** The nctId-based upsert path and the
  delete-and-reinsert-children logic are covered by `TrialRowNormalizerTest` with mocks,
  but not yet confirmed against a second live run over already-ingested studies.
- **Full-volume pulls.** Only a 50-study pull has been run; CT.gov has thousands of
  studies for real conditions. Nothing has tested paging through more than one page (50
  < the 100/page batch size, so the single live run so far never actually exercised the
  `pageToken` loop).
- **Failure handling on a malformed/unexpected payload shape** in a live pull (only
  covered by the fixture-based `ClinicalTrialsGovParserTest`, which uses one
  captured-shape sample, not the full variety of real CT.gov studies).

## Open work — pulling the full result set

The current constraints, in order of what you'll hit first when trying to pull
"thousands" instead of 50:

1. **`maxStudies` is capped at 500** by validation on `RequestClinicalTrialsIngest`
   (`@Max(value = 500)`). Raise this cap, or call the endpoint multiple times, to get
   beyond it.
2. ~~**Staging never dedups.**~~ — **fixed.** `staging_raw_trial` now has a composite
   unique constraint on `(trial_source_id, source_trial_id)`
   (`010-staging-raw-trial.yaml`, `uq_staging_raw_trial_source`).
   `ClinicalTrialsGovIngestJob.run()` checks
   `StagingRawTrialDbService.findByTrialSourceIdAndSourceTrialId` before staging each
   study: if a pending (unnormalized) row already exists for that `nctId`, it's skipped
   (counted in the new `stagingRowsSkipped` / `IngestResult.stagingRowsSkipped()`); if an
   already-normalized row exists, it's refreshed in place
   (`StagingRawTrialDbService.refreshForRenormalization` — updates the payload/
   `fetchedAt` and clears `normalizedAt`/`normalizationError` so it's picked up by the
   next `normalizePending` pass) rather than inserted as a new duplicate row.
3. **One synchronous request does fetch+stage+normalize together.** For thousands of
   studies, a single HTTP request doing all of that synchronously (per
   `IngestionController.ingestClinicalTrials`) may be slow or time out. Options if this
   becomes a problem: raise the HTTP timeout, split fetch/stage from normalize into two
   calls (`normalizePending` already accepts a `maxRows` independent of the ingest call),
   or make ingestion asynchronous. Not designed yet — decide when it's actually hit.
4. **No `filter.overallStatus` support.** If the goal is "thousands of *relevant*
   trials" rather than literally all trials ever registered for a condition (including
   long-completed/withdrawn ones), consider wiring up `filter.overallStatus` to narrow
   the pull rather than just raising `maxStudies`.
5. **Consider `countTotal=true`.** Requesting this would tell you up front how many
   studies actually match a given condition/term/location combination, which is useful
   for deciding realistic `maxStudies` values before firing off a large pull.

## Related documentation

- `datafetcher-module.md` (same directory) — generic module architecture
- `INGESTION_PLAN.md` — original build plan and design decisions for this
  feature
- `.claude/CURRENT_STATE.md` — overall project status, including the join-table gaps
  that limit what's linked once trials are normalized
- `_archive/database/clinical-trials-tables.md` — schema design and the authoritative
  field-mapping table
