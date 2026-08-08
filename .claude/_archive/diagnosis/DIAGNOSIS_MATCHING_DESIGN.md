# Diagnosis ↔ trial matching — design

How a stored patient diagnosis gets compared against trial eligibility criteria.

**Design intent, carried from `../clinical-trials-rag/clinical-trial-rag-project.md`:** this surfaces and explains
candidate trials so the final judgement stays with the patient, family, and oncology team. It must
never render a yes/no eligibility verdict. That constraint shapes the output design in §6.

## 1. Starting point: there is no patient data

Confirmed by reading the schema and domain objects (2026-08-07):

| Exists | State |
| --- | --- |
| `LabResult` / `LabResultComponent` | Populated from Epic — one A1C from the sandbox patient |
| `PatientMedication` | Scaffolded and tested but **empty** — Epic blocked the `MedicationRequest` fetch |
| `AppUser` | Login identity only (username, password hash, display name) |
| `Profile` | `nickname`, `fullname` — nothing clinical |

**No diagnosis table exists.** Changesets stop at `022-lab-result-component`. The Epic integration
fetches `Observation` and `MedicationRequest` only — it never fetched FHIR `Condition`, which is the
resource that carries diagnoses. `EligibilityRule` exists as a table but has never been populated;
ingestion writes only raw narrative eligibility text.

So one side of the comparison has to be built from scratch. Decision (2026-08-07): **manually
entered, structured fields**, not Epic-derived — Epic's sandbox patient is not the real patient, and
the OAuth/scope problems recorded in `CURRENT_STATE.md` are unresolved.

## 2. What the criteria actually test — measured, not assumed

Across the 767 real criteria chunks currently indexed:

| Fact | Chunks | Share |
| --- | --- | --- |
| metastatic / advanced | 69 | 9.0% |
| age | 37 | 4.8% |
| prior chemotherapy | 28 | 3.7% |
| ECOG / performance status | 25 | 3.3% |
| HER2 | 23 | 3.0% |
| ER/PR / hormone receptor | 18 | 2.3% |
| PD-L1 / immunotherapy | 16 | 2.1% |
| stage | 11 | 1.4% |
| menopausal status | 11 | 1.4% |
| measurable disease | 10 | 1.3% |
| triple negative | 9 | 1.2% |
| brain metastases | 7 | 0.9% |
| **BRCA / germline** | **1** | **0.1%** |

Two corrections to earlier assumptions, from this data:

- **BRCA barely appears** (1 chunk in 767). It was flagged as important because an evaluation query
  failed on it — but that failure was a *corpus* gap, not a signal of importance. Store it; do not
  build matching logic around it.
- **Prior treatment is almost never a boolean.** Real wording: *"within the past 12 months"*,
  *"not more than 3 prior chemotherapeutic regimens"*, *"prior systemic therapy for metastatic
  disease"*. A `hadChemo` flag cannot answer any of those — a count and a date are needed.

### What the wording looks like, and whether it is comparable

- **ECOG — cleanly comparable.** Every instance is a numeric range: `0 or 1`, `0-2`, `≤2`,
  `performance status 0-1`. A single integer compares reliably. Strongest case for a structured field.
- **Stage — comparable with a caveat.** `stage I-III`, `Stage I to III` are ranges, but **two AJCC
  editions appear** (7th and 8th). Same numeral can mean different things across editions.
- **Receptor status — messier than it looks.** Written as `Her2+`, `HER2-`, `HR+ve HER2-`,
  `HER2/neu positive`. Receptors travel **together** as a subtype (`HR+/HER2-`, triple-negative), so
  three independent booleans lose the clinically meaningful combination.

### Already free — do not rebuild

`Trial` already carries `minimumAge`, `maximumAge`, `sex`, and `healthyVolunteers` as structured
fields parsed from CT.gov. **Age and sex matching need no text parsing at all** — that covers the
4.8% age slice deterministically. Use these before reaching for anything cleverer.

## 3. The `PatientDiagnosis` entity

