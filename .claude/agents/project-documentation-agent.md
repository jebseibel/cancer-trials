---
name: project-documentation-agent
description: Walks project documentation files and updates them to accurately reflect the current codebase. May run against all docs or a single specified directory. Run this when docs may be out of date with the code.
---
project-documentation-agent

# Project Documentation Agent

<!--
  Runtime estimate (full run, ~54 docs):
  Low:  ~14 minutes (~4 docs/min)
  High: ~36 minutes (~1.5 docs/min)

  Slowest directories:  facility-recon, retire-cert-process, ts-info
  Fastest directories:  api-testing, current-issues
  Excluded directories: hosting, database/restapi-template.md

  Assumptions: 2–6 source file lookups per doc, ~300–500 total tool calls for a full run.
-->

## How to Launch

**Full run** (all documentation):
> "Run the project documentation agent"

**Single directory run**:
> "Run the project documentation agent on `.claude/_archive/fileloader`"

**Single directory run (recursive)**:
> "Run the project documentation agent on `.claude/_archive/fileloader/**`"

---

## Input
- If given a file path instead of task content, read that file first to obtain the task details.

## Purpose

Walk every documentation file in `.claude/` (including all `_archive` subdirectories) and update each file so its content accurately reflects the actual code. This is a long-running, autonomous task.

---

## Scope

### Input — full run (default)
If no input directory is specified, process all `.md` files under `.claude/` recursively, including all `_archive` subdirectories and `.claude/agents/`.

### Input — single directory run
If an input directory is provided (e.g., `.claude/_archive/fileloader`), process only the `.md` files in that directory. Do not recurse into subdirectories unless the input path ends with `/**`.

### Files to NEVER modify
- `.claude/_archive/database/restapi-template.md` — skip entirely, regardless of input
- All files under `.claude/_archive/hosting/` — skip entirely, regardless of input

---

## What "accurate" means

For each document, read the relevant source code and verify every factual claim:

- Class names, method names, package names
- Module names and Gradle module paths (`:ai-provider`, `:database`, etc.)
- File paths referenced in the document
- Field names on entities, DTOs, or services
- REST endpoint paths and HTTP methods
- Database table names, column names, Liquibase changelog file references
- Enum values, status names, constants
- Dependency relationships between modules
- Description of what a class, service, or process does

If a claim no longer matches the code, correct it. If a section describes something that no longer exists, remove it or note it as removed. Do not add fabricated detail — only write what is confirmed in the source code.

---

## What NOT to do

- Do NOT add code blocks or code samples to documents (except `restapi-template.md`, which is never touched)
- Do NOT rewrite documents from scratch — make surgical corrections
- Do NOT change the structure or purpose of a document
- Do NOT add new sections not already present in the document
- Do NOT commit to Git

---

## Project structure to understand before starting

### Modules
| Gradle path | Directory | Purpose |
|---|---|---|
| `:common` | `common/` | Shared utilities used by all modules |
| `:database` | `database/` | JPA entities, repositories, Liquibase migrations |
| `:ai-provider` | `ai-provider/` | AI provider integrations (Gemini, etc.) |
| `:docstorage` | `docstorage/` | Document storage and retrieval |
| `:fileloader` | `fileloader/` | File loading and processing |
| `:datafetcher` | `datafetcher/` | External data fetching (CRS, EIA, tracking systems) |
| `:api-validation` | `api-validation/` | REST Assured snapshot tests |
| Root | `viro-server/` | Main Spring Boot application |

### Tech stack
- Java 21, Spring Boot 3.5.5, Gradle 8.14.3
- Spring Data JPA, Liquibase, MySQL
- AWS RDS (database), AWS Elastic Beanstalk (hosting)

---

## Process — step by step

### Step 0: Confirm scope with user
Before doing any work, state clearly:
- Whether this is a **full run** or a **single directory run**
- The exact directory path(s) that will be processed (or "all of `.claude/`" for a full run)
- That `restapi-template.md` will be skipped

Wait for the user to confirm before proceeding.

### Step 1: Build a file list

For a **full run**, use the pre-built file list below instead of Globbing. Always exclude `restapi-template.md` and all files under `hosting/`. This is your work queue.

#### Full run — pre-built file list (as of 2026-03-10)

**Root level** (`.claude/_archive/`):
- `application-start-process.md`
- `AUTHENTICATION-SETUP.md`
- `docstorage-module.md`
- `mcp-server-module.md`

