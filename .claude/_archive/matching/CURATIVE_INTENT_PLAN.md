# Finding Trials That Are Trying to Cure Stage IV — Change Plan

Written 2026-08-14 from a read of `CriteriaSignalEvaluator`, `TrialMatchingService`,
`TrialMatchingController` and `RankedTrials.tsx`, rather than from the design docs.

Companion to `../CURRENT_STATE.md`, `../BREAST_FOCUS_PLAN.md`, and
`../diagnosis/patient-variant-and-treatment-tables.md`.

> ## ✅ BUILT 2026-08-21. Read `CURATIVE_STEP1_MEASUREMENT.md` alongside this.
>
> `treatmentGoalSignal` and `diseaseStageSignal` exist, the ranking tier is in place above concern
> count, `trial.treatment_goal` and `trial.disease_stage` are columns, and Trial Search has
> filters and badges for both.
>
> **⚠️ The measurement overturned this plan's central call.** Family 2 — response endpoints,
> described below as "the workhorse" — was measured across 2,473 trials and excluded entirely: 232
> trials use that vocabulary and 5 of 5 hand-checked were describing how an outcome is *measured*,
> not what the study aims at. Family 3 (oligometastatic/ablative), described here as a special
> case, carries 26 of the 38 survivors and is the actual signal. **Read the measurement before
> trusting the family priorities below.**
>
> Two pattern bugs also surfaced that this plan could not have predicted: `metastases` did not
> match `metastatic` (dropping the single clearest curative trial), and `resectable` matched
> inside `unresectable`.
>
> **Step 4 — the visible badge on the ranked list — is the one step not built.** A curative trial
> ranks first with its reason collapsed behind "What matched".

**Status when written: planned, nothing built.** `CriteriaSignalEvaluator` then had exactly five
signals: `diseaseTypeSignal`, `receptorSignal`, `treatmentLineSignal`, `pi3kSignal`,
`locationSignal`. It now has seven.

**Decided up front, so the rest of this plan is answerable to it:**

- **Broad definition** — aggressive/durable-remission intent, not only the literal word "cure".
- **A signal plus a ranking tier** — one list, a visible badge, nothing hidden.

---

## What is actually being asked for

> *"Trials that are trying to cure stage 4 cancer. They are out there but they are few. They
> are the primary ones I am trying to find."*

Two claims in that sentence, and they pull in opposite directions:

- **They are few.** So the tool must not lose them.
- **They are the primary target.** So finding them is not a filter added to the side of the
  ranking — it is a change to what the ranking is *for*.

Everything below follows from those two together. A feature that identifies these trials
correctly but leaves them ranked 40th has not delivered the request.

---

## The finding that should shape this decision

**The tool currently has no concept of what a trial is trying to achieve.** All five signals
answer *"is she eligible?"* — disease type, receptor polarity, treatment line, PI3K pathway,
location. Not one asks *"and if she got in, what is this trial trying to do for her?"*

That gap has a consequence worth stating plainly. In metastatic breast cancer the overwhelming
majority of trials are testing **disease control** — progression-free survival, a better
tolerated regimen, a longer interval before the next line. Those are worth having and they are
not what was asked for. Under the current ranking, a well-matched control trial with zero
concerns outranks a curative-intent trial that carries a single concern, **every time**. The
trials the user most wants are structurally disadvantaged by the sort order that exists today.

So this is not "add a sixth signal." It is the first signal about **goal** rather than
**eligibility**, and it is the first thing that has earned a place above concern count in the
ranking.

### The honest difficulty

**CT.gov does not publish treatment intent as a field.** There is no `curativeIntent` boolean,
no controlled vocabulary for it. It has to be inferred from prose — title, summary, and to a
lesser degree the criteria — which is exactly the kind of inference this project has been
burned by before:

- The 550 false concerns from `TREATMENT_NAIVE` read against a whole section.
- `HER2_POSITIVE_REQUIRED` missing "HER2-positivity" and ranking a HER2-positive trial 4th.
- The unit tests passing while the corpus said the signal was wrong.

**The lesson those produced governs this plan: measure against the corpus before building, and
again after.** Step 1 below is a measurement, not code, and it is not optional.

---

## What "trying to cure stage IV" actually looks like in trial text

The broad definition, decided above. Four families of language, and they are worth keeping
separate because they carry different confidence:

**1. Explicit intent language — highest confidence, lowest yield**
`cure`, `curative intent`, `curative-intent`, `eradicat*`. Rare, and unambiguous when present.

