# Backend Description

## Overview

The Breast Cancer Trial Finder backend is a RESTful API built with Spring Boot 3.5.7 and Gradle.
It provides CRUD operations across the trial and patient schemas, ingestion from
ClinicalTrials.gov and Epic FHIR, vector retrieval over eligibility criteria, and trial matching —
with JWT authentication and a grant-based authorization model.

## Architecture

### Layered architecture

The backend follows a 4-layer pattern:

1. **Controller Layer** (`web.controller`): REST endpoints with DTO conversion
2. **Service Layer** (`service`): Business logic and validation
3. **Database Service Layer** (`database.db.service`): Data persistence operations
4. **Entity Layer** (`database.db.entity`): JPA entities and repositories

### Package structure

```
com.seibel.cancer
├── common/
│   ├── domain/           # Domain objects (Trial, Patient, PatientDiagnosis, ...)
│   │   └── matching/     # TrialAssessment, EligibilitySignal, SignalOutcome
│   ├── enums/            # ActiveEnum, AccessLevel, ReceptorStatus, VariantStatus,
│   │                     # TreatmentStatus, TrackingSystem, CompResult
│   ├── exceptions/       # Business exceptions
│   └── util/             # Utilities (CodeGenerator)
├── config/               # SecurityConfig, WebConfig
├── database/
│   └── db/
│       ├── entity/       # JPA entities (TrialDb, PatientDb, UserDb, ... BaseDb)
│       ├── mapper/       # Entity ↔ Domain mappers
│       ├── repository/   # Spring Data repositories
│       └── service/      # Database service layer
├── security/             # JWT and authentication
├── service/ai/           # AiService (Spring AI, Anthropic), TrialDiagnosisMatchService,
│                         # TrialMatchAssessment - the AI trial check
├── service/              # Business service layer
│   └── matching/         # TrialMatchingService, CriteriaSignalEvaluator,
│                         # TrialClassificationBackfillService
└── web/
    ├── controller/       # REST controllers
    ├── request/          # Request DTOs
    ├── response/         # Response DTOs
    └── GlobalExceptionHandler
```

Two sibling modules sit below root and are called directly from it: `:datafetcher` (CT.gov and
Epic FHIR ingestion, normalization) and `:rag` (chunking, indexing, retrieval).

## Key components

### Core entities

Grouped by area:

1. **Trial**: `Trial` plus `ArmGroup`, `Condition`, `EligibilityRule`, `Intervention`, `Keyword`,
   `Location`, `Outcome`, `OverallOfficial`, `Sponsor`, `TrialSource`, `TrialStatus`
2. **Patient**: `Patient`, `PatientDiagnosis`, `PatientVariant`, `PatientPriorTreatment`,
   `PatientMedication`, `LabResult`, `LabResultComponent`, `Medication`
3. **Access**: `User` (login) and `UserPatient` (which patients a user may see, at what level)
4. **Matching / staging**: `SavedTrialMatch`, `SavedTrialMatchCriterion`, `StagingRawTrial`,
   `StagingRawFhirResource`, `UcHealthOAuthToken`

`Customer` and `Purchase` are inherited scaffolding from the copied-from project, not part of
this domain.

### Authentication
- **JwtUtil**: Generates and validates JWT tokens
  - Token expiration: 24 hours (configurable via `jwt.expiration`)
  - HS256 signature algorithm
  - Claims include username and issuedAt timestamp
  - Secret read from `JWT_SECRET`. ⚠️ **No default, deliberately** — a default would silently
    re-enable a known literal whenever the env var is unset, so the app fails to start instead
- **CustomUserDetailsService**: Loads user details from `UserDb`, filtering out soft-deleted
  accounts so a deleted user cannot log in
- **JwtAuthenticationFilter**: Extracts and validates the bearer token
- **LoginRateLimitFilter**: 5 consecutive failures per IP+username → 429 with `Retry-After`,
  registered ahead of authentication so a locked-out caller costs no bcrypt work
- **SecurityConfig**: Configures the filter chain
  - Permits `/api/auth/**`, static assets, and `/api/uchealth/callback` (Epic's OAuth redirect
    cannot carry a JWT)
  - `.anyRequest().authenticated()` for everything else
  - `@EnableMethodSecurity` is on, so `@PreAuthorize` is honoured rather than silently ignored
  - CORS enabled; stateless session management

### Authorization
- **`UserPatient`** grants a `User` access to a `Patient` at a ranked level:
  `VIEW_TRIALS(10) < VIEW_RECORD(20) < EDIT_RECORD(30) < OWNER(40)`
- **`CurrentUserService`** resolves the caller from `SecurityContextHolder` and enforces it —
  `requireAccess`, `requireAccessId`, `hasAccess`, `accessLevelFor`
