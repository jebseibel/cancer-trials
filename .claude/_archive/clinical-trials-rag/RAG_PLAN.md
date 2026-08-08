# Clinical Trial RAG — Build Plan

Implementation plan for the RAG described in `clinical-trial-rag-project.md`. That doc is
the "why and what"; this is the "how, in this codebase." Read both.

Companion docs: `../../PROJECT_PLAN.md` (overall project), `../../CURRENT_STATE.md` (where
things stand), `../_archive/datafetcher/datafetcher-module.md` (the CT.gov ingestion this
plan consumes).

## 1. Decisions made up front

These were settled by discussion before planning; recorded here so they don't get
re-litigated.

| Decision | Choice | Why |
| --- | --- | --- |
| Vector store | **Qdrant** (Docker, local) | pgvector's main argument is "you already run Postgres" — this project runs MySQL, so that argument doesn't apply. Qdrant is one purpose-built service instead of a whole second relational database, and its metadata filtering is a first-class strength rather than an add-on. First-class Spring AI vector store. Proven in production at Roche, Bosch, Tripadvisor, HubSpot, xAI. |
| Module | **New `:rag` module** | Clean seam. The shelved `ai-provider` module is real Spring AI code but carries another project's baggage (`viro.ai` config prefix, OpenRouter model service, ERCOT/WREGIS prompt YAMLs). Reviving it means cleanup before any RAG work starts. It stays commented out of `../../../settings.gradle`. |
| Embeddings | **Local ONNX, in-process** — `all-MiniLM-L6-v2`, 384 dims | No cloud account, no API key, no per-chunk cost, and **clinical text never leaves the machine** — which matters for a corpus tied to a real patient's situation. Cost is retrieval quality: this is a small general-purpose model and will be the weakest link in the system. That is an accepted, measurable tradeoff, not an oversight — §10's evaluation set is what turns "upgrade the embedding model" into a decision backed by numbers rather than a guess. Upgrade path in §12. |
| Generation | Deferred — see §9 | Not needed to prove retrieval works. Decide once retrieval quality is measurable. |

**The plan is reversible where it matters.** All retrieval code is written against Spring
AI's `VectorStore` and `EmbeddingModel` interfaces, never against Qdrant or Vertex
classes directly. Swapping either is a dependency + config change, not a rewrite.

## 2. What already exists that this builds on

Verified in the working tree, not assumed:

- **The corpus is ingested and normalized.** `trial.eligibility_criteria` holds the raw
  narrative text with "Inclusion Criteria:" / "Exclusion Criteria:" headers;
  `brief_summary` and `detailed_description` hold the prose. Child records
  (Intervention, ArmGroup, Outcome, Location, OverallOfficial) are populated per trial.
- **`TrialRowNormalizer`** (in `datafetcher`) is the per-row, per-transaction normalizer.
  It upserts a Trial by `nctId`, then delete-and-reinserts children. This is the natural
  hook point for keeping vectors in step with MySQL — see §6.
- **`TrialDbService.findByNctId` and `findByExtid`** already exist, which is what
  hydration after retrieval needs.
- **`004-ai-tables.yaml`** already defines `ai_soul` / `ai_prompt_gang` /
  `ai_prompt_envelope` / `ai_prompt`. These are prompt-management tables and are
  **unrelated to embeddings** — this plan does not touch them, and does not store vectors
  in MySQL.

## 3. Module wiring

New `:rag` module, added to `../../../settings.gradle` alongside the existing four.

**Dependency direction — this is the constraint that shapes everything.** The existing
rule is that `datafetcher` depends on `:database` and root depends on `datafetcher`, so
`datafetcher` calling root's `service` package would be circular. `:rag` sits in the same
position: it depends on `:common` and `:database`, and **calls `*DbService` classes
directly**, never root's `Service` layer. Root depends on `:rag`.

    :common  ←  :database  ←  :datafetcher  ←  root
                     ↑                          ↓
                     └────────  :rag  ←─────────┘

### The cycle problem, and how it's avoided

