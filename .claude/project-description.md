# Breast Cancer Trial Finder — Project Description

## Overview

A clinical-trial finder and tracker for a specific patient. It pulls trial data from
ClinicalTrials.gov, normalizes it into a relational schema, holds a structured patient record
(diagnosis, genomic variants, prior treatment), and ranks trials against that record — reporting
concerns and open questions rather than eligibility verdicts.

Backend is Java 21 / Spring Boot with a REST API; frontend is React + TypeScript served from the
same jar. The app is deployed at breastcancertrialfinder.com.

## Purpose

The tool exists to answer a real question for a real patient: which trials are worth asking her
oncology team about. Every design decision follows from that — see "Hard rules" below.

## Architecture

### Multi-module structure

Gradle multi-module build, root project name `cancer`:

- **Root module** — Spring Boot application: web layer (controllers, request/response DTOs),
  service layer including `service/matching/`, security, config
- **`:common`** — shared domain objects, enums, exceptions, utilities. No Spring dependency
- **`:database`** — JPA entities, repositories, mappers, db services, Liquibase migrations
- **`:datafetcher`** — ClinicalTrials.gov ingestion and UCHealth Epic FHIR ingestion
- **`:rag`** — chunking, embedding, indexing and retrieval over trial eligibility text
- **`:ai-provider`** — present on disk but commented out of `settings.gradle`; not part of the
  build
- **Frontend** — React/TypeScript with Vite, served as static assets from Spring Boot
- **`playwright/`** — a standalone Gradle build for MyChart scraping; not a module of this project

### Backend layered architecture

Each entity follows a consistent pattern, top to bottom:

1. **Controller** — REST endpoints, DTO conversion, Swagger annotations
2. **Service** — business logic and validation, pagination and sorting
3. **Database Service** — low-level persistence
4. **Entity** — JPA entities, repositories, and mappers

A per-controller package-private `Converter` translates between DTOs and domain objects at the
boundary.

### Backend package structure

```
com.seibel.cancer
├── common/         # Shared domain objects, enums, exceptions, utilities  (:common)
├── database/       # JPA entities, repositories, db services, Liquibase   (:database)
├── config/         # SecurityConfig, WebConfig
├── security/       # JwtUtil, JwtAuthenticationFilter, CustomUserDetailsService
├── service/        # Business services, plus service/matching/ for trial matching
└── web/            # Controllers, request/response DTOs, GlobalExceptionHandler
```

### Frontend structure

```
frontend/
├── src/
│   ├── pages/      # Login, Dashboard, PatientRecord, Diagnosis, Variants,
│   │               # PriorTreatment, TrialSearch, TrialDetail, RankedTrials,
│   │               # SavedTrials, Ingestion
│   ├── components/ # Layout, ProtectedRoute, FormControls, JobResultModal
│   ├── services/   # api.ts — API client with JWT interceptor
│   ├── types/      # api.ts — TypeScript interfaces and controlled vocabularies
│   └── lib/        # PatientContext, accessLevel, receptorSubtype, tier1Matching
```

## Technology stack

### Backend
- **Java 21**
- **Spring Boot 3.5.7** — web, data-jpa, security, validation, thymeleaf
- **Spring Security** — JWT authentication plus a `UserPatient` grant model for authorization
- **Gradle 8.14.3**
- **MySQL** — AWS RDS compatible
- **Liquibase** — schema version control
- **Qdrant** — vector store for trial-criteria retrieval
- **ONNX / all-MiniLM-L6-v2** — local embeddings, 384 dims, so clinical text never leaves the
  machine
- **Lombok**, **ModelMapper**, **Spring Data JPA**, **Swagger/OpenAPI**

### Frontend
- **React 19**, **TypeScript**, **Vite**, **React Router**, **Tailwind CSS**
- **TanStack React Query**, **Axios**, **React Hook Form**, **Zod**, **Recharts**

## Core domain

**Trial side** — `Trial` plus `ArmGroup`, `Condition`, `EligibilityRule`, `Intervention`,
`Keyword`, `Location`, `Outcome`, `OverallOfficial`, `Sponsor`, `TrialSource`, `TrialStatus`.

**Patient side** — `Patient`, `PatientDiagnosis`, `PatientVariant`, `PatientPriorTreatment`,
`PatientMedication`, `LabResult`, `LabResultComponent`, `Medication`, and `UserPatient` linking
a login to the records they may see.

**Matching and ingestion** — `SavedTrialMatch` / `SavedTrialMatchCriterion` for persisted
results; `TrialAssessment`, `EligibilitySignal`, `SignalOutcome` for evaluation output;
`StagingRawTrial` and `StagingRawFhirResource` as landing tables for raw payloads;
`UcHealthOAuthToken` for Epic auth.

