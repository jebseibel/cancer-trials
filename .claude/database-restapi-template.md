
## Project Goal
I am writing lots and lots of the same kind of code in my Java, Spring, Gradle projects. 

I repeat this pattern over and over. I have request objects/response objects, D
omain objects in a common place, and Entity objects in the database area. I have IntelliJ Ultimate installed with gradle.

## Generation Scope
- **Input**: Single Java Domain object (simple POJO extending BaseDomain)
- **Output**: Complete layered architecture (9 files + 3 test files per entity + builder methods added to shared class)
- **Mode**: One entity at a time

## Generated Artifacts (per entity)

### 1. **Domain Layer** (`com.seibel.scheduler.common.domain`)
- `{Entity}.java` - Business domain object
    - Extends `BaseDomain` (inherits: id, extid, createdAt, updatedAt, deletedAt, active)
    - Uses `@Data`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(callSuper = true)`
        - Contains only business-specific fields
        - **Input source for generation**

### 2. **Web Layer - Request DTOs** (`com.seibel.scheduler.web.request`)
- `Request{Entity}Create.java` - Create operation DTO
    - Extends `BaseRequest`
        - Uses `@Data`, `@EqualsAndHashCode(callSuper = true)`
        - All fields required with `@NotEmpty` and `@Size` validations
    - Validation messages follow pattern: "The {field} is required."

- `Request{Entity}Update.java` - Update operation DTO
    - Extends `BaseRequest`
        - Uses `@Data`, `@EqualsAndHashCode(callSuper = true)`
        - All fields optional (nullable)
    - Only `@Size` validations (no `@NotEmpty`)

### 3. **Web Layer - Response DTO** (`com.seibel.scheduler.web.response`)
- `Response{Entity}.java` - Outgoing data
    - Uses `@Data`, `@Builder`
        - Contains `extid` plus all business fields
    - No validation annotations

### 4. **Entity Layer** (`com.seibel.scheduler.database.database.db.entity`)
- `{Entity}Db.java` - JPA entity
    - Extends `BaseDb` (inherits: id, extid, createdAt, updatedAt, deletedAt, active with JPA annotations)
    - Uses `@Data`, `@EqualsAndHashCode(callSuper = true)`, `@Entity`, `@Table(name = "{entity_lowercase}")`
        - Has `serialVersionUID` constant
    - All business fields have `@Column` annotations with:  
      - `name` in snake_case  
      - `length` constraint  
      - `nullable` flag  
      - `unique` flag (where applicable)

### 5. **Mapper** (`com.seibel.scheduler.database.database.db.mapper`)
- `{Entity}Mapper.java` - Object conversions
    - Uses `@Component`, `@NoArgsConstructor`
        - Contains private `ModelMapper` instance
    - Methods: `toModel()`, `toDb()`, `toModelList()`, `toDbList()`
        - Handles null checks in list methods

### 6. **Repository** (`com.seibel.scheduler.database.database.db.repository`)
- `{Entity}Repository.java` - Data access interface
    - Uses `@Repository`
        - Extends `ListCrudRepository<{Entity}Db, Long>`
        - Standard methods:
            - `Optional<{Entity}Db> findByExtid(String extid)`
            - `List<{Entity}Db> findByActive(ActiveEnum active)`
            - `boolean existsByExtid(String extid)`
            - Additional `findBy{UniqueField}()` methods for unique business fields

### 7. **Database Service** (`com.seibel.scheduler.database.database.db.service`)
- `{Entity}DbService.java` - Database operations layer
    - Uses `@Slf4j`, `@Service`
        - Extends `BaseDbService` (passes entity name "{Entity}Db" to constructor)
    - Constructor-injected repository and mapper (no thisName field - use BaseDbService's entity name)
        - **Create method:**
            - Takes business fields as parameters (first required field marked `@NonNull`)
            - Signature: `throws DatabaseFailureException`
            - Creates local `extid` variable with UUID
            - Creates local `now` variable with LocalDateTime.now()
            - Builds entity, sets all fields including extid, createdAt, updatedAt, ActiveEnum.ACTIVE
            - Wraps repository.save() in try-catch
            - Calls `handleException("create", extid, e)` in catch block
            - Returns Domain object (return null after handleException - unreachable)
        - **Update method:**
            - Signature: `@NonNull String extid` + business fields, `throws DatabaseFailureException`
            - Uses `repository.findByExtid(extid).orElseThrow()` with DatabaseFailureException
            - Updates only non-null provided fields (if statements)
            - Updates `updatedAt` timestamp
            - Wraps repository.save() in try-catch
            - Calls `handleException("update", extid, e)` in catch block
            - Returns Domain object (return null after handleException - unreachable)
        - **Delete method:**
            - Signature: `@NonNull String extid`, `throws DatabaseFailureException`
            - Uses `repository.findByExtid(extid).orElseThrow()` with DatabaseFailureException
            - Soft delete: sets `deletedAt` and `ActiveEnum.INACTIVE`
            - Wraps repository.save() in try-catch
            - Calls `handleException("delete", extid, e)` in catch block
            - Returns boolean true (return false after handleException - unreachable)
        - **Find methods:**
            - `findByExtid(@NonNull String extid)` - returns Domain object directly (not Optional)
              - Uses `repository.findByExtid(extid).orElseThrow()` with DatabaseFailureException
              - No try-catch needed (throws DatabaseFailureException from orElseThrow)
            - `findAll()` - delegates to `findAndLog(repository.findAll(), "findAll")`
            - `findByActive(@NonNull ActiveEnum activeEnum)` - delegates to `findAndLog(repository.findByActive(activeEnum), String.format("active (%s)", activeEnum))`
        - **Helper method:**
            - `private List<{Entity}> findAndLog(List<{Entity}Db> records, String type)` - logs using `getFoundMessageByType()` and returns mapped list
    - Uses `BaseDbService` helper methods:
        - `getCreatedMessage(extid)`, `getUpdatedMessage(extid)`, `getDeletedMessage(extid)`
        - `getFoundMessage(extid)`, `getFoundFailureMessage(extid)`, `getFoundMessageByType(type, count)`
        - `handleException(operation, extid, e)` for standardized exception handling

### 8. **Business Service** (`com.seibel.scheduler.service`)
- `{Entity}Service.java` - Business logic layer
    - Uses `@Slf4j`, `@Service`
    - Extends `BaseService` (sets `thisName` to "{Entity}" in constructor)
    - Constructor-injected DbService
    - **Import required:** `import com.viro.database.exceptions.DatabaseFailureException;`
    - **Pattern for all methods:**
        - Validate inputs using `requireNonNull()` / `requireNonBlank()`
        - Log operation with `log.info()`
        - Delegate to DbService
        - Accept Domain objects, extract fields for DbService calls
    - **Methods (all declare `throws DatabaseFailureException`):**
        - `create({Entity} item) throws DatabaseFailureException` - validates item and required fields, extracts fields for DbService call, returns Domain object
        - `update(String extid, {Entity} item) throws DatabaseFailureException` - validates extid and item, extracts fields for DbService call, returns Domain object
        - `delete(String extid) throws DatabaseFailureException` - validates extid, returns boolean
        - `findByExtid(String extid) throws DatabaseFailureException` - validates extid, returns Domain object directly (not Optional)
        - `findAll() throws DatabaseFailureException` - no validation, returns List<Domain>
        - `findByActive(ActiveEnum activeEnum) throws DatabaseFailureException` - validates enum, returns List<Domain>
    - No try-catch blocks needed - exceptions propagate to controller

### 9. **Controller & Converter** (`com.seibel.scheduler.web.controller`)
- `{Entity}Controller.java` - REST API endpoints and DTO conversion (both classes in same file)

#### Public Controller Class
- `{Entity}Controller` - REST API endpoints
    - Uses `@RestController`, `@RequestMapping("/api/{entity_lowercase}")`, `@Validated`
    - Constructor-injected business service and converter
    - **Import required:** `import com.viro.database.exceptions.DatabaseFailureException;`
    - **Endpoints (all declare `throws DatabaseFailureException`):**
        - `GET /` - getAll() throws DatabaseFailureException → List<Response{Entity}>
          - Direct service call, no additional error handling
        - `GET /{extid}` - getByExtid(@PathVariable String extid) throws DatabaseFailureException → Response{Entity}
          - Calls service.findByExtid()
          - Check if result is null, throw `ResponseStatusException(NOT_FOUND)`
          - Otherwise return converted response
        - `POST /` - create(@Valid @RequestBody Request{Entity}Create) throws DatabaseFailureException → Response{Entity}
          - Convert request to domain
          - Call service.create()
          - Return converted response
        - `PUT /{extid}` - update(@PathVariable, @Valid @RequestBody Request{Entity}Update) throws DatabaseFailureException → Response{Entity}
          - Call `converter.validateUpdateRequest()` first
          - Convert request to domain
          - Call service.update()
          - Return converted response
        - `DELETE /{extid}` - delete(@PathVariable) throws DatabaseFailureException → ResponseEntity<Void>
          - Call service.delete()
          - If returns false, throw `ResponseStatusException(NOT_FOUND)`
          - Otherwise return `ResponseEntity.noContent()`
    - **Delegates all DTO conversions to Converter:**
        - Uses converter for Request→Domain conversions
        - Uses converter for Domain→Response conversions
    - **Error handling:**
        - Methods declare `throws DatabaseFailureException` and let Spring handle it
        - Service returns Domain objects directly (not Optional)
        - Only explicit error handling is null checks and false return values
        - Spring's exception handling converts DatabaseFailureException to HTTP 500

#### Package-Private Converter Class
- `{Entity}Converter` - DTO conversion logic
    - Package-private class (no public modifier)
    - Uses `@Component`
        - Constructor: no dependencies (stateless)
        - **Conversion methods:**
            - `toResponse({Entity} domain)` - single Domain → Response DTO
            - `toResponse(List<{Entity}> domains)` - list Domain → Response DTOs
            - `toDomain(Request{Entity}Create request)` - Create Request → Domain
            - `toDomain(String extid, Request{Entity}Update request)` - Update Request → Domain (includes extid)
        - **Manual builder-based mapping:**
            - Uses builder pattern to construct Domain objects from Request DTOs
            - Uses builder pattern to construct Response DTOs from Domain objects
            - All field mappings done explicitly (no ModelMapper)
        - **Validation:**
            - `validateUpdateRequest(Request{Entity}Update request)` - ensures at least one field provided in update requests

## Test Files (per entity)

### 10. **Mapper Tests** (`test/.../database.database.db.mapper`)
- `{Entity}MapperTest.java` - Spring Boot integration tests for mapper conversions
    - Uses `@SpringBootTest(classes = DatabaseTestApplication.class)`
    - Uses `@ActiveProfiles("test-database")`
    - Uses `@Autowired` to inject mapper
    - Tests all four methods: `toModel()`, `toDb()`, `toModelList()`, `toDbList()`
    - Verifies field mapping accuracy:
        - All fields copied correctly
        - BaseDomain/BaseDb fields mapped
        - Business fields mapped
        - List conversions work correctly
    - Uses `assertEquals()` assertions
    - **Uses DomainBuilderSystemDatabase helper to create test objects**

### 11. **Repository Tests** (`test/.../database.database.db.repository`)
- `{Entity}RepositoryTest.java` - Integration tests for repository
    - Uses `@SpringBootTest(classes = DatabaseTestApplication.class)`
    - Uses `@ActiveProfiles("test-database")`
    - Uses `@Autowired` for repository injection
    - Uses `@BeforeEach` to clear database before each test
    - Tests organized in `@Nested` classes:
        - **SuiteCrud:** Basic CRUD operations (create, update, soft delete)
        - **SuiteRepositoryQueries:** Custom query methods
    - Tests all custom query methods:
      - `findByExtid()` - returns record by external ID
      - `findByActive()` - returns only ACTIVE or INACTIVE records
      - `existsByExtid()` - checks existence
      - Additional `findBy{UniqueField}()` methods for unique business fields
      - `deleteAllInBatch()` - batch deletion
    - Uses `assertAll()`, `assertEquals()`, `assertNotNull()`, `assertTrue()`, `assertFalse()` assertions
    - **Uses DomainBuilderSystemDatabase helper to create test objects**

### 12. **Database Service Tests** (`test/.../database.database.db.service`)
- `{Entity}DbServiceTest.java` - Integration tests for database service
    - Uses `@SpringBootTest(classes = DatabaseTestApplication.class)`
    - Uses `@ActiveProfiles("test-database")`
    - Extends `AbstractDbServiceTest<{Entity}, {Entity}Db, {Entity}DbService, {Entity}Repository>`
    - Uses `@Autowired` to inject service, repository, and mapper
    - Implements abstract methods from base test:
        - `buildDbEntity()` - creates test entity
        - `createThroughService()` - calls service create with individual fields
        - `updateThroughService()` - calls service update with individual fields
        - `deleteThroughService()` - calls service delete
        - `findByExtidThroughService()` - calls service findByExtid
        - `findAllThroughService()` - calls service findAll
        - `findByActiveThroughService()` - calls service findByActive
        - `assertBusinessFieldsEqual()` - two versions for Db→Domain and Domain→Db
        - `reloadFromRepository()` - fetches entity from repository
        - `setBaseFields()` - wires up base test dependencies
    - Inherits comprehensive test suite from AbstractDbServiceTest:
        - **CreateTests:** Entity creation, field validation
        - **UpdateTests:** Updates with bad extid, deleted entities
        - **DeleteTests:** Soft delete behavior, error cases
        - **FindTests:** Find by extid, findAll, findByActive
    - Uses `assertEquals()`, `assertNotNull()`, `assertTrue()`, `assertThrows()` assertions
    - **Uses DomainBuilderSystemDatabase helper to create test objects**

## Test Utility Classes

### DomainBuilderUtils (Base Utility - Shared)
- **Location:** `test/.../testutils/DomainBuilderUtils.java`
- **Purpose:** Provides utility methods for generating random test data strings with specific patterns and size constraints
- **Not generated per entity** - this is a shared utility class

**Constants for field sizes:**
```java  
// Standard field sizes  
public static final int SIZE_CODE = 8;  
public static final int SIZE_NAME = 32;  
public static final int SIZE_DESC = 255;  
public static final int SIZE_UNIQUE = 64;  
public static final int SIZE_LABEL = 32;  
public static final int SIZE_VERSION = 16;  
public static final int SIZE_STATUS = 32;  
public static final int SIZE_RANDOM = 10;  
  