**2. Durable-remission endpoints — the workhorse**
`complete response`, `complete remission`, `pathologic complete response`, `no evidence of
disease`, `NED`, `durable response`, `long-term remission`, `event-free survival`,
`disease-free survival`, `relapse-free`. These state a goal beyond controlling growth.

⚠️ **`overall survival` is deliberately NOT in this list.** It is the default endpoint of
almost every serious oncology trial including pure control studies, so including it would
match most of the corpus and destroy the signal. This is the single easiest way to get this
feature wrong.

**3. Oligometastatic / local-ablative strategy — the clinically real path to "cure" in stage IV**
`oligometastatic`, `oligoprogressive`, `metastasis-directed therapy`, `SBRT`,
`stereotactic body radiotherapy`, `ablation`, `metastasectomy`, `consolidation`,
`total metastatic ablation`. This is the strategy under which a stage IV patient is
occasionally treated with curative intent at all, and it maps onto her situation directly —
a bone-dominant disease pattern with a small number of named sites.

**4. Modality signals — trials whose whole premise is durable immune control**
`CAR-T`, `chimeric antigen receptor`, `adoptive cell`, `TIL`, `tumor infiltrating lymphocyte`,
`cancer vaccine`, `neoantigen`, `bispecific`. Weaker on their own — a modality is not an
intent — so these should raise the signal only in combination with family 1, 2 or 3, never
alone. Recorded here because a curative-intent CAR-T trial often states the ambition nowhere
except in what it is.

### Where to read it from

**Title and summary first, criteria second — and this is a real decision.**

`diseaseTypeSignal` already established the precedent and the reasoning holds here even more
strongly: criteria text is about *who may enrol*, not *what the study is trying to do*.
Eligibility sections routinely mention "complete response" as a description of prior treatment
history ("patients who achieved a complete response to prior therapy"), which is the patient's
past, not the trial's goal. **Reading intent from criteria would invert the meaning on exactly
the phrases that matter most.**

`briefSummary` and `detailedDescription` are where a trial states its purpose, and both are
already on the `Trial` domain object — no new query, no join table, no schema change.

⚠️ **`detailedDescription` is populated for some trials and not others.** Confirm the fill rate
in step 1; if it is sparse, the signal leans on `briefSummary` and the plan is unchanged, but
knowing the number prevents a wrong diagnosis later when a trial reports UNKNOWN.

---

## Stage IV is the other half, and it is not the same question

"Trying to cure" and "stage IV" are two separate tests and the feature needs both. A curative
trial for early-stage disease is a correct match on intent and useless to her.

The patient is **de novo stage IV**, bone and extensive nodal metastases. Two
things follow:

- **Trials for stage 0-III disease are a mismatch**, and the corpus is full of them —
  adjuvant, neoadjuvant, and early-stage prevention studies. This is already a known miss:
  `CURRENT_STATE.md` records NCT07044310 as a stage 0-III trial that the tool had no way to
  flag.
- **Metastatic vocabulary is well-standardised**, which makes this the easier half:
  `metastatic`, `stage IV`, `stage 4`, `advanced`, `MBC`, `recurrent`, `M1`. And the exclusion
  direction is just as informative — a trial whose criteria exclude "metastatic disease" or
  "distant metastases" is stating early-stage scope explicitly.

⚠️ **`advanced` is ambiguous** — "locally advanced" is stage III, and matching it as metastatic
would admit exactly the early-stage trials this test exists to catch. Treat `locally advanced`
as a distinct phrase that does **not** count, and require `advanced` alone to co-occur with
`metastatic` or `stage IV` before it counts for anything.

**Recommendation: two signals, not one combined signal.** "Treatment goal" and "Disease stage"
answer different questions, can disagree, and a reader deserves to see which one fired. A
combined signal that reports CONCERN gives no way to tell whether the trial is the wrong stage
or merely the wrong ambition — and those lead to different conversations with her oncologist.

---

## Step 1 — Measure first. Do not skip this.

**This is the whole reason the previous signals eventually worked.** `CorpusSweep` exists for
exactly this and is already wired to run the real evaluator over every trial via the REST API,
read-only, off by default.

Before writing a signal, answer with numbers:

| Question | Why it decides something |
| --- | --- |
| How many of the 4,634 trials match each of the four language families? | If family 2 matches 2,000 trials, the pattern is too broad and the signal is worthless |
| How many match after the breast gate (the ~2,046 breast trials)? | This is the population a ranked list actually draws from |
| How many match **both** curative intent and stage IV? | **The headline number.** The user says "they are few" — this checks that against the corpus |
| What is the `detailedDescription` fill rate? | Decides whether the signal has one field to read or two |
| How many trials does `overall survival` alone match? | The check that excluding it was right |

