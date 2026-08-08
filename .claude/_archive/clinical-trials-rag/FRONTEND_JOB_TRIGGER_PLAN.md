# Frontend plan — job trigger buttons and result messages

How the Ingest / Backfill actions should behave in the UI: click a button, run a backend job,
get a clear message back.

Adapted from the working pattern in `~/projects/viro/viro-server` — specifically
`frontend/src/pages/Facilities.tsx` and `../../../frontend/src/services/api.ts`, which drive the
facility sync/recon jobs. **Note the viro `.claude` recon docs (including
`facility-recon-rule-architecture.md`) are entirely backend** — rules engines, decision objects,
field diffing. They contain no frontend guidance. This pattern was read out of the code.

## What viro does

Three buttons in a row on the Facilities page, each its own `useMutation`:

| Button | Endpoint | Pending label |
| --- | --- | --- |
| Sync | `POST /facility-recon/sync` | "Syncing..." |
| Recon | `POST /facility-recon/recon` | "Reconciling..." |
| Sync & Recon | `POST /facility-recon/sync-and-recon` | "Syncing..." |

Mechanics worth copying:

1. **Results go in a modal**, not an inline card — `setModalContent({ title, lines })` where
   `lines` is a list of `{label, value}` pairs. The modal is unmissable; an inline card below a
   long form gets scrolled past.
2. **Indented sub-labels group a combined result** — the Sync & Recon modal uses
   `'  Approved'`, `'  Pending'` under an `'Import'` heading, then the same under `'Recon'`.
3. **Blank spacer rows** (`{label: '', value: ''}`) separate sections.
4. **`queryClient.invalidateQueries` on success** — the list behind the modal refreshes itself.
5. **Spinner + verb change on the button** — `animate-spin` on the icon, label switches
   "Sync" → "Syncing...", button disabled while pending.
6. Each job gets **its own mutation object**, so two buttons can't share a pending state.

**One thing not to copy:** viro uses `alert()` for errors. The existing Ingest page's inline red
banner is better — keep it.

**One thing worth noting:** viro ships `sync-and-recon` as a third endpoint alongside the two
separate ones. This project decided (2026-08-07) to keep ingestion and backfill separate and
skip the combined call. Viro's choice suggests the two-step friction eventually justifies the
third endpoint — revisit once it has actually been felt, and see
`RAG_SESSION_STATE.md` → "Ingestion and indexing are two deliberate steps".

## What this project has now

`../../../frontend/src/pages/Ingestion.tsx` already does the core loop — `useMutation`, a disabled
spinner button, an inline result card, an inline red error banner. So this is not a rebuild.

Two real gaps:

- **No Backfill button.** `POST /api/rag/backfill` is only reachable by curl, so after ingesting,
  search silently returns nothing until someone remembers the second step. This is the biggest
  usability hole in the current UI.
- **No `invalidateQueries`.** After ingesting 1,000 trials, the Trial Search page still shows
  stale data until a manual reload.

## Plan

### 1. API service — add the RAG endpoints

`../../../frontend/src/services/api.ts` currently has only `ingestionApi.runClinicalTrials`. Add a
`ragApi` alongside it:

- `backfill()` → `POST /api/rag/backfill`, returns the backfill result
- `reindexTrial(extid)` → `POST /api/rag/reindex/{extid}`, for a single trial
- `search(params)` → `GET /api/rag/search` (not needed for buttons, but the endpoint exists and
  a Trial Search page will want it)

Types go in `../../../frontend/src/types/api.ts` next to `IngestionRequest` / `IngestionResult`:
`BackfillResult` with `trialsIndexed`, `chunksWritten`, `trialsSkipped`, `errors[]`.

### 2. Result modal component

New `../../../frontend/src/components/JobResultModal.tsx`, taking viro's shape:

```
{ title: string, lines: Array<{label: string, value: string | number}> }
```

Rules for the component:

- Renders nothing when content is null; closes on backdrop click and on a Close button.
- A line whose `label` and `value` are both empty renders as vertical space (the spacer idiom).
- A label starting with two spaces renders indented, for grouping under a heading.
- Long `errors[]` arrays are **not** dumped into `lines` — show a count, and list the first few
  with a "+N more" note. A 1,000-trial run can produce a lot of errors, and a modal that becomes
  a wall of text is worse than the inline card it replaced.

This is deliberately dumb — no job-specific knowledge. Each page builds its own `lines`.

### 3. Ingest page — Backfill button + refresh

`../../../frontend/src/pages/Ingestion.tsx`:

- Keep the existing form and its fields (condition, term, location, status, max studies).
- Add a **second button next to "Run Ingestion": "Backfill Search Index"**, its own
  `useMutation` against `ragApi.backfill()`, its own pending state and spinner label
  ("Indexing...").
- Convert both results to the modal. Ingestion lines: studies fetched, staging written, staging
  skipped, pending processed, trials normalized, error counts. Backfill lines: trials indexed,
  chunks written, trials skipped, errors.
- **`invalidateQueries(['trials'])`** after ingestion succeeds, so trial lists refresh.
- Keep the inline red banner for errors; do not adopt `alert()`.
- Keep the existing >2,000 amber warning about large pulls.

**Say plainly on the page that these are two steps.** A short line under the buttons — ingestion
loads trials into the database, backfill makes them searchable — is what stops the "I ingested
but search finds nothing" confusion. Without a combined endpoint, the UI has to carry that.

### 4. Consider a status line, not a progress bar

Ingestion of 1,000 trials took **82 seconds**; a full 18,773-trial pull is far longer, and
backfill of ~26,000 chunks is minutes of local embedding. A spinner with no feedback will feel
broken.

Real progress reporting needs a backend change (job id + a poll endpoint, or SSE) and is out of
scope here. Cheap mitigations, in order of effort:

1. An elapsed-time counter next to the spinner — nearly free, and enough to show it is alive.
2. Text setting the expectation ("this can take a few minutes for large pulls").
3. Only if those prove insufficient: an async job endpoint that returns immediately plus a
   status poll. That is the real fix, and a separate piece of work.

## Explicitly out of scope

- **Progress bars / percentage complete** — needs backend job tracking that does not exist.
- **The recon/diff rules engine.** Viro's docs describe reconciling *two* sources (CRS
  pending/approved vs. facility master) with accept/mistake human review. CT.gov is a single
  authoritative one-way feed, so there is nothing to reconcile against and no reviewer workflow
  to build. Porting it would be inventing requirements.
  **However**, one idea from it is worth keeping: today `TrialRowNormalizer` blindly overwrites
  on re-ingest, so **a change to a trial's eligibility criteria goes unnoticed**. Detecting and
  surfacing that is genuinely valuable for this project's purpose — tracked as a future idea, not
  part of this plan.
- **A combined ingest+backfill button** — deliberately deferred; see above.
