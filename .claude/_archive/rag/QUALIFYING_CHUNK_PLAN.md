# Chunking the Qualifying Facts — Change Plan

Written 2026-08-21 from an audit of `TrialChunker`, `EligibilityCriteriaChunker`,
`TrialIndexService`, `TrialRetrievalService` and `TrialRowNormalizer`, rather than from the
design docs.

Companion to `../CURRENT_STATE.md` and `../ingestion/QDRANT_SETUP.md`.

**Status: planned, nothing built.**

---

## What is being asked for

> *"We were chunking on the whole trial but really we need to focus on just what is needed to
> qualify. For example, diagnosis, biomarkers and variants."*

Refined during the interview to: **index the qualifying facts wherever they are** — not
"index less", and not "criteria only". The distinction matters, and an example is what
settled it.

## The example that reframed the problem

OPERA-01, viewed on the trial page:

- **Official title** — *"A Phase 3 Randomized, Open-Label Study of OP-1250 Monotherapy vs
  Standard of Care for the Treatment of ER+, HER2- Advanced or Metastatic Breast Cancer
  Following Endocrine and CDK 4/6 Inhibitor Therapy (OPERA-01)"*
- **Conditions** — `Breast Cancer`, `Advanced Breast Cancer`, `Metastatic Breast Cancer`,
  `ER Positive Breast Cancer`, `HER2 Negative Breast Carcinoma`

**The title carries her entire qualifying profile in one sentence** — receptor polarity, stage,
and treatment line. The conditions list carries it again as clean structured terms. Both are
more precise than the eligibility criteria, which spend paragraphs on washout windows and organ
function before reaching the same facts.

**Neither is indexed today.** That is the finding this plan is built on.

## What the audit found

### The chunker reads four fields, and the two best ones are not among them

`TrialChunker.chunk()` touches `briefSummary`, `detailedDescription`, `eligibilityCriteria`,
plus the intervention and outcome child rows. `briefTitle` and `officialTitle` are read for
nothing — `briefTitle` is `NOT NULL` on every row, so it is guaranteed-present, retrieval-dense
text that never reaches the index.

Seven chunk sources exist: `INCLUSION_CRITERION`, `EXCLUSION_CRITERION`, `ELIGIBILITY_UNPARSED`,
`BRIEF_SUMMARY`, `DETAILED_DESCRIPTION`, `INTERVENTION`, `OUTCOME`. There is no `TITLE`.

### Conditions are parsed, then deliberately thrown away

`ClinicalTrialsGovParser` reads `conditionsModule.conditions` and `NormalizedTrial` carries them
as `List<String> conditionNames`. But `TrialRowNormalizer.upsertConditionsAndSponsors()` inserts
a row into the global `medical_condition` lookup table and **never records which trial it came
from**. The table is name-unique and has no `trial_id`.

So the corpus holds a deduplicated vocabulary of every condition across 4,634 trials, with no
way to answer *"which trials have this condition."* Same shape for sponsors.

**Keywords are worse** — the parser never reads `conditionsModule.keywords` at all. The full
CRUD stack exists (entity, repository, mapper, db service, service, controller, DTOs, three test
classes) and nothing populates it. The `keyword` table is almost certainly empty.

### Only two metadata keys are filterable

Chunks carry up to eight metadata keys, but `TrialRetrievalService.buildFilter()` supports
exactly two clauses: `overallStatus == 'RECRUITING'` and `isExclusion == false`. `source`,
`ordinal`, `nctId`, `studyType` and `naturalKey` are stored and never filtered on.

`sex`, `minimumAge`, `maximumAge`, `healthyVolunteers` and `enrollmentCount` are all on `Trial`
and are not in chunk metadata at all, so they cannot be filtered even though the data is sitting
there. **There is no `phase` field on `Trial`** — phase filtering does not exist in the schema.

---

## Decisions taken, so they are not re-litigated

| Decision | Choice | Why |
| --- | --- | --- |
| Chunk granularity | **Keep per-criterion, enrich each** | `CriteriaSignalEvaluator` reuses this parse; changing granularity moves Tier 2 as well as search |
| Citation requirement | **Field attribution is enough** | "from the official title" / "from eligibility criteria" alongside matched text; exact line offsets not required |
| Enrichment prefix | **Official title + conditions list** | The two fields the OPERA-01 example proved carry the qualifying facts |
| Boilerplate | **Drop it — but measure first** | A corpus survey and a hand-checked sample before anything stops being indexed |

**The conditions decision makes schema work a prerequisite.** This is the one consequence worth
stating plainly up front: `trial_condition` does not exist, so Phase 2 below cannot start until
Phase 1 lands, and Phase 1 requires a re-ingest.

---

## The risk this plan is most exposed to

**Dropping boilerplate is the only step here that can lose information**, and this project has
been burned twice by pattern rules that looked reasonable in the abstract:

- `TREATMENT_NAIVE` read against a whole section produced **550 false concerns** while 41 unit
  tests passed.
