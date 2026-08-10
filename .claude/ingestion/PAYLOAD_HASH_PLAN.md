# Skip Unchanged Trials on Re-Pull — Change Plan

Make a repeat ingestion cost time proportional to **what changed**, not to corpus size.
Written 2026-08-10 from a read of `ClinicalTrialsGovIngestJob`, not from the design docs.
Companion to `../CURRENT_STATE.md` and `DEPLOYMENT_SEEDING.md`.

**Status: planned, nothing built.**

## The problem, measured

Normalization is the whole cost of an ingestion run. On 736 rows: fetch ~1.1s (0.4%), staging
~0.7s (0.3%), normalization ~257s (**99.3%**). About 349ms and ~38 DB round trips per trial.

The staging loop has three branches, keyed on whether a row was already normalized:

| Row state | Action today | Counted as |
| --- | --- | --- |
| Never staged | insert | staged |
| Staged, not yet normalized | skip — avoids a duplicate row | skipped |
| Staged, **already normalized** | **refresh payload + re-queue** | staged |

That third branch is correct for freshness and expensive for everything else. **Every
already-normalized trial is unconditionally re-queued**, so a second full pull re-normalizes
the entire corpus and costs the same as the first — roughly 15 minutes for 2,500 trials. The
skip counter reads near zero on a re-pull, not near the corpus size.

The refresh is right in principle: a trial that moved from RECRUITING to COMPLETED must be
updated, and this is what makes re-running keep a seeded corpus current. The waste is that the
same work happens for the overwhelming majority of trials where **nothing changed at all**.

## The change

Store a hash of the raw payload on the staging row. In the already-normalized branch, compare
the incoming payload's hash against the stored one:

- **Hash matches** — nothing changed. Skip. Leave `normalized_at` alone so the row never
  re-enters the pending queue.
- **Hash differs** — refresh and re-queue, exactly as today.

A re-pull then costs one hash per trial (microseconds) plus full normalization for only the
trials that actually moved.

## Why a hash and not a length

A byte length was the first idea and it is not safe. Payloads of identical length routinely
differ in the fields that matter most:

- `RECRUITING` → `SUSPENDED` — same character count
- `2026-08-01` → `2026-09-01` — same character count

A length check would report "unchanged" for precisely the changes worth catching, and it would
do so silently. **A false unchanged is the worst failure mode available here** — the corpus
goes stale and nothing surfaces it. SHA-256 costs the same order of magnitude to compute and
has no practical collision risk.

## The thing to verify first — checked 2026-08-10, **passes**

**Does the CT.gov payload contain any field that varies between fetches when nothing
meaningful has changed?**

A per-request timestamp, a response-scoped id, or a field ordering that is not stable would
make every hash differ on every pull. The change would then be an expensive no-op: full
normalization cost, plus a hash computation, plus a schema migration, for zero saving.

**Verified against the live API and the answer is no.** Two fetches of
`/studies/NCT05753657`, seconds apart, returned byte-identical responses. Two fetches of the
search endpoint the ingest job actually uses (`/studies?query.cond=breast+cancer&
filter.overallStatus=RECRUITING`) were also byte-identical, and each of the five individual
study objects — which is the unit the job hashes — compared equal under key-sorted
serialization. No per-request timestamps, no response-scoped ids, no unstable key ordering.

Worth re-checking if CT.gov ever changes their API version. If unstable fields do appear
later, the fallback is to hash a normalized projection of the payload with the offending
fields stripped, rather than abandoning the approach.

## Scope

| Layer | Change |
| --- | --- |
| Changeset | `staging_raw_trial` gains `payload_hash varchar(64)`, nullable |
| Entity | `StagingRawTrialDb` — field + `@Column(length = 64)` |
| Domain | `StagingRawTrial` — plain field |
| DTOs | Request create/update, response — follow the existing pattern |
| DbService | `create(...)` and `refreshForRenormalization(...)` accept and persist the hash |
| Ingest job | Compute the hash; compare in the already-normalized branch |
| Tests | Repository/DbService/mapper coverage for the new column, plus the branch logic |

Use the `database-column-change` skill for the column work — it covers every layer that pins a
column's shape and knows this project's rebuild trap.

**Nullable on purpose.** Rows staged before this change have no hash. A null must mean
"unknown, so refresh" — falling back to today's behavior — rather than "unchanged". Getting
this backwards would freeze the existing corpus permanently.

