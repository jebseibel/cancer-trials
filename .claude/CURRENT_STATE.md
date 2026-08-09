# Current State — Clinical Trials Finder

Where the project stands, what is deliberately unfinished, and what that blocks. Companion to
`PROJECT_PLAN.md` (the overall plan) and `_archive/clinical-trials/clinical-trials-tables.md`
(schema design). This is the "where are we right now" view — update it as things change rather
than keeping it as history.

**Last verified against the code: 2026-08-09.**

---

## Picking the project back up

Verified 2026-08-09 at end of session, immediately after a database rebuild:

| | |
| --- | --- |
| Trials in MySQL | **0** — the rebuild dropped 2,676. Re-ingest before anything works. |
| Qdrant | **stale/empty** — must be cleared and re-backfilled, chunks reference dead extids |
| AppUser | 1 row, `jeb`, extid `03c6958f-6a1f-46bf-96a7-330cf1bbe9ca` — **auto-seeded** |
| PatientDiagnosis | 1 row, real data, extid `441591db-fddf-423a-8d74-845d1f2e89e4` — **auto-seeded** |
| PatientVariant | 1 row, extid `a588da6c-2dd1-479c-a140-a1c279a02cdc` — **auto-seeded** |
| PatientPriorTreatment | 1 row, extid `86f4de7d-f5e3-4864-8e6d-53e878d98129` — **auto-seeded** |
| SavedTrialMatch | 0 — the 15 rows from 2026-08-08 were lost in the rebuild |
| Tests | `:database` **818**, root 3 — all passing |
| Working tree | large; being committed at end of this session |

**To resume, in order:**

1. `POST /api/ingestion/clinicaltrials` with
   `{"condition":"breast cancer","overallStatus":"RECRUITING","maxStudies":2500}` — a few
   minutes, and the new progress ticker shows both loops live.
2. Clear the Qdrant collection, then `POST /api/rag/backfill`. The collection must be
   **deleted**, not reused: its chunks reference trial extids that no longer exist.
3. Log in as `jeb`. The patient data is already there — see below.

### The patient rows now seed themselves

**This is new and it is the fix for the worst repeat-pain of a rebuild.** `PatientSeedLoader`
(root, `service/`) is a `CommandLineRunner` that recreates `AppUser`, `PatientDiagnosis`,
`PatientVariant` and `PatientPriorTreatment` on startup from gitignored CSVs in
`.claude/patient-data/`. **Verified working through a real rebuild on 2026-08-09** — all four
rows came back automatically with correct values.

Four rules it follows, each deliberate:

- **Seed if absent, never sync.** An existing row is left completely alone, so edits made
  through the UI survive a restart. The CSV is a floor, not a source of truth.
- **Keyed on username, not extid** — extids regenerate on every rebuild.
- **A missing file or directory is not an error**; the app boots fine without patient data.
- **A malformed seed never blocks startup.**

Settings live under `cancer.seed.patient.*` in `application.yml`; set `enabled: false` to skip.

⚠️ **The CSVs are the source of truth now.** If you edit patient data through the UI, the
change lives only in the database and the next rebuild reverts it to whatever the CSV says.
Update the CSV too, or accept the loss.

⚠️ **Vocabulary drift is the failure mode to watch.** The diagnosis CSV originally held
`AJCC`, `POSTMENOPAUSAL` and `HR+/HER2-` where the frontend expects `AJCC_8`, `POST` and
`HR_POSITIVE_HER2_NEGATIVE`. The backend stores plain varchars so it accepted them, but the
dropdowns rendered blank and a save would have silently cleared the fields. Fixed 2026-08-09;
check any new CSV value against `frontend/src/types/api.ts`.

### New clinical data — 2026-08-09, from the real record

A **UCHealth My Health Summary PDF** (generated 2026-08-08, 35,808 lines of extracted text)
was read and mapped into all three patient tables. It is at
`.claude/patient-data/my-health-summary.pdf`, gitignored, `600`.

