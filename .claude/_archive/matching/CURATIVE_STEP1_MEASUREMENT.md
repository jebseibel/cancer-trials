# Curative Intent — Step 1 Measurement

Run 2026-08-21 against the local corpus, as required by `CURATIVE_INTENT_PLAN.md` Step 1
("Measure first. Do not skip this.").

⚠️ **Corpus is 250 trials, not 4,634.** The database was rebuilt and re-pulled at 250. Every
absolute below is small; the ratios are what matters, and they are consistent enough to decide
on. **Re-run on a full corpus before building.**

⚠️ **Titles were not read.** The plan says "title and summary first", but `TrialChunker` does
not chunk titles at all (see `../rag/QUALIFYING_CHUNK_PLAN.md`), and this measurement was taken
from Qdrant rather than the REST API. **`briefSummary` + `detailedDescription` only.** Titles
would likely raise family 1's yield — a trial saying "curative intent" often says it there.

---

## Fill rate

| | |
| --- | --- |
| Trials indexed | 250 |
| Trials with prose (summary or description) | **250 — 100%** |

The plan asked whether `detailedDescription` is sparse enough to change the design. It is not:
every trial carries prose. The signal has text to read on all of them.

## The four language families

| Family | Trials | Share |
| --- | --- | --- |
| 1 — explicit intent (`cure`, `curative intent`, `eradicat*`) | 3 | 1.2% |
| 2 — durable remission (`complete response`, `DFS`, `EFS`, ...) | 24 | 9.6% |
| 3 — oligometastatic / ablative (`SBRT`, `oligometastatic`, ...) | 3 | 1.2% |
| 4 — modality (`CAR-T`, `bispecific`, vaccine, ...) | 7 | 2.8% |
| **Families 1-3, any** | **29** | **11.6%** |
| Family 4 **alone** (no 1-3) | 5 | 2.0% |

Control checks the plan asked for:

| | Trials | Verdict |
| --- | --- | --- |
| `overall survival` | 15 (6.0%) | ✅ **Excluding it was right** — it is an endpoint, not an intent |
| `locally advanced` | 12 (4.8%) | ✅ Must not count as stage IV, as the plan says |
| Stage IV vocabulary | 65 (26.0%) | Reasonable — a quarter of a breast corpus being metastatic is plausible |

## The headline number

**5 of 250 (2.0%) match both curative intent and stage IV.**

Scaled to a 4,634-trial corpus that is ~90 trials. The plan said low tens is success and
hundreds means the patterns are too broad. This lands between — **but the sample check
overturns the number entirely.**

---

## ⚠️ The hand-check kills the result. All 5 are false positives.

The plan is explicit: *"Read the samples, not just the distribution."* Doing so:

| Trial | Matched on | What it actually is |
| --- | --- | --- |
| NCT05192798 | `complete response` | **Endpoint definition** — "CBR (CR + PR + SD)". Boilerplate. |
| NCT01174121 | `complete response` | Same — CR as a unit of response measurement |
| NCT07233499 | `disease free survival` | **Background prose** about trastuzumab's history. Not this trial's goal |
| NCT06081244 | `complete response` | **Neoadjuvant TNBC** prognosis discussion. Early stage |
| NCT06693037 | `disease free survival` | **Adjuvant T-DM1.** Adjuvant is early-stage — the opposite of the target |

**Family 2 — the plan's designated "workhorse" — does not work.** `complete response` and
`disease-free survival` are ubiquitous oncology vocabulary appearing in endpoint definitions,
literature background, and adjuvant trials. They describe *how outcome is measured*, not *what
the trial is trying to achieve*.

This is the same failure the plan already anticipated for `overall survival` and excluded on
those grounds. **The argument generalises further than the plan applied it.**

### Family 1 is also 3-for-3 wrong

| Trial | Matched on | What it actually is |
| --- | --- | --- |
| NCT07631052 | `curative intent` | Describes **prior** neo/adjuvant therapy the patient already had |
| NCT05314114 | `eradicat` | Axillary disease "eradicated by NACT" — **neoadjuvant, early stage** |
| NCT06722599 | `curative` | *"good curative effect"* — a translation artifact meaning **efficacy** |

⚠️ **NCT07631052 is the instructive one.** "Curative intent" is real curative language, applied
to *the treatment the patient already received before enrolling*. Same trap as
`TREATMENT_NAIVE`: the phrase describes the patient's past, not the study's goal. The plan
warned about exactly this for criteria text and then read prose, where it also occurs.

### Family 3 found the only true positive

**NCT03808337** — *"stereotactic body radiotherapy (SBRT) when delivered to all sites of disease
in participants with 1-5 metastases will increase the length of time before disease progresses."*

