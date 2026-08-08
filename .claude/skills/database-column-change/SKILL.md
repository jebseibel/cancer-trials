---
name: database-column-change
description: Change one or more existing columns on a table - widen or narrow a type, rename, change nullability, add or drop a column - and update every layer that pins that column's shape (Liquibase changeset, JPA entity, request DTOs, test builder, tests). Use whenever asked to alter, widen, resize, rename, or add/drop a column on an entity that already exists. For creating a brand-new entity, use database-restapi-template or entity-full-stack instead.
---

# Database Column Change

## Project Goal

The other database skills all **create**. This one **modifies**: the entity already exists,
the files already exist, and the work is finding every place a column's shape is duplicated
and moving them together.

Scoped to **one table, any number of columns**, because the expensive parts — the database
rebuild and the test run — are per-table, not per-column. Batching them is strictly cheaper
than running this once per column.

Written from the real code (`Location`, `Trial`, `PatientDiagnosis`), not an idealised design.

## Input

- **Table / entity** — e.g. `location` / `Location`
- **One or more column changes**, each stating the change type and the new spec:

```
zip        widen    varchar(16) -> varchar(64)
status     rename   status -> recruitment_status
notes      add      varchar(1000), nullable
old_flag   drop
city       nullable not null -> nullable
```

If the caller gives only "make zip bigger", ask for the target size rather than guessing.

## Where a column's shape is pinned

**Verified 2026-08-08.** Only three of the eight layers constrain a column. Knowing which
five do *not* is what keeps this skill from touching files pointlessly.

| Layer | File | Pins the shape? |
| --- | --- | --- |
| Changeset | `database/src/main/resources/db/changelog/changes/NNN-{table}.yaml` | **Yes** — `type: varchar(n)`, `constraints: { nullable: false }` |
| Entity | `database/.../db/entity/{Entity}Db.java` | **Yes** — `@Column(name = "...", length = n)` |
| Request DTOs | `src/main/java/com/seibel/cancer/web/request/Request{Entity}Create.java` and `...Update.java` | **Yes** — `@Size(max = n)`, `@NotEmpty`/`@NotNull` |
| Response DTO | `src/main/java/com/seibel/cancer/web/response/Response{Entity}.java` | No — plain fields, no constraints |
| Domain POJO | `common/.../domain/{Entity}.java` | No — plain POJO |
| Mapper | `database/.../db/mapper/{Entity}Mapper.java` | No — ModelMapper, type-agnostic |
| Test builder | `database/src/test/.../testutils/DomainBuilderDatabase.java` | Only if the generator can exceed the new bound |
| Tests | `database/src/test/.../db/{mapper,repository,service}/{Entity}*Test.java` | Only if they assert the old shape |

**Missing one of the three is the characteristic failure.** Widening only the changeset leaves
the column wide while the app still rejects the value — which reads as "the schema change
didn't work" rather than "a validation annotation was missed."

For a rename or a type change, the untouched layers *do* come into play: the field name flows
through domain, mapper, DTOs, and every caller.

## Per-change-type checklist

### Widen (e.g. `varchar(16)` -> `varchar(64)`)

Safe — no existing data can violate a larger bound.

1. Changeset `type:`
2. Entity `@Column(length = ...)`
3. `@Size(max = ...)` in **both** request DTOs, and the message text with it
4. Test builder: usually nothing — generators are capped well below typical widths
5. Comment the changeset with *why*, if the widening is absorbing malformed source data
   rather than accommodating legitimately longer values. That distinction is not recoverable
   from the number alone.

### Narrow

Same three files, plus: **check existing data first.** Ask the user to run a count of rows
exceeding the new bound; never query the database directly. If any exist, stop and report —
narrowing past live data silently truncates on rebuild.

### Rename

The widest ripple. Beyond the three: domain field, entity field name, mapper if it maps
explicitly, both request DTOs, response DTO, every repository finder naming the property
(`findByOldName`), every service and controller reference, the test builder setter, tests, and
the frontend `types/api.ts` plus any page reading the field.

Grep for the old name across `--include="*.java" --include="*.ts" --include="*.tsx"` before
starting, and report the count so the caller sees the scope.

### Nullability

