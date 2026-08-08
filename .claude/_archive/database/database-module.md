# Database Module

## What It Is

The **database** module is the persistence layer that provides JPA entities, repositories, database services, and Liquibase migrations for the entire cancer application. It manages all database interactions with MySQL (local MySQL instance, not AWS RDS).

**Rules**
Table ID fields (primary fields) must never leave the database module in any form
All tables must have a EXTID field that is used to communicate to and from any communication outside the database module
Do not apply any INDEX fields on tables unless they are extid or id fields. We dont anticipate such a large amount of data that this will be needed.



## Why

Following industry-standard architecture:
- **Centralized Data Layer** - Single source of truth for all database entities
- **Repository Pattern** - Abstracts database operations
- **Database Versioning** - Liquibase manages schema evolution
- **Separation of Concerns** - Database logic separate from business logic
- **Reusability** - All modules access database through this layer

Note: when testing, run the tests in the database connection directory first. This will establish the 
database connection works

## What It Does

### 1. JPA Entities

***Needed Fields***
All dbEntites must have the following fields.

- protected Long id;
- protected String extid;
- protected LocalDateTime createdAt;
- protected LocalDateTime updatedAt;
- protected LocalDateTime deletedAt;
- protected ActiveEnum active;
Do not use the deletedAt field to check for active or inactive. use the Active field


### 2. Spring Data JPA Repositories

Provides repository interfaces for data access:

**Features:**
- Standard CRUD operations
- Custom query methods
- Pagination and sorting
- Query derivation from method names
- Native SQL queries when needed

### 4. Liquibase Database Migrations

Manages database schema evolution:

**Features:**
- Version-controlled schema changes
- Automatic migration on startup
- Data seeding and reference data (see `../csv-load/liquibase-csv-loading-pattern.md`)

**Migration Files:**
- YAML changesets in `db/changelog/changes/`
- Master changelog in `db.changelog-master.yaml` (a single `includeAll` on `changes/`)
- Tracked in the `databasechangelog` table

⚠️ **`spring.liquibase.drop-first` is OFF** in `application.yml` (so the UCHealth OAuth token
survives restarts). Consequence: **edits to an already-applied changeset do not take effect on
startup.** Rebuild the database via the n8n `clear-db` webhook for those. New changesets still
apply normally.

This project is not in production, so schema changes are made by editing the existing changeset
files rather than adding new ones.

## Architecture

```

┌────────────────▼────────────────────────┐
│  Database Module                        │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  Database Services              │   │
│  │  (Business logic interface)     │   │
│  └────────────┬────────────────────┘   │
│               │                         │
│  ┌────────────▼────────────────────┐   │
│  │  JPA Repositories               │   │
│  │  (Data access layer)            │   │
│  └────────────┬────────────────────┘   │
│               │                         │
│  ┌────────────▼────────────────────┐   │
│  │  JPA Entities                   │   │
│  │  (Domain models)                │   │
│  └────────────┬────────────────────┘   │
└───────────────┼─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│  Spring Data JPA / Hibernate            │
│  (ORM Framework)                        │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│  Liquibase                              │
│  (Schema Migration)                     │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│  MySQL (local)                          │
│  connection from RDS_* vars in .env     │
└─────────────────────────────────────────┘
```
## Dependencies

### Core Dependencies

```gradle
// Spring Boot
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'

// Database
runtimeOnly 'com.mysql:mysql-connector-j'

// Liquibase
implementation 'org.liquibase:liquibase-core'

// Utilities
compileOnly 'org.projectlombok:lombok'
```

## Current Tables

Verified 2026-08-08 from `database/src/main/resources/db/changelog/changes/`. All extend the
standard base fields (`id`, `extid`, `created_at`, `updated_at`, `deleted_at`, `active`).
Column-level detail for the clinical-trials tables is in
`../clinical-trials/clinical-trials-tables.md`; for the Epic/FHIR tables, in
`../../epic-integration/epic-tables.md`.

**Clinical trials core** — `trial`, `trial_source`, `trial_status`, `staging_raw_trial`,
`sponsor`, `medical_condition`, `medication`, `location`, `arm_group`, `intervention`,
`outcome`, `overall_official`, `eligibility_rule`, `keyword`

**Patient** — `app_user`, `patient_diagnosis`, `patient_medication`, `lab_result`,
`lab_result_component`

**Epic / UCHealth FHIR** — `uchealth_oauth_token`, `staging_raw_fhir_resource`

**AI prompt management** — `ai_soul`, `ai_prompt_gang`, `ai_prompt_envelope`, `ai_prompt`
(inherited, unused — see `../ai-prompt/ai-prompt-structure.md`)

**Auth / inherited scaffolding** — `user` (login identity), `customer`, `purchase`
(from the original template project, not clinical-trials data)

Note the many-to-many join tables designed in `clinical-trials-tables.md` — `trial_condition`,
`trial_sponsor`, `trial_phase`, `trial_std_age`, `trial_keyword` — were **never scaffolded**.
That is deliberate and is what blocks condition/sponsor/phase filtering.

## Key Features

- JPA entities extending `BaseDb`, one per table
- Spring Data repositories
- `*DbService` classes (extend `BaseDbService`) — the layer that `:datafetcher` and `:rag` call
  directly, since both sit below the root module's `service` package
- Liquibase migrations, `includeAll` on the `changes/` directory
- Soft deletes via `ActiveEnum` — records are never hard-deleted
- ModelMapper-based entity ↔ domain mappers (note: no enum validation — see
  `../code-style/enum-data-issue.md`)

