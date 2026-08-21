# Clinical Trial App Data: Comprehensive Variant & Drug Qualification Rules

This document outlines the core genetic variants and cancer therapies required to build an automated eligibility-matching algorithm for clinical trials, covering both targeted breast oncology and pan-cancer protocols.

---

## 1. Clinically Actionable Genetic Variants (Breast Cancer Focus)

To properly filter trial eligibility, the application database must distinguish between **Somatic** (tumor-acquired) and **Germline** (inherited) genetic variants.

### Somatic Variants (Tumor Mutations)
These variants drive tumor progression and act as primary targets for experimental therapeutics.

*   **PIK3CA**
    *   *Prevalence:* 30% to 40% of Hormone Receptor-positive (HR+) cases.
    *   *Hotspot Mutations:* `p.H1047R`, `p.E545K`, `p.E542K`.
    *   *Trial Relevance:* Primary eligibility target for PI3K/AKT/mTOR pathway inhibitors.
*   **TP53**
    *   *Prevalence:* ~35% overall; >80% in Triple-Negative Breast Cancer (TNBC).
    *   *Trial Relevance:* Marker of aggressive disease; utilized in novel cell-cycle checkpoint and DNA-damage response trials.
*   **ESR1**
    *   *Prevalence:* Up to 14% of metastatic HR+ tumors.
    *   *Hotspot Mutations:* `p.Y537S`, `p.D538G`.
    *   *Trial Relevance:* Dictates resistance to traditional aromatase inhibitors; opens eligibility for next-generation SERDs.
*   **ERBB2 (HER2)**
    *   *Prevalence:* 2% to 5% (specific to somatic mutations, distinct from copy number amplifications).
    *   *Hotspot Mutations:* `p.L755S`, `p.V777L`.
    *   *Trial Relevance:* Guides matching to specialized tyrosine kinase inhibitor (TKI) studies.
*   **AKT1 & PTEN**
    *   *Prevalence:* 4% to 12% of advanced cases.
    *   *Hotspot Mutations:* `p.E17K` (in AKT1) or functional loss of PTEN.
    *   *Trial Relevance:* Maps directly to AKT inhibitor trial inclusions.

### Germline Variants (Inherited Mutations)
These variants indicate hereditary cancer syndromes and are foundational filters for DNA-damage repair therapies.

*   **BRCA1**: High-penetrance mutation. Strongly associated with early-onset TNBC. Key inclusion factor for PARP inhibitor and platinum chemotherapy trials.
*   **BRCA2**: High-penetrance mutation. Frequently associated with HR+ diseases. Used alongside BRCA1 to filter for synthetic lethality studies.
*   **PALB2**: Moderate-to-high penetrance mutation. Functions closely with BRCA2 and is increasingly grouped into identical trial buckets.
*   **CHEK2 & ATM**: Moderate-penetrance DNA-repair mutations commonly evaluated in target-discovery and basket clinical trials.

---

## 2. Prior Medication & Trial Qualification Rules

Prior exposure to specific cancer drugs serves as a critical qualification pivot in trial protocols—acting either as a prerequisite (inclusion) or a barrier (exclusion).

### Section A: Breast Cancer Focused Therapies

#### CDK4/6 Inhibitors (HR+/HER2- Disease)
*   **Palbociclib**: Prior use is often a mandatory inclusion requirement for second-line endocrine trials.
*   **Ribociclib**: Frequently used as an exclusion criteria in novel cell-cycle studies to avoid overlapping mechanisms.
*   **Abemaciclib**: Progression on this drug often qualifies patients for targeted resistance trials.

#### HER2-Targeted Therapies (HER2+ Disease)
*   **Trastuzumab**: Baseline prerequisite; virtually all advanced HER2 trials require previous exposure.
*   **Pertuzumab**: Frequently bundled with Trastuzumab as a standard first-line pre-treatment requirement.
*   **Trastuzumab Deruxtecan (T-DXd)**: An industry-standard ADC. Prior exposure heavily impacts eligibility for subsequent next-gen ADCs.
*   **Ado-Trastuzumab Emtansine (T-DM1)**: Progression past T-DM1 serves as a classic inclusion marker for late-stage salvage therapies.

#### Endocrine Therapies (Hormone Receptor-Positive)
*   **Fulvestrant**: Progression on this drug is an inclusion gateway for testing novel oral SERDs.
*   **Aromatase Inhibitors (Letrozole, Anastrozole, Exemestane)**: Prior failure on an AI is required to establish "endocrine-resistant" trial cohorts.
*   **Tamoxifen**: Crucial historical variable for matching pre-menopausal or early-stage recurrence populations.

#### PARP Inhibitors (BRCA-Mutated Disease)
*   **Olaparib & Talazoparib**: Prior exposure to these agents almost universally excludes patients from other PARP-inhibitor or DNA-damaging clinical trials due to cross-resistance.

---

### Section B: Global Oncology & Pan-Cancer Therapies

