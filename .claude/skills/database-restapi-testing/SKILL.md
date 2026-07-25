---
name: database-restapi-testing
description: Generate the test suite (Mapper test, Repository test, DbService test) and test-builder methods (DomainBuilderDatabase) for an entity already scaffolded by database-restapi-template. Use whenever asked to add/generate tests for an entity's database layer, or when told "follow the restapi testing template" for an entity.
---

# Database REST API Testing

## Project Goal
Companion skill to `database-restapi-template`. That skill scaffolds the layered
architecture (domain, DTOs, entity, mapper, repository, db service, business service,
controller) for one entity at a time; this skill adds the test coverage and shared
test-builder methods once that scaffolding exists.

This spec is written from the actual current test suite (Customer/Purchase/User), not
an idealized design. Notably: there is **no `AbstractDbServiceTest` base class** and
**no `DatabaseFailureException`** in this codebase — don't reference either.

## Generation Scope
- **Input**: An already-scaffolded entity (Domain class, `{Entity}Db` entity, mapper,
  repository, DbService all exist)
- **Output**: 3 test files + builder methods appended to `DomainBuilderDatabase`
- **Mode**: One entity at a time

## Test Files (per entity)

### 1. **Mapper Test** (`database/src/test/java/com/seibel/cancer/database/db/mapper/{Entity}MapperTest.java`)
- Plain JUnit 5 class, **no Spring annotations** — `mapper = new {Entity}Mapper()` in a
  `@BeforeEach`. `PurchaseMapperTest`/`CustomerMapperTest`/`UserMapperTest` do not use
  `@SpringBootTest` at all; the mapper is a plain object with no injected dependencies
  beyond its own `ModelMapper`.
- Tests, using `DomainBuilderDatabase.get{Entity}()`/`get{Entity}Db()` to build inputs:
    - `toModel_shouldMapAllFields()` — every business field plus all BaseDb/BaseDomain
      fields (extid, createdAt, updatedAt, deletedAt, active) copied correctly
    - `toDb_shouldMapAllFields()` — same, the other direction
    - `toModelList_shouldMapAllItems()` / `toDbList_shouldMapAllItems()`
    - `toModelList_shouldHandleEmptyList()` / `toDbList_shouldHandleEmptyList()` — pass an
      empty list, assert result is non-null and empty
    - `toModelList_shouldHandleNullList()` / `toDbList_shouldHandleNullList()` — pass `null`,
      **assert the result is non-null and empty** (`assertNotNull` + `assertEquals(0, size())`).
      Do not assert `assertNull(...)` here — the mapper must return `List.of()` for null
      input, never `null` itself; treat a mapper that returns `null` as a bug to fix, not a
      behavior to match.

### 2. **Repository Test** (`database/src/test/java/com/seibel/cancer/database/db/repository/{Entity}RepositoryTest.java`)
- `@DataJpaTest`
- `@ActiveProfiles("test-database")` — **required**; `@DataJpaTest` alone does not activate
  this profile, and without it the datasource config in `application-test-database.yml`
  never applies.
- `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` — this
  project tests against the real configured MySQL datasource, never an embedded
  replacement.
- `@Autowired private {Entity}Repository repository;`
- `@BeforeEach void setUp() { repository.deleteAll(); }`
- Flat `@Test` methods (no `@Nested` classes in the current codebase), covering:
  `findByExtid` (found/not found), any unique-field finder (e.g. `findByCode`/`findByNctId`,
  found/not found), `findByActive` (active-only, inactive-only, using
  `Page<{Entity}Db> findByActive(active, pageable)`), `existsByExtid` (true/false),
  `save_shouldPersist{Entity}`, `findAll_shouldReturnAll{Entity}s`,
  `deleteById_shouldRemove{Entity}`
- Uses `DomainBuilderDatabase.get{Entity}Db(...)` to build test rows