- `HER2_POSITIVE_REQUIRED` matched `positive` but not `"HER2-positivity"`, so NCT05894239 —
  which requires HER2-positivity — scored zero concerns and ranked 4th. **The corpus sweep could
  not have caught it**; only reading the top-ranked results did.

The lesson both times: distributions look healthy while being wrong, and the samples are the
point. So the boilerplate rule gets a survey and a hand-check before it drops anything, and it
is the **last** phase rather than the first — the enrichment work is independently valuable and
carries no such risk.

---

## Phases

Ordered so that each phase is independently useful and the risky one comes last.

### Phase 0 — Baseline, before touching anything

Nothing here changes code. Without it, "did this improve retrieval" has no answer.

1. Record current `RetrievalEvaluation` numbers, including the two TRACKED queries.
2. Run `CorpusSweep` with `-Dsweep.enabled=true` and save the full output, samples included.
3. Save the current ranked list for the real patient record — the top ~20 trials with their
   signals. This is the comparison that matters most, because ranking is the test that
   matches how the tool is actually used.
4. Record the Qdrant point count.

⚠️ All of these need the backend live, and **running Gradle while it is live kills it** via
devtools. Measure retrieval over the REST API, which is how the 2026-08-10 numbers were
obtained.

### Phase 1 — Make conditions reachable

Schema and ingestion work. No chunking change yet.

1. A `trial_condition` join table — `trial_id` + `medical_condition_id`, extid-only across the
   API boundary per the project rule. A join table rather than a `trial_id` column on
   `medical_condition`, because conditions are genuinely many-to-many and the lookup table's
   name-uniqueness is worth keeping.
2. `TrialRowNormalizer.upsertConditionsAndSponsors()` records the association instead of
   discarding it. Add the link to `deleteExistingChildren()` so re-ingest is idempotent —
   conditions are currently never cleaned up, consistent with them having been global.
3. **A re-ingest is required.** The associations do not exist for any of the 4,634 trials
   already in MySQL and cannot be recovered without re-reading the staged payloads. Check
   whether `staging_raw_trial` still holds them — if it does, this is a normalize-only run
   rather than a full re-pull.
4. Sponsors get the same treatment if it is free at that point. Keywords do not — the parser
   change is separate work and conditions cover most of the same semantic ground.

**Verify before moving on:** OPERA-01 returns its five conditions through the API.

### Phase 2 — Enrich the criterion chunks

The actual chunking change.

1. Prefix each criterion chunk with the official title, falling back to `briefTitle` where
   `officialTitle` is null.
2. Prefix the conditions list from Phase 1.
3. Add a `TITLE` chunk source so the title is independently retrievable, not only as a prefix.
   ⚠️ **Any new source must own its own ordinal counter** — chunk ids hash
   `trialExtid:source:ordinal`, and colliding ids get deduplicated by the store before
   embedding, which fails the **entire trial** with "Embeddings must have the same number as
   that of the documents". This is the bug that lost whole trials on 2026-08-10.
4. Add field attribution to chunk metadata so "why did this match" can say where the text came
   from.
5. Cheap and unblocked while here: push `sex`, `minimumAge`, `maximumAge` into chunk metadata.
   Only 2 of 8 keys are filterable today and this costs nothing but a re-embed that is being
   paid anyway.

**Watch for embedding dilution.** The section-label suppression in `EligibilityCriteriaChunker`
exists because prefixing *"All 3 parts of Study:"* onto 42 chunks collapsed their embeddings
together. A title prefix repeated across every criterion in a trial is the same shape of risk —
it makes all of a trial's chunks more similar to each other. **Measure whether it helps before
assuming it does**, and be ready to prefer the standalone `TITLE` chunk over the prefix if the
prefix flattens within-trial discrimination.

### Phase 3 — Boilerplate, surveyed then dropped

1. Survey the corpus for candidate boilerplate categories — organ function, contraception,
   consent, performance status, life expectancy. Count how many criteria each rule would
   remove, corpus-wide.
2. **Hand-check a sample of what would be dropped**, per the 550-false-concerns lesson.
   Specifically check the boundary: "adequate organ function" is boilerplate, but a specific
   creatinine clearance threshold may be a real qualifying constraint for a patient on
   particular drugs.
3. Drop only the categories that survive the hand-check. Prose chunks (`BRIEF_SUMMARY`,
   `DETAILED_DESCRIPTION`) are a separate call from criteria boilerplate — decide them
   separately.
4. Re-run the Phase 0 measurements and compare.

⚠️ **A dropped chunk is invisible.** Under the no-verdicts rule a flag demotes but never removes
a trial; a criterion that stops being indexed removes the *evidence* silently, which is the same
failure one layer down. This is why the phase is last and why it is gated on a hand-check.

---

## The re-index, and how to not corrupt the corpus

Phases 2 and 3 each invalidate every stored vector. Both cost a full re-embed (~25 minutes
locally), so **batch them if they land close together** rather than paying twice.

1. **`force=true` is mandatory.** Backfill skips trials that are already indexed by probing
   Qdrant. After a chunking change every trial is still *present* but its chunks are stale, so a
   normal run skips the whole corpus and **reports success**.