One row per patient (realistically one row, ever). Follows the project's standard layering —
domain → entity → repository → db service → service → controller — and `BaseDomain`/`BaseDb` for
`extid`, timestamps, soft delete.

**Linked to `AppUser` by `appUserId` internally, exposed as `appUserExtid`** per the extid-only rule.

| Field | Type | Why this shape |
| --- | --- | --- |
| `cancerType` | String | Free text, e.g. "invasive ductal carcinoma of the breast". Matched semantically, not by equality — the corpus phrases this a hundred ways. |
| `stage` | String | `I`/`II`/`III`/`IV`, optionally with substage (`IIIA`). String not int, because substages exist. |
| `stageSystem` | String | AJCC edition (`AJCC_8`, `AJCC_7`). **Recorded because the corpus mixes editions** — without it, a stage comparison is quietly ambiguous. |
| `isMetastatic` | Boolean | The single most-tested fact (9.0%). Kept separate from stage: criteria say "metastatic" far more often than "stage IV". |
| `metastasisSites` | String | Comma-separated. Brain mets specifically is a common exclusion (0.9%) and worth being able to check. |
| `receptorSubtype` | String | The **combination** — `HR_POSITIVE_HER2_NEGATIVE`, `TRIPLE_NEGATIVE`, `HER2_POSITIVE`. Modelled as one field, not three booleans, because that is how criteria express it. |
| `erStatus`, `prStatus`, `her2Status` | String | The individual receptors as well (`POSITIVE`/`NEGATIVE`/`UNKNOWN`) — some criteria test one in isolation. Redundant with `receptorSubtype` on purpose; see §7. |
| `biomarkers` | String | Free text for everything else — BRCA, PD-L1, PIK3CA, MSI. Deliberately unstructured given BRCA's 0.1% frequency; structuring these is not yet justified. |
| `ecogStatus` | Integer | 0–4. The one field that supports genuine numeric comparison. |
| `priorChemoRegimens` | Integer | A **count**, not a flag — criteria say "not more than 3 prior regimens". |
| `lastChemoEndDate` | LocalDate | Enables "within the past 12 months" windows. A flag cannot. |
| `priorTreatments` | String | Free text listing modalities and agents (surgery, radiation, endocrine, specific drugs). |
| `hasMeasurableDisease` | Boolean | RECIST measurability, 1.3% of criteria. |
| `menopausalStatus` | String | `PRE`, `PERI`, `POST`, `UNKNOWN` — 1.4%, and it gates whole trial arms. |
| `dateOfBirth` | LocalDate | Age is derived, never stored — a stored age is wrong within a year. Compared against `Trial.minimumAge`/`maximumAge`. |
| `sex` | String | Compared against `Trial.sex`. |
| `diagnosisDate` | LocalDate | Context. |
| `notes` | String (longtext) | **Load-bearing, not a dumping ground.** Clinical reality does not fit the fields above, and this is what gets semantically compared when a criterion tests something unmodelled. |

### Column spec — `patient_diagnosis`

The table above explains *why* each field exists; this is the machine-readable column list, in the
format `domain-pojo-from-tables-doc` expects. `BaseDb` already supplies `id`, `extid`, `created_at`,
`updated_at`, `deleted_at`, `active` — never re-declare those.

