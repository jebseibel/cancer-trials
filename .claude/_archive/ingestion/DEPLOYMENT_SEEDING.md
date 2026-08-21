# Seeding the Trial Corpus on Deployment — Options

How a freshly deployed instance ends up with trials in it, so a user arriving for the first
time can search rather than being told to go and process trials first.

Written 2026-08-10. Companion to `PAYLOAD_HASH_PLAN.md` (which is what would make a re-pull
cheap) and `../../CURRENT_STATE.md`.

**Status as of 2026-08-14: ✅ option 2 was chosen and is the live procedure.** Phase 4 of
`../../hosting/DEPLOY_RUNBOOK.md` is exactly this option — `curl` against
`POST /api/ingestion/clinicaltrials` then `POST /api/rag/backfill`, run inside `tmux` so the
session surviving matters rather than the HTTP request, with an Nginx `proxy_read_timeout`
override on `^/api/(ingestion|rag/backfill)`. No application code was needed, as predicted.

The recommended sequencing at the end of this document was followed: option 2 first, then
payload hashing (✅ built, see `PAYLOAD_HASH_PLAN.md`). **Option 1 — ship the data as a restore —
remains unbuilt and is still the recommendation for production**, now that a corpus worth
capturing exists.

⚠️ **Whether Phase 4 has actually been run on the server is not recorded in any document.** As of
2026-08-11 prod had 0 Qdrant points. Check the box, not this file.

## What the user should experience

A user opens the app, their diagnosis is already there, and searching returns trials. Nothing
about ingestion, backfill, or corpus loading should be a prerequisite for that. The Process
Trials page stays — refreshing the corpus is a real thing to want — but it should be
maintenance, not setup.

Patient data already works this way: `PatientSeedLoader` recreates the patient, its OWNER grant,
PatientDiagnosis, PatientVariant and PatientPriorTreatment on startup from gitignored CSVs, and
it is verified through a real rebuild. The trial corpus is the remaining gap.

> *Written when the entity was `AppUser`; that table was dropped in changeset `030` and replaced
> by `patient` plus `user_patient` grants. The seeding behaviour is unchanged.*

## The trap: do not seed trials the way patient rows are seeded

The obvious move is another `CommandLineRunner` that pulls trials at startup. The two cases
look similar and are not remotely comparable in cost:

| | Patient rows | Trial corpus |
| --- | --- | --- |
| Volume | 4 rows | ~2,500 trials |
| Source | local CSV | network fetch from CT.gov |
| Startup cost | milliseconds | **~15 min ingest, plus backfill on top** |
| Failure mode | logged, boot continues | boot blocked on a third party |

At the measured ~349ms per trial normalization, a startup pull makes the app unhealthy for
15+ minutes on **every restart**. Behind a health check or a process supervisor that reads as
a failed boot and gets killed and restarted, indefinitely. A ClinicalTrials.gov outage becomes
a boot failure for an app whose corpus is already on disk.

Startup seeding is right for four rows from a local file. It is wrong for this.

## Option 1 — Ship the data, not the pull

Run the ingestion once locally, capture the result, restore it on the server.

**Captures:** the MySQL trial tables (`trial` and its children) and the Qdrant collection.

**Deploy becomes a restore** — seconds, no fetching, no embedding. This matters more on a
modest VPS than locally: a full backfill is ~64,000 local ONNX inferences on the breast-only
corpus, and the QA box will be slower at that than the development machine.

**Costs:** a bigger deploy artifact, and the corpus is only as fresh as the dump. Refreshing
means re-dumping, or running Process Trials on the server afterwards.

**This is the recommended option for production**, and doubly so while a re-pull remains
full-cost — see the sequencing note at the end.

## Option 2 — Post-deploy script

After the app is up and healthy, a script calls `POST /api/ingestion/clinicaltrials` and then
`POST /api/rag/backfill`. These are the same two endpoints the Process Trials buttons call, so
**this needs no new application code at all**.

