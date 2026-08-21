# Current State — Clinical Trials Finder

Where the project stands, what is deliberately unfinished, and what that blocks. Companion to
`PROJECT_PLAN.md` (the overall plan) and `_archive/clinical-trials/clinical-trials-tables.md`
(schema design). This is the "where are we right now" view — update it as things change rather
than keeping it as history.

**Last verified against the code: 2026-08-21.** **Everything in "What's built" further down is
engineering narrative kept for its reasoning and is dated where it happened — treat those
sections as history, not as current state.**

⚠️ **Runtime numbers below the "Picking the project back up" table are stale unless marked
otherwise.** Prod contents were last observed 2026-08-11. Local figures were re-measured
2026-08-21 and are in the table.

---

## Picking the project back up

**Measured 2026-08-21** unless noted:

| | |
| --- | --- |
| Trials in MySQL | **2,473** — the corpus was rebuilt and re-pulled this day |
| Qdrant | **71,712 points**, 2,473 distinct trials, **zero orphans**, 384 dims, Cosine |
| Curative-intent + stage IV | **38 trials (1.54%)** — see `matching/CURATIVE_STEP1_MEASUREMENT.md` |
| Early-stage trials | **823 (33.3%)** — a third of the corpus cannot apply to a metastatic patient |
| Patient / PatientDiagnosis / PatientVariant / PatientPriorTreatment | 1 row each — **auto-seeded** |
| Tests | `:common` **43**, root **128**, `:database` **862**, `:datafetcher` **55**, `:rag` **44** (1 skip: `RetrievalEvaluation`, needs a live backend by design) |
| Branch | `curative-work`, branched from `main` at `82e299b`. `main` is pushed and in sync with `origin/main`. |

⚠️ **The corpus shrank on purpose.** It was 4,634 trials, then a database rebuild regenerated
every extid while Qdrant kept its 136,345 points — leaving **4,634 orphans out of 4,884 indexed
trials**, so search matched chunks pointing at trials MySQL no longer had. That surfaced as a 500
on 2026-08-21; the collection was recreated and the corpus re-pulled to 2,473.

⚠️ **Nothing in the app detects that condition.** It has now happened three times. A startup check
comparing Qdrant's distinct trial count against MySQL's would catch it in one line at boot — see
`TODO.md`.

### 🚀 DEPLOYED — https://breastcancertrialfinder.com, 2026-08-11

**The app is live on a public host with her real record on it.** HTTPS via Let's Encrypt (expires
9 Nov, `certbot.timer` armed), HTTP redirects to HTTPS, both apex and `www`. She can sign in as
`jeb` and open "Trials for You".

| | |
| --- | --- |
| Host | Hostinger KVM, `ssh cancer`, 93.127.216.49, **1 vCPU / 3.9GB** |
| App | `/opt/cancer/cancer.jar`, systemd `cancer.service`, **port 8081** |
| Why 8081 | `/opt/cpss/cpss.jar` has owned 8080 for weeks — do not stop it |
| Also on the box | MySQL, 2 Ghost containers, 8 nginx sites. **Not a dedicated box.** |
| Swap | 4GB added (there was none), `vm.swappiness=10` |
| Qdrant | container up, collection green, **0 points** |
| Patient record | seeded from the 3 CSVs; prod AppUser extid regenerated |
| Corpus | **EMPTY — this is the remaining work** |

⚠️ **"Trials for You" returns nothing until the corpus is pulled.** Phase 4 of the runbook.
The ~14-minute MySQL pull is all that page needs; the embedding backfill (hours on 1 vCPU) only
powers semantic Trial Search.

