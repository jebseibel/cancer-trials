# Current State — Clinical Trials Finder

Snapshot of where the project stands, what's deliberately left unfinished, and what
that blocks. Companion to `PROJECT_PLAN.md` (overall plan) and
`_archive/clinical-trials/clinical-trials-tables.md` (schema design) — this doc is the "where are we right now"
view, meant to be updated as things change rather than kept as history.

## What's built

**Backend** — full layered scaffold (domain → entity → repository → db/service →
service → controller) for all core entities from `_archive/clinical-trials/clinical-trials-tables.md`: Trial,
TrialSource, StagingRawTrial, Sponsor, Condition, Medication, Location, ArmGroup,
Intervention, Outcome, OverallOfficial, EligibilityRule, Keyword, AppUser, TrialStatus.
Standard CRUD + pagination on each, following the project's REST API template.

Every entity that references a Trial (Location, ArmGroup, Intervention, Outcome,
OverallOfficial, TrialStatus) now also exposes a `GET /api/{entity}/by-trial/{trialExtid}`
(or `/by-appuser/{appUserExtid}` for TrialStatus) endpoint, so the frontend can fetch
"everything for this one trial" without a numeric id ever crossing the API boundary.

**Frontend** — old jobhunting-template pages (Customers/Purchases/Users) removed.
Three real pages now exist:
- **Trial Search** (`/trials`) — filters trials already in the DB by title/NCT
  number/summary text and overall status.
- **Trial Detail** (`/trials/:extid`) — full normalized record (summary, eligibility
  text, interventions, arm groups, outcomes, locations, contacts) plus a personal
  tracking control (status dropdown + notes) for the logged-in user.
- **Saved Trials** (`/saved-trials`) — the logged-in user's tracked trials, filterable
  by status (SAVED/INTERESTED/CONTACTED/RULED_OUT/ENROLLED).

Login still uses the original `User`/JWT auth (unchanged). Since `User` (login) and
`AppUser` (personal tracking) are separate tables with no FK between them, the frontend
matches them **by username** — a `useCurrentAppUser` hook fetches `/api/appuser` and
finds the row whose username matches the logged-in user. This requires one `AppUser` row
seeded per login account with a matching username; there's no UI for creating that link
yet, it has to be seeded directly.

## Hard rule adopted this session: extid only, everywhere

The app deliberately never exposes internal numeric ids to the frontend, and the
frontend never sends one back — every cross-entity reference on the wire is an `extid`
(UUID string), including what would naturally be a numeric foreign key (e.g. a
Location's trial, or a TrialStatus's trial and app user). Domain objects and JPA
entities still use numeric ids/FKs internally; the translation to/from extid happens in
each controller's converter, which resolves `trialExtid` → internal `Long trialId` via
`TrialRepository` before building the domain object, and the reverse when building a
response. Everything touched this session (Location, ArmGroup, Intervention, Outcome,
OverallOfficial, TrialStatus) follows this. Anything scaffolded *after* this session
should follow the same pattern from the start.

## What's deliberately left off — join tables / foreign keys

The many-to-many join tables designed in `_archive/clinical-trials/clinical-trials-tables.md` were never
scaffolded at all — no domain, entity, repository, service, or controller layer exists
for any of them yet:

- `trial_condition` — which conditions a trial studies
- `trial_sponsor` — which sponsors/collaborators back a trial, with role (LEAD/COLLABORATOR)
- `trial_phase` — which phase(s) a trial is in (a trial can have more than one)
- `trial_std_age` — standardized age groups (CHILD/ADULT/OLDER_ADULT)
- `trial_keyword` — free-text keywords

This is intentional, not an oversight — these are being added later. Until they exist:

- **Trial Search cannot filter by condition, sponsor, or phase.** The current search
  only filters on fields that live directly on the `trial` row itself (title, NCT
  number, summary text, overall status).
- **Trial Detail cannot show which conditions/sponsors/phases apply to a trial.** Those
  sections simply aren't in the page yet.
- The `Condition` and `Sponsor` entities/endpoints exist (full CRUD, needed as the
  lookup tables the join rows will eventually point at) but nothing links them to a
  `Trial` yet.