**Advantages:** nothing to build, and a failure does not block startup — the app boots, the
corpus arrives later.

**Costs:** the app is live-but-empty for the duration, so a user arriving in that window sees
an empty search. The script has to actually be run, which makes it a step someone can forget.
And a large pull over HTTP is exposed to whatever timeout sits in front of it — behind Nginx
that is `proxy_read_timeout`, which will fire long before a 2,500-trial run completes unless
raised.

**Best used as:** the immediate, zero-code answer while option 1 is set up.

## Option 3 — Async startup job

App boots immediately and reports healthy; a background task performs the pull afterwards,
guarded by a check that skips when trials already exist.

**Advantages:** self-contained, no external script, no deploy artifact to manage. Users can log
in throughout; the corpus appears when it is ready.

**Costs:** this is real work that does not exist yet — a job endpoint, the guard, and some way
to surface progress so "still loading" is distinguishable from "broken". `CURRENT_STATE.md`
already notes an async job endpoint becomes necessary at full-corpus scale, so this overlaps
with work that may be wanted anyway.

## Steps any option has to account for

**Deleting the Qdrant collection is not exposed by the app.** `RagIndexController` offers
`POST /api/rag/backfill`, `GET /api/rag/search`, and `POST /api/rag/reindex/{trialExtid}` —
there is no collection-management endpoint. Deleting a collection is a direct REST call to
Qdrant. Any procedure that rebuilds the corpus must include it as its own step.

**The collection is pre-created deliberately, not auto-created.** `application.yml` leaves
auto-creation false on purpose: auto-creation would infer vector dimensions at first write, so
a model or collection mismatch would surface as bad search results rather than as a startup
error. A restore or rebuild must create the collection with the right dimensions, not rely on
the app to do it.

**A stale collection is worse than an empty one.** Chunks reference trial extids; after a
rebuild those ids no longer exist. The collection must be deleted and re-created, never reused.

~~**Qdrant is currently published on all interfaces.**~~ ✅ **Fixed — verified 2026-08-14.**
`../../../docker-compose.yml` now binds both ports to `${QDRANT_BIND:-127.0.0.1}`, so the default is
localhost-only and a remote container needs an explicit override. Qdrant ships with no
authentication of any kind, so put real auth in front before ever setting `QDRANT_BIND=0.0.0.0`.

## The corpus goes stale, whichever option is chosen

Trials open and close. "Recruiting" as of the dump date is not recruiting forever, and a stale
corpus is a quiet failure — the app looks like it is working. Whatever seeds the corpus, there
has to be an answer for refreshing it. The Process Trials page is that answer today; a
scheduled refresh is the eventual one, and nothing in this project runs on a schedule yet by
deliberate choice.

## Sequencing

**`PAYLOAD_HASH_PLAN.md` changes the economics of every option here.** Today a re-pull
re-normalizes the entire corpus and costs the same as the first pull, because every
already-normalized trial is unconditionally re-queued. With payload hashing, a refresh costs
only the trials that actually changed — which makes "seed once, refresh periodically" viable
and makes options 2 and 3 far less painful.

Recommended order:

1. ✅ **Option 2 now** — zero code, unblocks a deployment immediately. **Done** — it is Phase 4
   of the deploy runbook.
2. ✅ **Payload hashing** — during a rebuild that is happening anyway, since it needs one.
   **Done**, shipped in `6043833`.
3. ⬜ **Option 1 for production** — once there is a corpus worth capturing and a cheap way to
   keep it current. **Still outstanding**, and both preconditions are now met.

⚠️ **The unanswered question option 1 runs into is the chunk key.** A Qdrant snapshot is only
valid against the exact MySQL rows it was built from, because chunk payloads key on `trialExtid`
and extids regenerate on every rebuild. Keying chunks on `nctId` instead — globally stable, so a
snapshot survives a rebuild independently — is the structural fix, and it is a chunking change,
so adopting it later costs a full `force=true` re-index. That cost grows with the corpus.
