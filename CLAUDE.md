# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Backend (Gradle, run from repo root):
- Build: `./gradlew build`
- Run a single test class: `./gradlew test --tests "com.seibel.cancer.database.db.service.TrialDbServiceTest"`
- Run a single test method: `./gradlew test --tests "com.seibel.cancer.database.db.service.TrialDbServiceTest.methodName"`
- Run tests for one module: `./gradlew :database:test`
- Run the app: user starts/stops the backend manually — do not run or kill the Spring Boot process yourself.

Frontend (`frontend/`, Vite + React):
- Dev server: `npm run dev` (only run this if explicitly asked to "start the front end")
- Build: `npm run build`
- Lint: `npm run lint`

Combined deployment build (bundles frontend into the Spring Boot jar):
- `./gradlew buildDeployment` — runs `npmInstall` → `npmBuild` → `cleanStatic` → `copyFrontend` → `build`, producing a single jar with the frontend under `src/main/resources/static`.
- `./gradlew killFrontend` kills any dev server on ports 5173-5175.

Database:
- Never connect to the database directly or run migrations yourself — always go through the REST API, and never attempt to drop/update the schema. The user manages the DB.
- "Rebuild the database" from the user means triggering the local n8n webhook at `http://localhost:5678/webhook/clear-db` (GET), not a Liquibase or SQL command.

## Architecture

Gradle multi-module project (root project name: `cancer`). `settings.gradle` includes four modules alongside the root: `:common`, `:database`, `:datafetcher`, `:rag`. A fifth module, `:ai-provider`, exists on disk with source in it but is commented out of `settings.gradle` ("shelved until AI keys/config are ready") — it does not build as part of the project, and **it is not what the app's AI feature uses**. That module carries eleven `ChatClient` beans across four providers, no tests, and prompt templates for energy-certificate workflows from the project it was lifted from. The live AI path is `service/ai/` in the root module: Spring AI's Anthropic starter, one `ChatClient`, one env var (`ANTHROPIC_API_KEY`). A top-level `playwright/` directory holds scraping work and is not a Gradle module.

Base Java package: `com.seibel.cancer` (main and test source sets). Main class is `CancerApplication`.

- **Root module** (`src/main/java/com/seibel/cancer/`) — the Spring Boot app itself: `web/` (controllers, request/response DTOs, `GlobalExceptionHandler`), `service/` (business logic, extends `BaseService`, plus `service/matching/` for trial-matching logic), `security/` (`JwtUtil`, `JwtAuthenticationFilter`, `CustomUserDetailsService`), `config/` (`SecurityConfig`, `WebConfig`).
- **`:common`** — shared, framework-light code with no Spring dependency: domain objects (extending `BaseDomain` / `BaseUniqueDomain`), enums, custom exceptions, `CodeGenerator` util, and `TrialTextClassifier` — clinical classification logic that lives here rather than in root because `:datafetcher` stamps its values at normalization and cannot see root.
- **`:database`** — JPA layer: entities (`db/entity`, all extend `BaseDb`), repositories (`db/repository`, Spring Data `JpaRepository`), mappers (`db/mapper`, entity ↔ domain conversion), db services (`db/service`, extend `BaseDbService`), plus Liquibase changelogs under `database/src/main/resources/db/changelog/`. Depends on `:common` (`api project(':common')`); it declares the Spring Boot plugin but does not apply it, since it's a library, not the bootable app.
- **`:datafetcher`** — external ingestion: a ClinicalTrials.gov client/parser/ingest job, and the UCHealth Epic FHIR side (OAuth client with PKCE, FHIR client, ingest job) plus normalization services that turn raw staged payloads into domain rows.
- **`:rag`** — vector-retrieval layer over trial text: chunking (trial and eligibility-criteria chunkers), indexing, retrieval, a backfill service, and a startup check against the vector store.

Layering per entity is consistent top-to-bottom: Controller → Service → DbService → Repository/Entity, with a Mapper converting between the JPA entity and the `:common` domain object. `BaseDb` supplies `id`, `extid` (UUID-style external identifier, used in all API paths instead of the numeric id), `createdAt`, `updatedAt`, `deletedAt`, and `active` (soft-delete via `ActiveEnum`, not hard deletes).

## Domain

The app is a clinical-trial finder/matcher. The domain splits into three broad groups:

- **Trial side** — `Trial` and its satellites: `ArmGroup`, `Condition`, `EligibilityRule`, `Intervention`, `Keyword`, `Location`, `Outcome`, `OverallOfficial`, `Sponsor`, `TrialSource`, `TrialStatus`.
- **Patient side** — `Patient`, `PatientDiagnosis`, `PatientVariant`, `PatientMedication`, `PatientPriorTreatment`, `LabResult`, `LabResultComponent`, `Medication`, and `UserPatient` (which links a `User` to the patient records they own).
- **Matching + ingestion** — `SavedTrialMatch` and `SavedTrialMatchCriterion` for persisted results; `AiTrialAssessment` for stored AI readings of a trial against a record (changeset `033`) — ⚠️ **clinical prose about a real patient, not a cache**; `matching/` domain types (`TrialAssessment`, `EligibilitySignal`, `SignalOutcome`) for evaluation output; `StagingRawTrial` and `StagingRawFhirResource` as landing tables for raw ingested payloads; `UcHealthOAuthToken` for Epic auth.

## Liquibase

Changelog entrypoint is `database/src/main/resources/db/changelog/db.changelog-master.yaml`, which does `includeAll` on `db/changelog/changes/`. Individual changesets are numbered, currently running `001` through `033` plus `100-load-init-data.yaml`.

Known issue: two number collisions exist — `011-trial-source.yaml` / `011-trial-status.yaml`, and `014-eligibility-rule.yaml` / `014-outcome.yaml`. `includeAll` resolves them by alphabetical filename so they do load deterministically, but the duplicate prefixes are unintentional and should be renumbered when convenient. Don't renumber them as a side effect of unrelated work.

`application.yml` enables Liquibase with **`drop-first: false`**, so a stored UCHealth OAuth token survives a restart. The changelog resolves off the combined classpath (contributed by `:database`), not from the root module's own `resources/`.

⚠️ **Editing an already-applied changeset breaks its checksum and fails startup.** Because `drop-first` is false, an edit does not simply re-run — Liquibase refuses to start with `Validation Failed: 1 changesets check sum`, and the only fixes are a full database rebuild (the n8n `clear-db` webhook, which destroys the corpus and orphans the vector store) or clearing checksums. **Add a new numbered changeset instead.** `031` and `032` were added this way with no rebuild.

## Configuration

Datasource config in `application.yml` reads `RDS_HOSTNAME` / `RDS_PORT` / `RDS_DB_NAME` / `RDS_USERNAME` / `RDS_PASSWORD` from the environment (`.env` supported via `spring-dotenv`). `build.gradle`'s standalone `liquibase {}` Gradle plugin block (used for CLI-driven Liquibase tasks, separate from the Spring Boot-managed migrations above) falls back to `localhost:3306/cpss` when `RDS_HOSTNAME` isn't set — that hostname is stale and should not be treated as the real local DB name (`.env` sets `RDS_DB_NAME=basic`).

## Frontend

`frontend/` is a separate Vite + React 19 + TypeScript + Tailwind app with routing, auth, and an API client in place. It is built independently (`npm run dev`) and, for deployment, copied into `src/main/resources/static` by the root `build.gradle`'s frontend tasks.

- **Pages** — `Login`, `Dashboard`, `PatientRecord`, `Diagnosis`, `Variants`, `PriorTreatment`, `TrialSearch`, `TrialDetail`, `RankedTrials`, `SavedTrials`, `Ingestion`.
- **Shared** — `components/` (`Layout`, `ProtectedRoute`, `FormControls`, `JobResultModal`, `SignalRow`), `services/api.ts` (the API client), `types/api.ts`, and `lib/` helpers including `PatientContext` (current-patient state), `accessLevel`, `receptorSubtype`, and `tier1Matching`.

## Naming leftovers

The project was copied from an older project (`cpss`, itself descended from `basic` → `springmulti` → `jobs`), was renamed once to `jobhunting`, and has since been renamed to `cancer`. Java packages (`com.seibel.cancer`), `settings.gradle`'s root project name (`cancer`), `build.gradle`'s `group`, and the main class (`CancerApplication`) are all aligned. Remaining known leftovers, left alone deliberately:

- Some `SEED_DATA` and sample test class names still use older `Basic`/`Sample` naming.
- `.env` / Spring config use `RDS_*` variable names; some test files under `database/src/test` still reference `CPSS_USERNAME`/`CPSS_PASSWORD`. Left intentionally — DB/env naming is the user's to manage, never touch it.
- `build.gradle`'s standalone `liquibase {}` block still falls back to `localhost:3306/cpss` as the DB name when `RDS_HOSTNAME` is unset.
- `.idea/modules/basicspring.main.iml` is an orphaned IDE file no longer referenced by `.idea/modules.xml`; IDE-managed, not hand-edited.
- A `JobResultModal` component in the frontend carries a name from the jobhunting era.

Don't "fix" the DB/env-var items opportunistically — those are explicitly out of scope for code-level renaming.
