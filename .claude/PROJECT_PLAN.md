# Clinical Trials Finder — Project Plan

## 1. Purpose

A local-first application to find and track clinical trials relevant to a specific
patient. Phase 1 scope is **ingestion and storage**: pull structured trial data from
ClinicalTrials.gov, normalize it into a relational schema, allow additional sources to
be added later (via Playwright scrapers), and provide a simple web UI to search saved
trials and track personal status/notes against each one (interested, contacted, ruled
out, enrolled). Matching/recommendation logic (structured filters, AI-assisted
eligibility parsing) is explicitly deferred to a later phase.

This app is single-user, runs entirely on your machine, and is not deployed publicly.

## 2. Reference project

This project mirrors the architecture and conventions of `~/projects/personal/cpss`
(a working Spring Boot + React app), rather than inventing new patterns. Where this
plan doesn't specify something, default to how cpss does it.

Carried over from cpss:
- Java 21, Spring Boot 3.5.x, Gradle, single module with layered packages
- MySQL in Docker + Liquibase migrations (numbered changelog YAML files)
- Entity base class pattern (`BaseDb`: `id`, `extid` UUID, `createdAt`/`updatedAt`/
  `deletedAt`, `active`) and soft deletes
- Layering: `domain` (POJO) → `entity` (JPA) → `repository` → `db/service` (CRUD) →
  `service` (business logic) → `web/controller` + `web/request`/`web/response` DTOs +
  per-controller package-private `Converter`