⚠️ **Read the samples, not just the distribution.** Recorded on 2026-08-11: the treatment
signal's distribution "looked healthy while being wrong" at 550 false concerns. The samples
with quoted evidence are what caught it. **Hand-check at least 20 matched trials and 20
misses.**

**If the both-signals number comes back in the low tens, that is success, not failure** — it
confirms the user's own framing and it means the ranking tier will be visible rather than
diluted. If it comes back in the hundreds, the patterns are too broad and need narrowing
before anything is built on them.

✅ **Resolved.** Step 1 ran on 2026-08-21 against the full corpus — see
`CURATIVE_STEP1_MEASUREMENT.md`. It was measured against Qdrant directly rather than through
`CorpusSweep`, which needs a live backend and a token; reading the indexed prose needs neither.

---

## Step 2 — Two new signals in `CriteriaSignalEvaluator`

Follow the existing shape exactly: a public method per signal, returning `EligibilitySignal`,
every branch able to return UNKNOWN, and **every flag carrying the quoted phrase that produced
it**. That last part is not decoration — it is what lets her check the reasoning instead of
trusting it, and this signal will be wrong more often than the receptor one.

### `treatmentGoalSignal(Trial)`

Reads title + summary + description. Takes no patient input at all, like `diseaseTypeSignal`.

| Outcome | When |
| --- | --- |
| **PASS** | Explicit intent (family 1), or a durable-remission endpoint (family 2), or an oligometastatic strategy (family 3) |
| **UNKNOWN** | A modality signal (family 4) with no supporting intent language — worth asking about, not worth claiming |
| **NOT_APPLICABLE** | No goal language found. The common case, and it must stay quiet |

⚠️ **There is deliberately no CONCERN outcome on this signal.** A trial pursuing disease
control rather than cure is not a *problem with the trial* — it is the entire mainstream of
metastatic breast cancer treatment, and flagging thousands of trials amber would be both
useless and frightening. **This signal only ever promotes.** It is the first signal in the app
whose job is finding opportunity rather than raising doubt, and that asymmetry is intentional.

### `diseaseStageSignal(Trial, PatientDiagnosis)`

Reads title + summary for the trial's stage scope, and the patient's recorded stage.

| Outcome | When |
| --- | --- |
| **PASS** | Trial names metastatic/stage IV scope and the record says stage IV |
| **CONCERN** | Trial appears to be for early-stage disease (adjuvant/neoadjuvant/stage 0-III, or criteria excluding metastatic disease) while the record says stage IV |
| **UNKNOWN** | Stage is not recorded on the diagnosis, or the trial does not state its scope |
| **NOT_APPLICABLE** | No stage language at all |

This one **does** raise concerns, because a stage mismatch is a genuine eligibility problem —
and per the no-verdicts rule it demotes and flags, never removes. She could be re-staged;
that judgement is not the tool's.

⚠️ **Check what `patient_diagnosis` actually stores for stage before writing the comparison.**
`CURRENT_STATE.md` records the vocabulary drift that rendered dropdowns blank (`AJCC` vs
`AJCC_8`), and the backend stores plain varchars that accept a wrong value silently. Verify
against `frontend/src/types/api.ts`, as the CSV check does.

---

## Step 3 — Wire into the ranking, above concern count

**This is the step that answers the actual request**, and it is a deliberate change to the
comparator in `TrialMatchingConverter.ranking()`.

Current order: breast first → fewest concerns → most passes → most applicable → nctId.

**Proposed: breast first → curative intent → fewest concerns → most passes → most applicable.**

The new tier sits **second, directly under the breast gate and above concern count.** That
placement is the whole feature and it deserves its reasoning stated:

- **Below breast** — a curative trial for another disease is still not for her.
- **Above concerns** — this is the deliberate part. A curative-intent trial with one concern
  should outrank a concern-free control trial, because *the user asked for the first kind*. A
  concern is a question to ask her oncologist, not a disqualification; the whole no-verdicts
  design says so. Ranking curative intent below concern count would bury exactly what this
  feature exists to surface.

⚠️ **This changes what she sees on the page she actually uses.** Every existing ranked result
moves. That is intended, and it is why step 1's measurement matters — if the signal is
over-broad, this tier promotes noise to the top of the one page that must not waste her
attention.