```
app_user_id                bigint                     -- FK -> app_user.id
cancer_type                varchar(255)   not null
stage                      varchar(16)                -- I, II, IIIA, IV
stage_system               varchar(16)                -- AJCC_8, AJCC_7
is_metastatic              tinyint(1)
metastasis_sites           varchar(500)               -- comma-separated: brain, bone, liver
receptor_subtype           varchar(64)                -- HR_POSITIVE_HER2_NEGATIVE, TRIPLE_NEGATIVE, HER2_POSITIVE
er_status                  varchar(16)                -- POSITIVE, NEGATIVE, UNKNOWN
pr_status                  varchar(16)                -- POSITIVE, NEGATIVE, UNKNOWN
her2_status                varchar(16)                -- POSITIVE, NEGATIVE, UNKNOWN
biomarkers                 varchar(1000)              -- free text: BRCA1, PD-L1 CPS 10, PIK3CA
ecog_status                int                        -- 0-4
prior_chemo_regimens       int                        -- count, not a flag
last_chemo_end_date        date
prior_treatments           varchar(2000)              -- free text: surgery, radiation, agents
has_measurable_disease     tinyint(1)
menopausal_status          varchar(16)                -- PRE, PERI, POST, UNKNOWN
date_of_birth              date                       -- age is derived, never stored
sex                        varchar(16)
diagnosis_date             date
notes                      longtext
```

**Note on `app_user_id`:** marked FK, so the POJO skill skips it by default. Keep it as
`appUserId` — the diagnosis has to belong to someone, and per the extid-only rule the controller
resolves `appUserExtid` → `appUserId`.

## 4. How matching works — three tiers, most reliable first

The point of separating tiers is that they carry very different confidence, and the UI must not
present a semantic guess as though it were a deterministic check.

### Tier 1 — deterministic, on structured trial fields

No text parsing. Compare against fields CT.gov already gives us:

- Derived age vs `Trial.minimumAge` / `maximumAge`
- `sex` vs `Trial.sex`
- Recruiting status (already a retrieval filter)

**High confidence.** These are the only checks that can honestly be called pass/fail.

### Tier 2 — retrieval-driven, using the diagnosis to build queries

This is where the existing RAG does the work. Turn diagnosis fields into semantic queries against
the indexed criteria chunks, and report what comes back with its similarity score:

- `ecogStatus` → query "ECOG performance status"
- `receptorSubtype` → query "HR positive HER2 negative" / "triple negative"
- `priorChemoRegimens` > 0 → query "prior chemotherapy"
- `isMetastatic` → query "metastatic disease"

The measured evaluation numbers say this works well for exactly these: ECOG scored **0.975**,
triple-negative **0.992**, prior chemotherapy **0.707**. Those were not chosen arbitrarily — they
are the evaluation set from `eval/retrieval-queries.md`.

**Crucially, `isExclusion` metadata already distinguishes the two directions.** A high-scoring
match on an *exclusion* criterion means the patient may be **disqualified** — the opposite of a
fit. That flag is already on every chunk and must drive the display.

### Tier 3 — the eligibility rule tree (not now)

`EligibilityRule` exists for parsing criteria into a comparable structure, and
`_archive/clinical-trials/clinical-trials-tables.md` marks its design as open questions. Populating it is a
much larger piece of work and is **out of scope**. Tier 2 gets most of the value without it.

## 5. What NOT to build

- **No eligibility scoring or ranking by "fit percentage".** A number implies a precision that
  cannot survive the parsing ambiguity above (AJCC editions, "within 12 months", receptor phrasing).
  It would be false confidence about a medical decision.
- **No auto-exclusion.** Never hide a trial because a criterion appears unmet. Surface the concern
  and let a person judge; a parsing failure must not silently remove an option.
- **No rules engine ported from viro's facility recon.** That reconciles two authoritative sources
  with an accept/reject workflow. This is one patient record compared against free text — different
  problem, and the machinery would not transfer.

## 6. Output shape

Per trial, grouped the way `TrialMatch` already groups chunk hits:

- **Deterministic checks** (Tier 1) — plain pass/fail, e.g. "Age 54 is within 18–75".
- **Points to look into** (Tier 2 inclusion matches) — the matched criterion text, its similarity
  score, and which diagnosis field triggered it.
- **Points to ask about** (Tier 2 exclusion matches) — same, flagged clearly as possibly
  disqualifying.
- **Not assessed** — criteria that matched nothing, stated plainly. Silence about a criterion must
  not read as "requirement met".

Framing follows the project doc: *"here's what to look into / ask about"*, never a verdict. That
last bucket matters most — an honest tool says what it could not evaluate.