- Patient-scoped endpoints take the target extid from the URL and check it against the caller's
  grants, so naming someone else's record returns a permission error rather than their data

### Database layer
- **Repositories**: Spring Data `JpaRepository` per entity
  - Standard: save, findById, delete
  - Custom: findByExtid, findByActive, findAllActive, existsByExtid
  - Paginated queries with findByActive(ActiveEnum, Pageable)
- **Entity Mappers**: Convert between Domain and Db objects
- **Database Services**: Low-level persistence
  - Create, update, delete, findByExtid, findAll, findByActive
  - Soft delete (marks INACTIVE, sets `deletedAt`)
  - Pagination support

⚠️ **`update()` cannot clear a field.** Every assignment is guarded by `if (item.getX() != null)`,
so null means "leave alone" and there is no way to unset a populated column through the API.
This affects every `*DbService` following the template.

### Service layer
- **BaseService**: Abstract base with validation helpers
  - `requireNonNull()`, `requireNonBlank()`
  - Lombok `@Slf4j` for logging
- **Entity Services**: CRUD delegating to the db service, input validation, pagination with a
  configurable max page size (100), allowed sort-field validation, `@Transactional` operations
- **`service/matching/`**: `TrialMatchingService` ranks the corpus against a patient record;
  `CriteriaSignalEvaluator` produces per-signal outcomes with the text that caused them, across
  **seven signals** — disease type, receptor polarity, treatment line, PI3K pathway, location, and
  since 2026-08-21 **treatment goal** and **disease stage**;
  `TrialClassificationBackfillService` re-derives the two stored classifications
- **Ranking is lexicographic**, since there is deliberately no score: breast trials first, then
  **treatment goal**, then fewest concerns, then most passes, then most applicable signals.
  Treatment goal sits above concern count on purpose — curative-intent trials are ~1.5% of the
  corpus, so ranking on concerns alone buried the ones most wanted beneath well-matched
  disease-control trials
- **`PatientSeedLoader`**: A `CommandLineRunner` that recreates patient rows on startup from
  gitignored CSVs. Seeds if absent, never syncs — an existing row is left alone, so UI edits
  survive a restart

### Web layer
- **Controllers**: RESTful endpoints with Swagger documentation
  - `GET /` — list with pagination (default 20) and optional active filter
  - `GET /{extid}` — get single entity
  - `POST /` — create
  - `PUT /{extid}` — update
  - `DELETE /{extid}` — soft delete
- **Converter Classes**: Package-private classes inline in controller files
  - `toDomain()` — request DTO → domain object
  - `toResponse()` — domain object → response DTO
- **Request DTOs**: Create (fields required) and Update (fields optional) variants
- **Response DTOs**: Include extid and all business fields — never the numeric id

### Exception handling
- **GlobalExceptionHandler**: REST controller advice with typed handlers
  - ResourceNotFoundException → 404
  - ValidationException → 400
  - MethodArgumentNotValidException → 400 with field errors
  - ConstraintViolationException → 400 with field errors
  - ResourceAlreadyExistsException → 409
  - ServiceException → 500
  - Generic Exception → 500
- **Custom Exceptions**: BaseServiceException, ServiceException, ValidationException,
  ResourceNotFoundException, ResourceAlreadyExistsException

### Utilities
- **CodeGenerator**: Generates unique codes for entities
- **ActiveEnum**: ACTIVE (1) / INACTIVE (0), with `isActive()` / `isInactive()`

## Database configuration

### Liquibase
- Changelog: `db/changelog/db.changelog-master.yaml`, `includeAll` over `changes/`, running `001`-`033`
- Automatic schema initialization on startup
- ⚠️ **`drop-first` is `false`**, so a stored UCHealth OAuth token survives a restart.
  Consequence: **edits to an already-applied changeset do not take effect on startup** — rebuild
  the DB via the n8n `clear-db` webhook. New changesets still apply normally
- MySQL driver: `com.mysql:mysql-connector-j`

### Data source
- Connection pool: HikariCP with 30s connection timeout
- URL from `RDS_HOSTNAME`, `RDS_PORT`, `RDS_DB_NAME`
- Credentials from `RDS_USERNAME`, `RDS_PASSWORD`
- Initialization failure timeout: 0 (fail fast)

### JPA configuration
- DDL-auto: none (Liquibase owns the schema)
- Show-sql: false
- Allow bean definition overriding: true

⚠️ **`BaseDb` uses `GenerationType.IDENTITY`**, which disables Hibernate JDBC batching entirely.
This is the blocker on batching the ~25 child INSERTs per trial during normalization.

## API endpoints

### Authentication
- `POST /api/auth/login` — login; rate-limited
- `POST /api/auth/register` — ADMIN-only

