# Patient Variants and Prior Treatments — Table Design

Two new patient-side tables whose sole job is **killing false matches**. Companion to
`../research/` (the three source docs), `trial-match-tables.md`, and `../CURRENT_STATE.md`.

Written 2026-08-09. MySQL, Liquibase-managed, `BaseDb` conventions throughout.

## Why these exist

On 2026-08-08 a semantic search over 249 trials ranked a **triple-negative** trial as the
single most relevant result (0.718) for an **ER+/PR−/HER2−** patient — above the trial that
actually matched the patient's profile line for line. Two more clinically wrong trials ranked among the
most relevant by the same mechanism: "HR-negative HER2-negative" and "HR-positive
HER2-negative" differ by one token, and embedding similarity cannot see the difference.

**That it was the top result is the whole problem.** These were not marginal hits buried deep
in a list where nobody would look — they were what the tool put forward first, which is what a
person reads and acts on. A false match that ranks last costs nothing; one that ranks first
costs a real conversation with an oncologist.

Receptor status was already in the database. Nothing read it.

That is the pattern these tables address. **Every column here exists to contradict a
high-similarity match that is clinically wrong.** A field that cannot kill a false match does
not belong in these tables, however clinically interesting it is.

The second failure from that same run is the one these tables prevent going forward: scoring
counted keyword co-occurrence, so a trial could score well on *both* "first-line
treatment-naive" and "post-CDK4/6 progression" — two mutually exclusive populations. Prior
treatment state is what separates them, and it was not captured anywhere.

## Design decisions

**Columns, not rows.** One row per patient per table, with a column per popular variant and
per popular drug class. The alternative — a row per finding — is more extensible and worse
here: a fixed set of columns renders as a single form the patient fills out once, and an
unanswered column is *visible* rather than silently absent. Extensibility is worth less than
completeness when there is one patient and the failure mode is a missing answer.

Each table carries a free-text `other` column for what the fixed set does not cover.

**Unknown is never negative.** From `../research/breast-cancer-clinical-trial-matching-chatgpt.md`
§16, and it is the single most important rule here. "Tested negative for BRCA1" and "never
tested for BRCA1" are clinically different: the first may exclude her from a PARP trial, the
second means the question is open and worth asking her oncologist. Collapsing them either
hides an option or invents a qualification she does not have.

Every status column therefore defaults to `NOT_TESTED` or `UNKNOWN`, never to a negative.

**Yes/no fields render as checkboxes; everything else gets the state it needs.** Where a
question genuinely has two answers, a checkbox is right. Where it does not, forcing two
answers is what produces the false match — see the CDK4/6 case below.

**No verdicts.** Unchanged from the project rule. These columns feed demotion and flagging,
never automatic exclusion. A mismatch moves a trial down the list and says why; it never
removes it.

---

## `patient_variant`

One row per patient. Molecular and germline findings, at the depth a patient can actually
answer from what they have been told.

```
app_user_id            bigint          not null    -- FK -> app_user.id
patient_diagnosis_id   bigint                      -- FK -> patient_diagnosis.id, nullable

-- Somatic (tumor) findings
pik3ca_status          varchar(16)     -- the most consequential column in this table
esr1_status            varchar(16)
tp53_status            varchar(16)
akt1_status            varchar(16)
pten_status            varchar(16)
erbb2_somatic_status   varchar(16)     -- somatic HER2 mutation, NOT the same as HER2 IHC

-- Germline (inherited) findings
brca1_status           varchar(16)
brca2_status           varchar(16)
palb2_status           varchar(16)
atm_status             varchar(16)
chek2_status           varchar(16)

-- Composite and supporting biomarkers
hrd_status             varchar(16)     -- homologous recombination deficiency
pdl1_status            varchar(16)     -- gates most TNBC immunotherapy trials
ki67_percent           int             -- 0..100, Luminal A vs B; nullable

-- Provenance and escape hatch
germline_test_done     varchar(16)     -- was germline panel testing done at all
somatic_test_done      varchar(16)     -- was tumor sequencing done at all
test_date              date
test_lab               varchar(255)    -- e.g. Foundation Medicine, Guardant, Invitae
other_variants         varchar(1000)   -- free text, anything not columned above
notes                  text
```

**Status column vocabulary** — the same five states everywhere:

```
DETECTED | NOT_DETECTED | VUS | NOT_TESTED | UNKNOWN
```

`VUS` (variant of uncertain significance) is a real result a patient will have been told, and
it is neither positive nor negative for trial purposes. Omitting it forces a wrong answer.