Changeset `constraints: { nullable: false }` pairs with `@NotEmpty` (String) or `@NotNull`
(other types) on the **create** DTO. The update DTO stays unconstrained — partial updates omit
fields by design.

Making a column NOT NULL requires existing rows to have a value; ask the user to check.

### Add

Effectively a mini-scaffold: changeset column, entity field + `@Column`, domain field, both
request DTOs, response DTO, mapper if explicit, builder method, and `validateUpdateRequest()`
in the controller's converter — **that last one is easy to miss.** A new field absent from
that null-check means a partial update containing only the new field is rejected as empty.

### Drop

Reverse of add. Grep for the field name first; a drop that leaves a reference behind fails to
compile, which is the good case. The bad case is dropping a column some other query still
selects.

## The rebuild trap — read before running

**Editing an already-applied changeset breaks its checksum.** Liquibase records a hash per
changeset; changing the file makes it no longer match, and startup fails with:

```
Validation Failed:
  1 changesets check sum
    db/changelog/changes/015-location.yaml::create_location_table::jeb
    was: 9:9c47... but is now: 9:1e27...
```

Two things make this sharper in this project:

- **`spring.liquibase.drop-first` is `false`** (so the UCHealth OAuth token survives restarts),
  so the changelog is not re-applied on boot. A rebuild is required.
- **The tests run against the same database as the app** —
  `database/src/test/resources/application-test-database.yml` uses the same `RDS_DB_NAME`.
  So the checksum failure breaks the *test suite* too, as a context-load error that looks
  nothing like a column problem.

**This project edits changesets in place rather than adding new ones** — it is not in
production, and `CLAUDE.md` states the convention. So a rebuild is the expected cost, not a
mistake to avoid.

**The rebuild is the user's action, always.** Never run it, never touch the database directly.
Tell them: rebuild via the n8n `clear-db` webhook (`http://localhost:5678/webhook/clear-db`,
GET), then restart the backend.

Warn them what a rebuild costs:
- all ingested trials, so re-ingest afterwards
- the Epic OAuth token, so UCHealth needs re-authorising
- vector-store consistency — chunks in Qdrant will reference trial extids that no longer
  exist, so re-backfill (or recreate the collection) after re-ingesting

## Order of work

1. **Read the changeset first** to confirm the current spec. Do not trust the request's
   "from" value — it may be stale.
2. **Grep the column and field name** across Java, TS, and TSX. Report the hit count before
   editing so the caller sees the scope.
3. **Apply every change to all affected layers**, table-wide, before compiling. One pass.
4. **Quote comma-bearing types**: `type: "decimal(10,2)"`. Unquoted, Liquibase's YAML parser
   reads the comma as a map separator and aborts the entire changelog. This has bitten this
   project twice.
5. **Compile** — `./gradlew compileJava compileTestJava`. This catches renames and drops.
6. **Stop and ask for the rebuild.** Tests cannot pass before it; a failure here is the
   checksum error, not a defect.
7. **After the rebuild**, run `./gradlew :database:test --tests "*{Entity}*"`, then the full
   `:database` suite to catch anything the shared builder affected.
8. **Verify counts from the XML**, not Gradle's exit code — Gradle reports success when zero
   tests run:

```bash
python3 -c "
import xml.etree.ElementTree as ET, glob
for p in sorted(glob.glob('database/build/test-results/test/TEST-*.xml')):
    r=ET.parse(p).getroot()
    print(r.get('name').split('.')[-1], r.get('tests'), 'failures='+r.get('failures'))
"
```

## Report back

- Every file changed, grouped by layer
- Any layer deliberately **not** touched, and why (e.g. "response DTO has no constraints")
- For a rename: the grep count before and after, confirming zero stale references
- That a database rebuild is required, and what it costs
- Test counts after the rebuild

## Do not

- Run the rebuild, drop the schema, or connect to the database. All three are the user's.
- Add a new changeset instead of editing the existing one. Against convention here.
- Change only the changeset. That is the characteristic bug this skill exists to prevent.
- Widen a column to accommodate malformed data without saying so in a comment. The next
  reader needs to know whether 64 means "postal codes can be this long" or "one site sends
  two codes in one field."