✅ **The login allowlist shipped** in `1b663cb` ("Hash passwords on user create/update, and add a
login allowlist"). Whether `LOGIN_ALLOWED_USERNAMES` is actually set in `/opt/cancer/.env` on the
server is a *deployment* question this document cannot answer — check the box, not the code.

Full procedure and every correction found by doing it: `hosting/DEPLOY_RUNBOOK.md`.

### Do this first when you return

✅ **`frontend-mobile` is merged and pushed.** `main` and `origin/main` are in sync as of
2026-08-21. Work since then sits on `curative-work`.

**Check whether the corpus was ever pulled on the server.** As of 2026-08-11 prod had 0 Qdrant
points and "Trials for You" would return nothing. Nothing in the code tells you whether that was
done since — verify against the running site rather than trusting this line.

⚠️ **Set `PATIENT_SEED_DIR` or confirm the default.** The patient CSVs moved to
`.claude/_archive/patient-data/` and the seed loader's default followed them. A wrong path here
fails silently by design — a missing directory is not an error — so the symptom is a patient
record that does not come back after a rebuild. There is now a test pinning the two defaults to
each other and to the files on disk.

⚠️ **`LOGIN_ALLOWED_USERNAMES` is empty**, so the seeded `admin` account can still log in. The
allowlist code shipped in `1b663cb`; setting the variable is a deployment action. Check the box,
not the repo.

✅ **The Rank Trials page is built and wired.** `TrialMatchingController`,
`ResponseTrialAssessment`, and `frontend/src/pages/RankedTrials.tsx` all exist; the REST boundary
this section used to list as missing was closed in `3152d32` and `9e9719e`. The
backend-vs-frontend decision is settled: matching is in the backend, in code.

⚠️ **Run `CorpusSweep` after any pattern change.** The 41 unit tests passed while the treatment
signal was producing 550 false concerns; only the corpus caught it.

### The retrieval question is answered: it was the corpus, not the model

Measured 2026-08-10 against the full 4,634-trial corpus, by querying `/api/rag/search`
directly. **`RetrievalEvaluation` itself could not be run to completion** — see the warning
below.

| Query | Old (249 trials) | Now (4,634) |
| --- | --- | --- |
| "trials studying a BRCA mutation" | 0.388 | **0.930** |
| "recruiting trials I could join now" | 0.526 | 0.600 |

**BRCA now matches a chunk whose literal text is "BRCA mutation".** The old corpus simply did
not contain BRCA criteria to find. **A bigger embedding model is not needed** — that was the
expensive path this measurement existed to rule out. Seven of the eight asserted cases score
≥0.969, most at 1.000.

**The colloquial query barely moved** (0.526 → 0.600), and that one does look like a real model
weakness with conversational phrasing rather than a corpus gap. Relevant if patients ever type
queries directly.

⚠️ **One asserted case now fails, and the test is probably the thing that is wrong.**
`"no prior chemotherapy"` returns `INCLUSION_CRITERION` at 1.000 where the case expects
`EXCLUSION_CRITERION`. The expectation was written when prior-chemo limits were only seen
phrased as exclusions; the larger corpus contains treatment-naive first-line trials that state
it as an inclusion. **That is the better answer, and it is the clinically relevant half of the
corpus for this patient** — she has had no cytotoxic chemotherapy ever. Decide whether the case
should accept either source before changing a threshold.

⚠️ **Caveat on all of the above.** The corpus is 45.5% breast, not pure breast, because a
mistaken first pull loaded 2,500 general-cancer trials and the decision was to leave them and
pull breast on top. A pure breast corpus would be denser still.

### The corpus pull — what went wrong and what fixed it

Worth reading before the next pull, because two of these will recur.

**The first pull loaded the wrong corpus.** The Process Trials condition box started empty,
and an empty box falls back to `cancer.ingestion.clinicaltrials.condition`, which defaults to
`cancer`. So it pulled 2,500 arbitrary trials off the top of an 18,773-trial result set —
11.6% breast. **Fixed**: the box now pre-fills with `breast cancer`, and the confirmation
dialog says "all cancer types" instead of "default" if you clear it. `maxStudies` still
defaults to 1,000 in the form and must be raised by hand.

**The backfill hit three separate failures, none of them the code being wrong:**

- **Qdrant crash-looped on `Too many open files`.** The container had Docker's default
  `nofile` of 1,024 while the host allows 1,048,576; Qdrant memory-maps every segment, and at
  ~40,000 points across 32 segments it panicked in an actix worker and aborted. `restart:
  unless-stopped` turned that into a loop — recover the collection, exhaust descriptors,
  abort. **Fixed**: `ulimits: nofile: 65536` in `docker-compose.yml`. Held through 78,000+
  points with 0 restarts. Whether 65536 is enough at full corpus size is still unproven.
- **The backend process died mid-run**, taking the job with it, at ~58%. Cause unknown — the
  console output would have said. If it recurs, suspect JVM heap: embedding runs at ~240% CPU
  and the machine has 58 GB, so system-wide pressure is unlikely but a heap ceiling is not.
- **A long-held HTTP request is fragile.** Both the frontend button and a plain `curl` hold
  one request open for the whole run. An early failure reported as `HTTP 000` after 420s was
  read as a client timeout; it was actually the Qdrant crash. An async job endpoint is the
  real fix and does not exist.

**None of these lost work**, because of the skip check below.

### ⚠️ Do not run Gradle while the backend is running

**This cost more time than any actual bug today.** The app runs with `spring-boot-devtools`,
which watches build output and hot-restarts on change. Any `./gradlew` invocation — even
`:rag:test` — rewrites those outputs, so devtools restarts against a half-written classpath and
the app dies. It surfaced twice with different messages, both of which look like real
configuration faults and are not:

- `Not a managed type: class ...UserDb`
- `ClassNotFoundException: ...PatientSeedProperties`

Both arrive on a `restartedMain` thread, which is the tell. The fix is a full restart, and
`./gradlew clean build -x test` if the outputs are genuinely inconsistent.

**This also made `RetrievalEvaluation` look broken.** Every attempt to run it via Gradle killed
the backend the test then failed to reach, producing `ConnectException` from the forked test
JVM while `curl` from a shell succeeded seconds earlier. Two wrong diagnoses came out of that —
test-JVM network isolation, and a too-short `connectTimeout`. The real cause was the test run
itself. **Either stop the backend before running Gradle, or measure retrieval over the REST API
instead**, which is how the numbers above were obtained.

### The patient rows now seed themselves

**This is new and it is the fix for the worst repeat-pain of a rebuild.** `PatientSeedLoader`
(root, `service/`) is a `CommandLineRunner` that recreates `AppUser`, `PatientDiagnosis`,
`PatientVariant` and `PatientPriorTreatment` on startup from gitignored CSVs in
`_archive/patient-data/`. **Verified working through a real rebuild on 2026-08-09** — all four
rows came back automatically with correct values.

Four rules it follows, each deliberate:

- **Seed if absent, never sync.** An existing row is left completely alone, so edits made
  through the UI survive a restart. The CSV is a floor, not a source of truth.
- **Keyed on username, not extid** — extids regenerate on every rebuild.
- **A missing file or directory is not an error**; the app boots fine without patient data.
- **A malformed seed never blocks startup.**

Settings live under `cancer.seed.patient.*` in `application.yml`; set `enabled: false` to skip.

⚠️ **The CSVs are the source of truth now.** If you edit patient data through the UI, the
change lives only in the database and the next rebuild reverts it to whatever the CSV says.
Update the CSV too, or accept the loss.

⚠️ **Vocabulary drift is the failure mode to watch.** The diagnosis CSV originally held
`AJCC`, `POSTMENOPAUSAL` and `HR+/HER2-` where the frontend expects `AJCC_8`, `POST` and
`HR_POSITIVE_HER2_NEGATIVE`. The backend stores plain varchars so it accepted them, but the
dropdowns rendered blank and a save would have silently cleared the fields. Fixed 2026-08-09;
check any new CSV value against `frontend/src/types/api.ts`.

### New clinical data — 2026-08-09, from the real record

A **UCHealth My Health Summary PDF** (generated 2026-08-08, 35,808 lines of extracted text)
was read and mapped into all three patient tables. It is at
`_archive/patient-data/my-health-summary.pdf`, gitignored, `600`.

⚠️ **It arrived in `.claude/diagnosis/`, which is NOT gitignored, world-readable, and one
`git add .` from being committed.** Moved on arrival. This is the second time a real medical
record has landed in a tracked directory — the PET/CT report did the same on 2026-08-08.
**Any new patient document goes straight to `_archive/patient-data/` before it is opened.**

What the record established, beyond what was already recorded:

- **Germline testing: a multi-gene panel, negative for pathogenic variants.**
  BRCA1, BRCA2, PALB2, ATM and CHEK2 are therefore `NOT_DETECTED`, not blank. This is the
  distinction the five-state vocabulary exists for: it moves PARP-inhibitor trials from
  *open question* to *genuine mismatch*.
- **Precise receptor values**, at the level of detail a pathology report carries.
  HER2 IHC low-positive, DISH not amplified.
- **Histology and stage**: invasive ductal carcinoma, with a full AJCC staging code.
- **ECOG 0 → 1** per the recent oncology note.
- **Palliative radiation** to named sites, left sacroiliac
  joint, and right proximal femur. Not previously recorded.
- **A bone-modifying agent, started several months into treatment.**
- **Corrected drug start dates** — two different dates for the same drug (an earlier
  note in the same record says 3/31 — unresolved).
- **A follow-up scan showing mixed response.** Some sites resolved,
  osseous lesions mixed, some larger and more avid. Progressive asymptomatic right femoral
  neck metastasis, fracture risk.
- **A documented decision to continue the current regimen rather than pivot to a PI3K
  inhibitor.** So `pi3kAktMtorStatus` is `NEVER` *despite* the PIK3CA mutation — she is
  PIK3CA-mutant and PI3K-inhibitor-naive, which is an inclusion criterion for a whole class
  of trials.
- **Confirmed: no cytotoxic chemotherapy, ever.** The neoadjuvant AC-then-taxane regimen was
  planned early on and abandoned when staging found metastatic disease. Every "chemo"
  mention in the record is supportive care or that abandoned plan.

**Two unresolved conflicts, recorded in the notes fields rather than silently resolved:**

- **A proliferation index (Ki-67) disagreed by several-fold between two notes in the oncology and
  radiation notes.** A 4.5× discrepancy on a proliferation index is not rounding. The user
  chose 45%; both values are in the notes. Worth asking the oncology team.
- **A drug start date** appears as two different dates in the same record.

**The tool was used for a real person for the first time (2026-08-08).** See
"First real search" below — it worked, and it surfaced a concrete design gap that reorders
the candidate list.

### First real search — 2026-08-08

The patient's real diagnosis is now in `patient_diagnosis`: de novo **stage IV invasive
carcinoma of the left breast**, **ER+ / PR− / HER2−**, **PIK3CA mutation detected**,
postmenopausal, ECOG 0, bone and extensive nodal metastases, on **abemaciclib (Verzenio) +
letrozole** since April 2026 with **no prior cytotoxic chemotherapy**. Sourced from an MRI
plus details supplied directly by the user.

A semantic search over all 249 trials using that profile returned real, relevant matches —
most notably **NCT05753657**, which matched at 0.717 on *"ER positive HER2 negative
metastatic breast cancer, harboring an activating PIK3CA mutation"*. That is the patient's profile
line for line, including the biomarker.

**But the top-scoring hit was wrong, and the reason matters.** NCT06685796 scored highest
(0.718) on the criterion *"HR-**negative**, HER2-negative"* — triple-negative disease. She
is HR-**positive**. Two more of the top ten (NCT07045311 triple-negative, NCT06770296
HER2-**positive**) got in the same way.

**Embedding similarity cannot distinguish receptor polarity.** "HR-negative HER2-negative"
and "HR-positive HER2-negative" differ by one token inside an otherwise identical phrase,
and the model scores them as near-identical. This is a known weakness of embedding models
around negation, not a tuning problem — no similarity threshold fixes it.

Receptor status gates **36% of this corpus** (HER2) and **28%** (ER/PR). So pure retrieval
is not sufficient for Tier 2, and the fix is already available: `er_status`, `pr_status`
and `her2_status` are structured columns on `patient_diagnosis`. Tier 2 must combine
retrieval with a structured receptor filter or re-rank rather than ranking on similarity
alone. `DIAGNOSIS_MATCHING_DESIGN.md` §4 anticipated combining them; this run is the
evidence that it is mandatory.

Consistent with the no-verdicts rule: a receptor mismatch should **demote and flag** a
trial, never silently remove it. Those three trials are wrong for her today, but receptor
status can be re-tested and the tool should not make that call for her.

### The unfinished task — resolved, kept for context

> **Updated 2026-08-14.** Both fixes listed below shipped: strict receptor polarity landed with
> the Tier 2 evaluator (`89f3960`) and the US location filter is a signal on every assessment.
> This section is history now, not a to-do.

**The user was trying to answer a real question about a family member's trial options and did not
get there.** The blocker was tooling, not design.

What was in flight when the session ended:

1. ✅ **Pull the corpus** — done 2026-08-10. 4,634 trials in MySQL, 2,108 of them breast.
   Backfill was still running at session end; see "Do this first" above.
2. **Re-run the match with two fixes the user approved** — still outstanding, and now
   unblocked:
   - **Strict receptor polarity.** The current filter accepts any trial whose criteria
     mention "HER2-negative" anywhere, so NCT07371585 — a HER2-**positive** first-line
     trial — scored 0.8333 against a HER2-negative patient. Reject trials *requiring*
     HER2-positive or triple-negative disease.
   - **US-wide location filter.** The user will travel anywhere in the USA. Geography was
     not part of matching at all, and the top-ranked trial (NCT05753657) has exactly one
     site, outside the US. `location` carries `trial_id` directly; no join table needed.

**On the scoring, which the user correctly noticed never reached 100%:** `top_score` was
`signals_matched / 6`, and 6/6 is unreachable by construction — a trial cannot be both
"first-line treatment-naive" and "post-CDK4/6 progression". Worse, the score counts keyword
co-occurrence in criteria text, not whether she qualifies. It is a reasonable *shortlist*
and a poor *ranking*. A real percentage needs Tier 3 (parse each criterion into a testable
predicate, evaluate against her structured fields, report pass/fail/unknown per criterion).

### Suggested next move after that

**Tier 2 matching with a structured receptor filter.** The data is real and the gap is
measured rather than hypothetical — see "First real search" below.

The two security items under *Deploying to QA* remain right regardless, and matter more
now that the database holds a real medical record rather than sample data.

---

## What's built

### Treatment goal and disease stage — 2026-08-21

**The first signals about what a trial is trying to do, rather than who it will enrol.** Every
signal before these answered "does she qualify"; none asked what a trial would be trying to
achieve for her. `CriteriaSignalEvaluator` now has **seven** signals, not five.

The request was specific: *"Trials that are trying to cure stage 4 cancer. They are out there but
they are few. They are the primary ones I am trying to find."*

**Measured before building, and the measurement overturned the plan.** Full numbers in
`matching/CURATIVE_STEP1_MEASUREMENT.md`. Across all 2,473 trials, **38 (1.54%)** are
metastasis-directed or curative-intent studies for stage IV disease — the "low tens" the plan
called success.

- **Response-endpoint vocabulary was excluded entirely.** The plan named "complete response" and
  "disease-free survival" as the workhorse. 232 trials use it; **5 of 5 hand-checked were
  describing how an outcome is measured**, not what the study aims at. Two were adjuvant trials.
- **Ablative language is the signal**: 26 of the 38, at near-perfect precision. It names something
  being done to a metastasis, so it cannot appear in an endpoint definition or a patient history.
- **Every cure-language false positive was a negation** — "considered non-curative", "unlikely to
  be cured", "are not curative". One token separates those from the real thing, the same reason
  embeddings cannot read receptor polarity. A 40-character lookbehind handles it.

⚠️ **Two pattern bugs surfaced that no amount of review would have caught.** `metastases` did not
match `metastatic`, which silently dropped **the single clearest curative trial in the corpus**
while the distribution looked healthy. And `resectable` matched inside `unresectable`, vetoing a
metastatic trial as early-stage. **That word-boundary bug is now its third occurrence in this
project** — check any new pattern against the words that contain it.

**Treatment goal ranks above concern count, and that placement is the feature.** Curative trials
are ~1.5% of the corpus, so ranking on concerns alone meant a well-matched disease-control trial
with zero concerns outranked a curative trial with one, every time. Identifying them correctly
and leaving them ranked 40th would not have delivered anything.

**Verified live**: NCT04563507 ranked **first** — SBRT to each metastatic lesion on
a CDK4/6-inhibitor-plus-aromatase-inhibitor backbone, matching the patient's own regimen and disease pattern. NCT05334459 (LRT with curative intent,
bone-only metastatic) also surfaces.

**Both are stored columns**, `trial.treatment_goal` and `trial.disease_stage` (changesets `031`,
`032`), so the 38 can be queried rather than only appearing inside a ranking run.
`TrialTextClassifier` lives in `:common` because `:datafetcher` stamps the values at
normalization and root reports them as signals, and datafetcher cannot see root — one copy of the
patterns, not two that drift.

⚠️ **Ingestion cannot repopulate them.** It skips trials whose payload hash is unchanged, so a
re-pull picks up nothing when only the *code* changed. `POST /api/matching/backfill-treatment-goals`
(ADMIN-only) re-derives both, and there is a "Recheck Treatment Goals" button on Process Trials.

### Semantic search reached the frontend — 2026-08-21

**Trial Search had no semantic search at all.** It fetched 200 trials and filtered substrings in
the browser. It now has two modes, and "By meaning" queries the vector store.

`criteriaOnly` restricts matching to eligibility-criteria chunks. Measured on a whole-profile
query: **15 of the top 25 hits were trial-design prose** — "first-in-human, open-label, phase
I/Ib" repeated across unrelated trials — crowding out the criteria that decide who can join.
Filtering removes them. Off by default, since prose is the right answer to "what is this trial
testing".

⚠️ **The first live call returned 500, and two guesses at the cause were both wrong.** The backend
log named it in one line: `ServiceException: TrialDb with extid=... not found`. Retrieval was
working; hydration hit an orphan chunk. **The guard for that already existed and could never
fire** — it checked for null, but `findByExtid` throws. One stale chunk failed the whole search.

⚠️ **`TrialIndexService.reindexTrial` has the identical dead guard**, so
`POST /api/rag/reindex/{extid}` with an unknown extid still 500s. Tracked as TODO 2b.


### Tier 2 matching — service layer, 2026-08-11

