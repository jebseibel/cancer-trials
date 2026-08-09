# Trial Match — Database Tables

Persisted results of a diagnosis-driven trial search. Companion to
`DIAGNOSIS_MATCHING_DESIGN.md` and `../CURRENT_STATE.md`. MySQL, managed via Liquibase.

## Why these tables exist

Until now a search was ephemeral: `GET /api/rag/search` computed matches live from Qdrant
and returned them, writing nothing. That is cheap and never stale, but it means **nothing
records why a trial was surfaced on a given day**. If a trial appears in August and is gone
in October, there is no way to reconstruct what changed — the diagnosis, the corpus, or the
query. For a tool informing a real treatment conversation, that traceability is worth the
storage.

**Accumulate, do not overwrite.** Every search run writes a new set of rows. History and
run-to-run diffs are the point; pruning old runs is a later decision.

## The snapshot rule

A persisted match is only interpretable alongside **what it was computed against**.
`patient_diagnosis` is a single row updated in place, with no version history, so a match
row cannot reference "the diagnosis as it was."

Therefore **`trial_match` denormalizes the diagnosis inputs that drove the match** —
receptor status, stage, biomarkers, and the query text. This is deliberate duplication. It
keeps a match self-contained and interpretable forever, without restructuring an entity
that is already wired through the whole stack.

If `patient_diagnosis` later gains real versioning, these snapshot columns become redundant
rather than wrong.

## Conventions

Same as the rest of the schema. Every table carries the `BaseDb` fields (`id`, `extid`,
`created_at`, `updated_at`, `deleted_at`, `active`); only additional fields are listed.

**Liquibase note:** decimal types must be quoted in the changeset YAML
(`type: "decimal(10,3)"`) — an unquoted comma breaks the YAML flow-mapping parser and
aborts the changelog mid-run. This has bitten this project twice.

**extid-only rule:** every cross-entity reference on the wire uses the target's `extid`,
never its numeric id, including FK-like fields.

---

## `trial_match`

One row per trial surfaced by one search run. A run that returns 10 trials writes 10 rows,
all sharing a `search_run_id`.

```
trial_id               bigint          not null    -- FK -> trial.id
app_user_id            bigint          not null    -- FK -> app_user.id, whose search this was
patient_diagnosis_id   bigint                      -- FK -> patient_diagnosis.id, the row used
search_run_id          varchar(36)     not null    -- UUID grouping every match from one run
query_text             text            not null    -- the exact query string sent to retrieval
top_score              decimal(6,4)    not null    -- best chunk score for this trial, 0..1
match_rank             int                         -- position within the run, 1 = highest
snapshot_er_status     varchar(16)                 -- diagnosis values AS OF this run,
snapshot_pr_status     varchar(16)                 --   denormalized on purpose, see above
snapshot_her2_status   varchar(16)
snapshot_stage         varchar(16)
snapshot_biomarkers    varchar(1000)
matched_at             datetime        not null    -- when the run executed
```

**`top_score` is `decimal(6,4)`** — cosine similarity in 0..1, four places is ample. Quoted
in the changeset.

**No unique constraint on (trial, run).** Accumulating is the point; a trial legitimately
appears once per run, across many runs.

**`patient_diagnosis_id` is nullable** so a match survives the diagnosis row being deleted
and recreated — which already happened once, on 2026-08-08, when a placeholder row had to
be replaced. The snapshot columns are what preserve meaning in that case.

## `trial_match_criterion`

One row per matching chunk — the evidence for *why* a trial surfaced. A `trial_match` with
three matching criteria has three children.

```
trial_match_id         bigint          not null    -- FK -> trial_match.id
chunk_text             text            not null    -- the criterion text that matched
score                  decimal(6,4)    not null    -- this chunk's similarity score
is_exclusion           tinyint(1)                  -- true when this is an exclusion criterion
source                 varchar(64)                 -- which field the chunk came from
ordinal                int                         -- chunk position within its source
```

**`is_exclusion` is the important column.** It is what lets a high-scoring exclusion render
as a *concern* rather than a fit, per `DIAGNOSIS_MATCHING_DESIGN.md` §5 — no verdicts, no
auto-exclusion, "unknown" is first-class. A match that scored highly against an exclusion
criterion is exactly the thing to surface for a human to ask about, and it must never
silently remove a trial from a list.

This is a child table with its own identity (it carries `BaseDb` fields), not a pure join
table — each row is real evidence, not a link between two entities.

**Re-running a search does not update these**, it writes a new `trial_match` with new
children. Nothing is diffed or merged.

---

## Known limitation this does not solve

**Embedding similarity cannot distinguish receptor polarity.** Measured 2026-08-08: a
triple-negative trial scored *highest* (0.718) against an ER+/PR−/HER2− patient because
"HR-negative HER2-negative" and "HR-positive HER2-negative" differ by one token. Persisting
matches records that a bad match happened; it does not prevent it.

The fix belongs in Tier 2 retrieval — combining the structured `er_status`/`pr_status`/
`her2_status` columns with similarity as a filter or re-rank. These tables are the place
that fix will be measured, since a stored run can be compared against a later one.

## Open questions

- **When are matches written?** Options: on every `GET /api/rag/search` (noisy — every
  exploratory query persists), or only from an explicit "run and save" endpoint. Leaning
  toward a separate endpoint so casual searching stays ephemeral. Not yet decided.
- **Pruning.** Accumulating forever is right for now, at a few hundred rows per run. Revisit
  if run counts grow.
- **Relationship to `trial_status`.** `trial_status` is the user's manual tracking
  (interested, contacted, ruled out). A match is machine-generated evidence. They stay
  separate, but a future UI likely wants to show "this trial matched at 0.72 on <criterion>"
  alongside the status.
- **Indexing.** At minimum `trial_match.search_run_id` (fetching a run), `trial_match.app_user_id`,
  `trial_match.trial_id`, and `trial_match_criterion.trial_match_id`.