⚠️ **It arrived in `.claude/diagnosis/`, which is NOT gitignored, world-readable, and one
`git add .` from being committed.** Moved on arrival. This is the second time a real medical
record has landed in a tracked directory — the PET/CT report did the same on 2026-08-08.
**Any new patient document goes straight to `.claude/patient-data/` before it is opened.**

What the record established, beyond what was already recorded:

- **Germline testing: a multi-gene panel, negative for pathogenic variants.**
  BRCA1, BRCA2, PALB2, ATM and CHEK2 are therefore `NOT_DETECTED`, not blank. This is the
  distinction the five-state vocabulary exists for: it moves PARP-inhibitor trials from
  *open question* to *genuine mismatch*.
- **Precise receptor values**, at the level of detail a pathology report carries.
  HER2 IHC low-positive, DISH not amplified.
- **Histology and stage**: invasive ductal carcinoma, with a full AJCC staging code.
- **ECOG 0 → 1** per the recent oncology note.
- **Palliative radiation** to named sites, left sacroiliac
  joint, and right proximal femur. Not previously recorded.
- **A bone-modifying agent, started several months into treatment.**
- **Corrected drug start dates** — two different dates for the same drug (an earlier
  note in the same record says 3/31 — unresolved).
- **A follow-up scan showing mixed response.** Some sites resolved,
  osseous lesions mixed, some larger and more avid. Progressive asymptomatic right femoral
  neck metastasis, fracture risk.
- **A documented decision to continue the current regimen rather than pivot to a PI3K
  inhibitor.** So `pi3kAktMtorStatus` is `NEVER` *despite* the PIK3CA mutation — she is
  PIK3CA-mutant and PI3K-inhibitor-naive, which is an inclusion criterion for a whole class
  of trials.
- **Confirmed: no cytotoxic chemotherapy, ever.** The neoadjuvant AC-then-taxane regimen was
  planned early on and abandoned when staging found metastatic disease. Every "chemo"
  mention in the record is supportive care or that abandoned plan.

**Two unresolved conflicts, recorded in the notes fields rather than silently resolved:**

- **A proliferation index (Ki-67) disagreed by several-fold between two notes in the oncology and
  radiation notes.** A 4.5× discrepancy on a proliferation index is not rounding. The user
  chose 45%; both values are in the notes. Worth asking the oncology team.
- **A drug start date** appears as two different dates in the same record.

### Where the session stopped

Everything above is in the database and committed. The next step is re-ingesting the corpus
(step 1 under "To resume") — it was dropped by the rebuild that proved the seed loader works.

**The tool was used for a real person for the first time (2026-08-08).** See
"First real search" below — it worked, and it surfaced a concrete design gap that reorders
the candidate list.

### First real search — 2026-08-08

The patient's real diagnosis is now in `patient_diagnosis`: de novo **stage IV invasive
carcinoma of the left breast**, **ER+ / PR− / HER2−**, **PIK3CA mutation detected**,
postmenopausal, ECOG 0, bone and extensive nodal metastases, on **abemaciclib (Verzenio) +
letrozole** since April 2026 with **no prior cytotoxic chemotherapy**. Sourced from an MRI
plus details supplied directly by the user.

A semantic search over all 249 trials using that profile returned real, relevant matches —
most notably **NCT05753657**, which matched at 0.717 on *"ER positive HER2 negative
metastatic breast cancer, harboring an activating PIK3CA mutation"*. That is the patient's profile
line for line, including the biomarker.

**But the top-scoring hit was wrong, and the reason matters.** NCT06685796 scored highest
(0.718) on the criterion *"HR-**negative**, HER2-negative"* — triple-negative disease. She
is HR-**positive**. Two more of the top ten (NCT07045311 triple-negative, NCT06770296
HER2-**positive**) got in the same way.

**Embedding similarity cannot distinguish receptor polarity.** "HR-negative HER2-negative"
and "HR-positive HER2-negative" differ by one token inside an otherwise identical phrase,
and the model scores them as near-identical. This is a known weakness of embedding models
around negation, not a tuning problem — no similarity threshold fixes it.