⚠️ **Stage does NOT get its own ranking tier.** It flows through the existing concern count
like every other eligibility signal. Adding a second new tier would make the sort order hard
to reason about, and stage mismatch is already well served by demotion.

---

## Step 4 — Make it visible on the page

`RankedTrials.tsx` currently collapses passes behind "What matched", which is right for
eligibility signals and **wrong for this one**. A curative-intent trial whose distinguishing
feature is hidden behind a toggle has not been surfaced.

- **A badge on the card**, beside the title where the locations already sit. Wording matters
  and should be careful: something like **"Aiming for remission"** or **"Goal: durable
  response"**. ⚠️ **Do not label it "Curative" or "Potential cure."** The tool does not make
  eligibility verdicts and it must not make prognostic promises either — that is the
  no-verdicts rule applied to hope rather than to exclusion, and it is the single most
  sensitive string in this application. The badge reports *what the trial is trying to do*,
  never what it will achieve.
- **The quoted phrase behind a "why?" toggle**, exactly like every other flag, so she can see
  the sentence that earned the badge.
- ⚠️ **Tap targets** — `MOBILE_PLAN.md` item 5 applies. A new toggle must clear 24px minimum,
  and this is a page she will read on a phone.

**No new filter checkbox.** `breastOnly` is the one control that hides trials and it stays the
only one — decided in `CURRENT_STATE.md` and still right. Ranking surfaces these; hiding
everything else is not needed and costs an option.

---

## Step 5 — Re-sweep and hand-check

Run `CorpusSweep` again after the signals land. Two things to confirm:

- The distribution matches step 1's prediction. A large divergence means the implementation
  and the probe disagree, and the probe was the thing that was hand-checked.
- **Rank the corpus and read the top 20 by eye.** Recorded 2026-08-11 and worth repeating:
  *"the corpus sweep did not catch this and could not have — only ranking surfaced it."*
  A sweep measures distributions; ranking is how the tool is actually used, and it is the test
  that found the HER2-positive trial sitting at position 4.

---

## What this deliberately does not do

- **No AI/LLM parsing of trial intent.** `WIRING_AN_AI_PROVIDER.md` §5 is directly on point:
  measure what the deterministic path fails on before adding a model. Step 1 produces that
  number. If patterns catch most curative trials, an LLM is being added to a rounding error;
  if they catch half, that is the argument for one — **and it is a good later candidate**,
  since intent genuinely is semantic judgement rather than keyword presence.
- **No new tables, no schema change, no re-backfill.** Everything reads fields already on
  `Trial`. This matters: filters live in chunk metadata, so a retrieval-based approach would
  cost a full re-index.
- **No change to what gets ingested.** These trials are already in the corpus if they are
  recruiting breast trials; the problem is that nothing recognises them.
- **No claim about outcomes.** The tool reports stated intent. Whether a trial achieves it is
  not knowable from its registration.

---

## Open questions

- **Does the stage signal belong in this change or its own?** They ship together here because
  "curative for stage IV" needs both halves to mean anything. But the stage signal is
  independently useful and lower-risk, so splitting it first is defensible if step 1 shows the
  goal patterns need more work.
- **Should `RetrievalEvaluation` gain curative-intent queries?** "trials trying to cure
  metastatic breast cancer" is exactly the colloquial phrasing the evaluation already measures
  weakly (0.600). Worth knowing whether retrieval finds these, independently of whether the
  signal does.
- **How does this interact with `SavedTrialMatch`?** A stored run denormalises the diagnosis
  snapshot but not the signal set. If ranking order changes, an old saved run becomes hard to
  compare against a new one. Not blocking — worth deciding before another run is stored.
- **Does she want these as a saved/tracked list automatically?** `trial_status` already has
  "interested". A curative-intent trial she has read is exactly the thing to track, but
  auto-tracking would be the tool making a choice for her.

---

## Suggested order

1. **Rebuild the local database** so the API answers — currently 500 on every data endpoint.
2. **Measure** (step 1). Patterns as a probe, corpus sweep, hand-check 20 hits and 20 misses.
3. **`treatmentGoalSignal`** with unit tests, then re-sweep.
4. **`diseaseStageSignal`** with unit tests, then re-sweep.
5. **Ranking tier** (step 3) — one line, and the biggest behavioural change here.
6. **Badge and "why?" on the card** (step 4).
7. **Rank and read the top 20 by eye** (step 5). This is the acceptance test, not the sweep.

Steps 2-4 are where the risk is. Step 5 is where the value is, and it is one line — but it
only earns its place if step 2 says the signal is sound.