`germline_test_done` / `somatic_test_done` are separate from the per-gene columns on purpose.
"Never had genetic testing" is one answer covering eleven genes, and asking it once is both
kinder to the patient and more informative than eleven independent `NOT_TESTED` values that
might mean "not on the panel."

### Why these genes

`PIK3CA` first: 30-40% of HR+ cases, and the patient has a detected PIK3CA mutation, which is
what matched NCT05753657 at 0.717 on her exact profile line. `ESR1` gates next-generation SERD
trials and marks AI resistance. `BRCA1/2` gate PARP inhibitor trials and are the germline
questions most often already answered. `PD-L1` gates most TNBC immunotherapy trials — included
even though this patient is HR+, because a status column that is `NOT_TESTED` still correctly
prevents a false immunotherapy match.

`ERBB2` somatic is named distinctly from HER2 receptor status because they are different tests
with different trial implications, and conflating them is an easy and expensive mistake.

---

## `patient_prior_treatment`

One row per patient. Drug-class exposure with enough state to distinguish the populations
trials actually split on.

**Named `patient_prior_treatment`, not `patient_medication`** — that name is taken by the
FHIR prescription mirror (changeset 020), which is a different thing: it records what Epic
prescribed, keyed on a non-null unique `fhir_resource_id`, so nothing can be hand-entered
into it. This table records *exposure and outcome*, which is what gates eligibility. The two
are complementary; neither replaces the other.

```
app_user_id            bigint          not null    -- FK -> app_user.id
patient_diagnosis_id   bigint                      -- FK -> patient_diagnosis.id, nullable

-- Priority 1 classes: the gates that appear most in this corpus
cdk46_status           varchar(24)     -- the single biggest eligibility gate
endocrine_status       varchar(24)     -- AI / tamoxifen / fulvestrant as a class
serd_status            varchar(24)     -- oral SERDs, separate from endocrine above
chemo_status           varchar(24)
her2_therapy_status    varchar(24)     -- trastuzumab, pertuzumab
her2_adc_status        varchar(24)     -- T-DXd, T-DM1
trop2_adc_status       varchar(24)     -- sacituzumab govitecan, dato-DXd
parp_status            varchar(24)     -- olaparib, talazoparib
pi3k_akt_mtor_status   varchar(24)     -- alpelisib, capivasertib, everolimus
immunotherapy_status   varchar(24)     -- pembrolizumab, atezolizumab

-- Chemotherapy sub-exposures: trials say "prior taxane", not "prior chemotherapy"
taxane_status          varchar(24)
anthracycline_status   varchar(24)
platinum_status        varchar(24)

-- Named drugs, for the classes above where the specific agent matters
current_drug_names     varchar(1000)   -- what she is on right now, free text
prior_drug_names       varchar(1000)   -- what she has been on, free text

-- Line and setting: cheap to ask, and they gate hard
lines_of_therapy_metastatic  int       -- 0 = treatment-naive in the metastatic setting
had_neoadjuvant        tinyint(1)      -- yes/no, renders as a checkbox
had_adjuvant           tinyint(1)      -- yes/no, renders as a checkbox
had_radiation          tinyint(1)      -- yes/no, renders as a checkbox
had_surgery            tinyint(1)      -- yes/no, renders as a checkbox
last_treatment_end_date      date      -- drives washout-window checks
currently_on_treatment tinyint(1)      -- yes/no, renders as a checkbox

other_treatments       varchar(1000)   -- free text
notes                  text
```

**Status column vocabulary** — five states, and the reason this is not a boolean:

```
NEVER | CURRENT | PROGRESSED | STOPPED_OTHER | UNKNOWN
```

- `NEVER` — never received this class. Qualifies for "naive" cohorts.
- `CURRENT` — on it now, has not progressed.
- `PROGRESSED` — took it, stopped because the disease progressed. Qualifies for
  "post-progression" cohorts.
- `STOPPED_OTHER` — took it, stopped for toxicity or completed a planned course. **Not the
  same as progressed** — a patient who completed adjuvant therapy years ago is often still
  eligible for first-line metastatic trials.
- `UNKNOWN` — not established.

### Why this cannot be a boolean

This is the concrete case that decides the design. **The patient is currently on abemaciclib
(a CDK4/6 inhibitor) and has not progressed on it.**

A boolean `prior_cdk46 = true` is literally true and clinically misleading: trials split into
*CDK4/6-naive* (first-line) and *post-CDK4/6 progression* populations, and a bare `true` reads
as the second. She belongs to neither — she is on it now. Matching her to post-CDK4/6 trials
on the strength of a boolean is the same category of error as the triple-negative match: a
true fact producing a wrong conclusion.

