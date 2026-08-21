# Qdrant Collection Setup

What to do when semantic search stops working because the collection is missing, and why the
app does not fix it for you. Companion to `DEPLOYMENT_SEEDING.md` and `../../CURRENT_STATE.md`.

Verified 2026-08-10 against a real recovery.

## The symptom

Preparing trials for search reports **zero trials indexed** and one error per trial, all
saying the same thing. The backend log carries:

```
NOT_FOUND: Not found: Collection `clinical_trial_chunks` doesn't exist!
```

The request itself succeeds — a per-trial failure is caught so one bad trial cannot abandon a
whole backfill — so this looks like a data problem rather than a missing setup step. That is
what makes it worth documenting.

Since 2026-08-10 the condition is caught up front: the backfill checks the store once before
starting and returns a single message naming the real cause, and a startup check logs
`SEARCH UNAVAILABLE` at boot rather than waiting for someone to press a button.

## Why the app does not create it

`initialize-schema` is `false` on purpose. Letting Spring AI auto-create the collection would
have it infer vector dimensions at first write, so a mismatch between the embedding model and
an existing collection would surface as **quietly bad search results** instead of a clear
error. A missing collection that announces itself is the better failure.

The consequence is that a rebuild, a fresh machine, or a manually cleared vector store leaves
this to be done by hand.

## The fix

Qdrant exposes REST on **6333** and gRPC on **6334**. The app speaks gRPC; the commands below
are REST, so they use 6333. Getting this backwards is a common few minutes lost.

Create the collection with **384 dimensions** and **Cosine** distance — 384 is what the
configured embedding model produces (Spring AI's default all-MiniLM-L6-v2), and the collection
must match it exactly:

```
PUT http://localhost:6333/collections/clinical_trial_chunks
Content-Type: application/json

{"vectors": {"size": 384, "distance": "Cosine"}}
```

The collection name comes from `QDRANT_COLLECTION`, defaulting to `clinical_trial_chunks`.

### Verify

```
GET http://localhost:6333/collections/clinical_trial_chunks
```

Expect `status: green`, `size: 384`, `distance: Cosine`. A freshly created collection reports
`points_count: 0`, which is correct — it fills up when trials are prepared for search.

`GET http://localhost:6333/collections` lists everything, and is the quickest way to confirm
whether the collection is missing or merely empty. Those are different problems: missing needs
this document, empty just needs a backfill.

## When this is needed

- **After a database rebuild.** Chunks reference trial extids that no longer exist, so the
  collection must be **deleted and recreated**, never reused. Stale chunks are worse than no
  chunks: search returns hits pointing at trials that cannot be loaded.
- **On a fresh machine or a new deployment**, before the first backfill.
- **After changing the embedding model.** A different model means different dimensions, and
  the collection has to be recreated to match. This is exactly the mismatch that
  `initialize-schema: false` exists to make visible.

## After recreating

The collection comes back empty, so trials already in MySQL are not searchable until they are
re-indexed. Run **Prepare for Search**. Nothing in MySQL is affected by any of this — trials,
the patient record, and saved matches are all untouched by a vector-store problem.
