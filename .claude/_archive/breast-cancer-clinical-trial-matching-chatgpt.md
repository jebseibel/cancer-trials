# Breast Cancer Clinical Trial Matching — Diagnosis, Biomarkers, and Treatment History

## Purpose

This document defines a practical starting point for a clinical-trial matching application focused on breast cancer.

The key design principle is:

> Do not model breast cancer as a single "variant" field. Clinical-trial eligibility is usually determined by a combination of histology, receptor/biomarker status, stage, molecular alterations, and prior/current treatment exposure.

---

# 1. Breast Cancer Types / Histology

The major breast-cancer categories worth capturing are:

| Type | Approximate frequency / importance | App priority |
|---|---:|---|
| Invasive ductal carcinoma (IDC) | ~70–80% of invasive cancers | Very high |
| Invasive lobular carcinoma (ILC) | ~10% | High |
| Mixed ductal/lobular | Smaller percentage | Medium |
| Ductal carcinoma in situ (DCIS) | Common non-invasive cancer | High, but separate from invasive disease |
| Inflammatory breast cancer | ~1–5% | High for specific trials |
| Paget disease | ~1–3% | Low |
| Other rare histologies | Usually <1% individually | Low initially |

For an MVP, start with IDC, ILC, mixed ductal/lobular, DCIS, inflammatory, Paget, and an "Other" category.

---

# 2. Receptor / Biomarker Classification

For clinical-trial matching, receptor status is often more important than histology.

At minimum capture:

```text
ER:   Positive / Negative / Unknown / Not Tested
PR:   Positive / Negative / Unknown / Not Tested
HER2: Positive / Negative / Low / Ultra-low / Unknown / Not Tested
```

Also consider:

```text
Ki-67
PD-L1
```

## Derived Clinical Subtypes

The application should derive commonly used groups rather than requiring users to enter them manually:

```text
HR-positive / HER2-negative
HR-positive / HER2-positive
HR-negative / HER2-positive
Triple-negative breast cancer (TNBC)
```

Where:

```text
HR-positive = ER-positive and/or PR-positive
Triple-negative = ER-negative + PR-negative + HER2-negative
```

HER2-low and HER2-ultralow should be represented separately from HER2-positive disease because newer therapies and trials can use these categories.

---

# 3. Stage

Capture:

```text
Stage 0 / DCIS
Stage I
Stage II
Stage III
Stage IV / metastatic
Unknown
```

For serious trial matching, eventually break this down into TNM:

```text
T
N
M
```

Also capture:

```text
Metastatic: Yes / No / Unknown
Recurrence: None / Local / Regional / Distant / Unknown
```

---

# 4. Molecular / Genetic Alterations

Important alterations to capture include:

```text
BRCA1
BRCA2
PALB2
PIK3CA
ESR1
AKT1
PTEN
TP53
```

For an extensible system, do not hard-code these as the only possible mutations.

Instead, have a general molecular finding structure:

```text
gene
variant
variant_type
pathogenicity
test_date
test_method
result
```

Then seed the system with the high-value breast-cancer genes above.

---

# 5. Recommended MVP Patient Model

A useful starting model is:

```text
Patient
  |
  +-- Cancer Diagnosis
  |     +-- cancer_type
  |     +-- histology
  |     +-- stage
  |     +-- T
  |     +-- N
  |     +-- M
  |     +-- metastatic
  |     +-- recurrence_status
  |
  +-- Biomarkers
  |     +-- ER
  |     +-- PR
  |     +-- HER2
  |     +-- Ki-67
  |     +-- PD-L1
  |
  +-- Molecular Findings
  |     +-- BRCA1
  |     +-- BRCA2
  |     +-- PIK3CA
  |     +-- ESR1
  |     +-- AKT1
  |     +-- PTEN
  |     +-- PALB2
  |     +-- TP53
  |
  +-- Treatment History
  |
  +-- Current Medications
```

---

# 6. Cancer Drugs That Can Change Trial Eligibility

Prior/current treatment is extremely important because clinical trials frequently specify eligibility based on previous therapy.

The application should capture both:

1. The exact drug.
2. The broader treatment class/exposure.

## Highest-Priority Drug Classes