`CURRENT` vs `PROGRESSED` vs `NEVER` is one dropdown on a form and it is the difference between
a useful shortlist and a misleading one.

### Why `lines_of_therapy_metastatic` is separate

`patient_diagnosis` already has `prior_chemo_regimens`, which counts chemotherapy only. Trials
gate on total prior lines in the metastatic setting — endocrine, targeted, and chemotherapy
together. The existing column cannot answer that, and a patient with zero chemotherapy but two
prior endocrine lines is not treatment-naive.

Zero is a meaningful value here, not a missing one: it means treatment-naive in the metastatic
setting, which is itself an inclusion criterion. Nullable, so "unknown" stays distinct from
"zero".

---

## Relationship to `patient_diagnosis`

These do not replace it. The split follows the research docs' recommendation of separating
Diagnosis / Biomarkers / Molecular / Treatment History:

- **`patient_diagnosis`** keeps what it has: histology, stage, receptor status (ER/PR/HER2),
  metastatic sites, ECOG, menopausal status, demographics. It is the "who she is clinically"
  row.
- **`patient_variant`** is the "what her tumor and germline carry" row.
- **`patient_prior_treatment`** is the "what she has been through" row.

**One row of each per patient, keyed on `app_user_id`**, with a nullable
`patient_diagnosis_id` so a row survives the diagnosis being deleted and recreated — which
already happened once, on 2026-08-08.

**Note the planned key change.** `_archive/patient/PATIENT_MODEL_PLAN.md` proposes replacing
`app_user_id` with `patient_id` across `patient_diagnosis` and `trial_status`, on the grounds
that a clinical fact belongs to the patient rather than to whoever is holding the laptop. That
plan is unbuilt, so these tables follow the current convention — but they will need the same
FK move if it ever ships. Building them now adds two tables to that migration, which is a real
cost and a small one; the alternative is blocking a needed feature on an unscheduled refactor.

**Two overlaps to resolve when these are built:**

- `patient_diagnosis.biomarkers` is a free-text varchar(1000) that currently holds the PIK3CA
  finding. Once `patient_variant` exists, that column is redundant. Recommend keeping it
  populated as a human-readable summary and treating `patient_variant` as authoritative for
  matching — the same both-derived-from-one-source pattern `display_text` follows elsewhere.
- `patient_diagnosis.prior_treatments` (free text) and `prior_chemo_regimens` (int) are
  superseded by the new table. Same recommendation: keep as narrative, match on the structured
  columns.

## What this deliberately does not include

Each of these appears in the research docs and is left out on purpose:

- **HGVS nomenclature, ClinVar and COSMIC IDs.** Proposed by
  `../research/breast-cancer-clinical-trial-matching-gemini.md`. A patient does not know the
  transcript reference for her PIK3CA mutation, and a column she cannot fill is a column that
  is always null. Revisit if variant data ever arrives from a structured lab feed rather than
  from a person.
- **Per-treatment episode rows** with `intent`, `best_response`, `duration_on_drug`. The
  research docs are right that this is the richer model; it is also a data-entry burden for
  one patient and a form nobody will finish. The `PROGRESSED` / `STOPPED_OTHER` distinction
  captures the eligibility-relevant part of `reason_stopped` at a fraction of the cost.
- **Pan-cancer drug classes** — EGFR/ALK, VEGF, anti-androgens, BRAF/MEK. Roughly half of the
  Gemini doc covers these. They are the opposite direction from narrowing to breast cancer.
- **Concomitant medications, organ function, lab criteria.** Real eligibility gates, but they
  belong to a later phase and mostly live in `lab_result` already.

## Open questions

- **Do these tables get their own page, or extend the Diagnosis page?** One long form is
  likelier to be completed in one sitting than three separate ones; three pages are easier to
  revisit. Leaning toward one page with three sections.
- **Who fills this in?** Written as patient-answerable, but much of it is more reliably read
  off a pathology or genomic report. If the reports are available, entry accuracy improves and
  `test_lab` / `test_date` become meaningful.
- **Should `patient_variant` rows be versioned?** Variant status changes when new testing is
  done, and a match persisted against the old state becomes uninterpretable. `trial_match`
  already solves this by denormalizing a snapshot; the same approach likely extends here.
- **Does HER2-low get its own column?** `HER2-low` and `HER2-ultralow` cut across the
  positive/negative split and are newly targetable with ADCs. It arguably belongs on
  `patient_diagnosis` beside `her2_status` rather than here. Worth deciding before the ADC
  columns get used for matching.
