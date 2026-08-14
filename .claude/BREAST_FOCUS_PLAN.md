# Narrowing to Breast Cancer — Change Plan

Written 2026-08-09, from a survey of the actual code rather than from the design docs.
Companion to `PROJECT_PLAN.md` (the original general-purpose plan, now marked historical) and
`CURRENT_STATE.md`.

> **Reviewed 2026-08-14. The decision this plan asks for was effectively taken, in practice
> rather than by declaration.** The app is named Breast Cancer Trial Finder and deployed at
> breastcancertrialfinder.com; the Process Trials form pre-fills `breast cancer`; and the
> matching layer hard-codes breast clinical knowledge — a `diseaseTypeSignal` gate plus
> receptor-polarity, CDK4/6 treatment-line and PI3K-pathway signals. That is exactly the
> "permission to hard-code clinical knowledge" this document argued narrowing was really about.
>
> Both blocking open questions are now answered (see the end). What was never done as a
> deliberate pass is the *cleanup* — the plan's step list has not been worked through
> item by item, so treat it as a checklist of what remains rather than as pending approval.

## The finding that should shape this decision

**Very little in this codebase is generic in a way that costs you anything.** The survey found
the opposite of what a "narrow the scope" task usually finds:

- The **schema is already breast-shaped**. `patient_diagnosis` carries `er_status`,
  `pr_status`, `her2_status`, `receptor_subtype`, and `menopausal_status`. Those are breast
  oncology fields. A lung or colorectal patient would leave all five null.
- The **frontend vocabularies are already breast-shaped**. `RECEPTOR_STATUS_VALUES`,
  `MENOPAUSAL_STATUS_VALUES`, and the AJCC staging list in `types/api.ts` encode breast
  practice.
- The **only genuinely generic things** are the ingestion defaults (`condition: cancer`) and
  the trial schema itself, which is a faithful mirror of ClinicalTrials.gov and is generic
  because *the source* is generic.

So the honest framing: **you are not removing generality, you are removing an aspiration.**
The app already only works well for breast cancer. This change makes that explicit and, more
importantly, unlocks work that being nominally general was blocking.

**What narrowing actually buys you is permission to hard-code clinical knowledge.** A general
tool cannot assume ER/PR/HER2 exist. A breast-only tool can, and that is exactly what the
measured Tier 2 failure needs.

## What this does not change

Worth stating up front, because it is most of the codebase and none of it needs touching:

- The trial schema and CT.gov ingestion pipeline. Trials are trials.
- The RAG stack — chunking, embedding, Qdrant, retrieval.
- The Epic/FHIR ingestion path and the Playwright scraper plan.
- Auth, the layered architecture, extid-only, soft deletes.
- `SavedTrialMatch` / `SavedTrialMatchCriterion`.

## The work, in dependency order

### 1. Change the ingestion default — smallest possible change, do it first

`cancer.ingestion.clinicaltrials.condition` defaults to `cancer`. Change to `breast cancer`.

This is a one-line change in `application.yml` plus the matching default in
`ClinicalTrialsIngestProperties`, and the comment block above it that quotes corpus sizes.

**Why it matters more than it looks.** The default corpus goes from 18,773 recruiting cancer
trials to 2,456 recruiting breast trials — 13% of the size. Consequences, all good:

- A full ingestion becomes feasible in one request. At the measured ~349ms/trial normalization
  cost, 2,456 trials is ~14 minutes rather than ~110. Still slow, but it no longer *requires*
  the async job endpoint that the general scope made mandatory.
- A full RAG backfill goes from ~490,000 local ONNX inferences to ~64,000.
- Retrieval quality should improve. `CURRENT_STATE.md` records that "BRCA mutation" scored
  0.388 and colloquial phrasing 0.526, and hypothesized a corpus gap rather than a model
  limit. A dense breast-only corpus is the cheapest test of that hypothesis.

Keep the request-level override. Ingesting something else stays possible; it just stops being
the default.

### 2. Make receptor status actually do something — the real prize

**This is the reason to do the whole change.** Right now `er_status`, `pr_status` and
`her2_status` are stored, exposed through DTOs, and **read by nothing**. The survey confirms
no service consumes them.

Meanwhile the measured failure from 2026-08-08 is precisely a receptor failure: a
triple-negative trial was ranked the *most relevant* result (0.718) for an ER+/PR−/HER2−
patient — ahead of the trial that actually matched her profile — because "HR-negative
HER2-negative" and "HR-positive HER2-negative" differ by one token and embedding similarity
cannot see the difference. Two more clinically wrong trials ranked among the most relevant by
the same mechanism. Being ranked first is what makes this urgent rather than cosmetic: it is
what a person reads and acts on.

Receptor status gates 36% of this corpus (HER2) and 28% (ER/PR). A breast-only tool can treat
receptor polarity as a **first-class, always-present axis** rather than one optional biomarker
among many — which is what makes a structured filter or re-rank worth building.

The design constraint stays: per the no-verdicts rule, a receptor mismatch **demotes and
flags**, never removes. Receptor status can be re-tested, and the tool should not make that
call for the patient.

**Table design now exists.** `diagnosis/patient-variant-and-treatment-tables.md` specifies
`patient_variant` and `patient_prior_treatment` — the structured inputs receptor-aware and
treatment-aware matching needs. Written after the three research docs in `research/`, which
independently converge on splitting Diagnosis / Biomarkers / Molecular / Treatment History.