Receptor status gates **36% of this corpus** (HER2) and **28%** (ER/PR). So pure retrieval
is not sufficient for Tier 2, and the fix is already available: `er_status`, `pr_status`
and `her2_status` are structured columns on `patient_diagnosis`. Tier 2 must combine
retrieval with a structured receptor filter or re-rank rather than ranking on similarity
alone. `DIAGNOSIS_MATCHING_DESIGN.md` §4 anticipated combining them; this run is the
evidence that it is mandatory.

Consistent with the no-verdicts rule: a receptor mismatch should **demote and flag** a
trial, never silently remove it. Those three trials are wrong for her today, but receptor
status can be re-tested and the tool should not make that call for her.

### The unfinished task — pick this up first

**The user was trying to answer a real question about a family member's trial options and did not
get there.** The blocker was tooling, not design.

What was in flight when the session ended:

1. **Ingest the full recruiting breast-cancer corpus.** Only 249 of **2,456** are in MySQL
   (~10%). The API call is
   `POST /api/ingestion/clinicaltrials {"condition":"breast cancer","overallStatus":"RECRUITING","maxStudies":2500}`.
   Measured rate: 250 trials in 9.5s, so the full pull should take ~2-4 minutes and fit in
   one request. Re-runs dedup by `nctId`. **This does not need a RAG backfill** — the
   deterministic filter reads `eligibility_criteria` straight from MySQL.
2. **Re-run the match with two fixes the user approved:**
   - **Strict receptor polarity.** The current filter accepts any trial whose criteria
     mention "HER2-negative" anywhere, so NCT07371585 — a HER2-**positive** first-line
     trial — scored 0.8333 against a HER2-negative patient. Reject trials *requiring*
     HER2-positive or triple-negative disease.
   - **US-wide location filter.** The user will travel anywhere in the USA. Geography was
     not part of matching at all, and the top-ranked trial (NCT05753657) has exactly one
     site, outside the US. `location` carries `trial_id` directly; no join table needed.

**On the scoring, which the user correctly noticed never reached 100%:** `top_score` was
`signals_matched / 6`, and 6/6 is unreachable by construction — a trial cannot be both
"first-line treatment-naive" and "post-CDK4/6 progression". Worse, the score counts keyword
co-occurrence in criteria text, not whether she qualifies. It is a reasonable *shortlist*
and a poor *ranking*. A real percentage needs Tier 3 (parse each criterion into a testable
predicate, evaluate against her structured fields, report pass/fail/unknown per criterion).

### Suggested next move after that

**Tier 2 matching with a structured receptor filter.** The data is real and the gap is
measured rather than hypothetical — see "First real search" below.

The two security items under *Deploying to QA* remain right regardless, and matter more
now that the database holds a real medical record rather than sample data.

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

Nine pages: Login, Dashboard, Trial Search, Trial Detail, Saved Trials, **Diagnosis**,
**Variants**, **Prior Treatment**, Ingestion. Structure and gotchas in
`_archive/frontend/frontend-module.md`.

**Variants and Prior Treatment are new (2026-08-09)** and deliberately separate pages rather
than sections of Diagnosis: three tables means three endpoints, so one Save button per page
avoids inventing a partial-write problem. They also draw on different source documents — a
pathology report, a genomic report, and a medication list — plausibly filled in on different
days. Shared `Section`/`Field`/`Select`/`BooleanSelect` live in `components/FormControls.tsx`,
extracted from `Diagnosis.tsx` so the three pages cannot drift apart.

Each carries an amber callout explaining the one thing a person filling it in is most likely
to get wrong: on Variants, that "not tested" is not "not detected"; on Prior Treatment, that
*how* a drug was stopped decides which half of the corpus applies.

### Patient variants and prior treatment — new 2026-08-09

`PatientVariant` (21 fields) and `PatientPriorTreatment` (24 fields), changesets `026`/`027`,
full layered stack plus 62 tests. Design and rationale in
`diagnosis/patient-variant-and-treatment-tables.md`; the three research documents behind it
are in `research/`.

