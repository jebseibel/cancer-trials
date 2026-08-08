# Skills Reference

Ways to launch a skill in this project:

1. **Plain language** — just ask, e.g. "run database-restapi-template on Condition" or
   "use the domain-pojo-from-tables-doc skill on sponsor."
2. **Slash command** — `/skill-name args`, e.g. `/database-restapi-template Condition`.
3. **Explicit trigger phrases** the user has already established — "follow the restapi
   template for X" or "follow the restapi testing template for X."

**Multiple targets at once?** Yes — ask for both in one message, e.g. "run
database-restapi-template on Sponsor and Medication." They're generated one at a time,
sequentially, not literally in parallel (each skill's own spec is one-target-at-a-time),
but one request covers both.

**Truly parallel, for many targets (e.g. 5+)** — ask for a fan-out of sub-agents, one
per target, each told which skill to run. Wording to use:

> "Launch 5 agents in parallel, one per target [list them]. Each agent should run the
> database-restapi-template skill (read .claude/skills/database-restapi-template/SKILL.md
> and follow it) on its assigned target."

This matters because a fresh sub-agent has no memory of this conversation — the prompt
must name the skill file explicitly and give it the target, not just say "run the skill
on X" the way you can in this chat.

**Generic template** for "find every target missing some artifact, then fan out one
skill run per target":

> "Identify every [target type] that has [existing artifact] but not [missing
> artifact], using [source doc/listing] to find targets and [file location] to check
> what already exists. Launch one agent per remaining target, in parallel, each told
> to read `.claude/skills/<skill-name>/SKILL.md` and run it on that target. Report back
> the full list of files created once all agents finish."

**Example: scaffold every remaining entity in one go.**

> "Using `../_archive/clinical-trials/clinical-trials-tables.md` as the source of table names and the file
> listing at `common/src/main/java/com/seibel/cancer/common/domain/` to see which
> Domain POJOs already exist, identify every core (non-join) table that has a Domain
> POJO but has not yet been scaffolded (no matching `{Entity}Db` in
> `database/src/main/java/com/seibel/cancer/database/db/entity/`). Launch one agent per
> remaining entity, in parallel, each told to read
> `.claude/skills/database-restapi-template/SKILL.md` and run it on that entity. Report
> back the full list of files created once all agents finish."

No separate "master agent" layer is needed — the main assistant is already the
orchestrator when you ask for a parallel fan-out in one message.

---

## `entity-full-stack` — all three, in one pass

Chains the three skills below in order: table spec → Domain POJO → 9-file scaffold + changeset →
tests. Collects every input up front so it runs without stopping, and compiles between stages so a
failure is attributed to the stage that caused it.

> "Run entity-full-stack on PatientDiagnosis, from
> .claude/clinical-trials-rag/DIAGNOSIS_MATCHING_DESIGN.md, extends BaseDomain, keep appUserId."

Give it the entity, the source doc, the base class, and any FK fields to keep. Missing anything, it
asks once for all of them and then runs clean — the base-class question in particular is one
`domain-pojo-from-tables-doc` **always** asks, so pre-answering it is what makes the chain
uninterrupted.

Use a single skill directly when you only want that stage, or when the entity already exists.

---

## `database-restapi-template`

Scaffolds the 9-file layered architecture (Domain input → Request/Response DTOs →
Entity → Mapper → Repository → DbService → Service → Controller+Converter) plus a
Liquibase changeset for a single Domain POJO. Input must already exist as a Domain
class under `common/src/main/java/com/seibel/cancer/common/domain/`. Does not
generate tests — that's `database-restapi-testing`'s job, run as a separate step.

**Single target:**

> "Run database-restapi-template on Condition."

**Multiple targets, sequential:**

> "Run database-restapi-template on Sponsor and Medication."

**Fan out, one agent per remaining entity:**