### 3. **Database Service Test** (`database/src/test/java/com/seibel/cancer/database/db/service/{Entity}DbServiceTest.java`)
- `@ExtendWith(MockitoExtension.class)` — a plain Mockito unit test, **not**
  `@SpringBootTest`. Mocks the repository and mapper directly; no Spring context, no DB.
- `@Mock private {Entity}Repository repository;`, `@Mock private {Entity}Mapper mapper;`,
  `@InjectMocks private {Entity}DbService service;`
- Tests, using `ArgumentCaptor<{Entity}Db>` to inspect what got passed to `repository.save(...)`:
    - `create_shouldGenerateUuidAndSetFields()` — stub `mapper.toDb(...)`/`repository.save(...)`/
      `mapper.toModel(...)`, call `service.create(...)`, assert the captured saved entity has
      a generated extid, `ActiveEnum.ACTIVE`, non-null createdAt/updatedAt
    - `create_shouldThrowException_whenRepositoryFails()` — repository throws
      `RuntimeException`, assert `service.create(...)` throws `ServiceException`
      (**not** `DatabaseFailureException`)
    - `update_shouldUpdateFieldsAndTimestamp()`, `update_shouldThrowException_whenNotFound()`,
      `update_shouldThrowException_whenRepositoryFails()` — same shape as create
    - `delete_shouldSetDeletedAtAndInactive()`, `delete_shouldThrowException_whenNotFound()`
    - `findByExtid_shouldReturnEntity_whenExists()`, `findByExtid_shouldThrowException_whenNotFound()`
    - `findAll_shouldReturnAllEntities()` — stub `repository.findAllActive()` (not
      `repository.findAll()`)
    - `findByActive_shouldReturnFilteredEntities()`
- If the DbService's `create`/`update` only take the Domain object (no positional-field
  overload — see `database-restapi-template`'s guidance for entities with many fields),
  write these tests against that signature; don't invent a positional-field call that
  doesn't exist.

## Test Utility Classes

### DomainBuilderUtils (shared, not generated per entity)
Location: `database/src/test/java/com/seibel/cancer/testutils/DomainBuilderUtils.java`

Provides random-value generators for common field types, each following the same
three-arity pattern (`getXRandom()`, `getXRandom(label)`, `getXRandom(label, random)`),
building on a shared `buildWithLabel(label, maxSize, random)` that truncates/pads to fit
a max size:
- `getCodeRandom` (SIZE_CODE=8), `getNameRandom` (SIZE_NAME=32),
  `getDescriptionRandom` (SIZE_DESC=255), `getUniqueRandom` (SIZE_UNIQUE=64),
  `getLabelRandom` (SIZE_LABEL=32), `getVersionRandom` (SIZE_VERSION=16),
  `getStatusRandom` (SIZE_STATUS=32)
- `getEmailRandom(prefix)`, `getPhoneRandom()`
- `getDateRandom()` → `LocalDate`, `getBooleanRandom()` → `Boolean`,
  `getDecimalRandom()` → `BigDecimal` (2-decimal-scale, 0–10000 range)
- `randomString(length)` — the underlying alphanumeric generator (Apache Commons
  `RandomStringUtils`)

**If the entity needs a field type/shape not covered above, add a small `getXRandom()`
generator here first**, following the existing pattern, rather than inlining ad-hoc
random-value logic inside the entity's `DomainBuilderDatabase` builder method. This is
a shared class — extend it, don't work around it.

### DomainBuilderBase (shared, not generated per entity)
Location: `database/src/test/java/com/seibel/cancer/testutils/DomainBuilderBase.java`
— provides `setBaseSyncFields(BaseDb item)`, called by every entity's builder to set
`createdAt`/`updatedAt` to now, `active` to `ACTIVE`, `deletedAt` to `null`.

### DomainBuilderDatabase (shared, methods appended per entity)
Location: `database/src/test/java/com/seibel/cancer/testutils/DomainBuilderDatabase.java`
— a single shared class with one section per entity (see the `// Customer` / `// Purchase`
/ `// User` sections already in the file), **not** a separate `DomainBuilderSystemDatabase`
class (that name appears in older design notes but the actual class is
`DomainBuilderDatabase`).

