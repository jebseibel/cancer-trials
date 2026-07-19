# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Backend (Gradle, run from repo root):
- Build: `./gradlew build`
- Run a single test class: `./gradlew test --tests "com.seibel.jobs.database.db.service.CustomerDbServiceTest"`
- Run a single test method: `./gradlew test --tests "com.seibel.jobs.database.db.service.CustomerDbServiceTest.methodName"`
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

Gradle multi-module project (root module name: `jobs`), currently three modules wired in `settings.gradle`: root, `:common`, `:database`. (`:ai-provider`, `:docstorage`, `:fileloader` are planned/documented modules not yet present in `settings.gradle`.)

Base Java package: `com.seibel.jobs` (main and test source sets).

- **Root module** (`src/main/java/com/seibel/jobs/`) — the Spring Boot app itself: `web/` (controllers, request/response DTOs, `GlobalExceptionHandler`), `service/` (business logic, extends `BaseService`), `security/` (`JwtUtil`, `JwtAuthenticationFilter`, `CustomUserDetailsService`), `config/` (`SecurityConfig`, `WebConfig`). Main class is `JobsApplication` — the class name is a naming leftover (see below) and hasn't been renamed to match the package.
- **`:common`** — shared, framework-light code with no Spring dependency: domain objects (`Customer`, `Purchase`, `User`, `BaseDomain`), enums (`ActiveEnum`, `CompResult`), custom exceptions, `CodeGenerator` util.
- **`:database`** — JPA layer: entities (`db/entity`, all extend `BaseDb`), repositories (`db/repository`, Spring Data `JpaRepository`), mappers (`db/mapper`, entity ↔ domain conversion), db services (`db/service`, extend `BaseDbService`), plus Liquibase changelogs under `database/src/main/resources/db/changelog/`. This module depends on `:common` (`api project(':common')`); it declares the Spring Boot plugin but does not apply it, since it's a library, not the bootable app.

Layering per entity is consistent top-to-bottom: Controller → Service → DbService → Repository/Entity, with a Mapper converting between the JPA entity and the `:common` domain object. `BaseDb` supplies `id`, `extid` (UUID-style external identifier, used in all API paths instead of the numeric id), `createdAt`, `updatedAt`, `deletedAt`, and `active` (soft-delete via `ActiveEnum`, not hard deletes).

Liquibase changelog entrypoint is `database/src/main/resources/db/changelog/db.changelog-master.yaml`, which does `includeAll` on `db/changelog/changes/`; individual changesets are numbered (`001-customer.yaml`, `002-user.yaml`, `003-purchase.yaml`, `100-load-init-data.yaml`). `application.yml` enables Liquibase with `drop-first: true` (non-production setting) and resolves the changelog off the combined classpath (contributed by `:database`), not from the root module's own `resources/`. This project is not in production, so schema changes can be made directly in the existing changelog files rather than adding new changesets.

Datasource config in `application.yml` reads `RDS_HOSTNAME` / `RDS_PORT` / `RDS_DB_NAME` / `RDS_USERNAME` / `RDS_PASSWORD` from the environment (`.env` supported via `spring-dotenv`). `build.gradle`'s standalone `liquibase {}` Gradle plugin block (used for CLI-driven Liquibase tasks, separate from the Spring Boot-managed migrations above) falls back to `localhost:3306/cpss` when `RDS_HOSTNAME` isn't set — that hostname is stale and should not be treated as the real local DB name (`.env` sets `RDS_DB_NAME=basic`).

Frontend (`frontend/`) is a separate Vite + React 19 + TypeScript + Tailwind app, currently just the default Vite scaffold (`App.tsx`, `main.tsx`) with no pages/routing/API client built yet. It is built independently (`npm run dev`) and, for deployment, copied into `src/main/resources/static` by the root `build.gradle`'s frontend tasks.

## Naming inconsistencies (partially cleaned up)

The project was copied from an older project named `cpss` (which itself had gone through `basic` → `springmulti` naming passes) and is being renamed to `jobs`. Java packages (`com.seibel.jobs`), `settings.gradle`'s root project name (`jobs`), and `build.gradle`'s `group` have been aligned. Remaining known leftovers, left alone deliberately:
- `JobsApplication` (root main class) and `SEED_DATA`/sample test class names still say `Basic`/`Sample`, not renamed to match the package.
- `.env` / Spring config use `RDS_*` variable names; some test files under `database/src/test` still reference `CPSS_USERNAME`/`CPSS_PASSWORD`. Left intentionally — DB/env naming is the user's to manage, never touch it.
- `build.gradle`'s standalone `liquibase {}` Gradle plugin block still falls back to `localhost:3306/cpss` as the DB name when `RDS_HOSTNAME` is unset.
- `application.yml`'s logging config references `com.seibel.jobs.loader.DataLoader`, a class that doesn't exist in the codebase — stale/aspirational config, not a bug to fix reactively.
- `.idea/modules/basicspring.main.iml` is an orphaned IDE file no longer referenced by `.idea/modules.xml`; IDE-managed, not hand-edited.

Don't "fix" the DB/env-var items opportunistically — those are explicitly out of scope for code-level renaming.