#### 1. Pan-Cancer Immunotherapies (Checkpoint Inhibitors)
*   **Pembrolizumab (Keytruda) / Nivolumab (Opdivo)**: Used broadly across solid tumors (Lung, Melanoma, Head & Neck, TNBC).
    *   *Inclusion Impact:* Phase II/III trials often require patients to be "checkpoint-refractory" to test new combination immunotherapies.
    *   *Exclusion Impact:* Patients with historical severe immune-related adverse events (irAEs) like pneumonitis or colitis are universally excluded from future immunotherapy trials.

#### 2. EGFR and ALK Inhibitors (Non-Small Cell Lung Cancer - NSCLC)
*   **Osimertinib (Tagrisso)**: The first-line standard for EGFR-mutant NSCLC.
    *   *Inclusion Impact:* Progression is required for trials testing next-gen EGFR inhibitors or combinations targeting secondary resistance mutations (e.g., `EGFR C797S`, `MET` amplification).
*   **Alectinib / Brigatinib / Lorlatinib**: ALK tyrosine kinase inhibitors.
    *   *Inclusion Impact:* Failure on early-generation ALK inhibitors qualifies patients for clinical studies evaluating next-generation bypass agents.

#### 3. VEGF/VEGFR Inhibitors (Anti-Angiogenesis Agents)
*   **Bevacizumab (Avastin) / Cabozantinib (Cabometyx)**: Used in Colorectal, Renal, and Liver cancers.
    *   *Exclusion Impact:* Because these drugs disrupt blood vessel formation, surgical trials or protocols testing drugs with high bleeding risks strictly exclude patients with recent exposure due to severe hemorrhage and wound-healing risks.

#### 4. Next-Generation Anti-Androgens (Prostate Cancer)
*   **Enzalutamide (Xtandi) / Apalutamide (Erleada) / Abiraterone (Zytiga)**:
    *   *Inclusion Impact:* Progression defines "Metastatic Castration-Resistant Prostate Cancer" (mCRPC), a mandatory prerequisite for trials testing radiopharmaceuticals (e.g., Pluvicto) or novel combination therapies.

#### 5. BRAF and MEK Inhibitors (Melanoma & Colorectal)
*   **Dabrafenib + Trametinib / Encorafenib + Cetuximab**:
    *   *Exclusion Impact:* If a patient possesses a targetable `BRAF V600E` mutation but has *not* received these approved standard-of-care agents, they are excluded from experimental salvage trials on ethical grounds until standard therapy fails.

---

## 3. Data Architecture Logic Matrix

Use this unified matrix to structure your backend matching algorithms:

| Variant or Drug | Class / Type | Inclusion Filter Logic (MATCH) | Exclusion Filter Logic (EXCLUDE) |
| :--- | :--- | :--- | :--- |
| **PIK3CA** | Somatic Mutation | Patient has positive variant status (`p.H1047R`, etc.). | Rarely disqualifies; used strictly as a positive selector. |
| **ESR1** | Somatic Mutation | Patient has acquired resistance mutation post-AI therapy. | Rarely disqualifies; confirms endocrine-resistant status. |
| **BRCA1 / BRCA2**| Germline Variant | Patient possesses a pathogenic inherited variant. | Required for targeted trials; excludes from general non-targeted cohorts. |
| **CDK4/6 Inhibitors**| Prior Therapy | Patient has documented progression *while on* the drug. | Patient has unresolved toxicities or is actively taking the drug. |
| **HER2 ADCs** | Prior Therapy | Patient has failed standard-of-care HER2 targeted agents. | Trial tests an identical drug payload (e.g., topoisomerase I inhibitors). |
| **PARP Inhibitors**| Prior Therapy | Patient requires trial for advanced non-DNA repair mechanism. | Excludes if the trial is testing a separate, competing PARP inhibitor. |
| **Pembrolizumab** | Prior Therapy | Patient is documented as "immunotherapy-resistant." | Patient has a documented history of severe immune-related toxicities (irAEs). |
| **Osimertinib** | Prior Therapy | Tracks progression to route patients into resistance trials. | Patient has not yet tried this first-line standard for EGFR+ disease. |
| **Bevacizumab** | Prior Therapy | Patient has advanced, heavily pre-treated colorectal/renal disease. | Active use or recent exposure within a protocol's strict washout window. |
| **Enzalutamide** | Prior Therapy | Patient meets the formal threshold for mCRPC classification. | Patient has hormone-sensitive disease that hasn't been challenged yet. |

---

## 4. Implementation Recommendations
1.  **Standardize Nomenclature**: Force variant inputs to use **HGVS nomenclature** formats (e.g., `NM_006218.4:c.3140A>G` or `p.H1047R`) to match incoming ClinicalTrials.gov registry API strings.
2.  **Integrate Reference Ontologies**: Map drug entries to **RxNorm** concepts and genetic variants to **ClinVar** or **COSMIC** IDs to ensure consistent filtering regardless of trial sponsor typos.
3.  **Incorporate Temporal Logic**: Build your database schema to accept a `discontinuation_date` for prior therapies. Your logic layer must calculate if the time elapsed satisfies a trial's **washout period** (typically 14 to 28 days for small molecules, or 4 to 6 weeks for monoclonal antibodies).