> **Status update 2026-08-14:** this section was written when the service layer was uncommitted
> and unreachable. It is now committed and wired end to end — controller, converter, response
> DTO and page all exist. The design reasoning below still holds; the "nothing calls it" framing
> does not.

Two decisions the previous session left open are settled in code. **Matching lives in the
backend**, not the frontend where Tier 1 sits — it has to be testable against the whole corpus
in bulk, inform ranking, and be reusable by Tier 3, none of which the browser can do. And
**there is no fit score**: `TrialAssessment` exposes concern/unknown/pass/applicable counts
instead of the old `signals_matched / 6`, which was unreachable by construction and counted
keyword co-occurrence rather than whether the patient qualifies.

Four signals: receptor polarity, treatment line against CDK4/6 history, PI3K pathway, and US
location. Each returns the criteria phrase that produced it, so a flag is never an unexplained
verdict.

✅ **The exclusion-context check now exists — 2026-08-11.** It was described in the class's own
javadoc and had never been written; criteria were read as one flat string, so a phrase meant
the same thing whether the trial required it or ruled it out.

**It reuses `EligibilityCriteriaChunker` from `:rag` rather than re-parsing.** Root already
depends on `:rag`, so no new module edge was needed. That parser is the one producing the
`isExclusion` metadata retrieval filters on, tuned against 50 real trials and surveyed across
all 4,634 — so there is one parser to keep correct, and a block this evaluator cannot attribute
to a section is the same block retrieval cannot attribute either.

What changed in behaviour:

- **A trial that excludes triple-negative disease is now a PASS**, not a concern. It was the
  worst case: TNBC is tested first and returns immediately, so ruling it out demoted a trial
  this hormone-positive patient actually fits.
- **Excluding HER2-positive disease reads as a HER2-negative requirement**, which is how many
  trials state it. The old `!her2NegAlsoMentioned` guard suppressed that false concern only
  when the trial happened to also write "HER2-negative" somewhere — **coincidence, not
  design**. An exclusion with no such phrase slipped straight through.
- **Prior CDK4/6 under Exclusion is a bar, not a requirement.** Previously read as a
  post-CDK4/6 trial.
- **UNPARSED blocks are read as inclusion**, unchanged from before. Legacy
  "DISEASE CHARACTERISTICS:" records never stated a division and inventing one would invert
  text that never said so.

⚠️ **`POST_CDK46` misses the most common phrasings, found while testing this.** It requires
"prior" adjacent to "CDK", so *"prior treatment with a CDK4/6 inhibitor"*, *"received a CDK4/6
inhibitor"* and *"any prior therapy with a CDK4/6 inhibitor"* all miss — only the terse *"prior
CDK4/6"* hits. Inside an exclusion section this no longer matters, since naming the class is
itself the bar and a separate `CDK46_MENTIONED` pattern handles it. **On the inclusion side the
gap is still open**, so a post-progression trial using the common wording reports NOT_APPLICABLE
instead of comparing against her CDK4/6 history.

**52 tests cover the evaluator**, including the six inversion cases, the phrasings above, and
six real trials the corpus sweep flagged wrongly.

### Treatment-line signal bound to the drug class — 2026-08-11

Found by the corpus sweep below, not by review. `TREATMENT_NAIVE` matches "first-line" and
"untreated", and it was read against a whole concatenated section — so **any** trial in **any**
disease saying "previously untreated" was compared against her CDK4/6 history. Result:
**550 concerns and zero passes** across 4,634 trials, with sampled flags reading *"no prior
treatment for their DLBCL"*, *"Relapsed AML"*, and *"untreated with anti-tumor therapy for
rectal cancer"*.

Two changes fixed it. **Patterns now co-occur within a single criterion**, not a section —
`sectionsOf` returns the individual criteria and `firstCriterionMatching` requires both patterns
in one of them. A section is every criterion concatenated, so co-occurrence there implies
nothing. And a new `RELEVANT_THERAPY_CLASS` pattern requires CDK4/6, one of its three drugs, or
endocrine therapy to be named in that same criterion — endocrine belongs there because she is on
abemaciclib **plus** letrozole, so an endocrine-naive requirement bears on her directly.

**Measured after: 550 → 160 concerns.** 390 false flags removed, and the remaining sample is
entirely CDK4/6 or endocrine.

A third adjacency bug surfaced in testing: `TREATMENT_NAIVE` required "prior" adjacent to
"therapy", so *"no prior endocrine therapy"* missed. Same trap as `POST_CDK46`, now fixed with a
bounded gap.

⚠️ **`Treatment history` still reports zero passes, and that is correct rather than broken.**
A pass requires `isNaive()` and she is not. The signal can only ever demote her — worth knowing
before it feeds a ranking.

### Known limitation: carve-outs inside exclusion criteria

**Deliberately not fixed — decision 2026-08-11.** An exclusion criterion naming CDK4/6 is read
as a bar, but ~15% of them contain a permission instead.

Measured across the corpus: **39 trials name CDK4/6 in an exclusion criterion.**

| Shape | Count | Flag correct? |
| --- | --- | --- |
| Hard lifetime bar | **29 (74%)** | Yes |
| Washout / recency window | 4 (10%) | Partly — a timing question, not disqualification |
| Explicit carve-out | 6 (15%) | **No — the criterion admits her** |

The carve-outs have no common keyword: *"may participate as long as..."* (NCT07060807),
*"except hormonal therapy in combination with a CDK 4/6 inhibitor"* (NCT07137871), a bare
parenthetical (NCT05362760). **NCT04523857 is the trap** — it carves out *"prior CDK4/6 therapy
with an agent other than abemaciclib"*, which still excludes her, so a naive carve-out rule
would turn a correct flag into a miss.

A keyword list for this would re-create the brittleness that produced the 550 false concerns,
on exactly the criteria where nuance decides eligibility. The considered alternative was
downgrading carve-out wording to UNKNOWN rather than CONCERN — honest under the no-verdicts
rule, since these are genuinely questions. **Deferred; the tool over-flags ~10 trials in 4,634
and that was judged acceptable against the risk of a wrong fix.**

⚠️ One diagnostic wrong turn worth not repeating: NCT07044310 was briefly recorded as a chunker
boundary bug. It is not — the chunker filed the line correctly, and the trial genuinely excludes
her (*"Receiving or will receive CDK 4/6 inhibitor"*, and it is a stage 0-III trial while she is
stage IV). The error came from locating lines with a substring search rather than by line index.

### Receptor patterns widened — 2026-08-11

`HORMONE_POSITIVE_REQUIRED` matched `HR`, `ER` and `hormone receptor` but not the spelled-out
`estrogen receptor positive` or `progesterone receptor positive`, so trials writing it longhand
scored NOT_APPLICABLE. **A missed PASS on this patient's most important axis** — she is ER
positive. Now matches the full words, the `(ER)`/`(PR)` parenthetical abbreviation form,
and `PgR`, which is the pathology-report spelling of progesterone receptor.

**`HER2[\s-]?(negative|-\b)`'s `-\b` branch never fired.** A hyphen followed by a space is not
a word boundary, so `HER2- metastatic` missed while `HER2-negative` hit via the other branch —
the alternative contributed nothing but looked like it handled the shorthand. The positive side
*did* handle `ER+`/`HR+`, so the asymmetry was invisible in review. Fixed with an explicit
lookahead.

Verified not to regress: `HER2 non-amplified` and `HER2 not amplified` still correctly miss the
HER2-positive pattern, so this patient's low-positive IHC record does not false-positive.

### The Rank Trials endpoint — 2026-08-11

`GET /api/matching/rank/{patientExtid}?breastOnly=&limit=` ranks the corpus against the record
already on file, best first. `GET /api/matching/trial/{trialExtid}/for/{patientExtid}` assesses
one trial, for Trial Detail. Both extid-only; controller plus package-private converter in one
file, per the existing convention.

> **Updated 2026-08-14:** these paths took `{appUserExtid}` when written. `AppUser` was dropped
> in changeset `030` and the parameter is now `{patientExtid}`, checked against the caller's
> grants via `CurrentUserService.requireAccessId(..., AccessLevel.VIEW_TRIALS)`.

**Verified live against the real record.** The top hits are genuinely the patient's profile — PI3K-pathway
HR+/HER2− breast trials with US sites — and every signal carries its quoted criteria text
through to the response.

**Ranking is lexicographic over honest counts**, since there is deliberately no score to sort
on: breast trials first, then fewest concerns, then most passes, then most applicable signals.
That last tier matters — a trial the tool could say something about outranks one it was silent
on, because silence is not a pass.

**`breastOnly` defaults to false and filters *before* assessment.** It is the one parameter that
hides trials, so per the no-verdicts rule it is an explicit caller choice rather than a default
the patient never sees. Filtering first also skips the expensive per-trial work on the ~54% of
the corpus that is other diseases; the pre-filter calls `diseaseTypeSignal` rather than
re-implementing it, so it cannot drift from the reported signal.

✅ **Resolved 2026-08-14.** When written, `SecurityConfig` was `.anyRequest().permitAll()` and
this endpoint returned a patient's assessment keyed on a guessable path. It is now
`.anyRequest().authenticated()`, and the path is checked against the caller's grants via
`CurrentUserService.requireAccessId(patientExtid, AccessLevel.VIEW_TRIALS)` — so a guessed extid
returns a permission error rather than someone's medical record.

### "Trials for You" — the page she actually uses — 2026-08-11

`frontend/src/pages/RankedTrials.tsx`, route `/ranked-trials`, **first in the nav, ahead of
Trial Search** — it is the one page that asks nothing of the reader. Trial Search requires
knowing what to type; this uses the Patient Record already on file.

**Runs on a button press, never on page load.** Ranking assesses thousands of trials in one
request and takes tens of seconds, so an unbidden spinner would read as a hang. The pending
state says "This can take up to a minute" for the same reason — for someone waiting, saying so
is the difference between "working" and "broken".

**Locations are on the card, directly under the trial title.** Requested by the user
2026-08-11: *"they need to travel to those cities"*. Travel is often what decides whether a
trial is possible at all — a perfect biological match three states away may be out of reach and
a mediocre one nearby may not be — so the cities belong beside the trial name, not inside a
signal a reader has to expand.

They were technically present before and effectively invisible: the location signal is a PASS,
and the page collapses passes behind "What matched", so she would never have seen them. Two
changes fixed that.

- `TrialAssessment` now carries `siteCities`, `siteCount` and `hasUnitedStatesSite` as
  structured fields, so the card renders them rather than parsing them back out of prose.
  `CriteriaSignalEvaluator.siteLabels` is shared with the signal, so the two cannot drift.
- The signal's own sample went from **3 cities to 8**, and the detail sentence now names them.
  "and 9 more" hid exactly the city that might be an hour away.

Non-US trials show their countries with an "Outside the United States" prefix in amber, rather
than appearing to have no locations. A trial with none says so plainly.

**Verified live 2026-08-11.** All 50 top-ranked trials return real cities — "Chicago, Illinois ·
Boston, Massachusetts · Las Vegas, Nevada and 3 more" — with zero missing. Ranking sorts US
trials to the top, so the non-US path had to be checked directly: **NCT05753657**, the
single-site trial (outside the US) that ranked first in the 2026-08-08 search, now reports
`hasUnitedStatesSite: false`, `siteCities: ["Israel"]` and a CONCERN, instead of silently
looking local. That trial is the reason geography became a signal at all.