## 7. Deliberate redundancy, and why

`receptorSubtype` overlaps `erStatus`/`prStatus`/`her2Status`, and `isMetastatic` overlaps `stage`.
Both are intentional: criteria are written both ways, and storing only the "normalised" form means
reconstructing the other at query time and getting it subtly wrong. Storage is free; a missed
eligibility match is not.

The UI should derive the subtype from the three receptor fields on entry so they cannot disagree.

## 8. Build order

1. `PatientDiagnosis` domain POJO — `domain-pojo-from-tables-doc` skill covers this shape.
2. Full layered scaffold via `database-restapi-template`, then tests via
   `database-restapi-testing`. Extid-only from the start.
3. Frontend form to enter and edit it. One patient, so a single edit page rather than a list.
4. **Tier 1 matching only**, surfaced on Trial Detail. Smallest useful slice, and fully
   deterministic — no ambiguity to explain.
5. Tier 2 on top, reusing `TrialRetrievalService` and the existing `isExclusion` flag.
6. An evaluation pass: enter the real diagnosis, run it against the 1,042 indexed trials, and check
   by hand whether the surfaced criteria make sense. Same discipline as
   `eval/retrieval-queries.md` — the measurement is the deliverable, not the code.

## 9. Open questions

### Do we need more patient tables than `PatientDiagnosis`?

**Decision (2026-08-07): no — build `PatientDiagnosis` alone, expand later.**

Every field in §3 is single-valued for a current diagnosis: one stage, one ECOG, one receptor
subtype, one chemo count. The two multi-valued fields (`metastasisSites`, `biomarkers`) are
comma-separated strings, which holds up because they are matched semantically rather than joined on.
This mirrors a call the project already made deliberately — `CURRENT_STATE.md` records the trial
join tables (`trial_condition`, `trial_phase`, …) being deferred rather than built up front.

Three places the single table strains, in the order they are likely to bite:

1. **`PatientTreatment` — the most likely second table.** `priorTreatments` free text plus a
   `priorChemoRegimens` count is a compression of what criteria actually test: *which* agent,
   *when* it ended, and whether disease progressed on it. Prior chemotherapy is the
   second-most-tested fact in the corpus (3.7%), so this is where the compression will show first.
   A table of (agent, modality, startDate, endDate, outcome) answers those properly.
   **Trigger to build it:** the first time a real question cannot be answered from the free-text
   field.

2. **Progression over time — clinically central, deliberately deferred.** Cancer is a sequence:
   diagnosed at stage II, treated, recurrence, restaged IV. One row holds only "now", so the
   history is lost — and criteria do test it ("progression on prior endocrine therapy"). Two ways
   to add it later: a `PatientDiagnosisHistory` table, or make `PatientDiagnosis` append-only with
   a `current` flag. **Start with one row anyway** — the current state has to work before history
   is worth modelling, and the append-only variant is a cheap migration.

3. **`PatientBiomarker`, only if biomarkers get structured.** Free text is justified today by
   BRCA's 0.1% frequency in the corpus. If that changes — a wider corpus, or a biomarker central
   to the actual case — a table of (marker, value, method, date) becomes right.

**What explicitly does not need a table:**

- **Age** — derive from `dateOfBirth`. A stored age is wrong within a year.
- **Lab values** — `LabResult` already exists and is populated from Epic. Some criteria test lab
  thresholds ("ANC ≥ 1000/µL"; "adequate liver function" scored 0.661 in the evaluation set), so
  lab matching should **reuse that table** rather than duplicating values onto the diagnosis.

### Other open questions

- **Who enters it?** No auth-linked patient concept exists beyond `AppUser`, and `AppUser` is
  matched to login `User` **by username** with no FK (see `CURRENT_STATE.md`). This design inherits
  that weakness.
- **AJCC edition mismatch** — with both 7th and 8th editions in the corpus, is a stage comparison
  trustworthy enough to present as Tier 1? Current answer: no, keep stage in Tier 2.
