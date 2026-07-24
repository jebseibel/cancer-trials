---
name: project-documentation-agent
description: Walks project documentation files and updates them to accurately reflect the current codebase. May run against all docs or a single specified directory. Run this when docs may be out of date with the code.
---
project-documentation-agent

# Project Documentation Agent

<!--
  Runtime estimate scales with however many .md files actually exist under .claude/
  at run time (this agent discovers them via Glob rather than a fixed list).
  Roughly: 2–6 source file lookups per doc, so a full run's tool-call count is
  ~5x the doc count. A directory with many small docs runs faster per-doc than one
  with few large, deeply cross-referenced docs.
-->

## How to Launch

**Full run** (all documentation):
> "Run the project documentation agent"

**Single directory run**:
> "Run the project documentation agent on `.claude/_archive/database`"

**Single directory run (recursive)**:
> "Run the project documentation agent on `.claude/_archive/database/**`"

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
If an input directory is provided (e.g., `.claude/_archive/database`), process only the `.md` files in that directory. Do not recurse into subdirectories unless the input path ends with `/**`.

### Files to NEVER modify
- `.claude/database-restapi-template.md` — skip entirely, regardless of input (this is the canonical layered-architecture pattern reference; other agents point to it and it must stay stable)
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

- Do NOT add code blocks or code samples to documents (except `database-restapi-template.md`, which is never touched)
- Do NOT rewrite documents from scratch — make surgical corrections
- Do NOT change the structure or purpose of a document
- Do NOT add new sections not already present in the document
- Do NOT commit to Git

---

## Project structure to understand before starting

### Modules
| Gradle path | Directory | Purpose |
|---|---|---|
| Root | (repo root) | Main Spring Boot application — web layer, security, config |
| `:common` | `common/` | Shared, framework-light domain objects, enums, exceptions |
| `:database` | `database/` | JPA entities, repositories, mappers, db services, Liquibase migrations |
| `:datafetcher` | `datafetcher/` | External data fetching |
| `:ai-provider` | `ai-provider/` | AI provider integrations (OpenAI/Anthropic/Gemini/OpenRouter via Spring AI) |

Confirm this table against `settings.gradle` before relying on it — modules get added
or removed over time and this list can go stale like any other doc.

### Tech stack
- Java 21, Spring Boot 3.5.x, Gradle
- Spring Data JPA, Liquibase, MySQL (local, via Docker for dev)
- Frontend: React + Vite + TypeScript + Tailwind, bundled into the Spring Boot jar for deployment

---

## Process — step by step

### Step 0: Confirm scope with user
Before doing any work, state clearly:
- Whether this is a **full run** or a **single directory run**
- The exact directory path(s) that will be processed (or "all of `.claude/`" for a full run)
- That `database-restapi-template.md` will be skipped

Wait for the user to confirm before proceeding.

### Step 1: Build a file list

For a **full run**, Glob `.claude/**/*.md` to discover the current work queue — do not
rely on a hardcoded list, since the doc set changes over time and a stale list will
silently skip new docs or fail on deleted ones. Always exclude
`database-restapi-template.md` and all files under `.claude/_archive/hosting/`.

For a **single directory run**, Glob `.md` files only in the specified input directory
(add `/**` to the path if recursive was requested).

Always exclude `database-restapi-template.md` and all files under `hosting/`. This is
your work queue.

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

- Never modify `database-restapi-template.md`
- Never commit to Git
- Never drop or alter the database
- Always read the source before updating a doc — do not guess
- If a document refers to a feature that is partially implemented, note it accurately
- If you cannot find the source for a claim, leave the claim unchanged and note it in the change log as "unverified"