- `CommandLineRunner`-based data loader pattern (like cpss's `DataLoader`/`CsvParser`)
  reused here as the pattern for the *normalization* step, not raw CSV loading
- React + Vite + Tailwind frontend, built via Gradle task and copied into
  `src/main/resources/static` for a single deployable JAR (no separate frontend server
  to run day-to-day)
- JWT auth via Spring Security, but trimmed to the minimum: no registration, no
  forgot-password/email flow. One (or two) users seeded via Liquibase/data loader.
  Login only.

Explicitly **not** carried over: multi-tenant `Company` model, full auth lifecycle,
email sending, Postman collection tooling (can add later if useful).

## 3. High-level architecture

```
┌─────────────────────────┐        ┌──────────────────────────┐
│ Node/Playwright scripts │        │  ClinicalTrials.gov v2   │
│  (ad hoc, other sources)│        │  REST API                │
└───────────┬──────────────┘        └────────────┬─────────────┘
            │ writes raw rows                    │ fetched by
            ▼                                     ▼
┌─────────────────────────────────────────────────────────────┐
│                     MySQL (Docker, local)                    │
│         staging tables, normalized by a Spring Boot job        │
│              into the core schema — see TABLES.md              │
└───────────────────────────┬───────────────────────────────────┘
                            │ JPA / Spring Data
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot application (single JAR)          │
│  ingestion (CT.gov client) │ service layer │ REST controllers  │
│                     React (Vite/Tailwind) static frontend       │
└─────────────────────────────────────────────────────────────┘
```

Key design decision: **staging table + normalization step**, mirroring cpss's
DataLoader-then-link pattern. Both the ClinicalTrials.gov ingestion job and any
Playwright scraper write into source-specific raw/staging tables first (JSON blob +
minimal identifying fields). A separate normalization service reads staging rows and
upserts into the shared, fully-normalized core schema (full table design in
[`clinical-trials-tables.md`](clinical-trials-tables.md)). This means:
- Adding a new source later = write a scraper that inserts into a new staging table +
  a small normalizer, without touching the core schema or the UI.
- Nothing is lost even if a source's fields don't map cleanly yet — the raw payload is
  always retained in staging.

## 4. Data source: ClinicalTrials.gov API v2

- Base URL: `https://clinicaltrials.gov/api/v2/`
- No API key required, no documented hard rate limit (use reasonable polling/backoff
  regardless)
- `GET /studies` — search/list, with `query.cond`, `query.term`, `query.locn`,
  `query.intr`, `query.spons`, `filter.overallStatus`, `pageSize` (max 1000),
  `pageToken`/`nextPageToken` for cursor pagination, `countTotal=true`
- `GET /studies/{nctId}` — single study fetch
- Stable identifier: `nctId` (format `NCT########`) at
  `protocolSection.identificationModule.nctId` — this is the natural key for the
  `trial` table
- Each study JSON has `protocolSection` (the part we care about), `derivedSection`,
  `hasResults`, optional `resultsSection`
- **Eligibility criteria is free text**, not structured: one string field with
  "Inclusion Criteria:" / "Exclusion Criteria:" headers and bullet items. Only
  `minimumAge`, `maximumAge` (free-text like "18 Years"), `sex`, `healthyVolunteers`,
  and `stdAges[]` are structured. Store the narrative text as-is; do not attempt to
  parse it into rules in this phase.

## 5. Database schema (MySQL, Liquibase-managed)

Full schema design (tables, columns, field mappings, open questions) lives in
[`clinical-trials-tables.md`](clinical-trials-tables.md), tracked and iterated on separately from this plan.

## 6. Backend package layout

```
com.seibel.clinicaltrials
├── ClinicaltrialsApplication.java
├── common/
│   ├── domain/             Domain model POJOs mirroring the core schema (see TABLES.md)
│   ├── enums/               Enums backing schema status/type columns
│   └── exceptions/          (mirror cpss: ResourceNotFoundException, ValidationException, etc.)
├── config/                  SecurityConfig, WebConfig
├── security/                 JwtUtil, JwtAuthenticationFilter, CustomUserDetailsService
├── database/
│   └── db/
│       ├── entity/           JPA entities, one per schema table, extending BaseDb
│       ├── repository/       Spring Data JPA interfaces
│       ├── service/           *DbService (CRUD, extends BaseDbService)
│       └── mapper/            *Mapper (entity ↔ domain)
├── ingestion/
│   ├── clinicaltrials/        ClinicalTrialsGovClient (REST client for /studies),
│   │                           ClinicalTrialsGovIngestJob (CommandLineRunner or
│   │                           @Scheduled — fetches pages, writes staging rows)
│   └── normalization/          TrialNormalizationService (reads pending staging rows,
│                                 parses JSON, upserts core tables), source-specific
│                                 parsers (ClinicalTrialsGovParser implements a
│                                 TrialSourceParser interface, so a future
│                                 EuCtrParser or PlaywrightScrapeParser can be added
│                                 without touching the service)
├── service/                   TrialService, TrialStatusService, SearchService, ...
└── web/
    ├── controller/             TrialController, TrialStatusController, AuthController,
    │                            IngestionController (manual "fetch now" trigger)
    ├── request/                 Request DTOs
    └── response/                 Response DTOs
```

The `TrialSourceParser` interface is the seam for the Playwright integration: each
source (ClinicalTrials.gov, a future scraper) implements it and is registered against
the source registry (see `clinical-trials-tables.md`). The normalization job doesn't care where a
staging row came from.

## 7. Playwright scraper integration

- Scrapers live outside this repo (or in a `scrapers/` folder, Node + Playwright, not
  part of the Gradle build).
- They connect directly to the same local MySQL database and insert rows into the
  staging table (schema in `clinical-trials-tables.md`) with a JSON payload shaped however is natural
  for that source (doesn't need to match CT.gov's shape).
- Spring Boot's normalization job polls the staging table for unprocessed rows,
  dispatches to the right `TrialSourceParser` by source, and upserts into the core
  tables — same code path regardless of whether the row came from the CT.gov API
  client or a Playwright script.
- Credentials for local MySQL access are shared via `.env` (same as cpss).

## 8. Frontend (React + Vite + Tailwind)

Mirrors cpss's `frontend/` structure:
```
pages/       Login.tsx, TrialSearch.tsx, TrialDetail.tsx, SavedTrials.tsx, Dashboard.tsx
components/  Layout.tsx, ProtectedRoute.tsx, TrialCard.tsx, StatusBadge.tsx
services/    api.ts   (axios client, JWT interceptor — same pattern as cpss)
types/       api.ts   (Trial, TrialStatus, SearchFilters, etc.)
```
Core screens for phase 1:
- **Login** — single/seeded users only
- **Trial Search** — query saved trials by condition/status/location/phase (filtering
  what's already in the DB; triggering a new CT.gov fetch is a separate explicit
  action, not implied by every search)
- **Trial Detail** — full normalized record: sponsors, conditions, interventions,
  outcomes, locations, eligibility text, plus the personal tracking control (status
  dropdown + notes) for the logged-in user
- **Saved/Tracked Trials** — list filtered by personal status, so you can see
  "interested" or "contacted" trials at a glance

## 9. Local dev environment

- `docker-compose.yml` for MySQL (mirror cpss's approach — check if cpss has one, or
  we set it up similarly to its `.env`-driven `RDS_*` variables pointing at localhost)
- `.env` for DB credentials (never committed)
- `./gradlew bootRun` for backend; `npm run dev` in `frontend/` for frontend dev
  server (Vite proxy to `:8080` for `/api`); `./gradlew buildDeployment` to build the
  single deployable JAR with frontend bundled, same as cpss

## 10. Phased roadmap

**Phase 1 — Foundation & ClinicalTrials.gov ingestion (this plan's scope)**
1. Project scaffold: Gradle, Spring Boot, MySQL/Docker, Liquibase master changelog
2. Core schema as Liquibase changesets (see `clinical-trials-tables.md` for the table design)
3. `ClinicalTrialsGovClient` + ingestion job (fetch by condition/location search terms
   you configure, page through results, write to staging)
4. `TrialNormalizationService` + `ClinicalTrialsGovParser` — staging → core tables
5. Minimal JWT auth, seeded users
6. REST API: search/list/detail trials, CRUD on `trial_status`
7. React frontend: login, search, detail, saved-trials views
8. Manual end-to-end test: run ingestion for your wife's actual condition, confirm
   real trials show up correctly normalized

**Phase 2 — Additional sources**
- First Playwright scraper (target TBD — likely a source not covered by
  ClinicalTrials.gov, e.g. a specific hospital's trial listing or EU registry)
- `TrialSourceParser` implementation for it
- Source-conflict handling if the same trial appears from two sources (dedupe by
  matching NCT number where present, otherwise manual review UI)

**Phase 3 — Matching / relevance (deferred, not designed yet)**
- Structured profile for your wife's condition/eligibility facts
- Filter/search refinement against that profile
- Possibly AI-assisted eligibility parsing of the free-text criteria — revisit once
  the storage layer is proven out

## 11. Open items to decide when we start building

- Exact search terms/conditions to configure for the initial CT.gov ingestion pull
- Whether ingestion runs on-demand (a button/endpoint) or on a schedule (`@Scheduled`)
  for phase 1 — recommend on-demand first, since trial data doesn't change fast
- Where Playwright scraper scripts will live (separate repo vs. `scrapers/` folder
  here) — can decide in phase 2
