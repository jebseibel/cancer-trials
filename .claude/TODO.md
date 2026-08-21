# TODO

Open work, highest value first. Each item names the plan that already covers it rather than
restating the design. Created 2026-08-21.

---

## 1. ✅ Flag trials that are stage IV and trying to cure — **BUILT 2026-08-21**

Requested and delivered the same day. Plan: `matching/CURATIVE_INTENT_PLAN.md`; the measurement
that shaped it: `matching/CURATIVE_STEP1_MEASUREMENT.md`.

**What shipped:**

- **Two signals**, not one — `treatmentGoalSignal` and `diseaseStageSignal`. "Trying to cure" and
  "for stage IV" are different questions that disagree often, and a combined flag would not say
  which was wrong.
- **Two columns**, `trial.treatment_goal` and `trial.disease_stage` (changesets `031`, `032`), so
  the 38 curative trials can be queried rather than only surfacing inside a ranking run.
- **A ranking tier above concern count.** Without it a control trial with zero concerns outranked
  a curative trial with one, every time — the trials most wanted were buried by the sort order.
- **Trial Search filters and badges** for both, plus locations beside the trial number.
- **A backfill endpoint** and a "Recheck Treatment Goals" button, because ingestion skips
  unchanged payloads and cannot repopulate an existing corpus.

**Verified live:** NCT04563507 ranked first — SBRT to each metastatic lesion on
a CDK4/6-inhibitor-plus-aromatase-inhibitor backbone, matching the patient's own regimen and disease.

⚠️ **The measurement overturned the plan it tested.** The plan named response-endpoint vocabulary
("complete response", "disease-free survival") as the workhorse; 232 trials say it and 5 of 5
hand-checked were describing how outcomes get measured. It is excluded. Ablative language carries
26 of the 38 survivors instead.

### Still open on this

- ⬜ **Step 4 of the plan — the visible badge on the ranked list.** A curative trial ranks first
  with its reason collapsed behind "What matched", so the page cannot say why it is at the top.
- ⬜ **Trial Detail shows the signals but Trial Search does not.** A trial badged "Treats the
  spread directly" there may still be early-stage; the stage badge covers this, but the two pages
  reason differently.
- ⬜ **Re-run the corpus measurement.** It was taken at 250 and 348 trials mid-pull, then at
  2,473. The script is `scratchpad/curative-measure.py` (not committed — it reads Qdrant directly,
  needs no auth).

---

## 2. ✅ The search 500 — **fixed and verified live 2026-08-21**

**It was never the filter.** The backend log named it:

```
ServiceException: TrialDb with extid=aee9ec4b-395d-43e7-99e1-22d9a96e2460 not found
```

Retrieval found chunks correctly, then `groupAndHydrate` tried to hydrate each trial from MySQL
and hit a **chunk pointing at a trial that no longer exists**. The vector store holds orphans:
a database rebuild invalidates every extid while the collection keeps its points, and nothing
reconciles the two.

**Why the existing guard did not catch it.** The code already anticipated this and checked
`if (trial == null) { log.warn(...); continue; }` — but **`TrialDbService.findByExtid` throws
`ServiceException` and never returns null**, so that branch could never execute. One stale chunk
turned the entire search into a 500.

Fixed by catching rather than null-checking, in `:rag` — `TrialDbService` throwing is what every
other caller depends on, so the change belongs on the retrieval side. Two tests pin it: one
orphan among good hits is skipped, and an all-orphan result returns empty rather than throwing.

✅ **Verified live 2026-08-21** — the same query returned matches after the fix.

⚠️ **Two wrong guesses preceded this**, both from inferring instead of reading the log: that
Spring AI would not parse `source in [...]` (a parser probe disproved it), and that the Qdrant
`IN` translation was at fault (the OR-of-equals rewrite did not fix the 500). **The log named
the cause in one line.** Read it first next time.

---

## 1b. ✅ The AI trial check — **BUILT 2026-08-21**

Reads one trial's criteria against the patient record, on Trial Detail. Fills the gap the
patterns cannot reach — a carve-out inside an exclusion, an unusual phrasing.

**It cannot report eligibility**, enforced by the response type having no such field rather than
by the prompt. Readings are stored (`ai_trial_assessment`, changeset `033`) with a snapshot of
the record they read and a hash of the prompt used.

⚠️ **The only feature that sends clinical text off the machine.** De-identified by an explicit
allowlist. See `CURRENT_STATE.md`, "What leaves the machine, and what does not".

### Still open on this

- ⬜ **Never verified against a real model.** Written and tested against a mock; the first live
  run has not happened. **Check the quoted criteria appear in the trial's own text** — a quote
  that is not there is fabrication and changes how far this can be trusted.
- ⬜ **No cost ceiling.** Every press is a paid call with nothing capping the rate. Fine for one
  reader; it would not be if the app were shared.
