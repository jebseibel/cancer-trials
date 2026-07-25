---
name: database-restapi-template
description: Generate the complete layered REST API architecture (domain, request/response DTOs, entity, mapper, repository, db service, business service, controller+converter, Liquibase changeset) for a single Java domain object in this project's Gradle multi-module Spring Boot layout. Use whenever asked to scaffold, generate, or wire up a new entity end-to-end, or when told "follow the restapi template" for an entity. For generating tests and test-builder methods for an already-scaffolded entity, use database-restapi-testing instead.
---

# Database REST API Template

## Project Goal
I am writing lots and lots of the same kind of code in my Java, Spring, Gradle projects.
I repeat this pattern over and over. I have request objects/response objects, Domain objects in a common place, and Entity objects in the database area. I have IntelliJ Ultimate installed with gradle.

## Generation Scope
- **Input**: Single Java Domain object (simple POJO extending BaseDomain)
- **Output**: Complete layered architecture (9 files per entity, no tests)
- **Mode**: One entity at a time
- Tests and test-builder methods are a separate step — see the `database-restapi-testing` skill once this scaffolding is done.

This spec is written from the actual current codebase (Customer/Purchase/User
entities), not an idealized design — follow these patterns exactly, including the
parts that look like duplication (e.g. two create/update overloads on DbService).

## Generated Artifacts (per entity)

