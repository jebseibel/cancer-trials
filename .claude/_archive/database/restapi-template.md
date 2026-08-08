# Impact of Changes to a Db Entity File

The ripple-effect checklist: when you change a `*Db.java` entity, these are the files that must
change with it. Verified against the real `Customer` stack 2026-08-08.

> **For generating a new entity from scratch, do not use this file.** Use the skills — they are
> the maintained versions of that pattern:
> - `.claude/skills/database-restapi-template/SKILL.md` — the 9-file layered scaffold
> - `.claude/skills/database-restapi-testing/SKILL.md` — the 3 test classes + builder methods
> - `.claude/skills/entity-full-stack/SKILL.md` — both of the above, chained
>
> This doc covers only the *modification* case, which the skills do not.

## The checklist

Adding, removing, or renaming a field on `CustomerDb` requires updating, in this order:

| # | File | Package |
| --- | --- | --- |
| 1 | `Customer.java` (domain) | `com.seibel.cancer.common.domain` |
| 2 | `CustomerMapper.java` | `com.seibel.cancer.database.db.mapper` |
| 3 | `CustomerRepository.java` | `com.seibel.cancer.database.db.repository` |
| 4 | `CustomerDbService.java` | `com.seibel.cancer.database.db.service` |
| 5 | `CustomerService.java` | `com.seibel.cancer.service` |
| 6 | `RequestCustomerCreate.java` | `com.seibel.cancer.web.request` |
| 7 | `RequestCustomerUpdate.java` | `com.seibel.cancer.web.request` |
| 8 | `ResponseCustomer.java` | `com.seibel.cancer.web.response` |
| 9 | `CustomerController.java` (+ its package-private converter, same file) | `com.seibel.cancer.web.controller` |
| 10 | `CustomerMapperTest.java` | `database/src/test/.../db/mapper` |
| 11 | `CustomerRepositoryTest.java` | `database/src/test/.../db/repository` |
| 12 | `CustomerDbServiceTest.java` | `database/src/test/.../db/service` |
| 13 | `DomainBuilderDatabase.java` — append/adjust builder methods | `database/src/test/.../testutils` |
| 14 | The entity's Liquibase changeset | `database/src/main/resources/db/changelog/changes/` |

## Things that are easy to miss

- **The converter is inside the controller file**, not a separate class. It is package-private
  and it is where extid ↔ numeric-id resolution happens.
- **`validateUpdateRequest()`** in that converter enumerates every field to check "at least one
  provided". A new field must be added to that condition or partial updates will silently
  accept an empty request.
- **`DomainBuilderDatabase` is append-only** and shared by every entity's tests — it was at 109
  methods as of the PatientDiagnosis work. Do not reorder or renumber it.
- **Changeset edits do not re-apply on startup.** `spring.liquibase.drop-first` is off, so a
  column added to an already-applied changeset needs a database rebuild (n8n `clear-db`
  webhook) before it exists.
- **Quote comma-bearing column types**: `type: "decimal(10,2)"`. Unquoted, Liquibase's YAML
  parser treats the comma as a map separator and aborts the changelog.
- **The extid-only rule applies to new FK-like fields.** A new `somethingId` on the entity is
  exposed as `somethingExtid` at the REST boundary, resolved both ways in the converter.

## Related

- `../code-style/restapi-code-style.md` — layering rules and controller style
- `database-module.md` — the persistence layer overview