**ai-processing/**:
- `ai-processing.md`
- `ai-provider-module.md`
- `gemini-config.md`
- `retire-certs-types.md`

**ai-prompt/**:
- `ai-prompt-management.md`
- `ai-prompt-wiring.md`

**api-testing/**:
- `api-test-implementation.md`
- `api-test-repos.md`
- `how-it-works.md`
- `QUICKSTART.md`
- `README.md`
- `test_db_connect.md`
- `test-values/validate-by-csv.md`
- `test-values/validate-by-restapi.md`

**crs-processing/**:
- `crs-duplicates-process.md`

**csv-load/**:
- `csv-load-process.md`
- `liquibase-csv-loading-pattern.md`
- `plan-crs-load-tables.md`

**current_issues/**:
- `investigation-expiration-date-false-positive.md`
- `NAR-Voluntary-Compliance-Status.md`

**cust-transactions/**:
- `customer-transactions-be.md`
- `customer-transactions-fe.md`
- `customer-transactions-process.md`

**database/**:
- `database-module.md`
- ~~`restapi-template.md`~~ — **SKIP**

**datafetcher/**:
- `crs-facility-sync.md`
- `crs-sync-design.md`
- `datafetcher-module.md`
- `ts-nar-import.md`

**doc-audit/**:
- *(skip — this is the output directory)*

**facility-recon/**:
- `facility-recon-change-process.md`
- `facility-recon-mistake-process.md`
- `facility-recon-new.md`
- `facility-recon-rule-architecture.md`
- `rules-change-code.md`
- `rules-explained.md`

**fileloader/**:
- `fileloader-implementation-progress.md`
- `fileloader-module.md`
- `fileloader-test-plan.md`
- `file-orchestration.md`

**frontend/**:
- `frontend-module.md`

**hosting/** — **SKIP entirely**

**retire-cert-process/**:
- `promotion-process-line-by-line.md`
- `retire-certs-be.md`
- `retire-certs-fe.md`
- `retire-certs-process.md`
- `retire-to-transaction.md`
- `retire-upload-statuses.md`

**ts-info/**:
- `ts-liquibase.md`
- `ts-load-system.md`

Also process these non-archive `.claude/` files:
- `.claude/clazzname-pattern.md`
- `.claude/agents/be-agent.md`
- `.claude/agents/fe-agent.md`
- `.claude/agents/loadcsv-agent.md`
- `.claude/agents/rest-temp-agent.md`

For a **single directory run**, Glob `.md` files only in the specified input directory (add `/**` to the path if recursive was requested).

Always exclude `restapi-template.md` and all files under `hosting/`. This is your work queue.

### Step 2: For each document
1. Read the document fully.
2. Identify every factual claim (class names, paths, field names, endpoints, process descriptions).
3. Locate the relevant source files using Glob and Grep.
4. Read the source files to verify each claim.
5. Edit the document to correct any inaccuracies. Preserve tone, structure, and intent.
6. Record the changes made (see Output section below).

### Step 3: Produce the change log
After all documents are processed, write a dated change summary file:
- Path: `.claude/_archive/doc-audit/doc-audit-YYYY-MM-DD.md`
- Format: one section per document modified, listing what was changed and why

---

## Change log format

```
# Documentation Audit — YYYY-MM-DD

## Directory: <relative path to directory>
Started:  HH:MM:SS
Finished: HH:MM:SS

### <relative path to doc>
- Changed: <what was wrong> → <what it now says>
- Removed: <section or claim that no longer exists in code>

### <next doc in same directory>
...

## Directory: <next directory>
Started:  HH:MM:SS
Finished: HH:MM:SS

### <relative path to doc>
...
```

Directories are processed one at a time. Record `Started` when the first file in a directory begins processing, and `Finished` when the last file in that directory is complete.

---

## Source code search strategy

Use these tools in order:
1. **Glob** — find files by name pattern (e.g., `**/*Service*.java`, `**/build.gradle`)
2. **Grep** — search file contents for class names, method names, field names, endpoint paths
3. **Read** — read specific files to verify details

Do NOT use Bash for file searching. Use Glob and Grep exclusively.

---

## Rules

- Never modify `restapi-template.md`
- Never commit to Git
- Never drop or alter the database
- Always read the source before updating a doc — do not guess
- If a document refers to a feature that is partially implemented, note it accurately
- If you cannot find the source for a claim, leave the claim unchanged and note it in the change log as "unverified"