### Entity CRUD

The five-endpoint shape above, at: `/api/armgroup`, `/api/condition`, `/api/eligibilityrule`,
`/api/intervention`, `/api/keyword`, `/api/labresult`, `/api/labresultcomponent`,
`/api/location`, `/api/medication`, `/api/outcome`, `/api/overallofficial`, `/api/patient`,
`/api/patientdiagnosis`, `/api/patientmedication`, `/api/patientpriortreatment`,
`/api/patientvariant`, `/api/sponsor`, `/api/stagingrawfhirresource`, `/api/stagingrawtrial`,
`/api/trial`, `/api/trialmatch`, `/api/trialmatchcriterion`, `/api/trialsource`,
`/api/trialstatus`, `/api/user`.

Plus `/api/customer` and `/api/purchase`, which are inherited scaffolding.

Entities referencing a trial also expose `GET /api/{entity}/by-trial/{trialExtid}`;
patient-scoped entities expose `GET /api/{entity}/by-patient/{patientExtid}`.

### Matching
- `GET /api/matching/rank/{patientExtid}?breastOnly=&limit=` — rank the corpus, best first
- `GET /api/matching/trial/{trialExtid}/for/{patientExtid}` — assess a single trial
- `GET /api/matching/ai/status` — whether the AI trial check is configured
- `GET /api/matching/ai/trial/{trialExtid}/for/{patientExtid}` — latest stored reading, 204 if none
- `POST /api/matching/ai/trial/{trialExtid}/for/{patientExtid}` — read this trial's criteria
  against the record. ⚠️ **The only endpoint that sends clinical text off the machine**; the
  payload is de-identified by an explicit allowlist in `TrialDiagnosisMatchService`
- `POST /api/matching/backfill-treatment-goals` — re-derive `treatment_goal` and `disease_stage`
  for every trial. **ADMIN-only.** Needed because ingestion skips trials whose ClinicalTrials.gov
  payload is unchanged, so a re-pull cannot pick up a change to the code that reads that payload

### Ingestion and retrieval
- `POST /api/ingestion/clinicaltrials` — pull and stage trials
- `POST /api/uchealth/observation`, `POST /api/uchealth/medicationrequest` — Epic FHIR pulls
- `POST /api/rag/backfill` — embed and index staged trials
- `GET /api/rag/search?criteriaOnly=` — semantic search. `criteriaOnly` restricts matching to
  eligibility-criteria chunks, dropping summaries, descriptions, interventions and outcomes;
  off by default, since prose is the right answer to "what is this trial testing"
- `POST /api/rag/reindex/{trialExtid}` — re-index one trial

## Build and deployment

### Build system
- Gradle 8.14.3, Java 21 toolchain
- Embedded frontend build tasks

### Frontend integration
- Built with Vite, copied to `src/main/resources/static`
- Tasks: `npmInstall`, `npmBuild`, `cleanStatic`, `copyFrontend`
- Deployment: `buildDeployment` (jar with frontend included)
- Development: `killFrontend` (kills dev servers on 5173-5175)

### Configuration
- Application name: `basic` (a naming leftover; the project is `cancer`)
- Server port: 8080, configurable via `PORT`. ⚠️ **Production runs on 8081** — another app owns
  8080 on that box
- Version: 0.0.2-SNAPSHOT
- Spring Boot DevTools enabled for development

⚠️ **Do not run Gradle while the backend is running.** DevTools watches build output and
hot-restarts on change, so any `./gradlew` invocation rewrites those outputs and the app dies
against a half-written classpath. The failures look like real configuration faults
(`Not a managed type`, `ClassNotFoundException`) and arrive on a `restartedMain` thread.

## Dependencies

### Spring Boot
- spring-boot-starter-web, -data-jpa, -security, -validation, -thymeleaf
- spring-boot-devtools (development only)
- springdoc-openapi-starter-webmvc-ui (Swagger UI, v2.6.0)

### JWT & security
- jjwt-api / jjwt-impl / jjwt-jackson (0.12.6)

### Database
- mysql-connector-j
- liquibase-core (4.29.2)

### Utilities
- modelmapper (3.2.0), lombok (1.18.34), spring-dotenv (4.0.0),
  commons-csv (1.11.0), threeten-bp (1.6.8), snakeyaml (2.2)

### Testing
- spring-boot-starter-test, junit-platform-launcher

## Deployment considerations
- Runs on Ubuntu 24.04
- Configured for MySQL, AWS RDS compatible
- Frontend served as static assets from Spring Boot
- JWT-based stateless authentication
- Liquibase migrations on startup
- Full deploy procedure in `hosting/DEPLOY_RUNBOOK.md`