| Priority | Drug class | Examples |
|---|---|---|
| 1 | CDK4/6 inhibitors | palbociclib, ribociclib, abemaciclib |
| 1 | Endocrine therapy | tamoxifen, anastrozole, letrozole, exemestane, fulvestrant |
| 1 | HER2-targeted therapy | trastuzumab, pertuzumab |
| 1 | HER2 antibody-drug conjugates | trastuzumab deruxtecan (T-DXd), ado-trastuzumab emtansine (T-DM1) |
| 1 | Chemotherapy | paclitaxel, docetaxel, doxorubicin, cyclophosphamide, carboplatin, capecitabine |
| 1 | Immunotherapy | pembrolizumab |
| 1 | PARP inhibitors | olaparib, talazoparib |
| 1 | PI3K/AKT/mTOR | alpelisib, inavolisib, capivasertib, everolimus |
| 1 | TROP2 antibody-drug conjugates | sacituzumab govitecan, datopotamab deruxtecan |
| 2 | SERDs / oral ER degraders | elacestrant, imlunestrant, vepdegestrant |
| 2 | Other HER2-targeted drugs | tucatinib, neratinib, lapatinib |
| 2 | Bone-directed therapy | denosumab, zoledronic acid |
| 2 | Other chemotherapy | gemcitabine, vinorelbine, eribulin, ixabepilone |
| 3 | Other immunotherapy | atezolizumab, nivolumab |
| 3 | Steroids / supportive oncology | dexamethasone, prednisone |
| 3 | Experimental therapies | Any investigational oncology agent |

---

# 7. High-Value Breast Cancer Drugs

## Endocrine Therapy

```text
tamoxifen
anastrozole
letrozole
exemestane
fulvestrant
elacestrant
imlunestrant
vepdegestrant
```

These are particularly important for HR-positive disease.

## CDK4/6 Inhibitors

```text
palbociclib
ribociclib
abemaciclib
```

This is one of the most important prior-treatment categories for HR-positive/HER2-negative trials.

## HER2 Therapy

```text
trastuzumab
pertuzumab
trastuzumab emtansine (T-DM1)
trastuzumab deruxtecan (T-DXd / Enhertu)
tucatinib
neratinib
lapatinib
```

## PARP Inhibitors

```text
olaparib
talazoparib
```

Especially important in patients with BRCA1/2 alterations.

## PI3K / AKT / mTOR

```text
alpelisib
inavolisib
capivasertib
everolimus
```

These become especially important when PIK3CA, AKT1, or PTEN biology is involved.

## Immunotherapy

```text
pembrolizumab
atezolizumab
nivolumab
```

Pembrolizumab is particularly important in TNBC trial history.

## TROP2 / Other ADCs

```text
sacituzumab govitecan
datopotamab deruxtecan
trastuzumab deruxtecan
trastuzumab emtansine
```

Antibody-drug conjugates are an increasingly important category for trial matching.

## Common Chemotherapy

At minimum, capture:

```text
paclitaxel
docetaxel
doxorubicin
cyclophosphamide
carboplatin
cisplatin
capecitabine
gemcitabine
vinorelbine
eribulin
ixabepilone
```

Rather than relying only on individual drugs, also derive chemotherapy exposure categories.

---

# 8. Chemotherapy Class / Exposure

Create broader exposure categories:

```text
taxane
anthracycline
platinum
antimetabolite
alkylating_agent
topoisomerase_inhibitor
microtubule_inhibitor
```

This lets a trial match criteria such as "previous taxane therapy" without requiring the trial matcher to know every individual taxane.

---

# 9. Treatment History — Critical Data Model

Do NOT simply store:

```text
drug = palbociclib
```

Instead, each treatment should contain:

```text
Treatment
---------
drug
drug_class

start_date
end_date

line_of_therapy

setting
    - neoadjuvant
    - adjuvant
    - metastatic
    - recurrent
    - maintenance

intent
    - curative
    - disease_control
    - palliative
    - preventive
    - unknown

reason_stopped
    - progression
    - toxicity
    - completed
    - patient_choice
    - physician_decision
    - other
    - unknown

best_response
    - complete_response
    - partial_response
    - stable_disease
    - progressive_disease
    - unknown

progressed_on_treatment
```

---

# 10. Treatment Line

Capture:

```text
1L
2L
3L
4L+
```

The line of therapy can materially change eligibility.

For example:

```text
Ribociclib
  Setting: metastatic
  Line: 1
  Duration: 14 months
  Reason stopped: progression
  Progressed on treatment: YES
```

is very different from:

