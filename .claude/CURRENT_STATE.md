# Current State — Clinical Trials Finder

Where the project stands, what is deliberately unfinished, and what that blocks. Companion to
`PROJECT_PLAN.md` (the overall plan) and `_archive/clinical-trials/clinical-trials-tables.md`
(schema design). This is the "where are we right now" view — update it as things change rather
than keeping it as history.

**Last verified against the code: 2026-08-08.**

---

## Do this first when picking the project back up

The working tree is clean and everything is committed, but the running environment is not ready:

1. **Start the backend** (you do this — never the assistant).
2. **Ingest trials.** The database was rebuilt, so `trial` is empty:
   `POST /api/ingestion/clinicaltrials` with `{"condition": "breast cancer", "maxStudies": 50}`.
3. **Backfill the vector store.** Qdrant is at **0 points** — semantic search returns nothing
   until this runs: `curl -X POST http://localhost:8080/api/rag/backfill` (~15s for 50 trials).
   Confirm Qdrant is up first: `docker compose up -d`.
4. **Seed an `AppUser` row** whose username matches your login. Without it, Trial Detail
   tracking, Saved Trials, and the Diagnosis page all show "no app-user profile linked".
   There is still no UI for this.

Then `bash rag/src/test/resources/eval/run-evaluation.sh` should be back to 8/8.

---

## What's built

### Backend

Full layered scaffold (domain → entity → repository → db service → service → controller) for
every core entity: Trial, TrialSource, StagingRawTrial, Sponsor, Condition, Medication,
Location, ArmGroup, Intervention, Outcome, OverallOfficial, EligibilityRule, Keyword, AppUser,
TrialStatus, PatientDiagnosis, plus the Epic/FHIR tables (UcHealthOAuthToken,
StagingRawFhirResource, PatientMedication, LabResult, LabResultComponent). Standard CRUD with
pagination on each.

Every entity referencing a Trial also exposes `GET /api/{entity}/by-trial/{trialExtid}`
(or `/by-appuser/{appUserExtid}` for TrialStatus and PatientDiagnosis).

### Frontend

Seven pages: Login, Dashboard, Trial Search, Trial Detail, Saved Trials, **Diagnosis**,
Ingestion. Structure and gotchas in `_archive/frontend/frontend-module.md`.

### CT.gov ingestion — verified working

Fetch → stage → normalize, on demand via `POST /api/ingestion/clinicaltrials`. Run repeatedly
against the live API including multi-page pulls of 1,000+ studies; re-runs dedup correctly by
`nctId`. Defaults live in `cancer.ingestion.clinicaltrials.*` and default to RECRUITING only.

### UCHealth / Epic FHIR — working against Epic's sandbox

OAuth (PKCE, no client secret) → MyChart login → callback → token stored → authenticated FHIR
call → payload staged → re-run dedups. Observation normalizes into `lab_result`.

### RAG — steps 1–7 of `RAG_PLAN.md` complete

Qdrant in Docker, local ONNX embeddings (all-MiniLM-L6-v2, 384 dims — clinical text never
leaves the machine), criterion-level chunking, indexing, backfill, retrieval, and an evaluation
set that passed 8/8 on a 1,289-chunk corpus. Generation (§9) is deliberately deferred.

### Diagnosis matching — Tier 1 of 3

`PatientDiagnosis` (21 fields) plus deterministic age/sex/recruiting checks surfaced on Trial
Detail and Trial Search. Tier 2 (retrieval-driven) and Tier 3 (rule tree) are not built.

---

## Hard rules this project follows

**extid only, everywhere.** No internal numeric id is ever exposed to the frontend, and the
frontend never sends one back — every cross-entity reference on the wire is an `extid`,
including what would naturally be a numeric foreign key. Domain objects and JPA entities use
numeric ids internally; each controller's converter translates at the boundary.

**No eligibility verdicts.** Per `_archive/diagnosis/DIAGNOSIS_MATCHING_DESIGN.md` §5: no fit
score or percentage, no auto-exclusion of trials, and "unknown" is a first-class result. A
failed check renders amber and never removes a trial from a list — a parsing failure must not
silently take away an option. The tool surfaces what to look into and ask about; the judgement
stays with the patient, family, and oncology team.