// Base prefixes  
public static final String BASE_CODE = "Cod_";  
public static final String BASE_NAME = "Nam_";  
public static final String BASE_DESC = "Des_";  
public static final String BASE_UNIQUE = "Unq_";  
public static final String BASE_LABEL = "Lbl_";  
public static final String BASE_VERSION = "Ver_";  
public static final String BASE_STATUS = "Sta_";  
  
// Minimum suffix lengths (for randomization)  
public static final int SUFFIX_MIN_CODE = 4;  
public static final int SUFFIX_MIN_NAME = 4;  
public static final int SUFFIX_MIN_DESC = 4;  
// ... etc for other types  
```  

**Key Methods (pattern repeated for each field type):**
- `getCodeRandom()` - generates random code with default prefix
- `getCodeRandom(String label)` - generates random code with custom prefix
- `getCodeRandom(String label, String random)` - generates code with custom prefix and suffix
- `getNameRandom()`, `getDescriptionRandom()`, etc. - same pattern for other field types

**Shared utility methods:**
- `buildWithLabel(String label, int maxSize, String random)` - Core builder ensuring max length respected
- `randomString()` / `randomString(int length)` - Generates random alphanumeric strings (uppercase)

**Features:**
- Automatically truncates labels if they exceed max size minus minimum suffix
- Ensures generated strings always fit within database field length constraints
- Adds random suffix (4-8 characters) for uniqueness
- Uses Apache Commons `RandomStringUtils` for generation

### DomainBuilderBase (Abstract Base - Shared)
- **Location:** `test/.../testutils/DomainBuilderBase.java`
- **Purpose:** Abstract base class that extends DomainBuilderUtils and adds BaseDb initialization
- **Not generated per entity** - this is a shared base class

```java  
public abstract class DomainBuilderBase extends DomainBuilderUtils {  
    protected static void setBaseSyncFields(BaseDb item) {        item.setCreatedAt(LocalDateTime.now());        item.setUpdatedAt(LocalDateTime.now());        item.setActive(ActiveEnum.ACTIVE);        item.setDeletedAt(null);    }}  