```text
Ribociclib
  Setting: adjuvant
  Reason stopped: completed planned treatment
  Progressed on treatment: NO
```

Both patients have received ribociclib, but they may qualify for very different studies.

---

# 11. Prior Treatment Exposure Flags

For efficient trial matching, derive high-level exposure flags from the treatment history.

```text
prior_endocrine_therapy
prior_cdk46_inhibitor
prior_chemotherapy
prior_taxane
prior_anthracycline
prior_platinum
prior_her2_therapy
prior_her2_adc
prior_trop2_adc
prior_parp_inhibitor
prior_pi3k_inhibitor
prior_akt_inhibitor
prior_mtor_inhibitor
prior_immunotherapy
prior_serd
prior_experimental_therapy
```

Each should ideally be tri-state or four-state:

```text
YES
NO
UNKNOWN
NOT_ASSESSED
```

Do not treat "not documented" as "no."

---

# 12. Treatment Response Flags

Some trials care specifically about whether the patient progressed during or after a treatment.

Capture:

```text
progressed_on_drug
progressed_after_drug
response_to_drug
duration_on_drug
time_since_last_dose
```

These are often more useful for trial matching than the mere fact that a patient once received the medication.

---

# 13. Other Prior Treatments That Can Affect Eligibility

Do not limit the treatment history to drugs.

Create separate categories for:

```text
Prior Cancer Therapy
Prior Radiation
Prior Surgery
Prior Investigational Therapy
Concomitant Medications
```

Some trials also care about:

```text
recent surgery
recent radiation
prior stem-cell transplant
prior investigational therapy
current systemic corticosteroids
immunosuppressive medications
anticoagulants
live vaccines
```

These should be modeled separately from cancer drugs.

---

# 14. Recommended Overall Data Model

```text
Patient
  |
  +-- Diagnosis
  |     |
  |     +-- Cancer type
  |     +-- Histology
  |     +-- Stage
  |     +-- TNM
  |     +-- Metastatic status
  |     +-- Recurrence
  |
  +-- Biomarkers
  |     |
  |     +-- ER
  |     +-- PR
  |     +-- HER2
  |     +-- HER2-low
  |     +-- HER2-ultralow
  |     +-- Ki-67
  |     +-- PD-L1
  |
  +-- Molecular Findings
  |     |
  |     +-- BRCA1
  |     +-- BRCA2
  |     +-- PALB2
  |     +-- PIK3CA
  |     +-- ESR1
  |     +-- AKT1
  |     +-- PTEN
  |     +-- TP53
  |     +-- Other
  |
  +-- Treatment History
  |     |
  |     +-- Drug
  |     +-- Drug class
  |     +-- Start/end
  |     +-- Line
  |     +-- Setting
  |     +-- Intent
  |     +-- Response
  |     +-- Progression
  |     +-- Reason stopped
  |
  +-- Radiation History
  |
  +-- Surgery History
  |
  +-- Investigational Therapy
  |
  +-- Concomitant Medications
  |
  +-- Trial Matching
        |
        +-- Inclusion criteria
        +-- Exclusion criteria
        +-- Matching evidence
        +-- Missing information
```

---

# 15. Recommended Architecture for the Trial Matcher

The matching system should not simply ask:

```text
Does patient have breast cancer?
```

It should evaluate:

```text
Diagnosis
    ↓
Histology
    ↓
Stage
    ↓
ER / PR / HER2
    ↓
Molecular alterations
    ↓
Prior treatment exposure
    ↓
Treatment line
    ↓
Treatment response/progression
    ↓
Current treatment
    ↓
Prior radiation/surgery
    ↓
Other eligibility criteria
    ↓
Trial match
```

A trial might effectively require something like:

```text
Metastatic
AND
HR-positive
AND
HER2-negative
AND
PIK3CA mutation
AND
prior endocrine therapy
AND
prior CDK4/6 inhibitor
AND
progression after CDK4/6 inhibitor
AND
2+ prior lines of therapy
```

Your data model needs to represent each condition independently.

---

# 16. Important Design Principle: Unknown Is Not Negative

This is especially important in clinical applications.

Never collapse:

```text
HER2 = Negative
```

and:

```text
HER2 = Unknown
```

Likewise:

```text
BRCA1 = Negative
```

is not the same as:

```text
BRCA1 = Not Tested
```

Use explicit states such as:

```text
POSITIVE
NEGATIVE
UNKNOWN
NOT_TESTED
PENDING
```