Three decisions about what she sees, all following from the no-verdicts rule:

- **Counts, never a percentage.** "2 to check · 1 to ask about · 4 matched", not a fit score.
- **Concerns and open questions are shown; matches are collapsed behind "What matched".** A
  green checklist reads like an eligibility verdict, and this tool does not make that call.
- **Quoted criteria text sits behind a "why?" toggle on every flag.** A reader has to be able
  to check the reasoning rather than trust it, but the wall of trial text should not be the
  first thing they see.

An amber callout states plainly that this is a starting point for conversations, not medical
advice; that only her care team decides eligibility; and that **a trial is never hidden because
of a flag**.

"Only breast cancer trials" is a checkbox, on by default, mapping to `breastOnly`. It is the one
control that hides anything, so it is visible and reversible rather than silent.

Typecheck and production build clean. Lint has only the pre-existing `Login.tsx` error.

**Measured 2026-08-11, after the batch-location fix:**

| Call | Time |
| --- | --- |
| `breastOnly=true`, limit 50 (the default the page sends) | **4.3s** |
| `breastOnly=false`, whole corpus | 8.6s |

Down from 43 seconds. Repeat runs are stable at ~4.2s, so it is not warm-cache luck. The
"up to a minute" wording in the pending state is now conservative rather than accurate — worth
softening once it has been watched under real use.

### Locations are fetched in batches — 2026-08-11

The first live ranking call took **43 seconds**. `assess()` ran one location query per trial, so
ranking ~2,000 breast trials meant ~2,000 round trips — the same N+1 shape that makes
normalization 99.3% of an ingestion run.

`LocationDbService.findByTrialIds` now fetches them grouped by trial id, chunked at 500 to stay
under MySQL's placeholder limit, and `assessAll` passes the map down. The single-trial path is
unchanged and still pays one query. A trial with no locations is **absent from the map rather
than mapped to an empty list**, so "no locations recorded" stays distinct from "not asked
about" — which is what the location signal reports as UNKNOWN.

**Measured: 43s → 4.3s** for the breast corpus (~2,000 trials), 8.6s for all 4,634. Verified
live 2026-08-11; `:database`'s 820 tests re-run in full because this touched a shared
repository and db service.

### The first live run found a bug the sweep missed — 2026-08-11

**NCT05894239 requires HER2-positivity. She is HER2-negative. It scored zero concerns and ranked
4th.**

`HER2_POSITIVE_REQUIRED` matched `positive` but the trial writes **"documenting
HER2-positivity"**, so nothing fired. Now `positiv\w*`, and the negative side is `negativ\w*`
since `negativity` had the identical gap.

The section-wide `!her2NegRequired` guard was also removed in favour of a per-criterion test
that ignores criteria naming *both* polarities — those are stating a comparison
("HER2-negative or HER2-positive by local testing"), not a requirement. That guard was the same
coincidence-based logic flagged earlier, this time suppressing correct concerns.

⚠️ **The corpus sweep did not catch this and could not have.** The sweep measures distributions;
this was one trial in 4,634 and the aggregate looked healthy. Only ranking — putting the best
trials on top and reading them — surfaced it. **Ranking is a different test from sweeping, and
it is the one that matches how the tool is actually used.** Re-swept after the fix: 11 trials
moved PASS → CONCERN, exactly the expected HER2-positive reclassification.

### Tier 2 measured against the corpus — 2026-08-11

**The unit tests said the evaluator worked; the corpus said it did not.** 41 tests passed while
the treatment signal was producing 550 false concerns. The tests were written by whoever wrote
the patterns, against phrasings they chose — circular in the way that matters. This measurement
is what caught it, and it is the same measure-before-building step skipped on 2026-08-08.

`CorpusSweep` (root test sources) runs the real evaluator over every trial via `/api/trial`,
read-only. Off by default; enable with `-Dsweep.enabled=true`, size with `-Dsweep.limit=N`.
It prints an outcome distribution plus samples with evidence text for hand-checking — **the
distribution alone looks healthy while being wrong, so the samples are the point.**

Full corpus, 4,634 trials, against the real record (ER+/PR−/HER2−, PIK3CA detected, CDK4/6
CURRENT, PI3K NEVER):

| Signal | PASS | CONCERN | UNKNOWN | N/A |
| --- | --- | --- | --- | --- |
| Receptor status | 513 (11.1%) | 555 (12.0%) | 0 | 3,566 (77.0%) |
| Treatment history | 0 | **160 (3.5%)** — was 550 | 0 | 4,474 (96.5%) |
| PI3K pathway | 60 (1.3%) | 0 | 0 | 4,574 (98.7%) |

**Receptor signals hand-checked and correct.** Concerns are genuinely triple-negative or
HER2-positive trials; passes are real, including ones only the widened regex catches.

**The unparsed rate is 5.3% (244 trials)** — close to the 4% estimated from a 50-trial sample,
so that estimate was sound.

One thing the distribution exposes that is not a bug:

- **PI3K raises zero concerns** because the only concern branch needs `isRuledOut()` and she is
  `DETECTED` — unreachable for this patient. With 98.7% NOT_APPLICABLE the signal is near-silent,
  and its `PIK3CA|PI3K|AKT1|PTEN` pattern matched a BRCA/PTEN **germline carrier** line
  (NCT03729115) that has nothing to do with PI3K as a trial target. Narrowing it is open work.

### The disease-type gate — 2026-08-11

**Built in response to the sweep above**, which showed the other signals reporting 77-88%
NOT_APPLICABLE because they were being asked about colorectal, AML and DLBCL studies.
`diseaseTypeSignal` is now the first signal on every assessment.

**It reads the title and summary, never the criteria.** Criteria text mentions breast in passing
on pan-tumour studies — "solid tumors including breast" — so gating on it admits exactly the
trials the gate exists to catch. Title-or-summary reproduces the corpus's known 45.5% breast
share, which is the check that it reads the right fields.

**A concern, never a removal.** A basket trial can still be open to her, and per the no-verdicts
rule an off-topic trial demoted to the bottom of a list is recoverable where a deleted one is
not. Basket trials — three or more other tumour types named — report UNKNOWN rather than PASS or
CONCERN, since whether they are currently enrolling breast patients is a real question.

Measured across all 4,634 trials: **2,046 PASS (44.2%), 2,521 CONCERN (54.4%), 67 UNKNOWN
(1.4%)**. Hand-checked concerns are correct — colorectal, Barrett's oesophagus, oral lesions,
head and neck.

**What the other signals look like on the 2,046 breast trials**, which is the population a
ranked list would actually draw from:

| Signal | PASS | CONCERN | N/A |
| --- | --- | --- | --- |
| Receptor status | 473 (23.1%) — was 11.1% | 440 (21.5%) | 1,133 (55.4%) — was 77.0% |
| Treatment history | 0 | 142 (6.9%) | 1,904 (93.1%) |
| PI3K pathway | 46 (2.2%) | 0 | 2,000 (97.8%) |

**Receptor coverage roughly doubles** and its silence drops from 77% to 55%. The signals were
always working; they were being diluted by a corpus that is 54% other diseases.

⚠️ **10 trials in 4,634 are breast studies that never write "breast"** — surgical-technique and
mammography-outreach studies saying "mastectomy", "nipple-sparing", "axillary". Adding those as
proxy terms was considered and rejected: the same sweep found one of the 10 is a **lung** cancer
screening study, so the proxies import noise into the one signal whose job is removing it. None
of the 10 are treatment trials.

### Backfill skips what is already indexed — new 2026-08-10

**Backfill re-embedded the entire corpus on every run.** The pull side had skipped unchanged
trials by `payload_hash` since earlier the same day; the far more expensive side had no
equivalent. Every run cost ~130,000 local ONNX inferences whether or not anything had changed.

`TrialIndexService.isIndexed(trialExtid)` now probes the store per trial and the backfill skips
on a hit. Measured: re-running 50 indexed trials went from **14.9s to 0.4s**.

**The probe asks Qdrant, not MySQL.** An `indexed_at` column would be cheaper — one indexed
read instead of a network round-trip — but it can lie. A cleared collection or a DB rebuild
leaves MySQL claiming "indexed" against an empty store, and clearing the collection is routine
here because a rebuild invalidates every chunk's trial extid. The cached-state version fails
exactly when it matters.

**A failed probe returns false**, so an unreachable store re-indexes rather than skipping. The
opposite default would silently skip the whole corpus and report success.

**`?force=true` bypasses the skip**, and it is not optional. After a chunking change or an
embedding-model swap every stored vector is stale despite the trial being unchanged, and
skipping on presence would leave the corpus silently mixed — vectors from two models are not
comparable. That is jobs 3 and 4 in `TrialBackfillService`'s own contract.

**`trialsAlreadyIndexed` is reported separately from `trialsSkipped`** — nothing-to-do versus
nothing-to-index. The same split the pull side made between "unchanged" and "already waiting".
Surfaced in the result modal as "Already searchable".

⚠️ **Re-indexing with changed chunking leaves orphans.** `reindexTrial` writes the new chunks
but never deletes points whose ids no longer exist, and `deleteChunksFor()` exists but nothing
calls it. Caught when a re-index left 1,475 points against 1,450 written. Handled that time by
recreating the collection; unfixed in code.

### Two chunker bugs — new 2026-08-10

Both were silently degrading the index before this session, and both were found by chasing a
single error in a 50-trial test run rather than by review.

**Colliding chunk ids lost whole trials.** A chunk id hashes `trialExtid:source:ordinal`, and
the eligibility chunker restarts its ordinal at 0 for each criteria section. A trial with two
populations — `NCT07393529` has separate Patients and Social Network Members blocks, each with
its own Inclusion/Exclusion lists — produced two `INCLUSION_CRITERION` chunks at ordinal 0.
The store deduplicates by id before embedding, so 15 documents returned 10 embeddings and the
`add()` call rejected **the entire trial**, not just the duplicates. `NCT07219277` failed
identically in the 250-trial pull on 2026-08-08 and was recorded there under a different
guess. Fixed by renumbering per source across the whole trial.

**Unicode bullets merged criteria together.** The `BULLET` pattern matched `*`, `1.` and `a)`
but not `•`. An unmatched bullet line falls through to the continuation branch and is appended
to the criterion above it, so two unrelated criteria became one chunk and the glyph itself was
embedded. A survey of all 4,634 trials found `•` on 205 lines across 107 trials, plus a tail of
`▪ ● · ○`; 10 of those lines have no space after the bullet. **115 trials (2.5%) chunk
differently now, recovering 217 criterion lines.**

**Hyphens are still deliberately not matched** — 100 lines against 55,042 asterisk-led, and
hyphens appear mid-sentence constantly ("HER2-positive", "day 1-21"). Comparison symbols
(`≥ ≤ < °`) lead lines as content, not markers, and must not match either; there is a test.

### Progress ticker on the backfill — new 2026-08-10