### 1. **Domain Layer** (`com.seibel.cancer.common.domain`)
- `{Entity}.java` - Business domain object
    - Extends `BaseDomain` (inherits: id, extid, createdAt, updatedAt, deletedAt, active)
    - Uses `@Data`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(callSuper = true)`
    - Contains only business-specific fields
    - **Input source for generation**

### 2. **Web Layer - Request DTOs** (`com.seibel.cancer.web.request`)
- `Request{Entity}Create.java` - Create operation DTO
    - Extends `BaseRequest`
    - Uses `@Data`, `@EqualsAndHashCode(callSuper = true)`
    - Required business fields get `@NotEmpty` + `@Size`; genuinely optional fields (per the
      Domain's own nullability, e.g. a nullable unique code) get only `@Size`
    - Validation messages follow pattern: "The {field} is required." / "The {field} must be
      at most {n} characters."

- `Request{Entity}Update.java` - Update operation DTO
    - Extends `BaseRequest`
    - Uses `@Data`, `@EqualsAndHashCode(callSuper = true)`
    - All fields optional (nullable), only `@Size` validations (no `@NotEmpty`)

### 3. **Web Layer - Response DTO** (`com.seibel.cancer.web.response`)
- `Response{Entity}.java` - Outgoing data
    - Uses `@Data`, `@Builder`
    - Contains `extid` plus all business fields
    - No validation annotations

### 4. **Entity Layer** (`com.seibel.cancer.database.db.entity`)
- `{Entity}Db.java` - JPA entity
    - Extends `BaseDb` (inherits: id, extid, createdAt, updatedAt, deletedAt, active with JPA annotations)
    - Uses `@Data`, `@EqualsAndHashCode(callSuper = true)`, `@Entity`, `@Table(name = "{entity_lowercase}")`
    - Has a `serialVersionUID` constant (any distinct long literal — doesn't need to follow a
      formula, just be present and unique per entity)
    - Business fields get `@Column` with `name` (snake_case), and as applicable:
      `length` for `varchar`-mapped Strings, `nullable`, `unique`, `columnDefinition = "text"`
      for long text fields, `precision`/`scale` for `BigDecimal`
    - Field type mapping: `bigint`→`Long`, `int`→`Integer`, `varchar/text/longtext`→`String`,
      `datetime`→`LocalDateTime`, `date`→`LocalDate`, `tinyint(1)`→`Boolean`,
      `decimal(p,s)`→`BigDecimal`

### 5. **Mapper** (`com.seibel.cancer.database.db.mapper`)
- `{Entity}Mapper.java` - Object conversions
    - Uses `@Component`, `@NoArgsConstructor`
    - Contains private `ModelMapper` instance
    - Methods: `toModel(Db)`, `toDb(Domain)`, `toModelList(List<Db>)`, `toDbList(List<Domain>)`
    - **List methods must return an empty list (`List.of()`), never `null`, when given a
      null input.** (This project's earlier mapper implementations returned `null` for a
      null list — that was a bug, not the convention; don't repeat it.)

### 6. **Repository** (`com.seibel.cancer.database.db.repository`)
- `{Entity}Repository.java` - Data access interface
    - Uses `@Repository`
    - Extends `JpaRepository<{Entity}Db, Long>` (not `ListCrudRepository` — this project uses
      `JpaRepository` throughout for the extra query/paging support)
    - Standard methods:
        - `Optional<{Entity}Db> findByExtid(String extid)`
        - `List<{Entity}Db> findByActive(ActiveEnum active)`
        - `Page<{Entity}Db> findByActive(ActiveEnum active, Pageable pageable)`
        - `boolean existsByExtid(String extid)`
        - `default List<{Entity}Db> findAllActive() { return findByActive(ActiveEnum.ACTIVE); }`
        - Additional `findBy{UniqueField}()` methods for unique business fields (e.g.
          `findByNctId`, `findByCode`)

### 7. **Database Service** (`com.seibel.cancer.database.db.service`)
- `{Entity}DbService.java` - Database operations layer
    - Uses `@Slf4j`, `@Service`
    - Extends `BaseDbService` (constructor calls `super("{Entity}Db")`)
    - Constructor-injected repository and mapper
    - Uses `ServiceException` (from `com.seibel.cancer.common.exceptions`), **not**
      `DatabaseFailureException` — `DatabaseFailureException` does not exist in this codebase.
    - **Create method(s):**
        - Always provide `create(@NonNull {Entity} item)`: generates `extid` (UUID) and `now`
          (LocalDateTime), maps `item` via `mapper.toDb(item)`, sets extid/createdAt/updatedAt/
          `ActiveEnum.ACTIVE` on the mapped record, saves, wraps in try/catch calling
          `handleException("create", extid, e)`, returns `mapper.toModel(saved)`
        - Only add a second overload taking each business field as an individual positional
          parameter (matching Customer/Purchase's pattern) **when the entity has a small
          number of fields (roughly ≤6)**. For entities with many fields, a long positional
          parameter list is error-prone (easy to transpose two adjacent `String` args) —
          skip the positional overload and use the Domain-object overload only. Confirm this
          choice with the user if the field count is borderline.
    - **Update method:** `update(@NonNull String extid, {Entity} item)` (or the positional-field
      variant, matching whichever create() style was chosen) — `repository.findByExtid(extid)
      .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)))`, then
      null-checked per-field updates (`if (item.getX() != null) record.setX(...)`), update
      `updatedAt`, save, wrap in try/catch calling `handleException("update", extid, e)`
    - **Delete method:** `delete(@NonNull String extid)` — find-or-throw as above, set
      `deletedAt` + `ActiveEnum.INACTIVE`, save, return `true`; catch wraps via
      `handleException("delete", extid, e)`, returns `false` (unreachable)
    - **Find methods:**
        - `findByExtid(@NonNull String extid)` — find-or-throw, returns Domain object directly
        - `findAll()` — `findAndLog(repository.findAllActive(), "findAll")`
        - `findAll(Pageable pageable)` — `repository.findByActive(ActiveEnum.ACTIVE, pageable).map(mapper::toModel)`
        - `findByActive(@NonNull ActiveEnum activeEnum)` — `findAndLog(repository.findByActive(activeEnum), ...)`
        - `findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable)` — paged variant
    - **Helper:** `private List<{Entity}> findAndLog(List<{Entity}Db> records, String type)`
    - Uses `BaseDbService` helpers: `getCreatedMessage`/`getUpdatedMessage`/`getDeletedMessage`/
      `getFoundMessage`/`getFoundFailureMessage`/`getFoundMessageByType`/`handleException`

### 8. **Business Service** (`com.seibel.cancer.service`)
- `{Entity}Service.java` - Business logic layer
    - Uses `@Slf4j`, `@Service`, class-level `@Transactional(readOnly = true)`
    - Extends `BaseService` (constructor calls `super({Entity}.class.getSimpleName())`)
    - Constructor-injected DbService
    - Uses `ServiceException` and `ResourceNotFoundException` (both from
      `com.seibel.cancer.common.exceptions`) — **not** `DatabaseFailureException`
    - **Methods** (mutating methods get their own `@Transactional`):
        - `create(Domain item)` — `requireNonNull(item, "{Entity}")`, log, delegate to
          `dbService.create(...)` inside try/catch rethrowing as `ServiceException`
        - `update(String extid, Domain item)` — `requireNonBlank(extid, "extid")` +
          `requireNonNull(item, "{Entity}")`, delegate, throw `ResourceNotFoundException` if
          the DbService call returns null, rethrow other failures as `ServiceException`
        - `delete(String extid)` — validate extid, delegate, rethrow as `ServiceException`
        - `findByExtid(String extid)` — validate, delegate, `ResourceNotFoundException` if null,
          rethrow as `ServiceException`
        - `findAll()` — delegate, rethrow as `ServiceException`
        - `findAll(Pageable pageable, ActiveEnum activeEnum)` — enforce a page-size cap
          (`MAX_PAGE_SIZE`, e.g. 100) and a sort-field whitelist (`ALLOWED_SORT_FIELDS`, e.g.
          the entity's title/name field plus `createdAt`/`updatedAt`) via a private
          `enforceCapsAndWhitelist(Pageable)` helper; delegate to `dbService.findAll(safe)` or
          `dbService.findByActive(activeEnum, safe)` depending on whether `activeEnum` is null
        - `findByActive(ActiveEnum activeEnum)` — validate, delegate, rethrow as `ServiceException`
    - No manual try/catch is needed only where the method has nothing to wrap — in practice
      every method here does wrap DbService calls in try/catch and rethrows as
      `ServiceException`, matching `PurchaseService`/`CustomerService`.

### 9. **Controller & Converter** (`com.seibel.cancer.web.controller`)
- `{Entity}Controller.java` - REST API endpoints and DTO conversion (both classes in same file)

#### Public Controller Class
- `{Entity}Controller` - REST API endpoints
    - Uses `@RestController`, `@RequestMapping("/api/{entity_lowercase}")`, `@Validated`,
      `@Tag(name = "{Entity}", description = "{Entity} CRUD endpoints")`, `@RequiredArgsConstructor`
    - Constructor-injected business service (`@RequiredArgsConstructor` on the `final` field);
      converter is instantiated directly as a field (`private final {Entity}Converter converter
      = new {Entity}Converter();`), not injected
    - **Endpoints:**
        - `GET /` — `getAll(@ParameterObject @PageableDefault(size = 20, sort = "{defaultSortField}")
          Pageable pageable, @RequestParam(required = false) ActiveEnum active)` returning
          `Page<Response{Entity}>` via `service.findAll(pageable, active).map(converter::toResponse)`
        - `GET /{extid}` — `getByExtid(@PathVariable String extid)` → `converter.toResponse(service.findByExtid(extid))`
        - `POST /` — `create(@Valid @RequestBody Request{Entity}Create request)` → build
          `Response{Entity}` from the created domain, return `ResponseEntity.created(location).body(...)`
          where `location = URI.create("/api/{entity_lowercase}/" + created.getExtid())`
        - `PUT /{extid}` — `update(...)`: call `converter.validateUpdateRequest(request)` first,
          then `service.update(extid, converter.toDomain(request))`, return converted response
        - `PATCH /{extid}` — thin wrapper that just calls the `update` method with the same args
        - `DELETE /{extid}` — `delete(@PathVariable String extid)`: call `service.delete(extid)`,
          return `ResponseEntity.noContent()` if true else `ResponseEntity.notFound()`
    - Uses `@Operation(summary = "...")` on each endpoint (springdoc/Swagger annotations already
      used throughout this codebase)

#### Package-Private Converter Class
- `{Entity}Converter` (no `public` modifier, declared below the controller class in the same file)
    - No annotations, no constructor dependencies (stateless)
    - **Conversion methods** (package-private, no explicit access modifier):
        - `toDomain(Request{Entity}Create request)` — builder-based, one field at a time
        - `toDomain(Request{Entity}Update request)` — same, for update requests
        - `toResponse({Entity} item)` — builder-based, includes `extid`
        - `toResponse(List<{Entity}> items)` — `items.stream().map(this::toResponse).toList()`
        - `validateUpdateRequest(Request{Entity}Update request)` — throws `ValidationException`
          (from `com.seibel.cancer.common.exceptions`) if every field on the request is null

## Liquibase Changeset

Add a new numbered changeset under `database/src/main/resources/db/changelog/changes/`
(next available number) creating the entity's table, including the standard base
columns (`id`, `extid`, `created_at`, `updated_at`, `deleted_at`, `active`) plus one
column per business field. The master changelog uses `includeAll`, so no separate
registration step is needed beyond adding the file. Follow the exact column shape used
in existing changesets (e.g. `003-purchase.yaml`): `extid` gets
`defaultValueComputed: "(UUID())"`, `created_at` gets
`defaultValueComputed: CURRENT_TIMESTAMP`, `active` gets `defaultValueNumeric: 1`.

If the Domain has fields explicitly noted as foreign keys (e.g. from a table-spec doc),
skip those columns entirely here — FK wiring is a separate, manual step, not part of
this skill's generation.

## Key Technical Details

### Base Classes
- **BaseDomain** - `@Data`, `@SuperBuilder`, `@NoArgsConstructor`
  Fields: id (Long), extid (String), createdAt, updatedAt, deletedAt (LocalDateTime), active (ActiveEnum)
- **BaseUniqueDomain** (extends BaseDomain) - adds `uniqueId`, `version`, `status` — only use
  this instead of `BaseDomain` when explicitly told to for a given entity
- **BaseDb** - `@Data`, `@MappedSuperclass`, implements `Serializable` — same fields as
  BaseDomain but with full JPA annotations; `active` uses `@Convert(converter = ActiveEnumConverter.class)`
- **BaseUniqueDb** (extends BaseDb) - JPA counterpart to BaseUniqueDomain
- **BaseRequest** - `@Data`, abstract class (currently empty)
- **BaseService** - abstract; `requireNonNull()`/`requireNonBlank()` validation helpers
  (with/without field name variants), uses `thisName` for error messages
- **BaseDbService** - abstract; constructor takes entity name; provides
  `getCreatedMessage`/`getUpdatedMessage`/`getDeletedMessage`/`getFoundMessage`/
  `getFoundFailureMessage`/`getFoundMessageByType`/`handleException` (which logs and throws
  `ServiceException`)

### Enums & Converters
- **ActiveEnum** - Integer-backed (ACTIVE=1, INACTIVE=0); `isActive()`/`isInactive()` helpers;
  `ALLOWED_VALUES` string for validation
- **ActiveEnumConverter** - `@Converter(autoApply = true)`, converts ActiveEnum ↔ Integer

### Exceptions (`com.seibel.cancer.common.exceptions`)
- `ServiceException` - general service/DB-layer failure, used by DbService and Service
- `ResourceNotFoundException` - thrown by Service when a lookup/update target doesn't exist
- `ValidationException` - thrown by the Converter when an update request has no fields set
- There is no `DatabaseFailureException` or `DatabaseAccessException` in this codebase —
  don't reference them.

### Dependencies
- **ModelMapper** - entity/domain conversions in Mapper classes
- **Lombok** - `@Data`, `@Builder`, `@SuperBuilder`, `@Slf4j`, `@RequiredArgsConstructor`, etc.
- **Spring Data JPA** - `JpaRepository` for repositories, `Page`/`Pageable` for paging
- **Jakarta Validation** - `@Valid`, `@NotEmpty`, `@Size` annotations
- **springdoc/Swagger** - `@Tag`, `@Operation`, `@ParameterObject` on controllers
- **Spring Boot** - core framework

### Naming Conventions
- **Packages:** standard Java package naming with deep nesting
- **Classes:** `{Entity}[Suffix]` pattern (e.g., `PurchaseDb`, `PurchaseService`)
- **Database columns:** snake_case (e.g., `created_at`, `deleted_at`)
- **Java fields:** camelCase (e.g., `createdAt`, `deletedAt`)
- **REST endpoints:** lowercase, singular entity name (e.g., `/api/purchase`)

### Key Features
- ✅ Fully implemented CRUD operations (no TODOs)
- ✅ Soft deletes (sets `deletedAt` and `INACTIVE` status)
- ✅ UUID-based external IDs (`extid`) for all entities
- ✅ Audit timestamps (createdAt, updatedAt, deletedAt) on all entities
- ✅ Validation annotations on Request DTOs (different for Create vs Update)
- ✅ ModelMapper for entity/domain conversions in Mapper classes
- ✅ Manual builder-based mapping in controllers via package-private Converter
- ✅ Comprehensive logging at all layers
- ✅ Standardized exception handling via `ServiceException`/`ResourceNotFoundException`
- ✅ Non-Optional return types — `findByExtid` returns Domain object directly, throws on not-found
- ✅ `@NonNull` annotations on required parameters throughout DbService
- ✅ ActiveEnum for soft-delete status tracking
- ✅ Pagination support (`Page<T>`/`Pageable`) with page-size cap + sort-field whitelist
- ✅ Direct Controller→Service communication (no intermediate web service layer)
- ✅ Separation of concerns: Controller (REST) + Converter (DTO mapping) in same file
- ⚠️ No JPA relationships (`@ManyToOne` etc.) generated — FK columns are skipped, wired manually later
- ⚠️ Database constraints defined in JPA `@Column` annotations, mirrored in the Liquibase changeset

## Generation Strategy

### Input Processing
1. Parse the input Domain class file
2. Extract class name (becomes `{Entity}`)
3. Extract business fields (exclude BaseDomain/BaseUniqueDomain fields)
4. Extract field metadata: name, type, nullability/uniqueness, length (from doc/annotations
   if available, otherwise reasonable defaults per type)

### Output Generation
For each Domain class input, generate 9 files plus a Liquibase changeset:
1. Domain class (already exists — this is input)
2. Request DTOs (Create and Update variants)
3. Response DTO
4. Entity (Db) class
5. Mapper class
6. Repository interface
7. Database Service class
8. Business Service class
9. Controller class (with package-private Converter in same file)
10. Liquibase changeset

## When invoked

When the user asks to scaffold/generate an entity following this template:
1. Confirm the Domain class input (path or content) before generating anything.
2. If the entity has more than ~6 business fields, confirm with the user whether
   DbService should skip the positional-field create/update overload (see section 7).
3. Generate all 9 artifacts plus the Liquibase changeset, in the layer order given,
   using the exact package paths, annotations, and method signatures specified.
4. Compile (`./gradlew compileJava` and the affected module) before reporting done.
5. Report back the full list of files created/modified.
6. Tests are not part of this skill — point the user to `database-restapi-testing`
   as the next step.