2. **Delete and recreate the collection**, then backfill from empty. `reindexTrial` deletes by
   `trialExtid` before writing, so per-trial it is clean — but a trial that errors mid-run keeps
   its old chunks while its neighbours have new ones, leaving the corpus silently mixed.
   Vectors from two chunking schemes are not comparable.
3. **384 dimensions, Cosine.** REST is on 6333, gRPC on 6334 — the app speaks gRPC and the
   recreate command is REST. See `../ingestion/QDRANT_SETUP.md`.
4. **Read the returned `errors` list**, not just that the call returned. A backfill can succeed
   overall while individual trials failed.

⚠️ **Prod is a different commitment.** A full re-embed is hours on the 1 vCPU box, and prod's
corpus state has not been recorded since 2026-08-11. Do the local work first and check the
server before assuming anything about what is there.

---

## What this plan deliberately does not do

- **Keywords.** Nothing ingests them; that is parser work, and conditions cover most of the
  same ground. Lowest priority of the three lookup tables.
- **Change chunk granularity.** Per-criterion stays, so `CriteriaSignalEvaluator` keeps the
  parse it depends on and there is one parser to keep correct.
- **Add a synthesized per-trial profile chunk.** Considered and set aside in favour of
  enrichment, which preserves line-level citation.
- **Retrieval-time re-ranking.** Fixing this in the chunker is the chosen route; re-ranking
  stays available if enrichment underdelivers.

---

## ✅ Shipped 2026-08-21 — the source filter

Built after the three measurements in `BASELINE_2026-08-21.md` showed this was the only
high-value change that needs no re-embed.

**`criteriaOnly` restricts search to eligibility-criteria chunks**, dropping summaries,
descriptions, interventions and outcomes. One clause in `TrialRetrievalService.buildFilter()`;
`source` was already in chunk metadata on all 136,345 points and had simply never been filtered
on.

Measured effect on a whole-profile query: **prose in the top 25 went from 15 to 0**, and criteria
from 9 to 25.

Three decisions worth keeping:

- **Opt-in, never a default.** Prose is the right answer to "what is this trial testing", so
  defaulting to true would silently break those queries. Same reasoning as `breastOnly` on the
  ranking endpoint — the parameter that hides things is an explicit caller choice.
- **`ELIGIBILITY_UNPARSED` counts as criteria.** It is criteria text the chunker could not split
  into sections, not prose. Excluding it would silently drop the ~5% of trials whose criteria
  carry no section header — the trials where a reader most needs the raw text. There is a test
  pinning this.
- **The test asserts the filter expression, not the results.** Spring AI translates the
  expression into each store's native syntax, so the expression is what this service owns.
  Whether Qdrant honours it was verified separately by querying the store directly:
  `source in [3 criteria types]` returns exactly 80,690 points, matching 39,441 + 41,000 + 249.

**Tests: `:rag` 42 (was 35), 1 skipped — `RetrievalEvaluation`, needs a live backend by design.
Root 94, 0 skipped. 0 failures in both.** Counts read from the test XML, not the build result.

### The 500 that followed, and what it actually was

The first live call returned **HTTP 500**, not results. Two guesses were made and both were
wrong — that Spring AI would not parse `source in [...]` (a parser probe disproved it), and that
the Qdrant `IN` translation was at fault (rewriting to OR-of-equals did not fix it).

**The backend log named the cause in one line:**

```
ServiceException: TrialDb with extid=aee9ec4b-395d-43e7-99e1-22d9a96e2460 not found
```

Retrieval was working. `groupAndHydrate` then hydrates each matched trial from MySQL, and hit a
**chunk pointing at a trial that no longer exists**. A database rebuild invalidates every trial
extid while the vector collection keeps its points, so orphans are routine here, not exceptional.

**The guard for this already existed and could never fire.** The code checked
`if (trial == null) { log.warn(...); continue; }`, but `TrialDbService.findByExtid` **throws
`ServiceException` and never returns null**. One stale chunk failed the entire search.

Fixed by catching rather than null-checking, on the `:rag` side — `TrialDbService` throwing is
the contract every other caller depends on. Two tests pin it: one orphan among good hits is
skipped, and an all-orphan result returns empty rather than throwing.

⚠️ **`TrialIndexService.reindexTrial` has the identical dead guard**, so
`POST /api/rag/reindex/{extid}` with an unknown extid 500s the same way. Left alone to keep this
fix narrow; tracked as item 2b in `../TODO.md`.

**Tests after the fix: `:rag` 44 (was 35), root 94, 0 failures, 1 skip.**

✅ **Verified live 2026-08-21.** After the fix, the same query returned matches through the
frontend's "By meaning" mode with `criteriaOnly=true`. The filter, the orphan skip, and the new
frontend path all work end to end.

### The lesson worth keeping

**Read the log before theorising.** Two rounds of inference — a bytecode probe, a parser probe, a
speculative rewrite — were spent on a cause the first line of the backend console stated plainly.
The same pattern appears elsewhere in this project's history: the `HTTP 000` timeout that was
really a Qdrant crash, and the `ConnectException` that was really Gradle killing the backend.