`datafetcher` must trigger re-indexing when a trial normalizes (§6), because
`TrialRowNormalizer` is the only place that knows "this specific trial's data just
changed." But `:rag` already sits below `datafetcher` in the graph via `:database`, so a
direct `datafetcher` → `:rag` dependency **closes a cycle** and Gradle rejects it at
configuration time.

This is the same wall the ingestion build already hit — `CURRENT_STATE.md` records that
`TrialService`/`ConditionService`/`SponsorService` grew `upsertByNctId`-style methods that
turned out to be uncallable from `datafetcher`, because root sits above it. Those methods
are still there, unused. Same trap, new module.

**Resolution: dependency inversion via a Spring application event.** `datafetcher`
publishes a "trial normalized" event carrying the trial extid. `:rag` consumes it. Neither
module references the other, so no arrow ever points from `datafetcher` to `:rag`:

- The **event class** is declared in `:database`, which both modules already depend on.
- `datafetcher` publishes it through Spring's `ApplicationEventPublisher`.
- `:rag` holds the listener (§6).
- root has both on the classpath, so Spring wires them at runtime.

**Why the event type lives in `:database`, not `:common`:** `:common` has zero Spring
imports today — verified, and `../../../CLAUDE.md` calls that framework-independence out
explicitly. Keep it that way. `:database` already depends on `spring-boot-starter` and is
where the `*DbService` beans live, so a Spring-shaped collaboration point belongs there.

A consequence worth stating plainly: if `:rag` is absent or its listener is disabled,
publishing the event is a no-op. **Ingestion behaves identically with or without the RAG
module deployed** — which is the property that makes §6's failure isolation possible.

Dependencies the module needs: Spring AI BOM, the Qdrant vector store starter, the Vertex
AI embedding starter, plus `:common` and `:database`. Note the existing `ai-provider`
module pins its own Spring AI BOM version — `:rag` should pin the BOM in one place that
root and `:rag` agree on, so there's no version skew if `ai-provider` is ever revived.

## 4. Package layout inside `:rag`

Base package follows the project convention: `com.seibel.cancer.rag`.

- `config/` — Qdrant and embedding configuration properties, collection bootstrap
- `chunk/` — the chunking strategy (§5). This is where most RAG quality lives.
- `index/` — turning a Trial + children into chunks + metadata and writing them to the
  vector store; the re-index entry point `datafetcher` calls
- `retrieve/` — metadata-filtered similarity search, returning trial extids + matched
  chunk text
- `generate/` — grounded answer assembly with citations (§9, deferred)

## 5. Chunking — the part that decides whether this works

The design doc is explicit that most RAG quality issues live here, and that eligibility
criteria should be split by inclusion/exclusion section rather than by arbitrary token
window. That is the plan.

**Chunk types, one per semantic unit:**

| Chunk type | Source | Rationale |
| --- | --- | --- |
| Inclusion criterion | One bullet from the "Inclusion Criteria:" section | A query like "no prior chemo" should match one specific criterion line, not a wall of text. This is what makes citation-back-to-source possible. |
| Exclusion criterion | One bullet from the "Exclusion Criteria:" section | Same, and crucially the inclusion/exclusion distinction must survive into metadata — matching an exclusion is the opposite of qualifying. |
| Brief summary | `trial.brief_summary` | Whole-trial semantic gist. |
| Detailed description | `trial.detailed_description`, split if long | Mechanism and study design detail. |
| Intervention | One per Intervention child | "trials studying this drug" queries. |
| Outcome measure | One per Outcome child | "trials measuring progression-free survival" queries. |

**Parsing caveat, flagged rather than hidden:** CT.gov eligibility text is
semi-structured, not structured. Header casing and bullet markers vary between sponsors,
and some trials put everything in one paragraph with no bullets. The parser needs a
defined fallback — when no inclusion/exclusion headers are found, emit the whole
criteria block as a single chunk tagged as unparsed, and count those. That count is a
quality metric to watch: if a large fraction of trials fall back, criterion-level
retrieval is quietly not working and the fix is parser work, not prompt work.

