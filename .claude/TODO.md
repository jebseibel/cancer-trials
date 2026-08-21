# TODO

Open work, highest value first. Each item names the plan that already covers it rather than
restating the design. Created 2026-08-21.

---

## 1. ⬜ Flag trials that are stage IV and trying to cure — **requested 2026-08-21**

**The thing the tool is most wanted for.** Plan: `matching/CURATIVE_INTENT_PLAN.md`, written
2026-08-14, complete and unbuilt.

> *"Trials that are trying to cure stage 4 cancer. They are out there but they are few. They are
> the primary ones I am trying to find."*

Two claims that pull against each other: **they are few**, so the tool must not lose them; and
**they are the primary target**, so this is not a filter bolted onto the ranking — it changes
what the ranking is *for*.

**Why it does not already work.** All five existing signals answer *"is she eligible?"* — disease
type, receptor polarity, treatment line, PI3K, location. **Not one asks what the trial is trying
to achieve.** In metastatic breast cancer the overwhelming majority of trials test disease
control, so under today's sort a well-matched control trial with zero concerns outranks a
curative-intent trial carrying one concern, every time. The trials most wanted are structurally
buried by the current ordering.

**The honest difficulty:** CT.gov publishes no treatment-intent field. It has to be inferred from
prose — the exact kind of inference that produced the 550 false concerns and the HER2-positivity
miss. So the plan's **Step 1 is measure-first, and it is not skippable.**

Shape of the work: two new signals (`treatmentGoalSignal`, `diseaseStageSignal`), a ranking tier
above concern count, a visible badge on the page, then a re-sweep and hand-check.

⚠️ Decided already, do not re-litigate: **broad definition** (aggressive/durable-remission intent,
not only the literal word "cure"), and **a signal plus a ranking tier** — one list, a badge,
nothing hidden.

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

## 2b. ⬜ `reindexTrial` has the identical dead guard

`TrialIndexService.reindexTrial` (line 53) does the same `findByExtid` then `if (trial == null)`.
Same dead branch, same consequence: **`POST /api/rag/reindex/{extid}` with an unknown extid
returns 500 instead of the intended "0 chunks written"**. Found while fixing item 2, left alone
to keep that fix narrow.

Worth a sweep for the pattern elsewhere — any `null` check on a `*DbService.findByExtid` result
is dead code hiding a 500.

---

## 3. ⬜ Finish the Phase 0 baseline

`rag/BASELINE_2026-08-21.md` holds the vector-store measurements. Still missing, all needing a
live backend:

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
- **Two Liquibase number collisions** — `011` and `014` are each used twice. Renumber when
  convenient, never as a side effect of unrelated work.