**Both use five-state vocabularies, not booleans**, and that is the whole point:

- Variants: `DETECTED | NOT_DETECTED | VUS | NOT_TESTED | UNKNOWN`
- Treatment: `NEVER | CURRENT | PROGRESSED | STOPPED_OTHER | UNKNOWN`

The treatment case is concrete rather than theoretical. The patient is **on a CDK4/6 inhibitor now
and has not progressed on it**. A boolean `priorCdk46 = true` is literally true and reads as
post-CDK4/6 — matching her to the wrong half of the corpus. `CURRENT` vs `PROGRESSED` vs
`NEVER` is one dropdown and it is the difference between a useful shortlist and a misleading
one.

**Named `PatientPriorTreatment`, not `PatientMedication`** — that name is taken by the FHIR
prescription mirror (changeset `020`), which records what Epic *prescribed*, is keyed on a
required unique `fhir_resource_id` so nothing can be hand-entered, and is still blocked and
empty. Different table, different job.

**Nothing reads these tables yet.** They are inputs waiting for Tier 2 matching.

### Ingestion progress ticker — new 2026-08-09

A character-per-record console bar on both ingestion loops: `*` inserted, `.` skipped, `!`
error, wrapping every N records with the line's first record number in a left gutter and
elapsed/rate/ETA at line end.

```
NORMALIZING
     1 | ****.**!***.*********!**********.***************  2.6s 28.4/s ETA 3.9s
```

Class in `common/progress/ProgressTicker.java` (framework-free, so `:common` stays
Spring-less); config in `datafetcher/config/ProgressTickerProperties.java` bound from
`cancer.ingestion.progress.*`. Design record in `agents/progress-ticker.md`, and the pattern
is now a skill at `skills/progress-ticker/`.

**`enabled` defaults to `true`, not `auto`.** Ingestion is triggered from the frontend against
a running backend, so the work happens on an HTTP request thread with no console — `auto`
resolved to false and the bar never drew. Console detection describes how the *process* was
launched, not whether anyone is watching.

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

### Persisted trial matches — new 2026-08-08

`SavedTrialMatch` and `SavedTrialMatchCriterion` (tables `trial_match`,
`trial_match_criterion`, changesets `024`/`025`), full layered stack plus 68 tests. Design
and rationale in `diagnosis/trial-match-tables.md`.

**Named `SavedTrialMatch`, not `TrialMatch`** — `rag.retrieve.TrialMatch` already exists as
the in-memory search result. The persisted row and the ephemeral result are different
things; the user chose to rename the new entity rather than touch working `:rag` code.

**One run is stored**, `search_run_id = 22ccb562-b4a4-4acb-ae68-739896d837c1`: 15 ranked
matches, 77 criterion rows, 8 of them flagged `is_exclusion` (all the same CNS-metastases
concern — a skull-bone lesion rather than brain parenchyma,
which is a question for her oncology team, not a disqualification). Those trials stayed in
the ranked list, per the no-verdicts rule.

**It was written directly through the REST API, not through `GET /api/rag/search`**, so the
open question of what triggers persistence is untouched. `query_text` on every row states
that this was a deterministic filter and that `top_score` is signals-matched/6, **not** a
cosine similarity — do not compare these scores against a future semantic run.

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

### Rebuilding the database — read before running the webhook

**The n8n `clear-db` webhook left the schema half-built once this session.** It dropped the
tables but not Liquibase's `databasechangelog`, so on restart Liquibase considered `001`-`023`
already applied and created only the two new changesets. Result: `trial_match` existed while
`app_user`, `user`, `trial_source` and everything else did not, and every endpoint except
the new ones returned 500.

`spring.liquibase.drop-first` is `false` (so a stored OAuth token survives a restart), which
is what makes this failure mode possible. **The user updated the webhook and the second
rebuild worked correctly** — but if a rebuild ever produces "Table 'cancer.X' doesn't exist"
on some endpoints and not others, this is the cause. The fallback is to flip `drop-first`
to `true`, restart, and flip it back.