> "Using `../_archive/clinical-trials/clinical-trials-tables.md` as the source of table names and the file
> listing at `common/src/main/java/com/seibel/cancer/common/domain/` to see which
> Domain POJOs already exist, identify every core (non-join) table that has a Domain
> POJO but has not yet been scaffolded (no matching `{Entity}Db` in
> `database/src/main/java/com/seibel/cancer/database/db/entity/`). Launch one agent per
> remaining entity, in parallel, each told to read
> `.claude/skills/database-restapi-template/SKILL.md` and run it on that entity. Report
> back the full list of files created once all agents finish."

---

## `database-restapi-testing`

Generates the 3 test files (`{Entity}MapperTest`, `{Entity}RepositoryTest`,
`{Entity}DbServiceTest`) plus builder methods appended to the shared
`DomainBuilderDatabase` class, for an entity already scaffolded by
`database-restapi-template`.

**Single target:**

> "Run database-restapi-testing on Condition."

**Fan out, one agent per entity missing tests:**

> "Using the file listing at
> `database/src/main/java/com/seibel/cancer/database/db/entity/` to see which entities
> are already scaffolded, identify every `{Entity}Db` that does not yet have all three
> of `database/src/test/java/com/seibel/cancer/database/db/mapper/{Entity}MapperTest.java`,
> `.../repository/{Entity}RepositoryTest.java`, and `.../service/{Entity}DbServiceTest.java`.
> Launch one agent per remaining entity, in parallel, each told to read
> `.claude/skills/database-restapi-testing/SKILL.md` and run it on that entity. Report
> back the full list of files created once all agents finish."

Note the check is against the mirrored `test` source tree, not the `entity` directory
itself — tests never live alongside the entity file in this project.

If fanning out many agents at once, expect repeated `./gradlew` approval prompts
unless `Bash(./gradlew *)` is allowlisted in `.claude/settings.json` (each agent runs
its own compile/test verification).

---

## `domain-pojo-from-tables-doc`

Turns a table's column list from a markdown design doc (e.g.
`clinical-trials-tables.md`) into just the Domain POJO input the template skill
needs. Skips FK columns entirely by default (add them back manually afterward, named
`{targetTable}Id`, if the project convention calls for keeping FK fields as plain
columns). Always asks whether the entity extends `BaseDomain` or `BaseUniqueDomain`
— never infers it.

**Single target:**

> "Run domain-pojo-from-tables-doc on sponsor, using .claude/clinical-trials-tables.md."

**Multiple targets:**

> "Run domain-pojo-from-tables-doc on every remaining core table in
> .claude/clinical-trials-tables.md that doesn't have a Domain POJO yet."

---

## `database-column-change`

The only skill that **modifies** an existing entity rather than creating one. Changes
one or more columns on a single table — widen, narrow, rename, change nullability,
add, or drop — and updates every layer that pins the column's shape.

Table-scoped, not column-scoped, because the expensive parts (the database rebuild and
the test run) are per-table. Batch all the changes for one table into a single run.

**Only three layers actually constrain a column** — the Liquibase changeset, the
entity's `@Column(length = ...)`, and `@Size`/`@NotEmpty` on the two request DTOs. The
domain POJO, mapper, and response DTO carry no constraints. Changing only the changeset
is the characteristic bug: the column goes wide while the app still rejects the value,
which reads as "the schema change didn't work."

**Single column:**

> "Run database-column-change on location: widen zip from varchar(16) to varchar(64)."

**Several columns on one table:**

> "Run database-column-change on trial: widen brief_title to varchar(512), make
> nct_id not null, and drop is_paid_study."

**Expect it to stop partway.** Editing an already-applied changeset breaks its Liquibase
checksum, and `drop-first` is off in this project — so the skill compiles, then stops and
asks you to rebuild the database (n8n `clear-db` webhook) before tests can run. A failure
before that rebuild is the checksum error, not a defect in the change.

Note the tests run against the *same* database as the app, so the checksum failure shows
up as a Spring context-load error across the whole `:database` suite, looking nothing
like a column problem.
