# Database Module

## What It Is

The **database** module is the persistence layer that provides JPA entities, repositories, database services, and Liquibase migrations for the entire jobhunting application. It manages all database interactions with AWS RDS MySQL (locally, a plain MySQL instance).

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
- Rollback capability
- Multi-environment support (dev, test, prod)
- Data seeding and reference data

**Migration Files:**
- SQL changesets in `db/changelog/changes/`
- Master changelog in `db.changelog-master.yaml`
- Tracked in `databasechangelog` table

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
│  AWS RDS MySQL                          │
│  (Relational Database)                  │
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

Live changesets in `database/src/main/resources/db/changelog/changes/`, in run order. All extend the standard base fields (id, extid, created_at, updated_at, deleted_at, active) — see `.claude/table-definitions.md` for full column-level detail on the job-search tables.

- customer
- user
- purchase
- company
- job_posting
- skill
- application
- contact
- friend
- job_posting_skill (join: job_posting ↔ skill)
- user_skill (join: user ↔ skill)
- friend_skill (join: friend ↔ skill)
- friend_company (join: friend ↔ company)
- friend_job_posting (join: friend ↔ job_posting)

## Key Features

✅ **JPA Entities** - Domain models for all database tables
✅ **Spring Data Repositories** - Clean data access layer
✅ **Database Services** - Business logic interface
✅ **Liquibase Migrations** - Version-controlled schema changes
✅ **Generic Base Classes** - Reusable components for CSV entities
✅ **Query Methods** - Type-safe database queries
✅ **Transaction Management** - Automatic transaction handling
✅ **Connection Pooling** - Efficient database connections
✅ **MySQL Integration** - Optimized for AWS RDS MySQL
✅ **Multi-Environment** - Dev, test, and production configurations