**A rebuild destroys the AppUser and the real PatientDiagnosis.** Both had to be recreated by
hand this session. Her diagnosis is now saved at
`.claude/patient-data/patient-diagnosis.csv` (gitignored, `600`, keyed on username `jeb`
rather than extid since extids regenerate). **The startup loader that would read it does not
exist yet** — the user asked for it, chose the gitignored-CSV approach over a committed
Liquibase seed, and it was not built before the session ended. That is a small, self-contained
task and it removes the worst repeat-pain of a rebuild.

Rebuild also invalidates Qdrant: chunks reference trial extids that no longer exist, so the
collection must be deleted and re-backfilled, not reused.

### Other

- **Two single-item failures from the 2026-08-08 250-trial pull**, neither blocking:
  `NCT06685796` failed to normalize (`Create operation failed for TrialDb`, generic message —
  needs the staging row inspected), and `NCT07219277` failed to index (`Embeddings must have
  the same number as that of the documents`). 249 of 250 normalized, 248 of 249 indexed.
- **`findByAppUserId` returned soft-deleted rows — fixed 2026-08-08.**
  `PatientDiagnosisRepository.findByAppUserId` had no `active` filter while the Diagnosis
  page takes `rows[0]`, so after replacing a diagnosis the page displayed and edited the
  **deleted** record. Now delegates to `findByAppUserIdAndActive(..., ACTIVE)`; callers
  unchanged, `:database` tests pass. **`TrialStatusRepository.findByAppUserId` has the same
  shape and has not been checked** — likely the same bug.
- **`PatientDiagnosisDbService.update()` cannot clear a field.** Every assignment is
  `if (item.getX() != null)`, so a null means "leave alone" and there is no way to unset a
  populated column through the API. Hit this when the placeholder row's stale
  `lastChemoEndDate` (2025-11-15, predating the real diagnosis) could not be cleared; the
  row had to be deleted and recreated. Affects every `*DbService` following this template.
- **No AppUser-seeding UI.** `User` (login) and `AppUser` (tracking) are separate tables with no
  FK, matched by username. Creating the row is manual and it does not survive a rebuild. One
  exists now for `jeb`, created via the API. The `passwordHash` field had to be supplied even
  though `AppUser` never authenticates — that requirement is exactly the design flaw
  `_archive/patient/PATIENT_MODEL_PLAN.md` removes.
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

## Deploying to QA (Hostinger KVM) — analysed 2026-08-08, not started

QA on your own KVM is a reasonable next step and a **different risk profile from
production**: your host, your data, and no real patient record required while the seeded
diagnosis stays placeholder. `_archive/hosting/qa-setup.md` already covers VPS provisioning
(Java 21, MySQL, Nginx, systemd) and is already renamed to this project.

`./gradlew buildDeployment` exists and bundles the frontend into the jar. All config is
env-var driven and `.env` is correctly gitignored.

**Required before it is reachable on a public IP:**