```  

**Features:**
- Provides consistent initialization for all BaseDb entities
- Sets audit timestamps to current time
- Sets active status to ACTIVE
- Ensures deletedAt is null (for non-deleted test entities)

### 13. **DomainBuilderSystemDatabase** (`test/.../testutils`)
- `DomainBuilderSystemDatabase.java` - Entity-specific test object builder methods
- **Generated with methods for each entity**
- Extends `DomainBuilderBase`

**Per-entity methods pattern (using Company as example):**

```java  
// Get Domain object with default values  
public static Company getCompany()  
  
// Get Domain object from existing Db entity  
public static Company getCompany(CompanyDb item)  
  
// Get Db entity with all defaults  
public static CompanyDb getCompanyDb()  
  
// Get Db entity with partial customization  
public static CompanyDb getCompanyDb(String code, String name)  
  
// Get Db entity with full customization  
public static CompanyDb getCompanyDb(String code, String name, String description, String extid)  
```  

**Implementation pattern for the full customization method:**
```java  
public static CompanyDb getCompanyDb(String code, String name, String description, String extid) {  
    CompanyDb item = new CompanyDb();    item.setExtid(extid != null ? extid : UUID.randomUUID().toString());    item.setCode(code != null ? code : getCodeRandom("CO_"));    item.setName(name != null ? name : getNameRandom("Company_"));    item.setDescription(description != null ? description : getDescriptionRandom("Company Description "));    setBaseSyncFields(item);    return item;}  