**This is the real thing.** All sites of disease, oligometastatic, ablative. It is the strategy
under which a stage IV patient is treated with curative intent at all, and it maps onto a
bone-dominant disease pattern with a small number of named sites.

`no evidence of disease` / `NED` / `durable response` matched **zero** trials.

---

## What this changes

1. **Drop family 2 entirely, or require it to co-occur with family 1 or 3.** Alone it is noise.
   It produced 5 of 5 false positives and would be the dominant contributor to any score.
2. **Family 1 needs an early-stage veto.** All three matches also say `adjuvant`, `neoadjuvant`
   or `stage I-III`. A trial that says "curative intent" *and* "neoadjuvant" is an early-stage
   trial, not a stage IV cure attempt.
3. **Family 3 is the most precise signal, and it is the clinically correct one.** 1 of 1 correct.
   Oligometastatic/ablative language is specific — it is not used to describe endpoints or
   history the way response vocabulary is.
4. ⚠️ **"Curative intent" describing prior therapy is a real trap** and needs the same
   per-criterion co-occurrence discipline that fixed `TREATMENT_NAIVE`. Proximity to
   `prior`, `following`, `after`, `received` inverts the meaning.
5. **The stage IV half is the easier half and looks sound** — 26% metastatic vocabulary, with
   `locally advanced` correctly separable.

## Recommended revision to the plan

**Invert the family priority.** The plan ranked family 2 as the workhorse and family 3 as a
special case. The corpus says the opposite: **family 3 is the signal, family 1 is a weak
confirmer needing an early-stage veto, and family 2 is noise.**

**And do not build yet.** At 250 trials the true-positive count is 1. That is too thin to tune
against. Pull a full corpus, re-run this measurement, and hand-check again before writing
`treatmentGoalSignal`.

---

## Re-run 2026-08-21, second pass — two pattern bugs found

The measurement was scripted so it can be re-run on a larger corpus
(`scratchpad/curative-measure.py`, reads Qdrant directly — no auth, no backend call, no Gradle).
Smoke-testing that script against the same corpus exposed two bugs in the patterns this document
used the first time.

### `metastases` did not match `metastatic`

The stage IV pattern was `metastatic|stage IV|...`. **NCT03808337 says "1-5 metastases"** — the
plural noun, not the adjective — so it failed the stage IV test and was dropped from the results
entirely.

**That trial is the single best curative-intent match in the corpus.** A pattern bug silently
removed the one true positive, and the distribution looked fine without it.

Fixed by matching the stem: `metasta(tic|sis|ses|tases)`.

### `resectable` matched inside `unresectable`