Per-entity method set to append, following the existing pattern exactly (`Company`
naming below is illustrative — use the real entity name):

```java
public static Company getCompany() { ... }                    // domain, all-random
public static Company getCompany(CompanyDb item) { ... }       // domain, from an existing Db row
public static CompanyDb getCompanyDb() { ... }                 // db, all-random
public static CompanyDb getCompanyDb(String a, String b) { ... }        // db, partial (2 most identity-relevant fields)
public static CompanyDb getCompanyDb(String a, String b, String c, String extid) { ... }  // db, full override
```

- The "full override" overload always ends with `extid` last, defaulting to
  `UUID.randomUUID().toString()` when `null` is passed.
- Every other field falls back to a random generator matched to its type
  (`getCodeRandom("XX_")` for a short unique code, `getNameRandom("Prefix_")` for a
  longer name field, `getDateRandom()`/`getBooleanRandom()`/`getDecimalRandom()` for
  those types, etc.) when `null` is passed.
- Always call `setBaseSyncFields(item)` before returning a `*Db` object.
- For entities with many fields (more than the ~3-4 that fit a readable "partial"
  overload), pick the 2 most identity-relevant fields (e.g. a unique code/id plus a
  title/name) for the partial overload, and cover the rest with random defaults in the
  full-override overload — don't try to expose every field as a positional parameter.

## Prerequisites this skill depends on (verify before generating)

These test files need a working Spring test context. If any of these are missing or
misconfigured for the module under test, tests will fail for infrastructure reasons
unrelated to the entity itself — check before assuming a generated test's failure is a
real bug in the entity's code:
- A `@SpringBootConfiguration`-annotated class must exist at or above the test class's
  package within the same source tree — for `:database` module tests this is
  `database/src/test/java/com/seibel/cancer/database/DatabaseTestApplication.java`
  (a bare `@SpringBootApplication`-annotated class). `@SpringBootTest`/`@DataJpaTest`
  search upward for one and do not create one implicitly for a library module. If it's
  missing, every context-loading test in the module fails with "Unable to find a
  @SpringBootConfiguration."
- Any test needing real datasource config must carry
  `@ActiveProfiles("test-database")` explicitly — `@DataJpaTest`/`@SpringBootTest` alone
  do not activate a non-default profile.
- The module under test must depend on `spring-dotenv` in its **test** source set (e.g.
  `testImplementation 'me.paulschwarz:spring-dotenv:4.0.0'` in `database/build.gradle`)
  if `.env`-sourced `${RDS_*}` placeholders are used in its test `application-test-database.yml`
  — the root module having this dependency does not extend it to other modules.
- The module's `test` Gradle task working directory must resolve to wherever `.env`
  actually lives (typically the repo root) — `spring-dotenv` reads `.env` relative to
  the JVM's working directory, and a module subdirectory's default working directory
  won't contain it. Set `workingDir = rootProject.projectDir` on the `test` task if needed.

If any of the above is missing, fix it once at the module level rather than working
around it per-entity — it's shared test infrastructure, not something to duplicate in
each entity's test files.

## When invoked

When the user asks to generate tests following this template for an entity:
1. Confirm the entity is already scaffolded (Domain, `{Entity}Db`, mapper, repository,
   DbService all exist) before generating anything.
2. Verify the prerequisites above are in place for the module under test; if something's
   missing, fix it at the module level and tell the user, rather than silently working
   around it in the generated tests.
3. Generate the 3 test files plus `DomainBuilderDatabase` builder methods, using the
   exact package paths, annotations, and method signatures specified.
4. Compile the test source set and run the new tests before reporting done; report any
   failures that are pre-existing/unrelated to the new entity separately from ones the
   new code actually introduced.
5. Report back the full list of files created/modified.