The ~25-minute backfill produced **no output at all** until a single summary line at the end,
which is indistinguishable from a hang. The ticker was wired to the two ingestion loops only.

Now wired via `cancer.rag.progress.*` (a nested `Progress` class on `RagProperties` — `:rag`
cannot use the datafetcher's `ProgressTickerProperties`, different module and prefix).
`flush-interval` is 5 rather than ingestion's 10, since a trial takes ~350ms.

**Already-indexed trials render as `.`, not `*`** — a resumed run is mostly these, and counting
them as successes would make a 30-second resume look like a full re-embed. In-loop error
logging is demoted to `debug` per the skill; failures still reach the caller in `errors`.

⚠️ **The ETA is a flat average, and this loop has two phases with a ~100× cost difference.**
While skipping indexed trials at ~250/s the ETA reads optimistically low, then climbs as real
embedding drags the average down. It gets worse before it gets accurate. A windowed rate would
fix it, at the cost of changing shared `:common` behaviour for every call site.

### Frontend rework for a non-developer user — new 2026-08-10

The app was shown to the patient herself, so developer vocabulary and developer-shaped
workflows had to go.

**Diagnosis, Variants and Prior Treatment are now three tabs on one page** (`PatientRecord`),
not three menu items. Each tab keeps its own Save button and its own endpoint, so the reason
they were separate pages — three tables, three writes, no partial-write problem — still holds.
Tabs mount only while selected, so switching away and back refetches rather than holding stale
form state behind a hidden tab. The old `/variants` and `/prior-treatment` routes redirect.

**"Ingest" is now "Process Trials"**, with three buttons: **Pull Trials and Prepare for
Search** (both steps, behind a confirmation dialog stating what it does and that it takes
minutes), **Pull Trials**, and **Prepare for Search**. A failed pull stops the combined run
rather than backfilling a half-loaded corpus.

**Backfill / Ingest / staging / normalize / chunks are gone from every user-visible string** —
button labels, result modals, error banners, and the Dashboard. Internal names are unchanged.

**The Dashboard's "Pull Latest Trials" button became a link.** It ran a multi-minute job from a
card whose neighbours all navigate, and it pulled *without* preparing for search — so trials
loaded that way silently never appeared in search results. That path no longer exists.

### Vector-store setup is now self-reporting — new 2026-08-10

The `clinical_trial_chunks` collection went missing (a rebuild, or a manual clear) and the
failure was badly reported. **Backfill caught the failure per trial and returned
*successfully* with zero indexed and one identical error per trial**, which reads as a data
problem rather than a setup step nobody ran.

Now the store is probed once before a backfill starts, and the run aborts with a single
message naming the real cause. A startup check logs `SEARCH UNAVAILABLE` at boot so the
condition is known before anyone presses a button — a warning, not a boot failure, since
ingestion, the patient record and Tier 1 matching all work fine without search.

Recreating the collection is a manual REST call by design; `_archive/ingestion/QDRANT_SETUP.md` has the
exact command, the 384-dimension/Cosine requirement, and the REST-6333-vs-gRPC-6334 trap.

### Payload hashing — new 2026-08-10

`staging_raw_trial.payload_hash` (SHA-256 hex, `varchar(64)`, nullable). A re-pull now skips
trials whose payload is byte-identical instead of re-normalizing them.

**This fixed a real cost.** Every already-normalized trial previously took the refresh branch
unconditionally, so a second full pull re-normalized the entire corpus and cost the same as the
first — ~15 minutes for 2,500 trials, since normalization is ~349ms/trial and 99.3% of a run's
wall-clock.

**The approach was verified against the live API before being built**: two fetches of the same
study, and of the search endpoint the ingest job actually uses, returned byte-identical
payloads. No per-request timestamps or unstable key ordering. Re-check if CT.gov ever versions
their API.

**A byte-length check was considered and rejected** — `RECRUITING` → `SUSPENDED` is the same
character count, as is a date change, so it would report "unchanged" for precisely the changes
worth catching.

**Null hash means refresh, never skip**, enforced in three places: the column is nullable so
pre-existing rows still refresh, the branch requires a non-null match, and the digest helper
returns null on failure so an unavailable algorithm degrades to the old behavior.

The pull result now separates **"Unchanged since last time"** from **"Already waiting to be
saved"** — two different skip reasons that previously collapsed into one number. Design and
open questions in `_archive/ingestion/PAYLOAD_HASH_PLAN.md`.

### Backend

Full layered scaffold (domain → entity → repository → db service → service → controller) for
every core entity: Trial, TrialSource, StagingRawTrial, Sponsor, Condition, Medication,
Location, ArmGroup, Intervention, Outcome, OverallOfficial, EligibilityRule, Keyword, Patient,
TrialStatus, PatientDiagnosis, PatientVariant, PatientPriorTreatment, plus the Epic/FHIR tables
(UcHealthOAuthToken, StagingRawFhirResource, PatientMedication, LabResult, LabResultComponent).
Standard CRUD with pagination on each.

Every entity referencing a Trial also exposes `GET /api/{entity}/by-trial/{trialExtid}`, and
patient-scoped entities expose `GET /api/{entity}/by-patient/{patientExtid}` — five such
endpoints, verified 2026-08-14. (These were `by-appuser` before changeset `030` dropped
`AppUser`.)

### Frontend

Seven routes: Login, Dashboard, Trial Search, Trial Detail, Saved Trials, **Diagnosis** (the
`PatientRecord` shell, with Diagnosis / Variants / Prior Treatment as tabs), and Process
Trials. Structure and gotchas in `_archive/frontend/frontend-module.md`.

**Variants and Prior Treatment became tabs on 2026-08-10**, having been separate pages when
added on 2026-08-09. The reason they were separate still governs the tab design: three tables
means three endpoints, so each keeps its own Save button and there is no partial-write problem
to solve. They also draw on different source documents — a pathology report, a genomic report,
and a medication list — plausibly filled in on different days. Shared
`Section`/`Field`/`Select`/`BooleanSelect` live in `components/FormControls.tsx`, extracted
from `Diagnosis.tsx` so the three cannot drift apart.

Each carries an amber callout explaining the one thing a person filling it in is most likely
to get wrong: on Variants, that "not tested" is not "not detected"; on Prior Treatment, that
*how* a drug was stopped decides which half of the corpus applies.

### Patient variants and prior treatment — new 2026-08-09

`PatientVariant` (21 fields) and `PatientPriorTreatment` (24 fields), changesets `026`/`027`,
full layered stack plus 62 tests. Design and rationale in
`diagnosis/patient-variant-and-treatment-tables.md`; the three research documents behind it
are in `_archive/research/`.

**Both use five-state vocabularies, not booleans**, and that is the whole point:

- Variants: `DETECTED | NOT_DETECTED | VUS | NOT_TESTED | UNKNOWN`
- Treatment: `NEVER | CURRENT | PROGRESSED | STOPPED_OTHER | UNKNOWN`

The treatment case is concrete rather than theoretical. The patient is **on a CDK4/6 inhibitor now
and has not progressed on it**. A boolean `priorCdk46 = true` is literally true and reads as
post-CDK4/6 — matching her to the wrong half of the corpus. `CURRENT` vs `PROGRESSED` vs
`NEVER` is one dropdown and it is the difference between a useful shortlist and a misleading
one.

**Named `PatientPriorTreatment`, not `PatientMedication`** — that name is taken by the FHIR
prescription mirror (changeset `020`), which records what Epic *prescribed*, is keyed on a
required unique `fhir_resource_id` so nothing can be hand-entered, and is still blocked and
empty. Different table, different job.

**Nothing reads these tables yet.** They are inputs waiting for Tier 2 matching.

### Ingestion progress ticker — new 2026-08-09

A character-per-record console bar on both ingestion loops: `*` inserted, `.` skipped, `!`
error, wrapping every N records with the line's first record number in a left gutter and
elapsed/rate/ETA at line end.

```
NORMALIZING
     1 | ****.**!***.*********!**********.***************  2.6s 28.4/s ETA 3.9s
```

Class in `common/progress/ProgressTicker.java` (framework-free, so `:common` stays
Spring-less); config in `datafetcher/config/ProgressTickerProperties.java` bound from
`cancer.ingestion.progress.*`. Design record in `agents/progress-ticker.md`, and the pattern
is now a skill at `skills/progress-ticker/`.

**`enabled` defaults to `true`, not `auto`.** Ingestion is triggered from the frontend against
a running backend, so the work happens on an HTTP request thread with no console — `auto`
resolved to false and the bar never drew. Console detection describes how the *process* was
launched, not whether anyone is watching.

### CT.gov ingestion — verified working

Fetch → stage → normalize, on demand via `POST /api/ingestion/clinicaltrials`. Run repeatedly
against the live API including multi-page pulls of 1,000+ studies; re-runs dedup correctly by
`nctId`. Defaults live in `cancer.ingestion.clinicaltrials.*` and default to RECRUITING only.

### UCHealth / Epic FHIR — working against Epic's sandbox

OAuth (PKCE, no client secret) → MyChart login → callback → token stored → authenticated FHIR
call → payload staged → re-run dedups. Observation normalizes into `lab_result`.

### RAG — steps 1–7 of `RAG_PLAN.md` complete

Qdrant in Docker, local ONNX embeddings (all-MiniLM-L6-v2, 384 dims — clinical text never
leaves the machine), criterion-level chunking, indexing, backfill, retrieval, and an evaluation
set that passed 8/8 on a 1,289-chunk corpus. Generation (§9) is deliberately deferred.

### Persisted trial matches — new 2026-08-08

`SavedTrialMatch` and `SavedTrialMatchCriterion` (tables `trial_match`,
`trial_match_criterion`, changesets `024`/`025`), full layered stack plus 68 tests. Design
and rationale in `diagnosis/trial-match-tables.md`.

**Named `SavedTrialMatch`, not `TrialMatch`** — `rag.retrieve.TrialMatch` already exists as
the in-memory search result. The persisted row and the ephemeral result are different
things; the user chose to rename the new entity rather than touch working `:rag` code.

**One run is stored**, `search_run_id = 22ccb562-b4a4-4acb-ae68-739896d837c1`: 15 ranked
matches, 77 criterion rows, 8 of them flagged `is_exclusion` (all the same CNS-metastases
concern — a skull-bone lesion rather than brain parenchyma,
which is a question for her oncology team, not a disqualification). Those trials stayed in
the ranked list, per the no-verdicts rule.

**It was written directly through the REST API, not through `GET /api/rag/search`**, so the
open question of what triggers persistence is untouched. `query_text` on every row states
that this was a deterministic filter and that `top_score` is signals-matched/6, **not** a
cosine similarity — do not compare these scores against a future semantic run.

### Diagnosis matching — Tier 1 of 3

`PatientDiagnosis` (21 fields) plus deterministic age/sex/recruiting checks surfaced on Trial
Detail and Trial Search. Tier 2 (retrieval-driven) and Tier 3 (rule tree) are not built.

---

## Hard rules this project follows

**extid only, everywhere.** No internal numeric id is ever exposed to the frontend, and the
frontend never sends one back — every cross-entity reference on the wire is an `extid`,
including what would naturally be a numeric foreign key. Domain objects and JPA entities use
numeric ids internally; each controller's converter translates at the boundary.

**No eligibility verdicts.** Per `_archive/diagnosis/DIAGNOSIS_MATCHING_DESIGN.md` §5: no fit
score or percentage, no auto-exclusion of trials, and "unknown" is a first-class result. A
failed check renders amber and never removes a trial from a list — a parsing failure must not
silently take away an option. The tool surfaces what to look into and ask about; the judgement
stays with the patient, family, and oncology team.

**Ingestion and indexing are two separate steps.** `POST /api/ingestion/clinicaltrials` writes
MySQL; `POST /api/rag/backfill` makes it searchable. Deliberate — the two have very different
costs (staging 1,000 trials is quick; embedding them is ~26,000 local inferences) and keeping
them apart keeps that visible.

**Module dependency direction.** `:datafetcher` and `:rag` depend on `:common`/`:database`, and
root depends on them — so neither can call root's `service` package without creating a cycle.
Both call `*DbService` classes directly. This has been hit twice; when a lower module must
trigger something upward, use a Spring event with the type declared in `:database`.

---

## Blockers and known gaps, worst first

### Security — must be resolved before any deployment

**Two of the four were fixed 2026-08-10** (uncommitted), chosen because they change nothing
about how the app behaves locally. The two that remain both need testing immediately after the
change, which is why they were left.

✅ **The JWT signing secret is now an env var.** `JwtUtil`'s inline literal is gone from source
entirely; `application.yml` has a `jwt:` block reading `${JWT_SECRET}`, and a fresh 512-bit
value lives in `.env`. **Neither the property nor the `@Value` carries a default, deliberately**
— a default would quietly re-enable the old literal whenever the env var is unset, so a
misconfigured deployment would sign tokens with a value that is public in the repo history.
Unset now fails startup, which is the failure you want. Consequence: **the backend will not
boot without `JWT_SECRET`**, and existing tokens are invalid, so log in again.

✅ **Qdrant is bound to `127.0.0.1`.** It was publishing 6333/6334 on all interfaces, and Qdrant
ships with no authentication of any kind. Override with `QDRANT_BIND` if a remote container
ever needs it, and put real auth in front first.

- ✅ **Endpoint security is restored — verified 2026-08-14.** `SecurityConfig` now ends in
  `.anyRequest().authenticated()`, with static assets and `/api/auth/**` explicitly permitted and
  `/api/uchealth/callback` correctly left public (Epic's OAuth redirect cannot carry a JWT).
  `@EnableMethodSecurity` is on, so `@PreAuthorize` is actually honoured rather than silently
  ignored. Landed in `6037500`. Note `RetrievalEvaluation` hits the REST API unauthenticated and
  will fail against a secured backend.
- ✅ **`UcHealthOAuthTokenController` no longer exists — verified 2026-08-14.** The only UCHealth
  controller is `UcHealthAuthController`. The full-CRUD-over-the-token-table exposure this item
  described is gone. If token CRUD is ever reintroduced, it needs the remove-versus-protect
  decision this item called for.

Full checklist in `_archive/hosting/qa-setup.md`.

### Epic / UCHealth

- **No refresh token.** Epic granted `patient/*.read fhirUser launch/patient openid` but
  silently dropped `offline_access`. The access token dies in ~1 hour with no way to renew it —
  every run past that needs a fresh interactive browser login. The refresh code path exists and
  is unit-tested but has never executed against Epic. Resolve before production authorization.
- **MedicationRequest fetch is blocked.** Epic rejects the patient search: *"Combination of
  parameters is not valid for any authorized sub-resource."* The app was registered for
  `MedicationRequest.*(Order Template Medication)` — a formulary catalog, not patient
  prescriptions. Re-registered for `Signed Medication Order` (R4); grant had not propagated.
  The whole `patient_medication` stack sits unused until it clears.
- **`DiagnosticReport` returns 403** despite the scope being registered. Not investigated.
- **Panel handling is untested against real data** — `lab_result_component` is exercised only
  by a hand-built CBC payload. The sandbox patient has one lab (an A1C) and no panels.

### Schema

- **The join tables were never scaffolded**: `trial_condition`, `trial_sponsor`, `trial_phase`,
  `trial_std_age`, `trial_keyword`, and `intervention_arm_group`. Deliberate, but it means
  **Trial Search cannot filter by condition, sponsor, or phase**, Trial Detail cannot show them,
  and they cannot be RAG retrieval filters. The normalizer populates the `Condition`/`Sponsor`
  lookup tables but links nothing to a trial. Adding them requires a full re-backfill, since
  filters live in chunk metadata.
- **`EligibilityRule` is scaffolded but never populated.** Ingestion writes only
  `trial.eligibility_criteria` as raw narrative text. Its extid conversion is also unresolved —
  `parentRuleId` is self-referencing and `criterionId` is polymorphic.
- **The tables doc has drifted from the schema** (found 2026-08-08, not yet fixed): the doc says
  `condition`, but the table is `medical_condition` (reserved word); `intervention_arm_group` is
  missing from the doc's own gap list; and two changesets share the number `014`
  (`014-eligibility-rule.yaml` and `014-outcome.yaml`).
- **`spring.liquibase.drop-first` is off** so the Epic OAuth token survives a restart.
  Consequence: **edits to an already-applied changeset do not take effect on startup** — rebuild
  the DB via the n8n `clear-db` webhook. New changesets still apply normally.

### Performance

**Normalization is the ingestion bottleneck** — measured on 736 rows: fetch ~1.1s (0.4%),
staging ~0.7s (0.3%), normalization ~257s (**99.3%**). About 349ms and ~38 DB round trips per
trial. A full recruiting-only cancer pull (~18,773 trials) is **~110 minutes and will not
survive an HTTP request timeout**; an async job endpoint becomes necessary at that scale.

Batching the ~25 child INSERTs per trial is **blocked by `BaseDb`'s
`GenerationType.IDENTITY`**, which disables Hibernate JDBC batching entirely. Two ways forward,
both needing your decision: change the id generation strategy (schema-wide, affects every
entity) or write native bulk-insert queries (bypasses `BaseDb`'s extid/timestamp handling).
Cheap and unblocked meanwhile: bulk `deleteByTrialId` instead of find-then-delete-per-child.

Also: `ClinicalTrialsGovClient` accumulates every study in a `List<JsonNode>` before staging any
of them, so an 18,773-trial pull holds ~280MB of parsed JSON in heap. The fix is streaming per
page, not a bigger page size.

### Rebuilding the database — read before running the webhook

**The n8n `clear-db` webhook left the schema half-built once this session.** It dropped the
tables but not Liquibase's `databasechangelog`, so on restart Liquibase considered `001`-`023`
already applied and created only the two new changesets. Result: `trial_match` existed while
`app_user`, `user`, `trial_source` and everything else did not, and every endpoint except
the new ones returned 500.

`spring.liquibase.drop-first` is `false` (so a stored OAuth token survives a restart), which
is what makes this failure mode possible. **The user updated the webhook and the second
rebuild worked correctly** — but if a rebuild ever produces "Table 'cancer.X' doesn't exist"
on some endpoints and not others, this is the cause. The fallback is to flip `drop-first`
to `true`, restart, and flip it back.

**A rebuild used to destroy the AppUser and the real patient rows.** `PatientSeedLoader` now
recreates all four from gitignored CSVs in `_archive/patient-data/` on startup — see "The
patient rows now seed themselves" above. Verified through two real rebuilds (2026-08-09 and
2026-08-10).

Rebuild also invalidates Qdrant: chunks reference trial extids that no longer exist, so the
collection must be deleted and re-created, not reused. The recreate command and its required
dimensions are in `_archive/ingestion/QDRANT_SETUP.md`.

**Recreating the collection is safe; recreating the container is also safe.** `docker compose
up -d` destroys and recreates the container, but the vectors live in the named volume
`cancer_qdrant_storage` and are re-mounted. Only `docker compose down -v` deletes them. This
was verified 2026-08-10 when the container had to be recreated mid-corpus to apply the
`ulimits` fix — 46,247 points survived intact. And even a total loss is now recoverable
without a full re-embed, since backfill skips what is already indexed.

### Other

- **One single-item failure from the 2026-08-08 250-trial pull remains**: `NCT06685796` failed
  to normalize (`Create operation failed for TrialDb`, generic message — needs the staging row
  inspected). **The second one is solved**: `NCT07219277`'s "Embeddings must have the same
  number as that of the documents" was the colliding-chunk-id bug, fixed 2026-08-10 — see "Two
  chunker bugs" above. It was never a one-off; any multi-population trial hit it.
- **`findByAppUserId` returned soft-deleted rows — fixed 2026-08-08.**
  `PatientDiagnosisRepository.findByAppUserId` had no `active` filter while the Diagnosis
  page takes `rows[0]`, so after replacing a diagnosis the page displayed and edited the
  **deleted** record. Now delegates to `findByAppUserIdAndActive(..., ACTIVE)`; callers
  unchanged, `:database` tests pass. **`TrialStatusRepository.findByAppUserId` has the same
  shape and has not been checked** — likely the same bug.
- **`PatientDiagnosisDbService.update()` cannot clear a field.** Every assignment is
  `if (item.getX() != null)`, so a null means "leave alone" and there is no way to unset a
  populated column through the API. Hit this when the placeholder row's stale
  `lastChemoEndDate` (2025-11-15, predating the real diagnosis) could not be cleared; the
  row had to be deleted and recreated. Affects every `*DbService` following this template.
- ✅ **The `AppUser` design flaw is resolved — 2026-08-14.** `User` (login) and `AppUser`
  (tracking) used to be separate tables with no FK, matched on a username string, and `AppUser`
  required a `passwordHash` it never used. `AppUser` is dropped (changeset `030`); the tracking
  entity is `Patient`, and `UserPatient` is the real FK between a login and the records they may
  see. This is what `_archive/patient/PATIENT_MODEL_PLAN.md` proposed.
  ⚠️ Seeding is still via `PatientSeedLoader` from gitignored CSVs — there is no UI for it.
- **No enum endpoint.** Frontend vocabularies are hardcoded `as const` arrays in
  `types/api.ts`; adding a backend enum value will not surface in the UI on its own.
- **`BasicApplicationTests.contextLoads()`** — status unconfirmed. It previously failed on a
  Liquibase parse error (unquoted `decimal(x,y)`) that has since been fixed; re-check.
- **Retrieval is weak on conceptual queries** — measured on the old 249-trial corpus: clinical
  terminology 0.66–0.99, but "BRCA mutation" 0.388 and colloquial phrasing 0.526. **The larger
  corpus now exists, so this is answerable rather than hypothetical** — the two TRACKED queries
  in `RetrievalEvaluation` report exactly these. Read them before considering a model upgrade,
  with the 45.5%-breast dilution caveat in mind.
- **Six of eight files in `.claude/agents/` lack YAML frontmatter** and therefore cannot be
  invoked as subagents. They work only as prompts referenced by path.

---

## Login rate limiting — 2026-08-11, verified live

`LoginRateLimitFilter` throttles `/api/auth/login` and `/register`: 8 consecutive failures →
**429 with `Retry-After`** for 15 minutes. Configurable via `LOGIN_MAX_ATTEMPTS` /
`LOGIN_LOCKOUT_MINUTES`. Registered ahead of `UsernamePasswordAuthenticationFilter` so a
locked-out caller is refused before any bcrypt work — otherwise the throttle still pays the cost
of every guess.

⚠️ **Keyed on IP + username. The first version was IP-only and that was a denial of service** —
caught by the very next check after "it works". Eight probes against a *nonexistent* username
locked the real account out of the same machine (`login jeb -> 429`). Behind Nginx or CGNAT
everyone shares an address, so an attacker could have locked the patient out of her own tool at
will. The IP half is kept as well: without it, one attacker locks a known username from anywhere.

Reading the username means reading the request body in a filter, and a servlet input stream is
single-pass — `CachedBodyRequest` buffers and replays it, or the controller receives an empty
body and every login breaks. Extraction is a bounded regex, not a JSON parse: this runs
pre-authentication on a public endpoint and must not throw on hostile input.

**Verified live, all four cases:**

| Check | Result |
| --- | --- |
| 9 failures on a junk username | `401 ×8` then **429** ✓ |
| `jeb` from the same machine | **200** — unaffected ✓ |
| 8 failures on `jeb`, then the *correct* password | **429** — lockout real ✓ |
| `admin` while `jeb` is locked | **401**, not 429 — isolated ✓ |
| Empty body | **400** validation — body still reaches the controller ✓ |

**Recovery: the counter is in-memory, so a backend restart clears any lockout instantly.** Worth
knowing before locking yourself out of prod.

There is now a **`login-rate-limit` skill** in `~/.claude/skills/` carrying this implementation,
the IP+username trap as its first design point, and the test that catches it. The user asked for
it so the pattern reaches his other projects.

## Passwords were never hashed outside register — fixed 2026-08-11

`UserService.create` and `.update` passed the raw password straight to the DB layer; only
`AuthController.register` called `passwordEncoder.encode`. So **every password set through the
user API was stored in plaintext**, and could never log in again, because authentication
compares against a BCrypt hash.

Found the hard way, changing the production password off `password123`: the PUT returned 200
with a normal-looking payload, and the new password then failed with 401. With `password123`
also dead and `admin`'s password unknown, there was no way back in through the API — recovery
took a full database rebuild.

`encodeIfPresent()` now hashes on both paths. Null still means "leave the password alone",
matching every other field in this schema's update path.

**`hosting/change-password.sh` is the safe way to do it**: prompts instead of taking an argument
(so the password reaches neither shell history nor `ps`), and verifies the stored value is a
real 60-character BCrypt hash before reporting success. That check turns this class of bug from
an hour of confusion into an immediate failure.

⚠️ **Two traps that cost time and are worth remembering:**

- **An interactive `mysql>` session holds an open transaction.** A `SELECT` there shows your own
  uncommitted write, so the session showed a correct 60-char hash while root on another
  connection saw the 20-char plaintext. Verify with `mysql -e '...'`, not from inside the session.
- **A database rebuild is free before the corpus exists and expensive after.** This one cost
  nothing because Qdrant had 0 points. After the embed it would be hours.

## ✅ The authorization gap — found 2026-08-11, CLOSED 2026-08-14

> **This section described the gap as deferred. It has since been fixed** in `c9cb30d` ("Give
> clinical data an owner, so a record can be shared and not just read"). The history is kept
> below because the reasoning still explains why the model is shaped the way it is.

**What exists now, verified 2026-08-14:**

- **`UserPatient`** joins a login `User` to a `Patient` with an access level — the FK that this
  section correctly identified as missing.
- **`AccessLevel` is ranked**, not boolean: `VIEW_TRIALS(10) < VIEW_RECORD(20) < EDIT_RECORD(30)
  < OWNER(40)`. A caller granted trial-viewing cannot read the underlying record.
- **`CurrentUserService` reads the token and enforces the check.** `requireAccess`,
  `requireAccessId`, `hasAccess` and `accessLevelFor` all resolve the caller from
  `SecurityContextHolder` and compare against the requested patient. `TrialMatchingController`
  calls `requireAccessId(patientExtid, AccessLevel.VIEW_TRIALS)` before doing any work.
- **`AppUser` is gone.** Changeset `030-drop-app-user.yaml` dropped the table; the domain object
  is `Patient`, and all five patient-scoped endpoints are `by-patient`, not `by-appuser`.

⚠️ **The URL still names the patient** — the endpoints are `/rank/{patientExtid}`, not `/rank/me`
as the deferred fix below proposed. That is now safe because the extid is checked against the
caller's grants rather than trusted, but it means an unauthorized extid returns a permission
error rather than being unaddressable.

**The original finding, for context:**

**The app authenticates but does not authorize.** The user asked the right question — *"users
should only be able to see their stuff"* — and the answer is that they cannot today.

Every patient endpoint takes its target from the URL: `/api/matching/rank/{appUserExtid}`,
`/api/patientdiagnosis/by-appuser/{extid}`, and the same `by-appuser` shape on variants and
prior treatment. **Nothing compares that extid to the caller.** `SecurityContextHolder` is
written once by `JwtAuthenticationFilter` and never read again — no controller or service
references it. Roles are granted and never checked, so `ROLE_USER`/`ROLE_ADMIN` are decorative.
`AppUser` has no FK to the login `User`; they match on a username string.

**Three doors closed the same day, each verified against the running app:**

- ✅ **`/api/auth/register` was anonymous.** A stranger could POST, receive a valid JWT, and read
  everything. **Proven: HTTP 201 with a working token.** Now ADMIN-only, via `@PreAuthorize`
  *and* a filter-chain rule. ⚠️ `@EnableMethodSecurity` was **not** enabled — `@PreAuthorize`
  would have been silently ignored and the endpoint would have looked protected while standing
  open. Now enabled.
- ✅ **Soft-deleted users could still log in.** `DELETE /api/user/{extid}` returns 204 and sets
  INACTIVE, but `loadUserByUsername` had no active filter. **Proven: delete 204, then login
  200.** A delete that does not revoke is worse than none — it reports success and changes
  nothing. Now refused.
- ✅ **A `log.warn` printed bcrypt hashes** on every registration. A password hash in a log file
  is a credential in plaintext on disk and in any log shipper. Removed.

⚠️ **Existing JWTs survive a delete.** Tokens are stateless and nothing can recall one; deleting
an account stops new logins only. A token blacklist or short expiry is the fix if that ever
matters.

**The real fix as proposed then: resolve identity from the token, not the URL.**
`/api/matching/rank/me`, with the server reading the username from `SecurityContextHolder` and
looking up that AppUser — so a caller has nowhere to name someone else. Same for every
`by-appuser` endpoint, plus the frontend calls.

✅ **Resolved differently, and better.** Rather than making the caller unable to name anyone, the
`UserPatient` grant model lets a caller name any patient and checks whether they may. That was
the right call: it supports the sharing case ("a record can be shared and not just read") that
`/rank/me` would have made impossible. The 2026-08-11 conclusion — *"it becomes a live problem
the moment a second real user exists"* — is what the grant model exists to answer.

## Getting her record onto prod — decided 2026-08-11, not yet done

**Only three files go to the server, 4.4 KB total**: `patient-diagnosis.csv`,
`patient-variant.csv`, `patient-prior-treatment.csv`. `scp` straight into
`_archive/patient-data/`, `700` on the directory and `600` on the files, owned by the app user —
never through git, never through the app.

⚠️ **`my-health-summary.pdf` (21 MB) never goes to the server. Decided by the user, permanently.**
Nor do `mri-scan.md` or `pet-scan-2026-03-16.md`. `PatientSeedLoader` does not read any of them
— they were the source documents used to populate the CSVs. Shipping them would put the largest
concentration of her medical data on a hosted box for no functional reason. **This is the single
biggest risk reduction available in the whole deploy and it costs nothing.**

The eventual replacement, per the user: a feature where a patient drops in their own medical
history and AI parses it into the structured fields. That keeps the source document transient
rather than resident.

**Order on the server: corpus first, patient data last.** The trial rebuild is 2-3 hours; her
record on a box with no trials to match against demonstrates nothing.

**Vocabulary verified 2026-08-11** — all controlled values in the three CSVs match
`frontend/src/types/api.ts` (`RECEPTOR_STATUS`, `RECEPTOR_SUBTYPE`, `VARIANT_STATUS`,
`TREATMENT_STATUS`, `STAGE_SYSTEM`, `MENOPAUSAL_STATUS`). The 2026-08-09 drift that rendered
dropdowns blank has not regressed. Re-run this check before any future push; the backend stores
plain varchars and will accept a wrong value silently.

⚠️ **Extids regenerate on prod.** Any URL or script keyed on a local extid will not work there —
including the AppUser extid used in every Rank Trials call. Fetch the prod one after seeding.

⚠️ **Her UI edits will silently revert.** `PatientSeedLoader` seeds-if-absent and never syncs, so
once she edits her record through the app the CSVs are stale and the next rebuild reverts her
changes. Acceptable for a demo; a real data-loss path if she starts using it in earnest.

**Still accepted rather than solved:** prod will hold a real medical record with **no encryption
at rest and no access log**. Auth, HTTPS and login rate limiting close the paths that matter
most; these two remain, and the user has chosen to proceed knowingly for a single-patient tool
on his own host.

## The deploy runbook — written 2026-08-11

**`hosting/DEPLOY_RUNBOOK.md` is the current deployment document.** Seven phases, ordered, with
the verification block at the end. It supersedes `_archive/hosting/qa-setup.md` for anything
security- or architecture-related; that guide's infrastructure steps are still good and are
folded in by reference.

**Four things the archived guide gets wrong**, each of which would break a deploy:

- It serves the frontend from Nginx at `/var/www/cancer`. **Wrong architecture** —
  `buildDeployment` bundles the SPA *inside the jar* and Spring serves it. Following the old
  guide gives two copies of the frontend, and Nginx serves whichever was last copied by hand.
- The jar is `cancer-0.0.2-SNAPSHOT.jar`, not `cancer-server.jar`.
- It health-checks `/actuator/health`. **There is no actuator dependency in this project.**
- Its security checklist describes the pre-2026-08-11 state and its step 15 is truncated
  mid-sentence.

`_archive/hosting/setup-n8n-user.md` is local-only: it recreates the MySQL user with the `'%'`
wildcard for a Docker n8n container. **Do not apply it to prod** — keep the account scoped to
`localhost`.

**A real bug was found while writing it:** `cors.allowed.origins` existed only as a `@Value`
default in `WebConfig` and was **not in `application.yml`**, so `CORS_ALLOWED_ORIGINS` would not
have bound — the browser would have blocked every API call from the real domain, failing
client-side with no server-side error. Property added.

## Deploying to QA (Hostinger KVM) — analysed 2026-08-08, not started

**The user raised deploying on 2026-08-10** and chose to clear the security blockers first.
Two of four are now done (see Security above). The framing that came out of that conversation
is worth keeping: **deploying the app is largely solved — `buildDeployment` works, config is
env-var driven — but a fresh deploy arrives with an empty MySQL and an empty Qdrant.**
Reproducing the current corpus on the target means a ~14-minute pull plus a ~25-minute backfill,
assuming the box has the CPU for ~130,000 local ONNX inferences.

**Seeding Qdrant is the harder half and is unanalysed.** `_archive/ingestion/DEPLOYMENT_SEEDING.md`
covers only MySQL. The binding constraint: chunk payloads key on `trialExtid`, and extids
regenerate on every rebuild, so a Qdrant snapshot is only valid against the exact MySQL rows it
was built from. Three options — native Qdrant snapshots paired with a MySQL dump; ship MySQL
and re-embed on deploy; or **key chunks on `nctId` instead of `trialExtid`**, which is globally
stable so a snapshot survives a rebuild independently. The third is the structural fix and the
one with a deadline: it is a chunking change, so adopting it later costs a full `force=true`
re-index.

QA on your own KVM is a reasonable next step and a **different risk profile from
production**: your host, your data, and no real patient record required while the seeded
diagnosis stays placeholder. `_archive/hosting/qa-setup.md` already covers VPS provisioning
(Java 21, MySQL, Nginx, systemd) and is already renamed to this project.

`./gradlew buildDeployment` exists and bundles the frontend into the jar. All config is
env-var driven and `.env` is correctly gitignored.

**Required before it is reachable on a public IP** — ✅ **all four resolved; the app deployed
2026-08-11 and these are kept as the checklist that got it there:**

1. ✅ **Restore endpoint security.** Done — `SecurityConfig` ends in
   `.anyRequest().authenticated()`, with `/api/uchealth/callback` kept public (Epic's redirect
   cannot carry a JWT).
2. ✅ **Move the JWT secret** to `JWT_SECRET` in the server's `.env` with a fresh value. Done —
   the inline literal is gone from source and startup fails if the var is unset.
3. ✅ **Bind Qdrant to `127.0.0.1`.** Done — override with `QDRANT_BIND` if a remote container
   ever needs it, and put real auth in front first.
4. **Change `UCHEALTH_REDIRECT_URI`** to the server address and add that URI to Epic's app
   registration, or the OAuth callback breaks. ⚠️ *Deployment-side; not verifiable from code.*

**Not needed for QA, but required before it holds a real record:** encryption at rest, an
audit log of who read what, and a backup story. Be deliberate that QA stays sample-only until
those exist.

**Two things that will bite:**

- The **Epic token dies in ~1 hour** with no refresh, so FHIR ingestion on QA needs an
  interactive browser login each time.
- **A large ingestion will not survive an HTTP timeout** — ~110 minutes for a full pull.
  Behind Nginx you will hit `proxy_read_timeout` first. Keep pulls small, or raise it.

---

## Candidate next steps

> **Superseded by `TODO.md` as of 2026-08-21** — that is the live list; this section is kept for
> the reasoning behind each item.
>
> **Reviewed 2026-08-14.** Items 1-3 and 7 are done — Tier 2, the Rank Trials page, and endpoint
> security all shipped. **Items 4-6 (the after-commit event hook, the join tables, and
> generation) remain open.**
>
> ⚠️ **Item 1's open question is answered.** The two TRACKED retrieval numbers were never read,
> but the corpus-versus-model question was settled a different way: BRCA went 0.388 → 0.930 once
> a real corpus existed, so a bigger embedding model was ruled out. The colloquial query
> (0.526 → 0.600) is still the one genuine weakness.

The ordering rationale that produced this list still stands: the corpus must exist before
retrieval can be measured, and retrieval should be measured before matching logic is built on
top of it. Building Tier 2 against an unmeasured corpus repeats the mistake that produced the
misleading scores on 2026-08-08.

The scope decision from `BREAST_FOCUS_PLAN.md` is also pending: narrowing the app to breast
cancer only. The survey there found the schema and frontend are *already* breast-shaped, so
the change is less about removing generality than about permission to hard-code clinical
knowledge — which is what receptor-aware matching needs. ✅ **Its one blocking question is
settled**: matching lives in the backend, in code. Tier 1 still sits in
`frontend/src/lib/tier1Matching.ts`, but Tier 2 is server-side — the frontend cannot be tested
against the corpus in bulk, cannot inform ranking, and cannot be reused by Tier 3.

1. ✅ **Re-run the evaluation against a real corpus** — the corpus exists as of 2026-08-10 and
   `RetrievalEvaluation` passed mid-backfill. **What remains is reading the two TRACKED numbers**
   and deciding whether the weak queries were a corpus gap or a model limit. Do that before
   considering a bigger embedding model.
2. **Tier 2 matching** — use diagnosis fields to build retrieval queries against indexed
   criteria, reusing the existing `isExclusion` chunk metadata so a high-scoring exclusion match
   is shown as a concern rather than a fit. `DIAGNOSIS_MATCHING_DESIGN.md` §4.
   **The service layer for this exists uncommitted** — see "Tier 2 matching is half-built" below.
3. **A "Rank Trials for Me" page** — the thing the whole tool is for, and the piece that turns
   Tier 2 from service code into an answer. One button: take the patient record already on file,
   run it against the corpus, and come back with trials ordered best-first with the concerns and
   open questions attached to each.

   The user should not have to compose a search. Trial Search asks someone to know what to type;
   this page asks nothing and uses the record they already filled in across the three Patient
   Record tabs.

   Two things it must not do, both settled already and both easy to lose here: **no fit
   percentage** — order by concern count and applicable-signal count, never a number that reads
   like a probability — and **nothing is removed**, so a receptor mismatch demotes and flags but
   still appears, because receptor status can be re-tested and the judgement is not the tool's to
   make.

   Ordering is the open question. Retrieval score and concern count are different axes and it is
   undecided which leads; `assessAll` deliberately preserves caller order rather than guessing.

   **No longer blocked** — the exclusion-context check landed 2026-08-11, so a trial that rules
   out triple-negative disease now reads as a fit rather than a concern. What stands between
   here and the page is the REST boundary: a controller, a converter, and a response DTO, none
   of which exist. The service layer is done and tested.

   ✅ **The disease-type gate landed 2026-08-11**, so a ranked list no longer has to carry the
   54% of the corpus that is other diseases. It demotes rather than filters, so the page still
   has to decide whether to hide non-breast trials behind a toggle or just rank them last.
4. ⬜ **The after-commit event hook** so new ingestions index themselves — `datafetcher` publishes
   a Spring event (type declared in `:database`), `:rag` consumes it after commit. Avoids the
   `datafetcher` → `:rag` cycle and keeps a Qdrant outage from rolling back ingested data.
   `RAG_PLAN.md` §3 and §6 settle the design.
5. ⬜ **The join tables**, extid-only from the start, then condition/sponsor/phase filters on
   Trial Search and sections on Trial Detail.
6. ⬜ **Generation** (`RAG_PLAN.md` §9) — the grounded "why might this fit" answer with citations.
   Deferred until retrieval was proven; it now is. The chunk-per-criterion strategy is what
   makes line-level citation possible.
7. ✅ **Restore endpoint security** — done 2026-08-14 (`6037500`). `SecurityConfig` ends in
   `.anyRequest().authenticated()` and the app is live on a public host.

---

## Git state

**Verified 2026-08-21.** `main` and `origin/main` are in sync at `82e299b`. Current work is on
**`curative-work`**, 11 commits ahead of `main` and unmerged.

⚠️ **The remote moved.** GitHub redirects `Cancer.git` to `cancer-trials.git`; the local remote
URL was updated on 2026-08-21, so a stale clone may still be pushing through the redirect.

Commits on `curative-work`, newest first:

| Commit | What |
| --- | --- |
| `d146ceb` | Tell trials for stage IV apart from trials for early disease |
| `ec6277a` | Show where a trial runs while someone is still choosing one |
| `97178c5` | Let someone search for the trials that are aiming higher |
| `43fbc7e` | Give the treatment-goal recheck a button |
| `7529b8d` | Give the treatment goal a way to be recomputed |
| `3dd31d7` | Explain a trial on the page where someone is reading it |
| `0dd89cb` | Record what a trial is trying to do, so it can be searched for |
| `2db64df` | Stop asking whether she wants trials for her own cancer |
| `f9fb39d` | Ask what a trial is trying to do, not only who can join |
| `dc6d9e7` | Lock the login door after five tries, not eight |
| `de6afc8` | Point the seed loader at where the patient data actually is |

Test counts read from the test XML, not the build result: `:common` **43**, root **128**,
`:database` **862**, `:datafetcher` **55**, `:rag` **44** — 1 skip, 0 failures.

Frontend typecheck and build clean; one pre-existing lint error in `Login.tsx`.

⚠️ **Editing an applied changeset breaks its checksum and fails startup.** Adding
`treatment_goal` to `005-trial.yaml` did exactly that, and would have forced a database rebuild —
destroying a 2,473-trial corpus and its index to add a column. New changesets (`031`, `032`)
apply with no rebuild. Prefer a new number over an edit, always.

### Files that must never be committed

**`_archive/patient-data/`** — gitignored at directory level, `700`/`600`. Holds the real
PET/CT and MRI reports, the full My Health Summary PDF, and the three seed CSVs. All real
patient data.

**`playwright/.auth/mychart-session.json`** — a live authenticated session to a real medical
record. Gitignored; treat as a credential.

⚠️ **This has nearly gone wrong twice.** On 2026-08-08 `.claude/diagnosis.md` (the PET/CT
report) was `git add`-ed and staged before being caught. On 2026-08-09 the My Health Summary
PDF arrived in `.claude/diagnosis/`, which is tracked and world-readable. Neither was ever
committed or pushed. **Before any commit, confirm no patient file is in the index**, and put
any new patient document in `_archive/patient-data/` before opening it.

---

## Playwright MyChart scraper — new 2026-08-08

Standalone Gradle Java build at `playwright/` (NOT in root `settings.gradle`), modelled on
the user's existing `~/projects/viro/viro-playwright`. Plan and findings in
`_archive/playwright/PLAYWRIGHT_SCRAPE_PLAN.md`.

**Step 1 is done and verified against the real portal:** login works, and **session reuse
works** — a second run restored saved storage state and authenticated in ~3 seconds with no
MFA prompt (`SESSION REUSED`). MFA fires on a first login from a new device and the code goes
to the patient's phone, so the wait is 10 minutes. Repeatable scraping is viable; how long
the device trust lasts is still unknown.

**Why this exists at all:** Epic has **no MCP server** (their HIMSS 2026 "Agent Factory" is
health-system tooling, not a patient-accessible server), and the third-party FHIR MCP servers
are wrappers over the same SMART on FHIR API — same scopes, same blocked grants. More
importantly, Epic's patient-facing API has **no messaging resource at all**, so portal
messages and appointment notes are unreachable by any API. Scraping is not a workaround for
slow grants; it is the only route to those categories.

**Steps 2-6 are designed but not built.** The design questions were answered from real
screenshots: the dedup key is `eorderid` from the detail URL (unverified whether it survives
a session change — **confirm before step 4 writes to staging**), and a CBC panel confirmed
the `lab_result` → `lab_result_component` parent/child split is correct. Three extraction
hazards are documented, along with a provider comment that is per-encounter rather than
per-result and has no column anywhere.