**Ingestion and indexing are two separate steps.** `POST /api/ingestion/clinicaltrials` writes
MySQL; `POST /api/rag/backfill` makes it searchable. Deliberate — the two have very different
costs (staging 1,000 trials is quick; embedding them is ~26,000 local inferences) and keeping
them apart keeps that visible.

**Module dependency direction.** `:datafetcher` and `:rag` depend on `:common`/`:database`, and
root depends on them — so neither can call root's `service` package without creating a cycle.
Both call `*DbService` classes directly. This has been hit twice; when a lower module must
trigger something upward, use a Spring event with the type declared in `:database`.

---

## Blockers and known gaps, worst first

### Security — must be resolved before any deployment

- **All endpoint security is disabled.** `SecurityConfig` line 55 is
  `.anyRequest().permitAll()`. The original JWT rule set is preserved commented-out directly
  below it. On restore, `/api/uchealth/callback` must stay `permitAll` — Epic's OAuth redirect
  cannot carry a JWT.
- **The JWT signing secret is a hardcoded literal in the repo.** There is no `jwt:` block in
  `application.yml`, so `JwtUtil`'s inline default is live. Anyone with repo access can forge a
  token. Move it to `JWT_SECRET` in `.env`.
- **`UcHealthOAuthTokenController` exposes full CRUD over the token table**, including reading
  refresh tokens back over HTTP.

Full checklist in `_archive/hosting/qa-setup.md`.

### Epic / UCHealth

- **No refresh token.** Epic granted `patient/*.read fhirUser launch/patient openid` but
  silently dropped `offline_access`. The access token dies in ~1 hour with no way to renew it —
  every run past that needs a fresh interactive browser login. The refresh code path exists and
  is unit-tested but has never executed against Epic. Resolve before production authorization.
- **MedicationRequest fetch is blocked.** Epic rejects the patient search: *"Combination of
  parameters is not valid for any authorized sub-resource."* The app was registered for
  `MedicationRequest.*(Order Template Medication)` — a formulary catalog, not patient
  prescriptions. Re-registered for `Signed Medication Order` (R4); grant had not propagated.
  The whole `patient_medication` stack sits unused until it clears.
- **`DiagnosticReport` returns 403** despite the scope being registered. Not investigated.
- **Panel handling is untested against real data** — `lab_result_component` is exercised only
  by a hand-built CBC payload. The sandbox patient has one lab (an A1C) and no panels.

### Schema

- **The join tables were never scaffolded**: `trial_condition`, `trial_sponsor`, `trial_phase`,
  `trial_std_age`, `trial_keyword`, and `intervention_arm_group`. Deliberate, but it means
  **Trial Search cannot filter by condition, sponsor, or phase**, Trial Detail cannot show them,
  and they cannot be RAG retrieval filters. The normalizer populates the `Condition`/`Sponsor`
  lookup tables but links nothing to a trial. Adding them requires a full re-backfill, since
  filters live in chunk metadata.
- **`EligibilityRule` is scaffolded but never populated.** Ingestion writes only
  `trial.eligibility_criteria` as raw narrative text. Its extid conversion is also unresolved —
  `parentRuleId` is self-referencing and `criterionId` is polymorphic.
- **The tables doc has drifted from the schema** (found 2026-08-08, not yet fixed): the doc says
  `condition`, but the table is `medical_condition` (reserved word); `intervention_arm_group` is
  missing from the doc's own gap list; and two changesets share the number `014`
  (`014-eligibility-rule.yaml` and `014-outcome.yaml`).
- **`spring.liquibase.drop-first` is off** so the Epic OAuth token survives a restart.
  Consequence: **edits to an already-applied changeset do not take effect on startup** — rebuild
  the DB via the n8n `clear-db` webhook. New changesets still apply normally.

### Performance

**Normalization is the ingestion bottleneck** — measured on 736 rows: fetch ~1.1s (0.4%),
staging ~0.7s (0.3%), normalization ~257s (**99.3%**). About 349ms and ~38 DB round trips per
trial. A full recruiting-only cancer pull (~18,773 trials) is **~110 minutes and will not
survive an HTTP request timeout**; an async job endpoint becomes necessary at that scale.

