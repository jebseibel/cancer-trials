> **This file has no YAML frontmatter, so it is not a registered subagent** — it cannot be
> invoked via the Agent tool. It works as a prompt you paste or reference by path. Add
> `name:`/`description:` frontmatter (see `enum-migration-agent.md`) to make it invocable.

## Input
- If given a file path instead of task content, read that file first to obtain the task details.

# Load CSV Data via Liquibase

## Pattern Reference

**Read `.claude/_archive/csv-load/liquibase-csv-loading-pattern.md` before starting.** It is the
single source of truth for this pattern — CSV format, the `loadData` changeset shape, how base
columns are filled by database defaults, empty-string behaviour, and troubleshooting.

Do not duplicate that content here. If the pattern changes, change it there.

## Task

Given a table and a set of reference/seed data, add it to the Liquibase load.

1. **Write the CSV** to `database/src/main/resources/db/data/`, following the existing numeric
   prefix convention (`01-customer.csv`, `02-user.csv`, `03-purchase.csv` — use the next number).
   Header names must match the database column names exactly. Do **not** include `id`, `extid`,
   `created_at`, `updated_at`, `deleted_at`, or `active`.

2. **Add a `loadData` block** to
   `database/src/main/resources/db/changelog/changes/100-load-init-data.yaml`. All CSV loading
   in this project lives in that one changeset — add a block to it rather than creating a new
   changeset file. For a single row, use `insert` instead (see the `trial_source` seed there).

3. **Confirm the table exists** in an earlier-numbered changeset. If it does not, that table
   needs creating first — that is the `database-restapi-template` skill's job, not this one.

4. **Verify the column list** in the changeset matches the CSV header exactly, in order.

## Constraints

- Never connect to the database directly and never run migrations. The user owns the database.
- Changesets run **once**. Editing an already-applied changeset has no effect on startup —
  `spring.liquibase.drop-first` is off in this project. A database rebuild is required, which is
  the user's call (the n8n `clear-db` webhook).
- Quote any column type containing a comma: `type: "decimal(10,2)"`. Unquoted, the YAML parser
  reads the comma as a map separator and aborts the whole changelog.

## Report Back

State which CSV was written, which changeset block was added, and that a database rebuild is
needed before the data appears.