```  

**Features:**
- Multiple overloaded methods per entity for flexibility
- Uses custom prefixes for each entity (e.g., "CO_" for Company code)
- Generates UUID for extid when not provided
- Uses appropriate random generators based on field type
- Calls `setBaseSyncFields()` to initialize BaseDb fields
- Uses mapper to convert Db entities to Domain objects
- All parameters optional via overloading (null = use random default)

**Generation requirements:**
- Generate method set for each entity in the project
- Use entity-specific prefixes (e.g., "CO_" for Company, "PROD_" for Product)
- Match field types to appropriate random generators (code→getCodeRandom, name→getNameRandom, etc.)
- Include overloads for: no params, partial params, all params
- Always include the Domain object conversion methods

## Key Technical Details

### Base Classes
- **BaseDomain** - `@Data`, `@SuperBuilder`, `@NoArgsConstructor`
    - Fields: id (Long), extid (String), createdAt, updatedAt, deletedAt (LocalDateTime), active (ActiveEnum)

- **BaseDb** - `@Data`, `@MappedSuperclass`, implements `Serializable`
    - Same fields as BaseDomain but with full JPA annotations
    - Uses `@Convert(converter = ActiveEnumConverter.class)` for active field

- **BaseRequest** - `@Data`, abstract class (currently empty)

- **BaseService** - Abstract class with validation helpers
    - Methods: `requireNonNull()`, `requireNonBlank()` (with/without field name)
    - Uses `thisName` for error messages

- **BaseDbService** - Abstract class with logging/exception helpers
    - Constructor takes entity name
    - Provides message formatting methods
    - Provides `handleException()` for standardized error handling

### Enums & Converters
- **ActiveEnum** - Integer-backed (ACTIVE=1, INACTIVE=0)
    - Has helper methods: `isActive()`, `isInactive()`
        - Provides `ALLOWED_VALUES` string for validation

- **ActiveEnumConverter** - `@Converter(autoApply = true)`
    - Converts ActiveEnum ↔ Integer for JPA

### Exceptions
All extend `Exception` with simple constructors:
- `DatabaseFailureException` - General database error
- `DatabaseAccessException` - Access errors

### Dependencies
- **ModelMapper** - Used in mapper classes for entity/domain conversions
- **Lombok** - Used throughout for `@Data`, `@Builder`, `@Slf4j`, etc.
- **Spring Data JPA** - `ListCrudRepository` for repositories
- **Jakarta Validation** - `@Valid`, `@NotEmpty`, `@Size` annotations
- **Spring Boot** - Core framework
- **JUnit 5** - Testing framework for unit tests
- **Mockito** - Mocking framework for unit tests
- **Spring Boot Test** - `@DataJpaTest` for repository integration tests
- **Apache Commons Lang3** - `RandomStringUtils` for test data generation

### Naming Conventions
- **Packages:** Standard Java package naming with deep nesting
- **Classes:** {Entity}[Suffix] pattern (e.g., CompanyDb, CompanyService)
- **Database columns:** snake_case (e.g., created_at, deleted_at)
- **Java fields:** camelCase (e.g., createdAt, deletedAt)
- **REST endpoints:** lowercase with hyphens (e.g., /api/company)

### Key Features
- ✅ Fully implemented CRUD operations (no TODOs)
- ✅ Soft deletes (sets deletedAt and INACTIVE status)
- ✅ UUID-based external IDs (extid) for all entities
- ✅ Audit timestamps (createdAt, updatedAt, deletedAt) on all entities
- ✅ Validation annotations on Request DTOs (different for Create vs Update)
- ✅ ModelMapper for entity/domain conversions in Mapper classes
- ✅ Manual builder-based mapping in controllers via package-private Converter
- ✅ Comprehensive logging at all layers
- ✅ Standardized exception handling:
  - DbService: Uses `handleException()` from BaseDbService, `orElseThrow()` with DatabaseFailureException
  - Service: Declares `throws DatabaseFailureException`, propagates exceptions naturally (no try-catch)
  - Controller: Declares `throws DatabaseFailureException`, lets Spring's global exception handler convert to HTTP 500
  - Only explicit error handling in controller for null checks and boolean false returns
- ✅ Non-Optional return types - findByExtid returns Domain object directly, throws exception if not found
- ✅ `@NonNull` annotations on required parameters throughout service layers
- ✅ ActiveEnum for soft delete status tracking
- ✅ Lombok annotations throughout for boilerplate reduction
- ✅ Standard REST practices (proper HTTP methods and status codes)
- ✅ Validation helper methods in base classes
- ✅ **Unit and integration tests for Mapper, Repository, and DbService**
- ✅ **Test utility classes for consistent test data generation**
- ✅ **Direct Controller-to-Service communication (no intermediate web service layer)**
- ✅ **Separation of concerns: Controller (REST) + Converter (DTO mapping) in same file**
- ⚠️  Simple entities only (no relationships initially)
- ⚠️  Database constraints defined in JPA annotations

## Implementation Approach
- **Technology**: Gradle custom task or buildSrc plugin
- **Generation**: Command/task per entity
- **Input**: Domain object class file
- **Framework**: Spring Boot + Gradle
- **Template Engine**: Consider using FreeMarker or Velocity for code generation
- **Parsing**: Use Java reflection or AST parsing to extract field information from Domain class

## Generation Strategy

### Input Processing
1. Parse the input Domain class file
2. Extract class name (becomes {Entity})
3. Extract business fields (exclude BaseDomain fields)
4. Extract field metadata:
    - Field name
    - Field type
    - Validation requirements (from annotations or inferred from type)
    - JPA constraints (length, nullable, unique)

### Output Generation
For each Domain class input, generate all 12 files plus builder methods:
1. Domain class (already exists - this is input)
2. Request DTOs (Create and Update variants)
3. Response DTO
4. Entity (Db) class
5. Mapper class
6. Repository interface
7. Database Service class
8. Business Service class
9. Controller class (with package-private Converter in same file)
10. Mapper Test class
11. Repository Test class
12. Database Service Test class
13. **DomainBuilderSystemDatabase methods** (append to existing shared class)

### Configuration Requirements
- Package base path: `com.seibel.basicspring`
- Entity naming convention
- Field naming conventions (camelCase → snake_case)
- Default field constraints (lengths, nullable, unique)
- REST endpoint path patterns
- Entity-specific prefixes for test data generation

---  

## Next Steps
1. ✅ Review sample objects to understand exact patterns - **COMPLETE**
2. Define input format (Domain object structure) - **IN PROGRESS**
3. Choose template engine (FreeMarker, Velocity, or custom)
4. Implement field metadata extraction
5. Create templates for each of the 12 file types + builder methods
6. Implement Gradle task/plugin
7. Add configuration options
8. Test with sample entities
9. Document usage instructions

## Example Domain Input

```java  
package com.seibel.scheduler.common.domain;  
  
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
public class Company extends BaseDomain {  
    private String code;      // length=8, nullable=false, unique=true    private String name;      // length=32, nullable=false, unique=true    private String description; // length=255, nullable=false, unique=false}  
```  

**Note:** Field constraints (length, nullable, unique) need to be either:
- Defined in annotations on Domain class
- Configured in a separate mapping file
- Inferred from field types and naming conventions