This prevents the trial matcher from incorrectly excluding or including patients.

---

# 17. Recommended MVP Priority

If building this incrementally, implement in this order.

## Phase 1 — Core eligibility

```text
Cancer type
Histology
Stage
Metastatic status
ER
PR
HER2
```

## Phase 2 — Treatment history

```text
Endocrine therapy
CDK4/6 inhibitors
Chemotherapy
HER2 therapy
Immunotherapy
PARP inhibitors
ADC exposure
```

## Phase 3 — Molecular matching

```text
BRCA1
BRCA2
PIK3CA
ESR1
AKT1
PTEN
PALB2
```

## Phase 4 — Detailed treatment matching

```text
Line of therapy
Treatment duration
Progression on treatment
Response
Reason stopped
Time since treatment
```

## Phase 5 — Full clinical eligibility

```text
Prior radiation
Prior surgery
Investigational therapies
Concomitant medications
Organ function
Performance status
Laboratory criteria
Age
Menopausal status
Pregnancy status where applicable
```

---

# 18. Recommended Drug Dictionary Structure

Do not hard-code a flat list of drug names into the application.

Use a drug dictionary:

```text
Drug
----
id
generic_name
brand_name
drug_class
mechanism
cancer_types
active
```

Example:

```text
drug:
  generic_name: ribociclib
  brand_name: Kisqali
  drug_class: CDK4/6 inhibitor
  mechanism: CDK4/CDK6 inhibitor
  cancer_types:
    - breast
  active: true
```

Then treatment history references the drug:

```text
Treatment
---------
patient_id
drug_id
start_date
end_date
line_of_therapy
setting
response
progressed_on_treatment
reason_stopped
```

This makes the system extensible as new drugs and trial requirements appear.

---

# 19. Use Standard Terminologies Where Possible

For a production clinical-trial application, prefer established terminology systems rather than creating a proprietary vocabulary.

Useful standards/resources include:

- NCI Thesaurus (NCIt) for cancer terminology.
- RxNorm for normalized medication concepts.
- SNOMED CT where appropriate for clinical concepts.
- LOINC for laboratory/clinical measurements.
- ClinicalTrials.gov terminology and structured trial data.

The internal application can have friendly names while retaining standardized identifiers underneath.

---

# 20. Final Recommended Breast-Cancer Matching Schema

At a high level:

```text
BREAST CANCER
│
├── Diagnosis
│   ├── Histology
│   ├── Stage
│   ├── TNM
│   ├── Metastatic
│   └── Recurrence
│
├── Biomarkers
│   ├── ER
│   ├── PR
│   ├── HER2
│   ├── HER2-low
│   ├── HER2-ultralow
│   ├── Ki-67
│   └── PD-L1
│
├── Molecular
│   ├── BRCA1
│   ├── BRCA2
│   ├── PALB2
│   ├── PIK3CA
│   ├── ESR1
│   ├── AKT1
│   ├── PTEN
│   └── TP53
│
├── Treatment History
│   ├── Endocrine
│   ├── CDK4/6
│   ├── HER2
│   ├── HER2 ADC
│   ├── TROP2 ADC
│   ├── PARP
│   ├── PI3K/AKT/mTOR
│   ├── Immunotherapy
│   └── Chemotherapy
│
├── Other Cancer Treatment
│   ├── Radiation
│   ├── Surgery
│   └── Investigational therapy
│
├── Current Medications
│   ├── Cancer medications
│   ├── Steroids
│   ├── Immunosuppressants
│   └── Other medications
│
└── Trial Matching
    ├── Inclusion criteria
    ├── Exclusion criteria
    ├── Matched criteria
    ├── Failed criteria
    └── Missing information
```

## Bottom Line

For a clinical-trial application, the most important information is not simply **"which breast cancer variant does she have?"**

The useful patient representation is:

**Histology + Stage + ER/PR/HER2 + Molecular alterations + Prior treatments + Treatment sequence + Treatment response/progression.**

In particular, make **prior CDK4/6 therapy, endocrine therapy, HER2 therapy, chemotherapy, immunotherapy, ADCs, PARP inhibitors, and PI3K/AKT/mTOR therapy** first-class concepts in the matching engine.

> **Clinical disclaimer:** This schema is an application/data-modeling starting point, not a clinical decision rule. Actual trial eligibility should be determined from the current study's official inclusion/exclusion criteria and appropriate clinical review.
