# Retrieval evaluation set

A fixed set of queries with hand-labeled expectations, so retrieval quality is a **number**
rather than a feeling. This is what makes an embedding-model change assessable: run it before
and after, compare.

Built against the 50-trial breast-cancer corpus (1,289 chunks) ingested 2026-08-07. **If the
corpus is re-ingested with different search terms, these expectations must be re-checked** —
they are labeled against this specific set of trials.

## How expectations were set

Each query is labeled by what the *text obviously means*, not by what the current model
returned. That distinction matters: labeling from output would make the test tautological and
it could never detect a regression.

Two kinds of assertion, because they fail differently:

- **`minTopScore`** — the best match must score at least this. Catches "found nothing useful".
- **`expectSource`** — the best match must come from this field type. Catches "found the right
  words in the wrong place", e.g. matching an outcome measure when the answer is an
  eligibility criterion.

Scores are cosine similarity, 0..1, from `all-MiniLM-L6-v2` (384 dims).

## The set

| # | Query | minTopScore | expectSource | Notes |
|---|---|---|---|---|
| 1 | `ECOG performance status 0 or 1` | 0.90 | INCLUSION_CRITERION | Exact clinical terminology; near-verbatim in corpus. Any drop here means something is badly wrong. |
| 2 | `pregnant or breastfeeding` | 0.85 | EXCLUSION_CRITERION | Universal exclusion; appears in many trials with varied wording ("breast-feeding", "Women who are pregnant"). Tests morphology handling. |
| 3 | `brain metastases` | 0.80 | EXCLUSION_CRITERION | Common exclusion. Tests a short domain phrase. |
| 4 | `triple negative breast cancer` | 0.75 | INCLUSION_CRITERION | Disease subtype; must match the inclusion criterion naming it, not a summary mentioning it. |
| 5 | `HER2 positive` | 0.60 | INCLUSION_CRITERION | Tests abbreviation + symbol handling — corpus writes this as `Her2+`, `HER2-`, `RE+, HER2-`. |
| 6 | `measurable disease RECIST` | 0.60 | INCLUSION_CRITERION | Standard oncology eligibility concept with an acronym. |
| 7 | `adequate liver function` | 0.60 | INCLUSION_CRITERION | Concept expressed in corpus as specific lab thresholds — tests generalization past literal wording. |
| 8 | `no prior chemotherapy` | 0.60 | EXCLUSION_CRITERION | **Expected source is EXCLUSION.** Prior-chemo restrictions are written as exclusions, so a *correct* answer here is a disqualifying match. Guards the exclusion-flag logic. |

## Known-weak queries — tracked, not asserted

These currently perform **poorly**. They are recorded rather than asserted so a model upgrade
can be measured against them. Do not lower the bar to make them pass.

| Query | Current top score | What comes back | Why it fails |
|---|---|---|---|
| `trials studying a BRCA mutation` | **0.37** | `Luminal B breast cancer sub type`, `No evidence of hereditary cancer` | Requires knowing BRCA is a hereditary-cancer gene. MiniLM has no such domain knowledge; it lands on lexically adjacent text. Also partly a corpus gap — few BRCA-specific criteria in these 50 trials. |
| `recruiting trials I could join now` | **0.46** | `Subjects volunteered to join the study, signed informed consent` | Colloquial phrasing. The model latched onto "join" as *consent* rather than the intent (find open trials). Recruiting status is a **metadata filter**, not a semantic query — arguably user-education, not a model failure. |

**The pattern these expose:** retrieval is strong when the query is phrased like the corpus,
and degrades on everyday language or when domain knowledge is required. That is the predicted
weakness of a small general-purpose model on dense clinical text (`RAG_PLAN.md` §12).

## Baseline: all-MiniLM-L6-v2, 384 dims

Recorded 2026-08-07, so an upgrade has something to beat.

| # | Query | Top score | Top match source | Pass |
|---|---|---|---|---|
| 1 | ECOG performance status 0 or 1 | 0.975 | INCLUSION_CRITERION | yes |
| 2 | pregnant or breastfeeding | 0.983 | EXCLUSION_CRITERION | yes |
| 3 | brain metastases | 0.911 | EXCLUSION_CRITERION | yes |
| 4 | triple negative breast cancer | 0.992 | INCLUSION_CRITERION | yes |
| 5 | HER2 positive | 0.764 | INCLUSION_CRITERION | yes |
| 6 | measurable disease RECIST | 0.697 | INCLUSION_CRITERION | yes |
| 7 | adequate liver function | 0.661 | INCLUSION_CRITERION | yes |
| 8 | no prior chemotherapy | 0.707 | EXCLUSION_CRITERION | yes |
| — | trials studying a BRCA mutation | 0.370 | INCLUSION_CRITERION | tracked, not asserted |
| — | recruiting trials I could join now | 0.464 | INCLUSION_CRITERION | tracked, not asserted |

## Caveats worth remembering

- **8 queries is small.** Enough to catch a regression or a clear improvement; not enough to
  rank two decent models confidently.
- **Scores are not comparable across embedding models.** A different model produces a
  different score distribution, so after switching, compare *rankings and pass/fail*, and
  re-baseline the thresholds rather than assuming the old numbers transfer.
- **These are single-query judgments, not full relevance judgments.** A rigorous eval would
  label every trial per query and compute recall. This is a deliberate 80/20.