- ⬜ **The stored readings have no UI beyond the latest one.** History accumulates and only the
  most recent is shown.

---

## 2b. ⬜ `reindexTrial` has the identical dead guard

`TrialIndexService.reindexTrial` (line 53) does the same `findByExtid` then `if (trial == null)`.
Same dead branch, same consequence: **`POST /api/rag/reindex/{extid}` with an unknown extid
returns 500 instead of the intended "0 chunks written"**. Found while fixing item 2, left alone
to keep that fix narrow.

Worth a sweep for the pattern elsewhere — any `null` check on a `*DbService.findByExtid` result
is dead code hiding a 500.

---

## 3. ⬜ Finish the Phase 0 baseline

⚠️ **The corpus changed underneath this.** The baseline in `rag/BASELINE_2026-08-21.md` was taken
against 4,884 indexed trials of which 4,634 were orphans — a rebuild had invalidated their extids
while Qdrant kept the points. The corpus is now **2,473 trials, 71,712 points, zero orphans**, so
those numbers describe a state that no longer exists. The *ratios* still hold (criteria are ~58%
of the index either way); the absolutes do not.

Still missing, all needing a live backend:

- `RetrievalEvaluation` and its two TRACKED queries
- `CorpusSweep` (`-Dsweep.enabled=true`) — distribution **plus samples**
- The ranked list for the real patient record, top ~20 with signals

That last one matters most: **ranking is the test that caught the HER2-positivity bug the sweep
could not.**

---

## 4. ⬜ Chunking — the qualifying facts

`rag/QUALIFYING_CHUNK_PLAN.md`. Phase 1 (the `trial_condition` join table) is the prerequisite,
and it needs a re-ingest.

⚠️ Two findings from 2026-08-21 that change the plan as written:

- **Fix merged chunks before dropping boilerplate.** The 0.38% of criteria a naive drop would
  wrongly delete are concentrated in chunks where several criteria got merged into one. Dropping
  first deletes the evidence the merge bug exists.
- **Reconsider the title prefix.** Titles are made of design vocabulary
  (*"A Phase 3 Randomized, Open-Label Study of..."*), and design vocabulary measurably triples
  prose contamination. A naive title prefix would inject that into every chunk in the corpus.

---

## 5. ⬜ Sharing endpoints — backend step 8

`POST/GET/DELETE /api/patient/{extid}/share`. `access/SESSION_STATE.md`. Everything before it
landed; this is the sharing feature itself. Frontend items 6-8 are blocked on it.

**None of it matters with one patient and one login** — it becomes real when a second person has
an account.

---

## 6. ⬜ Admin-only ingestion

`frontend/ADMIN_ONLY_INGESTION_PLAN.md`. Four `@PreAuthorize` annotations plus a `SecurityConfig`
matcher. Independent of everything else, confirmed not started 2026-08-14.

---

## 7. ⬜ The after-commit indexing hook

New ingestions index themselves. `datafetcher` publishes a Spring event (type declared in
`:database`), `:rag` consumes it after commit — avoids the `datafetcher` → `:rag` cycle and keeps
a Qdrant outage from rolling back ingested data.

---

## Known-broken, unowned

- **Re-indexing leaves orphans.** `reindexTrial` writes new chunks but never deletes points whose
  ids no longer exist. `deleteChunksFor()` exists; only `reindexTrial` calls it, and a trial
  deleted from MySQL leaves its chunks forever.
- **`update()` cannot clear a field.** Every assignment is `if (item.getX() != null)`, so null
  means "leave alone" and no field can be unset through the API. Affects every `*DbService`.
- **`POST_CDK46` misses common phrasings on the inclusion side** — requires "prior" adjacent to
  "CDK", so *"received a CDK4/6 inhibitor"* misses.
- **Keywords are never ingested.** Full CRUD stack exists; the parser never reads
  `conditionsModule.keywords`. The table is almost certainly empty.
- **The frontend has no test runner.** `diagnosisSummary.ts`, `tier1Matching.ts` and
  `accessLevel.ts` all carry real logic and none is unit-tested; adding vitest would cover them.
- **Two Liquibase number collisions** — `011` and `014` are each used twice. Renumber when
  convenient, never as a side effect of unrelated work.
- **Nothing detects an orphaned vector store.** A database rebuild regenerates every trial extid
  while Qdrant keeps its points, and the app cannot tell. It happened three times before it was
  noticed on 2026-08-21, and cost a debugging session. A startup check comparing Qdrant's distinct
  trial count against MySQL's would catch it in one line at boot — cheaper than the after-commit
  hook already on this list, and it addresses the failure that actually keeps recurring.
- **The `admin` account is seeded and ADMIN.** `02-user.csv` creates it with a known-format bcrypt
  hash, and `LOGIN_ALLOWED_USERNAMES` is empty, so nothing blocks it. Setting that to `jeb` closes
  it; the allowlist code already shipped. ⚠️ Check the deployed box, not just this repo.