Batching the ~25 child INSERTs per trial is **blocked by `BaseDb`'s
`GenerationType.IDENTITY`**, which disables Hibernate JDBC batching entirely. Two ways forward,
both needing your decision: change the id generation strategy (schema-wide, affects every
entity) or write native bulk-insert queries (bypasses `BaseDb`'s extid/timestamp handling).
Cheap and unblocked meanwhile: bulk `deleteByTrialId` instead of find-then-delete-per-child.

Also: `ClinicalTrialsGovClient` accumulates every study in a `List<JsonNode>` before staging any
of them, so an 18,773-trial pull holds ~280MB of parsed JSON in heap. The fix is streaming per
page, not a bigger page size.

### Other

- **No AppUser-seeding UI.** `User` (login) and `AppUser` (tracking) are separate tables with no
  FK, matched by username. Creating the row is manual.
- **No enum endpoint.** Frontend vocabularies are hardcoded `as const` arrays in
  `types/api.ts`; adding a backend enum value will not surface in the UI on its own.
- **`BasicApplicationTests.contextLoads()`** — status unconfirmed. It previously failed on a
  Liquibase parse error (unquoted `decimal(x,y)`) that has since been fixed; re-check.
- **Retrieval is weak on conceptual queries.** Measured: clinical terminology scores 0.66–0.99,
  but "BRCA mutation" scores 0.388 and colloquial phrasing 0.526. Partly a corpus gap rather
  than a model limit — try a larger recruiting-only corpus before considering a model upgrade.
- **Six of eight files in `.claude/agents/` lack YAML frontmatter** and therefore cannot be
  invoked as subagents. They work only as prompts referenced by path.

---

## Candidate next steps

Nothing here is decided.

1. **Re-run the evaluation against a recruiting-only corpus.** Ingestion now defaults to
   RECRUITING, but that has not been exercised end-to-end followed by a backfill and eval run.
   That tells you whether the weak queries were a corpus problem — do this before considering a
   bigger embedding model.
2. **Tier 2 matching** — use diagnosis fields to build retrieval queries against indexed
   criteria, reusing the existing `isExclusion` chunk metadata so a high-scoring exclusion match
   is shown as a concern rather than a fit. `DIAGNOSIS_MATCHING_DESIGN.md` §4.
3. **The after-commit event hook** so new ingestions index themselves — `datafetcher` publishes
   a Spring event (type declared in `:database`), `:rag` consumes it after commit. Avoids the
   `datafetcher` → `:rag` cycle and keeps a Qdrant outage from rolling back ingested data.
   `RAG_PLAN.md` §3 and §6 settle the design.
4. **The join tables**, extid-only from the start, then condition/sponsor/phase filters on Trial
   Search and sections on Trial Detail.
5. **Generation** (`RAG_PLAN.md` §9) — the grounded "why might this fit" answer with citations.
   Deferred until retrieval was proven; it now is. The chunk-per-criterion strategy is what
   makes line-level citation possible.
6. **Restore endpoint security** before this goes anywhere but localhost.

---

## Git state

Branch `uchealth-fhir-ingestion`, working tree clean. Seven commits on top of the FHIR
baseline (`4de3ef1`):

| Commit | What |
| --- | --- |
| `a95729b` | `.claude` docs cleanup — removed inherited ViroTrade/jobhunting content, corrected the rest |
| `41355a5` | `:rag` module — chunking, embedding, Qdrant indexing, retrieval |
| `0b1a94e` | CT.gov ingestion made configurable; `filter.overallStatus`; normalize-limit bug fixed |
| `43b72ff` | `LocationRepositoryTest` latitude truncation fix |
| `db7347c` | `PatientDiagnosis` entity, full stack, 34 tests |
| `c621dce` | Backfill button + `JobResultModal` on the Ingest page |
| `0f55855` | Diagnosis page + Tier 1 eligibility matching |

Test counts: `:database` 688, `:datafetcher` 34, `:rag` 25 (1 skipped — needs a running
backend). All passing. Frontend typecheck and build clean; one pre-existing lint error in
`Login.tsx`.

Not merged to `main`; this work is still on the branch.
