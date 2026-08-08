---
name: entity-full-stack
description: Run the complete entity pipeline in one pass - Domain POJO from a tables doc, then the full layered REST API scaffold, then the test suite. Chains domain-pojo-from-tables-doc, database-restapi-template, and database-restapi-testing in order, with compile/test verification between stages. Use when asked to "do the full stack" or "run all three skills" for an entity, or to take a table spec all the way from design doc to tested code. For a single stage, invoke that skill directly instead.
---

# Entity Full Stack

## Purpose

Takes one table from a markdown design doc all the way to tested, compiling code by running the
three existing skills in sequence:

1. `domain-pojo-from-tables-doc` — table spec → Domain POJO
2. `database-restapi-template` — Domain POJO → 9-file layered scaffold + Liquibase changeset
3. `database-restapi-testing` — scaffold → Mapper/Repository/DbService tests + builder methods

**This skill adds no generation logic of its own.** Each stage is executed by reading that skill's
own `SKILL.md` and following it exactly. This file only handles sequencing, collecting inputs up
front, and verifying between stages.

## Collect ALL inputs before starting

The point of this chain is one uninterrupted run, so everything the three skills would otherwise
stop and ask for must be settled first. **If any of these is missing or ambiguous, ask for all of
the missing ones in a single question, then proceed without further interruption.**

| Input | Why it is needed | Default if unstated |
| --- | --- | --- |
| **Entity name** | Names every generated file | — must be given |
| **Source doc + table section** | `domain-pojo-from-tables-doc` reads the column list from it | — must be given |
| **`BaseDomain` or `BaseUniqueDomain`** | That skill **always asks and never infers** — the whole reason a naive chain stalls | `BaseDomain` |
| **FK columns to keep** | FK columns are **skipped entirely** by default; the project convention is to add them back as `{targetTable}Id` | keep none |
| **Module for tests** | `database-restapi-testing` verifies module prerequisites | `:database` |

Confirming these in one question, then running clean, is what distinguishes this from invoking the
three skills yourself.

## Sequence

### Stage 1 — Domain POJO

Read `.claude/skills/domain-pojo-from-tables-doc/SKILL.md` and follow it for the named table.

- The base-class question is **already answered** from the inputs above — do not ask again.
- Add back any FK fields the user asked to keep, named `{targetTable}Id`, since that skill skips
  them by default.
- **Report which columns were skipped as FKs.** This is the one place a silent omission causes a
  wrong scaffold downstream, because stage 2 generates from whatever fields exist.

### Stage 2 — Layered scaffold

Read `.claude/skills/database-restapi-template/SKILL.md` and follow it, using the stage 1 POJO as
input.

- Generates the 9 layers plus a Liquibase changeset.
- **Watch for `decimal(x,y)` column types in the changeset.** Unquoted, the comma breaks
  Liquibase's YAML flow-mapping parser and aborts the changelog mid-run — a recurring bug in this
  project. Always emit `type: "decimal(10,2)"` with quotes.
- That skill's own step 4 requires `./gradlew compileJava` plus the affected module. **Do that
  before stage 3** — a scaffold that does not compile cannot have tests generated against it.

### Stage 3 — Tests

Read `.claude/skills/database-restapi-testing/SKILL.md` and follow it.

- It has a **prerequisites section to verify before generating** (a `@SpringBootConfiguration` at
  or above the test class, datasource config, `spring-dotenv` on the module's test source set,
  the `test` task working directory resolving to `.env`). Check those rather than assuming.
- Appends builder methods to the shared `DomainBuilderDatabase` — append, never overwrite.
- Run the generated tests. `./gradlew :{module}:test --tests "*{Entity}*"`.

## Verify between stages, not just at the end

The reason to chain rather than fire three prompts is that failures get attributed to the stage
that caused them:

| After | Run | If it fails |
| --- | --- | --- |
| Stage 1 | Nothing to compile yet — re-read the POJO against the table spec | Fix before scaffolding; a wrong POJO propagates into 9 files |
| Stage 2 | `./gradlew compileJava` + the affected module | **Stop.** Do not generate tests against code that does not compile |
| Stage 3 | `./gradlew :{module}:test --tests "*{Entity}*"` | Report the failures; do not claim done |

**Gradle reports `BUILD SUCCESSFUL` even when no tests ran.** Confirm counts from
`{module}/build/test-results/test/TEST-*.xml` rather than trusting the exit code.

## Report at the end

- Every file created or modified, grouped by stage.
- The compile and test results, with actual counts.
- Columns skipped as FKs, and any added back.
- Anything the user still has to do by hand — this project's convention is that the **user** starts
  and stops the backend and manages the database, so a new changeset needs their action to apply.

## Scope limits

- **One entity per run.** For several entities, run this once each, or use the parallel fan-out
  pattern in `.claude/skills/skills-reference.md` (one sub-agent per entity, each told to read this
  skill).
- **Does not create join tables or wire foreign keys** beyond adding the `{targetTable}Id` fields
  requested up front.
- **Never touches the database.** No migrations are applied, no schema is dropped. Changesets are
  written as files only.
- **Not for an entity that already exists.** If the Domain class is already there, invoke
  `database-restapi-template` or `database-restapi-testing` directly instead — this skill assumes
  it is starting from a table spec.

## Note for reuse in other projects

This chain is portable, but the three skills it calls are **not** — they hardcode
`com.seibel.cancer.*` packages, this project's module layout, and its `.claude` doc paths. Making
the set reusable elsewhere means parameterising the base package and module names first. Once that
is done, moving all four to `~/.claude/skills/` makes them available in every project without any
packaging; a plugin is only needed to share them with other people.