Patient-facing vocabularies are deliberately five-state rather than boolean —
`VariantStatus` is `DETECTED | NOT_DETECTED | VUS | NOT_TESTED | UNKNOWN`, and `TreatmentStatus`
is `NEVER | CURRENT | PROGRESSED | STOPPED_OTHER | UNKNOWN`. "Not tested" is not "not detected",
and a patient currently on a drug is not the same as one who progressed on it; collapsing either
to a boolean matches the patient to the wrong half of the corpus.

## Key features

- **Complete CRUD** on every core entity, paginated and sortable
- **Soft deletes** — rows marked INACTIVE with a `deletedAt` timestamp, never hard-deleted
- **UUID-based external ids** — `extid` is the only identifier crossing the API boundary
- **JWT authentication** plus ranked authorization: `VIEW_TRIALS < VIEW_RECORD < EDIT_RECORD <
  OWNER`, enforced by `CurrentUserService`
- **Login rate limiting** — 8 consecutive failures per IP+username returns 429 with `Retry-After`
- **Two-step ingestion** — pulling trials and making them searchable are separate operations
- **Trial ranking** — assesses the corpus against the patient record on file and orders by
  concern and applicable-signal counts

⚠️ There is **no automatic string cleanup**. Empty strings are stored as-is; a blank CSV cell
loaded by Liquibase lands as `''`, not NULL.

## Hard rules this project follows

**extid only, everywhere.** No internal numeric id is ever exposed to the frontend or accepted
back, including where a numeric foreign key would be natural.

**No eligibility verdicts.** No fit score or percentage, no auto-exclusion of trials, and
"unknown" is a first-class result. A failed check flags and demotes but never removes a trial —
a parsing failure must not silently take an option away. The judgement stays with the patient,
family, and oncology team.

**Ingestion and indexing are separate steps.** Writing trials to MySQL and embedding them into
the vector store have very different costs, and keeping them apart keeps that visible.

**Module dependency direction.** `:datafetcher` and `:rag` depend on `:common`/`:database`, and
root depends on them — so neither can call root's `service` package without creating a cycle.
Both call `*DbService` classes directly.

## Development environment

- **OS**: Ubuntu 24.04
- **Backend**: Java 21, Spring Boot — started and stopped by the user
- **Frontend**: `npm run dev` in `frontend/`
- **Database reset**: n8n webhook at `http://localhost:5678/webhook/clear-db` (GET)

## API endpoints

All entity endpoints follow the same shape and are keyed on `extid`:

- `GET /api/{entity}` — list, paginated
- `GET /api/{entity}/{extid}` — details
- `POST /api/{entity}` — create
- `PUT /api/{entity}/{extid}` — update
- `DELETE /api/{entity}/{extid}` — soft delete

Entities: `armgroup`, `condition`, `eligibilityrule`, `intervention`, `keyword`, `labresult`,
`labresultcomponent`, `location`, `medication`, `outcome`, `overallofficial`, `patient`,
`patientdiagnosis`, `patientmedication`, `patientpriortreatment`, `patientvariant`, `sponsor`,
`stagingrawfhirresource`, `stagingrawtrial`, `trial`, `trialmatch`, `trialmatchcriterion`,
`trialsource`, `trialstatus`, `user`.

Entities referencing a trial also expose `GET /api/{entity}/by-trial/{trialExtid}`, and
patient-scoped entities expose `GET /api/{entity}/by-patient/{patientExtid}`.

### Authentication
- `POST /api/auth/login` — login; rate-limited
- `POST /api/auth/register` — ADMIN-only

### Matching
- `GET /api/matching/rank/{patientExtid}?breastOnly=&limit=` — rank the corpus, best first
- `GET /api/matching/trial/{trialExtid}/for/{patientExtid}` — assess a single trial

### Ingestion and retrieval
- `POST /api/ingestion/clinicaltrials` — pull and stage trials
- `POST /api/uchealth/observation`, `POST /api/uchealth/medicationrequest` — Epic FHIR pulls
- `POST /api/rag/backfill` — embed and index staged trials
- `GET /api/rag/search` — semantic search over criteria
- `POST /api/rag/reindex/{trialExtid}` — re-index one trial

Two entity endpoints — `/api/customer` and `/api/purchase` — are inherited scaffolding from the
project this was copied from and are not part of the clinical-trials domain.

## Build and deployment

### Local development
- Backend on port 8080 (`PORT` env var)
- Vite dev server on 5173
- MySQL with Liquibase migrations on startup

### Production
- `./gradlew buildDeployment` — builds the frontend and bundles it into a single jar
- Configuration is entirely environment-variable driven; `.env` is gitignored
- Deployed on a Hostinger KVM behind Nginx with Let's Encrypt HTTPS; see
  `hosting/DEPLOY_RUNBOOK.md`
