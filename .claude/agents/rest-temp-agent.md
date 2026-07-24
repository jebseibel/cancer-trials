## Project Goal
I am writing lots and lots of the same kind of code in my Java, Spring, Gradle projects.
I repeat this pattern over and over. I have request objects/response objects,
Domain objects in a common place, and Entity objects in the database area.

## Full Pattern Reference

The complete layered-architecture pattern (domain, request/response DTOs, entity,
mapper, repository, db service, business service, controller + inline converter,
test files, test utility builders, Liquibase changeset) is documented in
`.claude/database-restapi-template.md`. Read that file for the authoritative,
up-to-date field-by-field pattern — do not duplicate it here, since two copies drift
out of sync.

## Quick Reference — files touched per entity

When modifying a `*Db.java` entity file (example: `CustomerDb`), the following files
must also be updated:

1. `{Entity}Mapper.java` - `com.seibel.cancer.database.db.mapper`
2. `{Entity}Repository.java` - `com.seibel.cancer.database.db.repository`
3. `{Entity}DbService.java` - `com.seibel.cancer.database.db.service`
4. `{Entity}Service.java` - `com.seibel.cancer.service`
5. `Request{Entity}Create.java` - `com.seibel.cancer.web.request`
6. `Request{Entity}Update.java` - `com.seibel.cancer.web.request`
7. `Response{Entity}.java` - `com.seibel.cancer.web.response`
8. `{Entity}Controller.java` - `com.seibel.cancer.web.controller` (the package-private `{Entity}Converter` class lives inline in this same file, not a separate file)
9. `{Entity}MapperTest.java` - `database/src/test/java/com/seibel/cancer/database/db/mapper`
10. `{Entity}RepositoryTest.java` - `database/src/test/java/com/seibel/cancer/database/db/repository`
11. `{Entity}DbServiceTest.java` - `database/src/test/java/com/seibel/cancer/database/db/service`
12. `DomainBuilderSystemDatabase.java` - `database/src/test/java/com/seibel/cancer/testutils` (append builder methods)
13. Liquibase changeset YAML - `database/src/main/resources/db/changelog/changes/` (create table + add to master changelog)

## Generation Scope
- **Input**: Single Java Domain object (simple POJO extending `BaseDomain`)
- **Output**: Complete layered architecture per file list above, plus test files and
  builder methods added to the shared test utility class
- **Mode**: One entity at a time

## Notes
- Package base path: `com.seibel.cancer`
- Follow `.claude/database-restapi-template.md` exactly for field-level conventions
  (naming, validation annotations, exception handling, base-class fields) — this file
  is only the quick file-location checklist, not the pattern itself.
- I have IntelliJ Ultimate installed with Gradle.