**Metadata attached to every chunk** — this is what makes filtered retrieval possible:
trial extid (never the numeric id, per the project's extid-only rule), nctId, chunk type,
inclusion-vs-exclusion flag, overall status, study type, and the source field the chunk
came from. Phase and condition are deliberately **not** included yet — see §8.

## 6. Indexing and keeping vectors in step with MySQL

This is the main new invariant the project takes on, and the honest risk of choosing an
external vector store: **vectors can drift from MySQL.**

### Same trigger, not same transaction

The relational insert and the vector insert are two writes of the same logical event, and
something must guarantee both happen. But MySQL and Qdrant are separate systems with no
shared transaction — there is no two-phase commit available here. So making them *atomic*
is not on the table. The real choice is which failure mode to accept.

**Rejected — indexing inside the normalization transaction:**

- Qdrant down or an embedding call times out → exception → `@Transactional` rolls back →
  **the trial data is lost from MySQL too**, and `TrialNormalizationService` catches the
  throw and marks the staging row errored. A vector-store outage becomes a data-collection
  outage.
- Subtler failure in the other direction: if indexing succeeds and the transaction then
  rolls back for an unrelated reason, Qdrant holds chunks for a trial MySQL does not have.
  The rollback does not reach into Qdrant. Silent drift.

**Chosen — indexing after commit, on the same trigger:**

1. `TrialRowNormalizer.normalize()` runs in its transaction — upsert Trial by `nctId`,
   delete-and-reinsert children. Unchanged from today.
2. Transaction commits.
3. Re-index fires for that trial extid: delete existing chunks, write the new ones.
4. If step 3 fails, log it, flag the trial as needing re-indexing, and continue to the
   next staging row.

The ordering is enforced structurally rather than by convention: `datafetcher` publishes
the event inside the transaction (§3), and `:rag` consumes it with a transactional event
listener bound to the **after-commit** phase. The listener physically cannot run before the
commit lands, so a later edit cannot accidentally reintroduce the coupling.

Step 3 deliberately mirrors the delete-and-reinsert shape `TrialRowNormalizer` already uses
for child records — same idempotency guarantee, so re-running an index is always safe.

### Why the asymmetry is the right call

**MySQL is the source of truth; vectors are derived and rebuildable from it.** Every chunk
can be regenerated from `trial.eligibility_criteria` and the child records. A rolled-back
CT.gov fetch cannot be regenerated without going back to the API. When only one write can
be protected, protect the relational one.

The cost, stated plainly: **this is eventual consistency.** There will be windows where a
trial is searchable relationally but not semantically, and the system must tolerate that
rather than assume it away. That is a degraded search result, not lost data.

### Backfill is the reconciliation mechanism

A backfill / re-index path, runnable independently of ingestion, is therefore not a
convenience — it is what closes the consistency gap. It serves three jobs:

- Index the corpus already in MySQL, which all predates the RAG.
- Sweep up trials flagged as needing re-indexing after a step-3 failure.
- Re-index everything after a chunking change (§5) or an embedding-model change (§12).

Chunking strategy *will* change, so treat full re-index as a routine operation, not an
emergency one. Because embedding is a paid per-chunk API call, backfill needs batching and
a resumable cursor rather than one call per chunk.

## 7. Retrieval

Combine metadata filters with semantic similarity, per the design doc. Spring AI's
portable filter expression compiles down to Qdrant's native filters, so filters are
written once against the `VectorStore` interface.

Retrieval returns chunk matches with trial extids and similarity scores. Full trial records
are then hydrated from MySQL via `TrialDbService.findByExtid`.

**Accept this consequence explicitly:** Qdrant cannot join to the `trial` table. Retrieval
is two round trips — vector search, then relational hydration. This is the standard RAG
shape and it fits the extid-only rule cleanly, but it does mean no single query mixes
similarity with relational filters. Anything that must be filterable at search time has to
be duplicated into chunk metadata (§5), which is why §8 matters.

Two knobs to tune against real queries, not guessed at up front: top-k, and the similarity
threshold below which a match is discarded. Tune them after §10 exists.

## 8. Known blocker — the join tables

`CURRENT_STATE.md` records that `trial_condition`, `trial_sponsor`, `trial_phase`,
`trial_std_age`, and `trial_keyword` were never scaffolded, and that the normalizer
currently upserts Condition and Sponsor lookup rows **without linking them to a trial**.

The direct consequence for this plan: **phase and condition cannot be used as retrieval
filters yet**, because nothing knows which conditions or phases belong to a trial. The
design doc names phase filtering as a core requirement, so this is a real gap in the
end state, not a nice-to-have.

This does not block the RAG. Status-filtered semantic retrieval over eligibility text is
the hard and valuable part, and it works today. But the chunk metadata schema should be
designed so phase and condition can be added later, and adding them will require a full
re-index (§6) — another reason to make re-index routine.

Distance and location filtering, also named in the design doc as a stretch goal, has the
same shape: `Location` rows carry latitude/longitude, so it's feasible, but it needs
geo-aware filtering rather than plain equality and is deliberately out of scope here.

## 9. Generation — deliberately deferred

Retrieval quality is what makes or breaks this, and it is measurable on its own. Building
generation first would mean tuning prompts against retrieval that hasn't been validated.

So: build and evaluate §5–§7 first, with retrieved chunks surfaced raw. Add generation
once retrieval is good, and decide the chat provider then.

When it lands, the design doc's constraints are requirements, not suggestions:

- Explain **why** a trial might fit, citing the specific eligibility line matched
- Never frame output as a yes/no verdict — "here's what to look into / ask about"
- Always surface sources so criteria can be checked against the original listing

The chunk-per-criterion strategy in §5 is what makes line-level citation possible. That is
the reason for it, beyond retrieval quality.

**Note on providers:** Anthropic has no embeddings API, so Gemini handling embeddings does
not constrain the chat provider. Embedding and chat models are chosen independently.

## 10. Evaluation — build this early, not last

Without it, every later tuning decision is guesswork.

Assemble a small fixed set of realistic queries — the design doc's own examples are a good
start ("which trials would she qualify for with stage III and no prior chemo", "what trials
nearby are studying this mutation", "explain the difference between these two trials") —
and for each, record which trials *should* come back. Then measure whether they do, at a
given top-k and threshold.

This set is what makes chunking changes, threshold changes, and any future embedding-model
change assessable instead of vibes-based. It should exist before tuning starts.

Also worth tracking: the unparsed-criteria fallback rate from §5.

## 11. Order of work

1. Scaffold `:rag`, wire it into `../../../settings.gradle` and root, get an empty Spring context
   starting with the Qdrant and Vertex starters present.
2. **Done.** Qdrant stood up via `../../../docker-compose.yml` at the repo root (pinned
   `qdrant/qdrant:v1.12.4`, ports 6333 REST / 6334 gRPC, named volume so vectors survive a
   restart). Collection `clinical_trial_chunks` pre-created at **384 dims, Cosine**.
   Created explicitly over REST rather than relying on Spring AI's `initialize-schema`,
   which defaults to off and whose behaviour changed in a recent version.
   Note `docker compose down -v` deletes the volume and forces a full re-index; plain
   `down` does not.
3. Verify one embedding round trip locally before building anything on top of it. No
   credentials to configure, but the **first `embed()` call downloads and caches the ONNX
   model** (default cache: `${java.io.tmpdir}/spring-ai-onnx-model`), so the first call is
   slow and needs network access. Confirm the returned vector length is **384** — that is
   the check that the model and the collection agree.
4. Build chunking (§5) with unit tests over real captured payloads. The existing
   `sample-clinicaltrials-study.json` fixture is the starting point; add fixtures for the
   awkward eligibility-text shapes, since those are what the fallback path exists for.
5. Build indexing (§6) plus the backfill path. Index the corpus already in MySQL.
6. Build retrieval (§7). Expose it read-only so results can be inspected by hand.
7. Build the evaluation set (§10). Tune top-k and threshold against it.
8. Then, and only then, generation (§9).

## 12. Open items

- ~~**Embedding model and dimensions.**~~ **Decided: local ONNX embeddings, Spring AI's
  default `sentence-transformers/all-MiniLM-L6-v2` at 384 dimensions.** Starter is
  `spring-ai-starter-model-transformers`; it runs in-process, so there is no cloud
  account, no API key, and no per-chunk cost. **The Qdrant collection is created with 384
  dimensions** — that number and the model must match exactly, or writes fail on first use
  rather than erroring at startup.

  Two properties of this model to keep in mind: it is small (80MB, ~14,200 sentences/sec),
  and its input window is short (~256 word pieces), so long chunks — `detailed_description`
  in particular — will be truncated. That is another argument for the fine-grained chunking
  in §5 rather than large blocks.

  **Expect to upgrade this.** A general-purpose MiniLM on dense oncology eligibility text
  is the weakest component in the design. Two upgrade routes, both requiring a collection
  recreate and full re-index (routine per §6):
  1. **Stay local, go bigger** — override `spring.ai.embedding.transformer.onnx.model-uri`
     and `.tokenizer.uri` to any HuggingFace ONNX model. A 768-dim `-base` model (BGE, E5)
     is materially stronger at ~400MB. Keeps the no-cloud, no-cost, data-stays-local
     properties.

     **Attempted 2026-08-07 with `BAAI/bge-base-en-v1.5` and reverted.** Findings worth
     keeping, since they will recur on any large local model:
     - The ONNX export is at `onnx/model.onnx`, **not** the repo root (root path 404s).
       Verified 200 for BGE-base, E5-base, and all-mpnet-base-v2.
     - **It fails with `OutOfMemoryError`.** Spring AI's `ResourceCacheService` reads a
       *remote* model entirely into a `byte[]` before writing it to disk
       (`StreamUtils.copyToByteArray` → `InputStream.readAllBytes`), so a 440MB model needs
       ~440MB of contiguous heap. Note `org.gradle.jvmargs` in `../../../gradle.properties` does
       **not** cover this — it sizes the Gradle daemon, not the forked test JVM
       (`maxHeapSize = '4g'` is now set in `../../../rag/build.gradle`, and was kept after the revert).
     - **The fix is a `file:` URI, not a bigger heap.** The in-memory read only happens for
       remote resources, so downloading the model with curl and pointing `model-uri` at the
       local file bypasses the problem entirely. `../../../rag/src/test/resources/eval/precache-model.sh`
       does the download and prints the URIs to set. This also means the backend JVM does not
       need a raised `-Xmx`.
     - `TransformersEmbeddingModel` exposes `setModelResource`, `setTokenizerResource`, and
       `setResourceCacheDirectory` if the cache location needs moving.
     - E5 models want `query:` / `passage:` prefixes to reach full accuracy. Spring AI does
       not add them, and omitting them silently degrades results — a reason to prefer BGE.
  2. **Go hosted** — Google's `gemini-embedding-001` ranks 2nd on MedRAG (OpenAI's
     `text-embedding-3-large` ranks 11th), so it is the strongest option for medical text.
     Costs money per chunk, requires credentials, and sends clinical text to a third party.

  Build §10's evaluation set before choosing — it is what makes the difference measurable
  instead of assumed.
- **Where Qdrant runs.** The project has no `../../../docker-compose.yml` yet;
  `PROJECT_PLAN.md` lists adding one for MySQL as an open item. Qdrant is a second
  service that wants the same treatment.
- **Config and credentials.** Vertex AI needs a GCP project id and location; Qdrant needs
  host/port and optionally an api key. These follow the existing `../../../.env` pattern. Never
  commit credentials.
- **Security.** `SecurityConfig` currently permits all requests (see `CURRENT_STATE.md`).
  Any retrieval or chat endpoint added here inherits that, and is covered by the existing
  TODO to restore JWT enforcement before deployment.