**Open decision — where this logic lives.** Tier 1 matching is currently in the *frontend*
(`frontend/src/lib/tier1Matching.ts`, ~180 lines, age and sex checks). Receptor logic needs
the trial's eligibility text, which the frontend has. But putting clinical inference in the
frontend means it cannot be tested against the corpus in bulk, cannot inform ranking, and
cannot be reused by a future Tier 3. Recommend **backend**, with the frontend continuing to
render whatever the backend returns. This is the single biggest architectural question in this
plan and it should be settled before code is written.

### 3. Reframe the diagnosis model as breast-specific

Currently `cancer_type` is a free-text string, and the Diagnosis page placeholder suggests
"invasive ductal carcinoma of the breast."

Options, in increasing order of commitment:

- **Leave it free-text**, and treat breast as an assumption rather than a constraint. Zero
  work. Weakest.
- **Constrain it to a breast histology vocabulary** (invasive ductal, invasive lobular, DCIS,
  inflammatory, and so on). Makes the app self-documenting and the field useful for filtering.
- **Drop the field**, since a breast-only app knows the answer. Not recommended — histology
  genuinely varies within breast cancer and affects trial eligibility.

Recommend the middle option. It is the one that turns a free-text field into something
matching can use.

Related, and cheap: the AJCC stage list in `types/api.ts` is already breast-appropriate. No
change needed.

### 4. Rename, or don't — decide deliberately

The Java package is `com.seibel.cancer`, the DB is `cancer`, the app is `basic` in
`application.yml`. `CLAUDE.md` documents a prior rename pass (`cpss` → `jobhunting` →
`cancer`) and explicitly leaves DB/env naming alone as the user's to manage.

**Recommend not renaming.** A package rename touches every file in the repo, has no functional
benefit, and this project has already absorbed three renames. "Cancer" remains accurate — it is
a superset of what the app now targets, which is the harmless direction for a name to be wrong
in. Revisit only if the app is ever shared with someone who would be confused by it.

If you do want it, that is a separate, mechanical, one-sitting task — not part of this change.

### 5. Documentation

`PROJECT_PLAN.md` §1 still describes a general trials app for "a specific patient," with
matching deferred to Phase 3. That framing is now two changes out of date: matching is no
longer deferred (Tier 1 ships, Tier 2 is the active work) and the scope is no longer general.

Rewrite §1 and §10's roadmap. Leave the architecture sections alone — they are still accurate.

## What narrowing costs you

Stated honestly, since nothing in this plan is reversible for free:

- **The multi-patient direction gets harder to reach.** `_archive/patient/PATIENT_MODEL_PLAN.md`
  exists and the schema has `app_user_id` on the diagnosis. Hard-coding breast assumptions into
  matching does not block multiple *breast* patients, but it does block "a friend with lung
  cancer wants to use this."
- **A second cancer type becomes a rewrite of the matching layer**, not a config change. That
  is the deliberate trade: matching quality now, generality later, and the receptor evidence
  says matching quality is what the tool actually lacks.
- **Corpus decisions become sticky.** Re-ingesting a general corpus later means a full
  re-backfill, since filters live in chunk metadata.

None of these are reasons not to do it. They are reasons to do step 2 in the backend, where it
can be tested and later generalized, rather than in the frontend where it cannot.

## Suggested order

1. Ingestion default → `breast cancer` (one line, immediate benefit, reversible).
2. Full corpus ingest + RAG backfill on the new default. This also finally completes the
   unfinished task at the top of `CURRENT_STATE.md`.
3. Re-run the retrieval evaluation. Settles the corpus-gap hypothesis with real numbers before
   any matching work is built on top of it.
4. Settle the frontend-vs-backend question for matching logic.
5. Build receptor-aware Tier 2 matching.
6. Constrain `cancer_type` to a breast histology vocabulary.
7. Update `PROJECT_PLAN.md`.

Steps 1-3 are cheap, sequential, and produce evidence that should inform 4-5. Do not skip 3 —
building matching on an unmeasured corpus repeats the mistake that produced the current
misleading scores.

## Open questions

- ✅ ~~**Backend or frontend for receptor matching?**~~ **Settled: backend.**
  `CriteriaSignalEvaluator.receptorSignal` is server-side, and the reasoning held — the frontend
  could not be measured against the whole corpus in bulk, could not inform ranking, and could not
  be reused by Tier 3. Tier 1 remains in `frontend/src/lib/tier1Matching.ts`.
- ✅ ~~**Does the diagnosis stay one row per user, or does narrowing make multi-patient more
  attractive?**~~ **Multi-patient won, and it shipped 2026-08-14.**
  `_archive/patient/PATIENT_MODEL_PLAN.md` is no longer unbuilt — `patient` plus `user_patient`
  grants replaced `app_user`, so the schema is multi-patient-capable and a record can be shared.
  See `access/PATIENT_ACCESS_PLAN.md`.
- **Should the ingestion condition become a fixed constant rather than a default?** Keeping it
  overridable costs nothing and preserves an escape hatch. Recommend keeping it.
- **Does `EligibilityRule` (scaffolded, never populated) get built for Tier 3, or dropped?**
  Narrowing makes parsing breast criteria into predicates more tractable, since the vocabulary
  is bounded. Still a large piece of work.
