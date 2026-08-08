---
name: domain-pojo-from-tables-doc
description: Generate a plain Domain POJO (extending BaseDomain or BaseUniqueDomain) in common.domain from a table's column list described in a markdown tables doc (e.g. clinical-trials-tables.md). Use whenever asked to turn a table spec/section from a design doc into a Domain class, or to "make the POJO for" a table, ahead of running database-restapi-template. Stops at the POJO — does not generate DB entity, repository, service, or controller layers.
---

# Domain POJO from Tables Doc

## Purpose

Bridges a hand-written table spec (a markdown doc describing tables the way
`clinical-trials-tables.md` does — column name, MySQL type, nullable/unique/FK notes as
inline comments) into this project's actual input format for the
`database-restapi-template` skill: a plain Java Domain POJO under
`com.seibel.cancer.common.domain`.

This skill is intentionally narrow. It produces **one file** (the Domain class) and
stops there, so it can be reviewed and hand-edited before the much larger
`database-restapi-template` skill is invoked on it. It never generates the DB entity,
repository, mapper, service, controller, tests, or Liquibase changeset — that is the
next skill's job.

## Generic by design

This is not specific to `clinical-trials-tables.md` or to any one project's schema.
Any markdown document that lists tables with a column block (name, type, and inline
notes) is valid input. The user will point at a specific table section within
whatever doc they're using.

## Input

- A markdown file path (e.g. `../../_archive/clinical-trials/clinical-trials-tables.md`) and a table name/section
  within it — OR a pasted column block directly, if there is no doc yet.
- The table's own column list, e.g.:
  ```
  nct_id                        varchar(16)     unique
  brief_title                   varchar(500)    not null
  overall_status                varchar(32)                -- RECRUITING, COMPLETED, ...
  enrollment_count               int
  healthy_volunteers             tinyint(1)
  ```

## What this skill does NOT do

- Does not resolve or wire foreign keys. Columns noted as FK (e.g. `trial_id bigint
  not null -- FK -> trial.id`) are **skipped entirely** — do not emit a field for
  them. The user handles relationships by hand, later, at the entity layer.
- Does not generate join tables (pure many-to-many link tables with no independent
  identity, per the doc's own "Conventions" section) as a Domain class at all — flag
  these to the user instead of generating anything, since they don't get a
  `BaseDomain`-derived POJO in this project's convention.
- Does not touch the entity/repository/mapper/service/controller/test layers.
- Does not guess at a base class silently (see below).

## Base class selection — always ask

Every generated Domain class extends either `BaseDomain` or `BaseUniqueDomain`
(`common.domain`). **Always ask the user which one applies for this table** before
generating — do not infer it from column names, even if the table happens to include
columns like `unique_id`/`version`/`status` that look like a match for
`BaseUniqueDomain`. Confirm explicitly every time.

- `BaseDomain` fields already provided (never re-declare): `id`, `extid`, `createdAt`,
  `updatedAt`, `deletedAt`, `active`.
- `BaseUniqueDomain` (extends `BaseDomain`) additionally provides: `uniqueId`,
  `version`, `status`. If the user says this table extends `BaseUniqueDomain`, drop
  any of the table's own `unique_id`/`version`/`status` columns from the generated
  field list — they're inherited.

Per the doc's own conventions section, the base `id`/`extid`/`created_at`/`updated_at`
/`deleted_at`/`active` columns are usually documented once up front and *not* repeated
per-table — don't emit fields for these regardless of whether the table's own section
happens to restate them.

## Field type mapping (MySQL doc type → Java)

| MySQL type (as written in doc) | Java field type       |
|---------------------------------|------------------------|
| `bigint`                         | `Long`                |
| `int`                            | `Integer`              |
| `varchar(n)`, `text`, `longtext` | `String`               |
| `datetime`                       | `LocalDateTime`        |
| `date`                           | `LocalDate`            |
| `tinyint(1)`                     | `Boolean`              |
| `decimal(p,s)`                   | `BigDecimal`           |

Field name: snake_case column name → camelCase Java field name.

## Output

A single file: `common/src/main/java/com/seibel/cancer/common/domain/{Entity}.java`

```java
package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class {Entity} extends BaseDomain {  // or BaseUniqueDomain, per user's answer
    private String someField;
    private Integer anotherField;
    // ... one field per non-base, non-FK column, in doc order
}
```

Add only the imports actually needed for the field types used (e.g. `LocalDateTime`,
`LocalDate`, `BigDecimal`).

## When invoked

1. Identify the table name and locate its column block (from the given doc path/section,
   or the pasted block).
2. If the table looks like a pure join table (per the doc's own conventions, or no
   independent id/extid), stop and tell the user — do not generate a Domain class for it.
3. List which columns will be skipped as FKs (if any) and confirm that's expected.
4. Ask the user: does this table extend `BaseDomain` or `BaseUniqueDomain`? Wait for
   the answer.
5. Map remaining columns to Java fields per the type table above.
6. Write the single Domain class file.
7. Report the file path and the field list generated (and any FK/join-table columns
   skipped), so the user can review before running `database-restapi-template` on it.