## Implementation detail

### Where the hash is computed

In the ingest job's staging loop, from the same string that is already being persisted — the
result of the client's raw-JSON call, which is what the loop hands to the DbService today.
Hashing that exact string, rather than re-serializing the parsed node separately, is what
guarantees the stored hash always describes the stored payload. Two separate serializations of
the same node can differ in whitespace or key order and would produce a hash that does not
match its own payload.

SHA-256 via `java.security.MessageDigest`, UTF-8 bytes, rendered as lowercase hex. No new
dependency — the JDK covers this. A small private helper in the ingest job is enough; it does
not belong in `:common` unless a second call site appears.

### The branch change

The already-normalized branch currently refreshes unconditionally. It gains one comparison
ahead of that: when the stored hash is non-null and equal to the freshly computed one, count
the row as skipped and tick the skip glyph, leaving `normalized_at` untouched so the row never
re-enters the pending queue. Otherwise fall through to the existing refresh call.

The two other branches are unchanged in behavior — the insert branch simply also persists the
hash it computed, and the still-pending branch continues to skip, since that row is already
queued and its payload will be normalized regardless.

### Signature changes

Both write paths need the hash threaded through:

- The positional `create(...)` overload on `StagingRawTrialDbService` — the one the ingest job
  calls, not the Domain-object overload — gains a hash parameter.
- `refreshForRenormalization(extid, rawPayload, fetchedAt)` gains one too, and must set it
  alongside the payload. **A refresh that updates the payload but not the hash is the
  characteristic bug here**: the row would then carry a hash describing the previous payload,
  and the next pull would compare against a stale value and skip a trial that had in fact
  changed. Payload and hash must be written together, always.

The Domain-object `create(StagingRawTrial)` overload needs nothing beyond the new field, since
it maps whatever the domain object carries.

### Counters and the progress bar

A skipped-because-unchanged row must tick exactly one glyph, like every other path through the
loop. The existing skip counter is the right home for it — it already means "seen this, did
nothing". Note the result modal's wording then becomes load-bearing in a new way: on a re-pull
the skipped count will be large and the saved count small, which is the intended outcome and
reads as a no-op if the labels do not explain it.

Watch for the failure mode the ticker exists to surface: a record that reaches neither counter
disappears from the summary entirely. Wiring the ticker originally found exactly that bug in
this same loop.

### What to assert in tests

- Identical payload on a re-pull → skipped, `normalized_at` preserved, no refresh call
- Changed payload → refreshed, `normalized_at` cleared, hash updated to the new value
- **Null stored hash → refreshed**, never skipped — the pre-existing-rows case
- Refresh writes payload and hash together, so the two can never disagree
- The same payload string hashes identically across calls (guards against accidentally hashing
  a re-serialization rather than the stored string)

## The rebuild cost

Editing an applied changeset breaks its Liquibase checksum, and `spring.liquibase.drop-first`
is `false` here. So this needs a database rebuild via the n8n `clear-db` webhook, which is the
user's action. A rebuild drops all ingested trials, invalidates the Qdrant collection, and
costs a full re-ingest — so **do this when the corpus is already due to be rebuilt**, not as a
standalone change on a populated database.

## What this does not fix

**Backfill is a separate endpoint with the same shape of problem.** `POST /api/rag/backfill`
reads from MySQL and embeds; whether it re-embeds trials whose text has not changed is its own
question with its own answer, and embedding is far more expensive per trial than normalizing.
The same hash could plausibly gate it — a trial whose payload hash is unchanged since its last
successful index does not need re-embedding — but that is a second change against different
code, and the retrieval consequences of skipping have not been thought through here.

**The fetch does not get cheaper.** Every study payload is still downloaded to discover that
most are unchanged. `ClinicalTrialsGovClient` also accumulates every study in a
`List<JsonNode>` before staging any of them, so a large pull holds the whole response in heap.
Streaming per page is the fix for that, and it is unrelated to this plan.

## Sequencing

This optimizes a **re-pull**. It is worth nothing until there is a corpus to re-pull, and its
migration is cheapest to absorb during a rebuild that was happening anyway. Do it after the
corpus exists and before any routine refresh cadence is established — not before the first
ingestion.