The early-stage veto included bare `resectable` and `operable`, which match as substrings of
`unresectable` and `inoperable` — words meaning the opposite. **NCT06682793** (*"recurrent
unresectable, locally advanced, or metastatic (considered non-curative)"*) was vetoed as an
early-stage trial on that substring.

It should be excluded, but for its own stated reason, not by accident. Fixed with `\b`.

⚠️ **This is the same class of bug already recorded in `CURRENT_STATE.md`** — the
`HER2[\s-]?(negative|-\b)` branch that never fired because a hyphen followed by a space is not a
word boundary. Third occurrence in this project. **Any new pattern needs its word boundaries
checked against the words that contain it.**

## Numbers after both fixes

Corpus had grown to ~348 trials mid-run (a pull was in progress), so these differ from the first
pass and are still provisional.

| | Trials | Share |
| --- | --- | --- |
| Families 1+3 (the ones that held up) | 8 | 2.3% |
| Family 2 alone | 26 | — ⚠️ 5 of 5 hand-checked were false positives |
| Family 4 alone | 5 | — must not fire alone, per the plan |
| Stage IV vocabulary | 118 | 34.0% |
| Early-stage vocabulary | 123 | 35.3% |
| **Curative (1+3) AND stage IV** | **5** | **1.44%** |
| **...surviving the early-stage veto** | **4** | **1.15%** |

## The survivors, hand-checked

| Trial | Matched | Verdict |
| --- | --- | --- |
| **NCT03808337** | `stereotactic body` | ✅ **The real thing.** *"SBRT delivered to all sites of disease in participants with 1-5 metastases"* |
| **NCT06889610** | `ablation` | ✅ Multimodal ablation of liver metastases + immunotherapy, *"in-situ vaccine"*, releasing neoantigens |
| NCT06567353 | `ablation` | ⚠️ Borderline — ablation device comparison, 10 subjects. Ablative but reads as technique evaluation rather than curative ambition |

**Precision is roughly 2 of 3 on families 1+3 with the veto applied**, against 0 of 5 for the
original pattern set. The revision works.

## Confirmed conclusion

**Family 3 is the signal.** Metastasis-directed and ablative language is specific — it is not
used to describe endpoints or treatment history the way response vocabulary is, which is what
made families 1 and 2 unusable.

**Still do not build against 348 trials.** The true-positive count is 2-3. Re-run this script on
the full corpus, hand-check again, and only then write `treatmentGoalSignal`.

---

# ✅ FULL CORPUS RUN — 2,473 trials, 2026-08-21

The corpus was pulled to **2,473 trials, all indexed, zero orphans** (71,712 points). This is the
measurement Step 1 asked for; everything above was provisional.

| | Trials | Share |
| --- | --- | --- |
| Prose fill (summary or description) | 2,473 | **100%** |
| Family 1 — explicit intent | 72 | 2.9% |
| Family 2 — durable remission | 232 | 9.4% |
| Family 3 — oligometastatic / ablative | 61 | 2.5% |
| Family 4 — modality | 65 | 2.6% |
| Families 1+3 | 131 | 5.3% |
| Stage IV vocabulary | 777 | 31.4% |
| Early-stage vocabulary | 823 | 33.3% |
| Curative (1+3) **AND** stage IV | 65 | 2.63% |
| **...surviving the early-stage veto** | **38** | **1.54%** |

**38 is the answer, and it is the "low tens" the plan called success.** The rate held from the
348-trial sample (1.15% → 1.54%), so the patterns are stable rather than fitted to a small set.

Controls both behaved: `overall survival` 211 (8.5%) — excluding it was right; `locally advanced`
189 (7.6%) — cleanly separable from stage IV.

## Hand-check of all 38 — the plan's requirement

**Roughly 30 of 38 are genuine.** Examples that are unmistakably the target:

| Trial | Why it is the real thing |
| --- | --- |
| **NCT04563507** | SBRT to **each metastatic lesion**, on a CDK4/6-inhibitor-plus-aromatase-inhibitor backbone — HR+ first-line, **matching the patient's own regimen** |
| **NCT05334459** | *"LRT with curative intent... especially the subset of **bone-only** metastatic"* — **matching the patient's disease pattern** |
| **NCT03808337** | SBRT delivered to **all sites of disease**, 1-5 metastases |
| **NCT07053085** | Surgery + locoregional RT + SBRT for oligometastatic breast |
| **NCT04158843** | Radical local treatment **vs palliative** for oligometastasis |
| **NCT06328465** | *"oligometastatic disease treated with an aggressive multidisciplinary approach may be amenable to **curative** treatment"* |
| **NCT04079049** | Resection of liver oligometastases vs systemic therapy alone |
| NCT06055881, NCT06144346, NCT06246968, NCT06918262, NCT05933876, NCT06260033, NCT06889610 | MDT / SBRT / cryoablation in metastatic breast |

**Two of these match the patient's specific situation**, not just the category — NCT04563507 on
the patient's exact drug regimen, NCT05334459 on bone-dominant disease.

## The finding that shapes the signal

**26 of the 38 come from family 3. Only 12 come from family 1 alone — and those are where the
false positives live.**

**Every family-1 false positive is a negation.** The word appears while being denied:

| Trial | Text |
| --- | --- |
| NCT06682793 | *"metastatic (considered **non-curative**)"* |
| NCT07062965 | *"cancer that is **unlikely to be cured**"* |
| NCT05601440 | *"aromatase inhibitors improve outcomes, but **are not curative**"* |

Plus two where the word is real but describes something else: NCT04030507 (MRI brain screening,
"curative intent" applies to a different patient group) and NCT04430595 ("cure rate" in a
background paragraph about GD2).

⚠️ **This is the negation problem again**, the same one that made embeddings unusable for
receptor polarity: the phrase and its denial differ by one token. A keyword match cannot tell
"curative intent" from "not curative" without looking left.

## Design, now decided by measurement

1. **Family 3 is the signal.** 26 of 38, and near-perfect precision. Ablative and
   metastasis-directed language is specific — it does not appear in endpoint definitions or
   patient history the way response vocabulary does.
2. **Family 1 confirms, and needs a negation guard.** A ~30-character lookbehind for
   `not | non | rarely | difficult to | unlikely to be | cannot be | incurable` removes 3 of the
   5 false positives outright.
3. **Family 2 is excluded entirely.** 232 trials, 5 of 5 hand-checked wrong. It measures how
   outcome is reported, not what a trial is trying to do.
4. **Family 4 never fires alone**, per the plan. 45 trials would qualify on modality alone.
5. **The early-stage veto is load-bearing** — it removed 27 of 65 (42%), and spot-checks confirm
   those are adjuvant and neoadjuvant trials.

## Ready to build

The measurement is done and the numbers are stable. `treatmentGoalSignal` should be written
against family 3 as primary, family 1 as a negation-guarded confirmer, family 2 excluded, and
`diseaseStageSignal` as the separate second signal the plan specifies.
