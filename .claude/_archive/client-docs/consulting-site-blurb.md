# Clinical Trial Matcher — Consulting Site Blurb

Anonymized project summary for the consulting site. No patient specifics, no domain name —
see `project-blurb.md` for the internal, fuller version with AI/RAG detail.

---

**Clinical Trial Matching Platform** — a full-stack Java/React application built to help a
family member with metastatic cancer find relevant clinical trials, built from a real need
and now handling a real patient's care research.

The system ingests trial data from ClinicalTrials.gov, normalizes it into a relational
schema, and matches it against a structured patient profile — diagnosis, biomarkers, prior
treatment — using a combination of semantic search and deterministic clinical-logic checks.
Free-text eligibility criteria are chunked and embedded locally (no clinical data ever
leaves the machine for search), then layered with rule-based signals that catch what
similarity search alone gets wrong — for example, distinguishing "HER2-positive" from
"HER2-negative" trials, which differ by one word but are clinically opposite.

Deliberately, the tool never renders a verdict: no eligibility score, no auto-filtered
results. Every trial stays visible with the concerns and open questions attached, because
the judgment belongs to the patient and their care team, not the software.

**Stack:** Java 21 / Spring Boot, multi-module Gradle, MySQL + Liquibase, Qdrant vector
store with local ONNX embeddings, React + TypeScript + Tailwind, JWT auth with a
grant-based sharing model.

**Highlights:**
- Retrieval-augmented search tuned against a measured evaluation set, not by feel
- A deterministic matching layer that catches negation and polarity errors embedding
  similarity can't ("HR-positive" vs "HR-negative") — caught by measuring against the real
  corpus, not by unit tests alone
- Privacy-conscious by design: local embeddings by default, with any one deliberate
  exception clearly bounded and disclosed
- Deployed and in active use