**When these land**, expect: new domain/entity/repository/service/controller layers per
join table (thin — these are pure link tables, no `BaseDb` fields per
`_archive/clinical-trials/clinical-trials-tables.md`'s conventions section), a Trial Detail section for each,
and Trial Search filter controls for condition/sponsor/phase. Follow the extid-only rule
above for any new endpoint that exposes a trial/condition/sponsor reference.

## ClinicalTrials.gov ingestion — built, not yet manually verified

Full ingestion pipeline built this session per `_archive/datafetcher/INGESTION_PLAN.md` (read that
doc for the detailed design/decisions). Summary:

- **`datafetcher` module** now has real content and is wired into the root app
  (`implementation project(':datafetcher')` added to root `build.gradle`):
  - `ClinicalTrialsGovClient` — pages through CT.gov's `GET /studies` v2 API.
  - `ClinicalTrialsGovIngestJob` — fetches studies, writes `StagingRawTrial` rows.
  - `TrialSourceParser` interface + `ClinicalTrialsGovParser` — parses a staging row's
    raw JSON into a `NormalizedTrial` (Trial + child records + condition/sponsor names),
    per the field-mapping table in `_archive/clinical-trials/clinical-trials-tables.md`.
  - `TrialNormalizationService` + `TrialRowNormalizer` — reads pending staging rows,
    upserts `Trial` by `nctId`, delete-and-reinserts child records
    (Location/ArmGroup/Intervention/Outcome/OverallOfficial) on re-normalization,
    dedup-upserts `Condition`/`Sponsor` by name, marks each staging row
    normalized-or-errored. Each row normalizes in its own transaction
    (`TrialRowNormalizer`, a separate bean from `TrialNormalizationService` — needed
    because a self-invoked `@Transactional` call is silently ignored by Spring's proxy).
- **New endpoint:** `POST /api/ingestion/clinicaltrials` (root module,
  `IngestionController`) — takes `{condition, term, location, maxStudies}` in the
  request body, runs fetch → stage → normalize synchronously, returns a summary. No
  scheduled/automatic ingestion — on-demand only, per your call. No frontend trigger
  button yet either — that's a later task; for now it's called directly (Swagger/curl).
- **Design correction mid-build:** `TrialService`/`ConditionService`/`SponsorService`
  (root module) got `upsertByNctId`/`findOrCreateByName`-style additions early in the
  build, but `datafetcher` can't actually call them — `datafetcher` depends on
  `:database`, and root depends on `datafetcher`, so `datafetcher` calling into root's
  `service` package would be circular. Resolution: `TrialNormalizationService`/
  `TrialRowNormalizer` call the `*DbService` classes in `:database` directly, bypassing
  root's Service layer entirely for ingestion. The root Service-layer additions are
  harmless but currently unused by ingestion — they just sit there as normal
  REST-facing service methods.
- **Tests:** `ClinicalTrialsGovParserTest` (fixture-based, a captured-shape sample
  payload at `datafetcher/src/test/resources/sample-clinicaltrials-study.json`) and
  `TrialRowNormalizerTest` (mocked `*DbService` collaborators, covers new-trial vs.
  existing-trial-with-children-refreshed paths) — both passing. `./gradlew build`
  succeeds project-wide.
- **Not yet done:** manual end-to-end verification against the live ClinicalTrials.gov
  API (start the backend, call the endpoint with a real condition, confirm rows land
  correctly and re-running dedupes by nctId) — this needs you to start the backend
  yourself per project convention, hasn't happened yet this session.
- **Not committed.** Everything above (`datafetcher/src/**`, `IngestionController` +
  its DTOs, the `*DbService` additions, `build.gradle`/`datafetcher/build.gradle`
  changes, the `100-load-init-data.yaml` seed row, `_archive/datafetcher/INGESTION_PLAN.md`) is
  sitting as uncommitted changes in the working tree.

## UCHealth / Epic FHIR ingestion — working end to end against Epic's sandbox

Design and build plan live in `epic-integration/UCHEALTH_INGESTION_PLAN.md`; the schema is in
`epic-integration/epic-tables.md`. This section is the "where did we stop" view.

**Verified working against Epic's real sandbox** (test patient Camila Lopez):
OAuth authorize → MyChart login → consent → callback → token stored → authenticated
FHIR R4 call → payload staged verbatim → re-run dedups (0 written, 1 skipped).

**What's built:**

- **OAuth (public client + PKCE, no client secret).** `UcHealthOAuthProperties` (config
  from `.env`), `PkceChallengeStore` (in-memory state→verifier, 15-min TTL, single use),
  `UcHealthOAuthClient` (authorize URL, code exchange, refresh, `ensureValidToken()`),
  and `UcHealthAuthController` (`GET /api/uchealth/authorize`, `GET /api/uchealth/callback`).
- **Fetch + stage.** `UcHealthFhirClient` (Bearer auth, Bundle next-link pagination,
  filters entries by `resourceType`), `UcHealthIngestJob` (dedup-before-insert from day
  one, matching the CT.gov pattern).
- **Normalization.** `FhirSourceParser` (interface, deliberately *not* `TrialSourceParser`),
  `EpicObservationParser`, `NormalizedLabResult`, `FhirRowNormalizer` (per-row
  transaction, upsert by `fhirResourceId`, delete-and-reinsert components),
  `FhirNormalizationService`.
- **Endpoint.** `POST /api/ingestion/uchealth/observation` — fetch → stage → normalize
  in one call. **Never yet run against a live backend** (added after the last successful
  pull, which was staging-only).
- **Entities scaffolded + tested:** `UcHealthOAuthToken`, `StagingRawFhirResource`,
  `PatientMedication`, `LabResult`, `LabResultComponent` (changesets `018`–`022`).
- **Tests:** 34 in `datafetcher` (11 for the parser, against a real captured payload at
  `datafetcher/src/test/resources/sample-epic-observation.json`), 67 for the
  LabResult/LabResultComponent DB layers, 34 for PatientMedication. All passing.

**Pick up here tomorrow:**

1. Start the backend, **re-authorize** (`http://localhost:8080/api/uchealth/authorize`,
   logging in as Epic's sandbox test patient — Camila Lopez; credentials are in Epic's
   sandbox docs, not recorded here), then
   `POST /api/ingestion/uchealth/observation` and confirm a row actually lands in
   `lab_result`. This is the one untested link in the chain.
2. Retry `POST /api/ingestion/uchealth/medicationrequest` — the Epic grant may have
   propagated overnight (see blocker below).

**Blockers / open issues, worst first:**

- **No refresh token.** Epic granted `patient/*.read fhirUser launch/patient openid` but
  silently dropped `offline_access`. The access token dies in ~1 hour and there is no way
  to renew it — every ingestion run past that needs a fresh interactive browser login.
  The refresh code path exists and is unit-tested but has **never executed against Epic**.
  Fine for sandbox; defeats the purpose against a real record. Resolve before the
  production authorization.
- **MedicationRequest fetch is blocked.** Epic rejects the patient search with
  *"Combination of parameters is not valid for any authorized sub-resource."* The app was
  registered for `MedicationRequest.*(Order Template Medication)` — a formulary catalog,
  not patient prescriptions. Re-registered for **`Signed Medication Order` (R4)**; the
  grant hadn't propagated by end of session. The whole `patient_medication` stack
  (changeset `020`, scaffolded + tested) sits unused until this clears.
- **`DiagnosticReport` returns 403** despite `.Read/.Search (Results) (R4)` being
  registered. Not investigated — not needed for the current slice.
- **`MedicationStatement` is unavailable in R4.** Epic's catalog offers it only in
  DSTU2/STU3. Dropped from scope; `MedicationRequest` covers prescriptions.
- **`spring.liquibase.drop-first` was turned off** (`application.yml`) so the OAuth token
  survives a restart — it was being wiped on every boot. Trade-off: edits to an
  *already-applied* changeset no longer take effect on startup; rebuild the DB (n8n
  `clear-db` webhook) for those. New changesets still apply normally.
- **Panel handling is untested against real data.** `lab_result_component` and the
  parser's component logic are exercised only by a hand-built CBC payload — Camila's
  sandbox record has exactly one lab (an A1C) and no panels.
- **`UcHealthOAuthTokenController` exposes full CRUD over the token table**, including
  reading refresh tokens back over HTTP. Harmless for sandbox; remove or restrict before
  real credentials.
- **Nothing is committed.** All 88 changed/untracked files are sitting in the working
  tree, including everything above.

## What's deliberately left off — join tables / foreign keys

The many-to-many join tables designed in `_archive/clinical-trials/clinical-trials-tables.md` were never
scaffolded at all — no domain, entity, repository, service, or controller layer exists
for any of them yet:

- `trial_condition` — which conditions a trial studies
- `trial_sponsor` — which sponsors/collaborators back a trial, with role (LEAD/COLLABORATOR)
- `trial_phase` — which phase(s) a trial is in (a trial can have more than one)
- `trial_std_age` — standardized age groups (CHILD/ADULT/OLDER_ADULT)
- `trial_keyword` — free-text keywords

This is intentional, not an oversight — these are being added later. Until they exist:

- **Trial Search cannot filter by condition, sponsor, or phase.** The current search
  only filters on fields that live directly on the `trial` row itself (title, NCT
  number, summary text, overall status).
- **Trial Detail cannot show which conditions/sponsors/phases apply to a trial.** Those
  sections simply aren't in the page yet.
- The `Condition` and `Sponsor` entities/endpoints exist (full CRUD, needed as the
  lookup tables the join rows will eventually point at), and the new ingestion pipeline
  now populates them (dedup by name) — but nothing links them to a `Trial` yet.

**When these land**, expect: new domain/entity/repository/service/controller layers per
join table (thin — these are pure link tables, no `BaseDb` fields per
`_archive/clinical-trials/clinical-trials-tables.md`'s conventions section), a Trial Detail section for each,
Trial Search filter controls for condition/sponsor/phase, and the ingestion normalizer
updated to link parsed conditions/sponsors/phases to the trial instead of just
upserting the lookup rows. Follow the extid-only rule above for any new endpoint that
exposes a trial/condition/sponsor reference.

## Other known gaps

- **EligibilityRule is not extid-converted.** Its `parentRuleId` (self-referencing tree)
  and `criterionId` (polymorphic — points at either `Condition` or `Medication`
  depending on `criterionType`) make the extid conversion more involved than the other
  child entities, and the table's own design is still marked "open questions" in
  `_archive/clinical-trials/clinical-trials-tables.md`. Its `/by-trial/` endpoint was deliberately *not* added
  this session — skip it until the rule-tree design is settled, then convert it
  alongside adding that endpoint. The ingestion pipeline also does not populate
  `EligibilityRule` — it only writes `trial.eligibility_criteria` as raw narrative text,
  per `_archive/clinical-trials/clinical-trials-tables.md`'s note that rule population is manual.
- **No AppUser-seeding UI.** Creating an `AppUser` row (and making its username match a
  login `User`) is a manual step, not something the app does for you.
- **Pre-existing test failure**, unrelated to this session's work:
  `BasicApplicationTests.contextLoads()` fails with a Liquibase changelog parse error
  (`Error parsing db/changelog/changes/005-trial.yaml : Unexpected node: 2`). Confirmed
  via `git log` that this file wasn't touched this session — flagged, not fixed.

## Suggested next steps (not yet decided/scheduled)

1. Manually verify the ingestion endpoint end to end (start backend, call
   `POST /api/ingestion/clinicaltrials` with a real condition, confirm DB rows and
   re-run dedup behavior) — see checklist in `_archive/datafetcher/INGESTION_PLAN.md`. Then commit
   the ingestion work.
2. Add a frontend button to trigger ingestion — **done**: `frontend/src/pages/Ingestion.tsx`
   (route `/ingestion`, "Ingest" nav link) calls `POST /api/ingestion/clinicaltrials` and
   shows result counts/errors.
3. Design and scaffold the join tables (`trial_condition`, `trial_sponsor`,
   `trial_phase`, `trial_std_age`, `trial_keyword`), extid-only from the start, and
   update the normalizer to link parsed data through them.
4. Add condition/sponsor/phase filters to Trial Search and display sections to Trial
   Detail once the above lands.
5. Decide how `EligibilityRule`'s extid conversion should work given the polymorphic
   `criterionId`, then add its `/by-trial/` endpoint and a Trial Detail section for it.
6. ~~Fix the `005-trial.yaml` Liquibase parse error~~ — **done**: unquoted
   `decimal(x,y)` column types break Liquibase's YAML flow-mapping parser (the comma
   reads as a map separator). Fixed in `005-trial.yaml` (`paid_amount`) and
   `015-location.yaml` (`latitude`/`longitude`) by quoting the type string, e.g.
   `type: "decimal(10,2)"`. Watch for this pattern in any future decimal/numeric column.
7. **TODO — re-enable endpoint security before deployment.** All Spring Security
   authorization was temporarily disabled in `SecurityConfig.java`
   (`authorizeHttpRequests` now `.anyRequest().permitAll()`) to simplify local dev.
   The original JWT-protected rule set is preserved commented-out directly below it in
   the same file — restore that block (and remove the permit-all override) before any
   deployment or any exposure beyond localhost. JWT filter/login/register endpoints
   themselves were not removed, only the enforcement that requires a token on `/api/**`.
