# Liquibase CSV Data Loading Pattern

How to load reference/seed data from CSV files into the database during startup via Liquibase.

> **Status in this project (verified 2026-08-08).** In active use. `100-load-init-data.yaml`
> loads three CSVs from `database/src/main/resources/db/data/` — `01-customer.csv`,
> `02-user.csv`, `03-purchase.csv` — and also seeds the `trial_source` row for
> ClinicalTrials.gov with a plain `insert`.
>
> Note the customer/user/purchase data is inherited scaffolding from the original template
> project, not clinical-trials data.
>
> **There is no `clean_empty_strings` stored procedure in this project** and no
> `StringCleanupListener`, despite `.claude/CLAUDE.md` claiming both. See *Empty strings* below.

## 1. Create the CSV

Place it in `database/src/main/resources/db/data/`. The convention here is a numeric prefix
matching load order: `01-customer.csv`, `02-user.csv`, `03-purchase.csv`.

```csv
code,name,contact_name,description,contact_email,contact_phone
AATEST,AA Test,Eric Arnold,Test customer,eric@test.com,
```

- UTF-8, comma-separated
- Header row column names must match the database column names
- Double-quote any value containing a comma
- Leave empties blank — not `""` (see *Empty strings*)

**Do not include base columns.** No `id`, `extid`, `created_at`, `updated_at`, `deleted_at`, or
`active` — those are filled by database defaults (next section).

## 2. Add a loadData changeset

This project puts every CSV load in the single `100-load-init-data.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: load-init-data
      author: jeb
      labels: load_csv_data
      changes:
        - loadData:
            tableName: customer
            file: db/data/01-customer.csv
            relativeToChangelogFile: false
            encoding: UTF-8
            separator: ','
            quotchar: '"'
            columns:
              - column: { name: code, type: string }
              - column: { name: name, type: string }
              - column: { name: contact_name, type: string }
```

Key settings: `relativeToChangelogFile: false` means the path resolves from the classpath root
(`src/main/resources/`), which is why `db/data/...` works. `type: string` tells Liquibase how to
read the CSV cell — it is not the database column type.

For a single row, skip the CSV and use `insert` directly, as the `trial_source` seed does in
the same file.

## 3. Master changelog picks it up automatically

`db.changelog-master.yaml` is a single `includeAll` on `db/changelog/changes`, so a new
changeset file needs no registration:

```yaml
databaseChangeLog:
  - includeAll:
      path: db/changelog/changes
```

## Base columns are handled by database defaults

`loadData` bypasses JPA entirely and inserts via raw SQL, so `BaseDb` is not involved. When a
column is absent from the INSERT, MySQL applies its `DEFAULT`. The table-creation changesets
(e.g. `001-customer.yaml`) declare them:

```yaml
- column: { name: extid, type: varchar(36), constraints: { nullable: false, unique: true }, defaultValueComputed: "(UUID())" }
- column: { name: created_at, type: datetime, constraints: { nullable: false }, defaultValueComputed: CURRENT_TIMESTAMP }
- column: { name: active, type: int, defaultValueNumeric: 1 }
```

| Column | Default | Result |
| --- | --- | --- |
| `id` | AUTO_INCREMENT | sequential |
| `extid` | `(UUID())` | random UUID per row |
| `created_at` | `CURRENT_TIMESTAMP` | load time |
| `updated_at` / `deleted_at` | none | NULL |
| `active` | `1` | active |

This is a pure database feature. It works regardless of what the JPA entity says.

## Empty strings

Because `loadData` bypasses JPA, a blank CSV cell is inserted as an **empty string**, not NULL.
Nothing in this project converts them afterwards — there is no `clean_empty_strings` procedure
and no entity listener, so what the CSV contains is what lands in the table.

Options: leave the column nullable and accept `''`, pre-clean the CSV, or add a
`- sql:` step after the load. Prefer pre-cleaning the CSV — it keeps the fix visible in the data
rather than hidden in a migration.

## Changesets run once

Liquibase records executed changesets in `databasechangelog` and will not re-run one, **even if
the CSV changes**. To reload:

- Add a new changeset with a different `id`, or
- Rebuild the database (the n8n `clear-db` webhook — see `.claude/CLAUDE.md`)

Relevant here: `spring.liquibase.drop-first` is **off** in `application.yml` (so the UCHealth
OAuth token survives restarts), which means edits to an already-applied changeset do not take
effect on startup. A rebuild is required.

## Troubleshooting

**File not found** — confirm the file is under `src/main/resources/db/data/` and that
`relativeToChangelogFile: false` is set. Forward slashes only.

**Column not found / constraint violation** — CSV header names must match the database column
names exactly, and the table must be created by an earlier-numbered changeset.

**Data doesn't load** — the changeset `id` is probably already in `databasechangelog`. Check
there before assuming the YAML is wrong.

**YAML parse error mid-changelog** — if a column type contains a comma, quote it:
`type: "decimal(10,2)"`. Unquoted, Liquibase's YAML flow-mapping parser reads the comma as a map
separator and aborts the changelog. This has bitten this project twice
(`005-trial.yaml`, `015-location.yaml`).

## File organization

```
database/src/main/resources/db/
├── changelog/
│   ├── db.changelog-master.yaml          # includeAll on changes/
│   └── changes/
│       ├── 001-customer.yaml             # CREATE TABLE ...
│       ├── ...
│       └── 100-load-init-data.yaml       # all loadData lives here
└── data/
    ├── 01-customer.csv
    ├── 02-user.csv
    └── 03-purchase.csv
```
