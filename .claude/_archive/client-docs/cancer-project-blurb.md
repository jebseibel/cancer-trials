# Project Blurb

A one-paragraph description of the project, with the AI/RAG details included.

---

**Cancer Trial Finder** is a Java 21 / Spring Boot 3.5 multi-module application that helps match
cancer patients to clinical trials. It ingests trial data from ClinicalTrials.gov and patient
records from UCHealth's Epic FHIR API (OAuth with PKCE), normalizes both into a MySQL schema, and
exposes them through a REST API backed by a React/TypeScript frontend. The AI layer is a
retrieval-augmented (RAG) search built on Spring AI: trial text is split by a purpose-built chunker
that produces one chunk per eligibility criterion — derived from measured bullet and header
conventions in real trial data rather than assumptions — so a query like "no prior chemotherapy"
matches a specific criterion line and can be cited back to it. Chunks are embedded locally by an
in-process ONNX sentence-transformer model (all-MiniLM-L6-v2, 384 dimensions), meaning no API keys,
no per-chunk cost, and no clinical text leaving the machine, and are stored in a Qdrant vector
store, with an optional criteria-only mode that keeps trial-design prose from crowding out the
eligibility text that decides who can join. Semantic search runs two-phase — vector similarity with metadata filters (recruiting status,
inclusion vs. exclusion) returns trial identifiers, which are then hydrated from MySQL — and all of
it is written against Spring AI's VectorStore/EmbeddingModel interfaces so the store or model can be
swapped through configuration. Retrieval quality is tuned against a versioned evaluation set rather
than by feel, and a separate ai-provider module (OpenRouter client, tool registry, cost tracking,
audit logging) sits shelved on disk awaiting keys, so generation on top of retrieval is a wiring
step, not a rewrite. Alongside retrieval, a deterministic matching layer reads each trial's own
prose for the two things ClinicalTrials.gov does not publish as fields — what a trial is trying
to achieve and what stage of disease it studies — because those are what decide whether a trial
is worth a patient's time, and embedding similarity cannot see either.
