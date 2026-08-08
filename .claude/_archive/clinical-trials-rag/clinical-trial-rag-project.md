# Clinical Trial Tracker — Spring AI RAG Project

## Overview
A RAG (Retrieval-Augmented Generation) application built with Spring AI to help track and understand clinical trials relevant to a specific cancer diagnosis. Unlike a toy project (cookbook, resume site), this uses a real, useful corpus: clinical trial listings with dense eligibility criteria and technical descriptions — exactly the kind of unstructured text RAG is designed to handle.

**Design intent:** this is a discovery/triage tool, not a decision-maker. It should surface and explain candidate trials with citations back to the source eligibility text, so the final judgment stays with the patient, family, and oncology team.

## Why this is a strong RAG case study
- Trial eligibility criteria and descriptions are long, jargon-heavy free text — hard to search with plain keyword matching, well-suited to semantic search.
- Natural, realistic queries: "which trials would she qualify for with stage III and no prior chemo," "what trials nearby are studying this mutation," "explain the difference between these two trials."
- Requires combining semantic search with metadata filtering (phase, recruiting status, distance, cancer type) — more realistic practice than simpler corpora.
- Forces good practice around grounding/citations, since answers should point back to the exact eligibility line that matched.

## Data Source
- **clinicaltrials.gov API (v2)** — free, public, structured JSON. No scraping needed.
- Pull trials matching cancer type/stage/mutation periodically (e.g., scheduled job).
- Key fields to ingest: eligibility criteria (free text), brief/detailed description, arms/interventions, outcome measures, phase, recruiting status, locations.

## Architecture / Technical Shape

### 1. Ingestion pipeline
- Fetch trials from the clinicaltrials.gov API based on search criteria (condition, location, status).
- Chunk long text fields (eligibility criteria, descriptions) — this is where most RAG quality issues live, so give it real thought (e.g., split by inclusion/exclusion sections rather than arbitrary token windows).
- Embed chunks using Spring AI's `EmbeddingModel`.
- Store embeddings + metadata in a vector store.

### 2. Vector store
- **pgvector** is a natural fit — Spring AI has a built-in `VectorStore` abstraction for it.
- Store metadata alongside embeddings: phase, recruiting status, location/distance, cancer type, trial ID (for linking back to clinicaltrials.gov).

### 3. Retrieval
- Combine metadata filters (recruiting status = "recruiting", phase, distance radius) with semantic similarity search over criteria/description text.
- Tune top-k and similarity threshold based on how tightly you want matches to fit.

### 4. Generation
- Use Spring AI's `PromptTemplate` to inject retrieved chunks into the prompt.
- Have the LLM explain *why* a trial might fit — citing the specific eligibility line it matched — rather than just listing search results.
- Avoid framing output as a yes/no verdict; frame it as "here's what to look into / ask about."

### 5. Optional stretch goals
- Streaming responses for the chat interface.
- Source citations rendered as clickable links back to clinicaltrials.gov.
- Scheduled re-ingestion to catch newly posted or updated trials.
- Distance/location filtering using geocoding for site addresses.

## Core Spring AI concepts this project exercises
- `EmbeddingModel` for generating embeddings
- `VectorStore` abstraction (pgvector)
- `PromptTemplate` for grounding generation in retrieved context
- Chunking strategy for long, structured-but-messy real-world text
- Metadata filtering combined with semantic search
- (Optional) streaming chat responses

## Important caveat
This tool should assist discovery and understanding, not replace medical judgment. Keep the UX and prompt design oriented around "help me find and understand options" rather than "tell me what to do." Always surface sources/citations so the eligibility criteria can be double-checked against the original listing.
