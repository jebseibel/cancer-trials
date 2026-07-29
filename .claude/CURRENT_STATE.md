# Current State — Clinical Trials Finder

Snapshot of where the project stands, what's deliberately left unfinished, and what
that blocks. Companion to `PROJECT_PLAN.md` (overall plan) and
`clinical-trials-tables.md` (schema design) — this doc is the "where are we right now"
view, meant to be updated as things change rather than kept as history.

## What's built

**Backend** — full layered scaffold (domain → entity → repository → db/service →
service → controller) for all core entities from `clinical-trials-tables.md`: Trial,
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

The many-to-many join tables designed in `clinical-trials-tables.md` were never
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
`clinical-trials-tables.md`'s conventions section), a Trial Detail section for each,
and Trial Search filter controls for condition/sponsor/phase. Follow the extid-only rule
above for any new endpoint that exposes a trial/condition/sponsor reference.

## Other known gaps

- **EligibilityRule is not extid-converted.** Its `parentRuleId` (self-referencing tree)
  and `criterionId` (polymorphic — points at either `Condition` or `Medication`
  depending on `criterionType`) make the extid conversion more involved than the other
  child entities, and the table's own design is still marked "open questions" in
  `clinical-trials-tables.md`. Its `/by-trial/` endpoint was deliberately *not* added
  this session — skip it until the rule-tree design is settled, then convert it
  alongside adding that endpoint.
- **No ClinicalTrials.gov ingestion job yet.** `ClinicalTrialsGovClient`,
  `ClinicalTrialsGovIngestJob`, `TrialNormalizationService`, `ClinicalTrialsGovParser`
  (see `PROJECT_PLAN.md` section 6/10) don't exist yet. Right now, any `Trial` rows in
  the database have to be created directly through the REST API (e.g. via Swagger) —
  there's no automated pull from ClinicalTrials.gov.
- **No AppUser-seeding UI.** Creating an `AppUser` row (and making its username match a
  login `User`) is a manual step, not something the app does for you.
- **Pre-existing test failure**, unrelated to this session's work:
  `BasicApplicationTests.contextLoads()` fails with a Liquibase changelog parse error
  (`Error parsing db/changelog/changes/005-trial.yaml : Unexpected node: 2`). Confirmed
  via `git log` that this file wasn't touched this session — flagged, not fixed.

## Suggested next steps (not yet decided/scheduled)

1. Design and scaffold the join tables (`trial_condition`, `trial_sponsor`,
   `trial_phase`, `trial_std_age`, `trial_keyword`), extid-only from the start.
2. Add condition/sponsor/phase filters to Trial Search and display sections to Trial
   Detail once the above lands.
3. Build the ClinicalTrials.gov ingestion + normalization pipeline (Phase 1 items 3–4
   in `PROJECT_PLAN.md`) so trials stop needing manual entry.
4. Decide how `EligibilityRule`'s extid conversion should work given the polymorphic
   `criterionId`, then add its `/by-trial/` endpoint and a Trial Detail section for it.
5. Fix (or decide to leave) the `005-trial.yaml` Liquibase parse error — currently
   blocks the one Spring context-load test from passing.
