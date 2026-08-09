# Breast Cancer Variants & Drug Classes for Clinical Trial Matching

Reference notes for building a clinical trial matching app. Covers (1) the molecular/receptor subtypes used to define trial populations and (2) the drug classes most commonly used as inclusion/exclusion criteria.

---

## Part 1: Breast Cancer Subtypes (Variants)

### Primary classification: receptor status
Used in the vast majority of trial eligibility criteria. Based on ER, PR, and HER2 testing (IHC/FISH), done routinely at diagnosis.

| Subtype | ER/PR | HER2 | Approx. % of cases | Notes |
|---|---|---|---|---|
| **HR+/HER2−** (Luminal A/B) | Positive | Negative | ~65–70% | Most common; Luminal A = low proliferation (Ki-67 < 20%), Luminal B = higher Ki-67 |
| **HER2+** (HR+ or HR−) | Either | Positive | ~15–20% | IHC 3+ or IHC 2+/FISH amplified |
| **Triple-negative (TNBC)** | Negative | Negative | ~10–15% | Most aggressive subtype; heterogeneous genetic profile |
| **HER2-low** (newer category) | Either | IHC 1+ or 2+/ISH− | Cuts across other subtypes | Now targetable with ADCs like trastuzumab deruxtecan |

### Layer 2: germline mutation status (increasingly a trial filter)
- **BRCA1 / BRCA2** — most commonly tested; especially relevant in TNBC
- **PALB2, ATM, CHEK2** — appear in newer trials, lower frequency
- **HRD (homologous recombination deficiency) status** — composite biomarker, used for PARP inhibitor eligibility

### Layer 3: research-grade molecular subtypes
- **PAM50 intrinsic subtypes**: Luminal A, Luminal B, HER2-enriched, Basal-like, Normal-like
- **TNBC-specific subtyping**: BLIS (HRD-associated), LAR, MES (PI3K pathway mutations common), Immunomodulatory (high immune infiltration, better prognosis)

### App-building recommendation
Build the core schema around the receptor-status table (used in >90% of trial inclusion criteria), then add BRCA1/2 and PIK3CA as secondary filters since these appear frequently in newer targeted-therapy trials.

---

## Part 2: Drug Classes That Affect Trial Qualification

Prior-treatment history is one of the biggest eligibility gates after biomarker status. Trials commonly filter on: drug class exposure (naive vs. treated), line of therapy, treatment setting (neoadjuvant/adjuvant vs. metastatic), and washout window (time since last dose).

### 1. Endocrine therapy (HR+ trials)
- **Aromatase inhibitors**: letrozole, anastrozole, exemestane
- **SERMs**: tamoxifen
- **SERDs**: fulvestrant; newer oral SERDs (e.g. elacestrant)
- Trials often split on "endocrine-sensitive" vs. "endocrine-resistant" status

### 2. CDK4/6 inhibitors — the single biggest eligibility gate currently
- **Palbociclib, ribociclib, abemaciclib**
- Most trials split into "CDK4/6i-naïve" (first-line) vs. "post-CDK4/6i" (progressed on/after) populations
- Washout/disease-free interval also matters (e.g. some trials allow prior adjuvant use only if >12 months disease-free before enrollment)

### 3. HER2-targeted agents (HER2+ trials)
- **Antibodies**: trastuzumab, pertuzumab
- **Antibody-drug conjugates (ADCs)**: trastuzumab deruxtecan (T-DXd/DS-8201), trastuzumab emtansine (T-DM1), trastuzumab duocarmazine
- **Tyrosine kinase inhibitors**: lapatinib, tucatinib, neratinib, pyrotinib
- Many later-line trials require prior trastuzumab exposure as an inclusion criterion

### 4. PI3K/AKT/mTOR pathway inhibitors (biomarker-linked, HR+ setting)
- **Alpelisib** (PIK3CA-mutant), **everolimus**, **capivasertib**
- Often paired with fulvestrant in post-CDK4/6i trials

### 5. PARP inhibitors (BRCA-mutant trials)
- **Olaparib, talazoparib**
- Eligibility almost always gated by germline BRCA1/2 status

### 6. Chemotherapy (esp. TNBC and later-line trials)
- **Taxanes**: paclitaxel, docetaxel
- **Anthracyclines**: doxorubicin, epirubicin
- **Platinum agents**: carboplatin, cisplatin
- **Antimetabolites**: capecitabine
- Number of prior chemo "lines" is a very common eligibility gate

### 7. Immunotherapy (TNBC, PD-L1+ trials)
- **Pembrolizumab, atezolizumab**
- Usually gated by PD-L1 status, not just prior use

### 8. ADCs beyond HER2 (newer TNBC/HR+ agents)
- **Sacituzumab govitecan** (Trop-2 ADC)
- Increasingly used as a "prior therapy" filter in later-line TNBC and HR+ trials

### App-building recommendation
Prioritize a schema with:
1. **Drug class exposure** (yes / no / naive)
2. **Line of therapy** (1st, 2nd, 3rd+)
3. **Setting** (neoadjuvant / adjuvant vs. metastatic)
4. **Washout window** (months since last dose)

CDK4/6 inhibitor status and number of prior chemo lines are the two filters most consistently used across active trials — highest value for lowest schema complexity.

---

*Compiled from published literature and ClinicalTrials.gov listings as a starting reference. Not exhaustive — recommend validating against a larger ClinicalTrials.gov sample before finalizing filter priorities.*