1. **Restore endpoint security.** `SecurityConfig` line 55 is `.anyRequest().permitAll()` —
   on a KVM that means anyone who finds the IP reads everything. Uncomment the preserved rule
   set; keep `/api/uchealth/callback` public (Epic's redirect cannot carry a JWT).
2. **Move the JWT secret** to `JWT_SECRET` in the server's `.env` and generate a fresh value.
   The committed default must not be what protects a public host.
3. **Bind Qdrant to `127.0.0.1`.** `docker-compose.yml` currently publishes 6333/6334 on all
   interfaces — fine locally, wrong on a public box.
4. **Change `UCHEALTH_REDIRECT_URI`** to the server address and add that URI to Epic's app
   registration, or the OAuth callback breaks.

**Not needed for QA, but required before it holds a real record:** encryption at rest, an
audit log of who read what, and a backup story. Be deliberate that QA stays sample-only until
those exist.

**Two things that will bite:**

- The **Epic token dies in ~1 hour** with no refresh, so FHIR ingestion on QA needs an
  interactive browser login each time.
- **A large ingestion will not survive an HTTP timeout** — ~110 minutes for a full pull.
  Behind Nginx you will hit `proxy_read_timeout` first. Keep pulls small, or raise it.

---

## Candidate next steps

Nothing here is decided. **Steps 1-3 have a natural order and should be done first**: the
corpus must exist before retrieval can be measured, and retrieval should be measured before
matching logic is built on top of it. Building Tier 2 against an unmeasured corpus repeats
the mistake that produced the misleading scores on 2026-08-08.

The scope decision from `BREAST_FOCUS_PLAN.md` is also pending: narrowing the app to breast
cancer only. The survey there found the schema and frontend are *already* breast-shaped, so
the change is less about removing generality than about permission to hard-code clinical
knowledge — which is what receptor-aware matching needs. Its one blocking question is whether
matching logic lives in the backend or stays in the frontend, where Tier 1 currently sits
(`frontend/src/lib/tier1Matching.ts`). Recommend backend: the frontend cannot be tested
against the corpus in bulk, cannot inform ranking, and cannot be reused by Tier 3.

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

Test counts: `:database` **818** (was 756; +62 for `PatientVariant` and
`PatientPriorTreatment`), root 3. All passing. Frontend typecheck and build clean; one
pre-existing lint error in `Login.tsx`.

Not merged to `main`; this work is still on the branch.

The 2026-08-08 and 2026-08-09 work was committed together at the end of the 2026-08-09
session — `SavedTrialMatch`/`SavedTrialMatchCriterion`, `PatientVariant`/
`PatientPriorTreatment`, the progress ticker, the seed loader, the two new frontend pages,
and the docs.

### Files that must never be committed

**`.claude/patient-data/`** — gitignored at directory level, `700`/`600`. Holds the real
PET/CT and MRI reports, the full My Health Summary PDF, and the three seed CSVs. All real
patient data.

**`playwright/.auth/mychart-session.json`** — a live authenticated session to a real medical
record. Gitignored; treat as a credential.

⚠️ **This has nearly gone wrong twice.** On 2026-08-08 `.claude/diagnosis.md` (the PET/CT
report) was `git add`-ed and staged before being caught. On 2026-08-09 the My Health Summary
PDF arrived in `.claude/diagnosis/`, which is tracked and world-readable. Neither was ever
committed or pushed. **Before any commit, confirm no patient file is in the index**, and put
any new patient document in `.claude/patient-data/` before opening it.

---

## Playwright MyChart scraper — new 2026-08-08

Standalone Gradle Java build at `playwright/` (NOT in root `settings.gradle`), modelled on
the user's existing `~/projects/viro/viro-playwright`. Plan and findings in
`.claude/playwright/PLAYWRIGHT_SCRAPE_PLAN.md`.

**Step 1 is done and verified against the real portal:** login works, and **session reuse
works** — a second run restored saved storage state and authenticated in ~3 seconds with no
MFA prompt (`SESSION REUSED`). MFA fires on a first login from a new device and the code goes
to the patient's phone, so the wait is 10 minutes. Repeatable scraping is viable; how long
the device trust lasts is still unknown.

**Why this exists at all:** Epic has **no MCP server** (their HIMSS 2026 "Agent Factory" is
health-system tooling, not a patient-accessible server), and the third-party FHIR MCP servers
are wrappers over the same SMART on FHIR API — same scopes, same blocked grants. More
importantly, Epic's patient-facing API has **no messaging resource at all**, so portal
messages and appointment notes are unreachable by any API. Scraping is not a workaround for
slow grants; it is the only route to those categories.

**Steps 2-6 are designed but not built.** The design questions were answered from real
screenshots: the dedup key is `eorderid` from the detail URL (unverified whether it survives
a session change — **confirm before step 4 writes to staging**), and a CBC panel confirmed
the `lab_result` → `lab_result_component` parent/child split is correct. Three extraction
hazards are documented, along with a provider comment that is per-encounter rather than
per-result and has no column anywhere.
