# Backend Description

## Overview
The BasicSpring backend is a RESTful API built with Spring Boot 3.5.7 and Gradle. It provides comprehensive CRUD operations for managing customers, purchases, and users with JWT-based authentication and a structured layered architecture.

## Architecture

### Layered Architecture
The backend follows a 4-layer pattern:

1. **Controller Layer** (`web.controller`): REST endpoints with DTO conversion
2. **Service Layer** (`service`): Business logic and validation
3. **Database Service Layer** (`database.db.service`): Data persistence operations
4. **Entity Layer** (`database.db.entity`): JPA entities and repositories

### Package Structure
```
com.seibel.basic
├── common/
│   ├── domain/           # Business domain objects (Customer, Purchase, User)
│   ├── enums/            # Enums (ActiveEnum, CompResult)
│   ├── exceptions/       # Business exceptions
│   └── util/             # Utilities (CodeGenerator)
├── config/               # Spring configuration
├── database/
│   ├── db/
│   │   ├── entity/       # JPA entities (CustomerDb, PurchaseDb, UserDb, BaseDb)
│   │   ├── mapper/       # Entity ↔ Domain mappers
│   │   ├── repository/   # Spring Data repositories
│   │   └── service/      # Database service layer
├── security/             # JWT and authentication
├── service/              # Business service layer
└── web/
    ├── controller/       # REST controllers
    ├── request/          # Request DTOs
    ├── response/         # Response DTOs
    └── GlobalExceptionHandler
```

## Key Components

### Core Entities
1. **Customer**: Code, name, contact information, email, phone
2. **Purchase**: Customer reference, items, status (renamed from Order)
3. **User**: Username, email, role

### Authentication
- **JwtUtil**: Generates and validates JWT tokens
  - Token expiration: 24 hours (configurable via jwt.expiration property)
  - HS256 signature algorithm
  - Claims include username and issuedAt timestamp
  - Configurable secret key via jwt.secret property
- **CustomUserDetailsService**: Loads user details from UserDb entity
- **JwtAuthenticationFilter**: Extracts token from request headers and validates
- **SecurityConfig**: Configures security filter chain
  - Permits auth endpoints (/api/auth/**)
  - Requires JWT for all other /api endpoints
  - Enables CORS
  - Stateless session management

### Database Layer
- **Repositories**: Spring Data `JpaRepository` for each entity
  - Standard methods: save, findById, delete, etc.
  - Custom methods: findByExtid, findByActive, findAllActive, existsByExtid
  - Paginated queries with findByActive(ActiveEnum, Pageable)
- **Entity Mappers**: Convert between Domain and Db objects
- **Database Services**: Handle low-level persistence operations
  - Create, update, delete, findByExtid, findAll, findByActive methods
  - Soft delete implementation (marks as INACTIVE, sets deletedAt)
  - Pagination support

### Service Layer
- **BaseService**: Abstract base with validation helpers
  - requireNonNull(), requireNonBlank() methods
  - Uses Lombok @Slf4j for logging
- **Entity Services** (CustomerService, PurchaseService, UserService):
  - CRUD operations delegating to database service
  - Input validation
  - Pagination support with configurable max page size (100)
  - Allowed sort fields validation
  - Business logic coordination with exception handling
  - Transactional operations (@Transactional annotations)

### Web Layer
- **Controllers**: RESTful endpoints with Swagger documentation
  - GET / - List with pagination (default 20 items) and optional active filter
  - GET /{extid} - Get single entity
  - POST / - Create entity
  - PUT /{extid} - Update entity
  - DELETE /{extid} - Soft delete entity
- **Converter Classes**: Inline package-private classes in controller files
  - toDomain() - Convert request DTO to domain object
  - toResponse() - Convert domain object to response DTO
- **Request DTOs**: Create and Update variants
  - Create: All fields required with validation
  - Update: All fields optional for partial updates
- **Response DTOs**: Include extid and all business fields

### Exception Handling
- **GlobalExceptionHandler**: REST controller advice with typed exception handlers
  - ResourceNotFoundException → HTTP 404
  - ValidationException → HTTP 400
  - MethodArgumentNotValidException → HTTP 400 with field errors
  - ConstraintViolationException → HTTP 400 with field errors
  - ResourceAlreadyExistsException → HTTP 409
  - ServiceException → HTTP 500
  - Generic Exception → HTTP 500
- **Custom Exceptions**:
  - BaseServiceException: Base class for all service exceptions
  - ServiceException: General business logic errors
  - ValidationException: Input validation failures
  - ResourceNotFoundException: Entity not found
  - ResourceAlreadyExistsException: Duplicate resource

### Utilities
- **CodeGenerator**: Generates unique codes for entities
- **ActiveEnum**: Tracks entity active/inactive status
  - ACTIVE (1), INACTIVE (0)
  - Helper methods: isActive(), isInactive()

## Database Configuration

### Liquibase
- Changelog: `db/changelog/db.changelog-master.yaml`
- Automatic schema initialization on startup
- Drop-first enabled in non-production environments
- MySQL driver: `com.mysql:mysql-connector-j`

### Data Source
- Connection pool: HikariCP with 30s connection timeout
- URL: Configured via RDS_HOSTNAME, RDS_PORT, RDS_DB_NAME environment variables
- Credentials: RDS_USERNAME, RDS_PASSWORD environment variables
- Initialization failure timeout: 0 (fail fast)

### JPA Configuration
- DDL-auto: none (Liquibase handles schema)
- Show-sql: false (no SQL logging by default)
- Allow bean definition overriding: true

## Security Features

### Authentication
- JWT token-based (stateless, no session-based authentication)
- Token stored in HTTP Authorization header (Bearer scheme)
- Username/password credentials for login
- Token extraction and validation from request headers

### Password Handling
- Uses Spring Security password encoding
- Configured for secure password comparison

### CORS
- Enabled globally in SecurityConfig
- Allows cross-origin requests from frontend

### Authorization
- Stateless session management
- Public endpoints: /api/auth/login, /api/auth/register, static assets
- Protected endpoints: All /api/** require valid JWT token
- Token validation on every protected request

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login with username/password
- `POST /api/auth/register` - Register new user

### Customer Management
- `GET /api/customer` - List customers (paginated, default 20)
- `GET /api/customer/{extid}` - Get customer details
- `POST /api/customer` - Create customer
- `PUT /api/customer/{extid}` - Update customer
- `DELETE /api/customer/{extid}` - Soft delete customer

### Purchase Management
- `GET /api/purchase` - List purchases (paginated, default 20, sortable by: customer, status, createdAt, updatedAt)
- `GET /api/purchase/{extid}` - Get purchase details
- `POST /api/purchase` - Create purchase
- `PUT /api/purchase/{extid}` - Update purchase
- `DELETE /api/purchase/{extid}` - Soft delete purchase

### User Management
- `GET /api/user` - List users (paginated, default 20)
- `GET /api/user/{extid}` - Get user details
- `POST /api/user` - Create user
- `PUT /api/user/{extid}` - Update user
- `DELETE /api/user/{extid}` - Soft delete user

## Build & Deployment

### Build System
- Gradle 8.14.3
- Java 21 toolchain
- Embedded frontend build tasks

### Frontend Integration
- Frontend built with Vite and copied to `src/main/resources/static`
- Build tasks: npmInstall, npmBuild, copyFrontend, cleanStatic
- Deployment task: buildDeployment (builds JAR with frontend included)
- Development task: killFrontend (terminates dev servers on ports 5173-5175)

### Configuration
- Application name: basic
- Server port: 8080 (configurable via PORT env var)
- Version: 0.0.2-SNAPSHOT
- Spring Boot DevTools enabled for development

## Dependencies

### Spring Boot
- spring-boot-starter-web: REST API support
- spring-boot-starter-data-jpa: ORM/persistence with JPA repositories
- spring-boot-starter-security: Authentication/authorization
- spring-boot-starter-validation: Input validation
- spring-boot-starter-thymeleaf: Template engine
- spring-boot-devtools: Hot reload for development
- springdoc-openapi-starter-webmvc-ui: Swagger UI (v2.6.0)

### JWT & Security
- jjwt-api (0.12.6): JWT token API
- jjwt-impl (0.12.6): JWT implementation
- jjwt-jackson (0.12.6): Jackson integration for JWT

### Database
- mysql-connector-j: MySQL JDBC driver (version with mysql.cj.jdbc.Driver)
- liquibase-core (4.29.2): Schema versioning and migrations

### Utilities
- modelmapper (3.2.0): Object mapping
- lombok (1.18.34): Boilerplate reduction (@Data, @Slf4j, etc.)
- spring-dotenv (4.0.0): .env file support
- apache commons-csv (1.11.0): CSV processing
- threeten-bp (1.6.8): Date/time utilities
- snakeyaml (2.2): YAML parsing

### Testing
- spring-boot-starter-test: Integration tests
- junit-platform-launcher: JUnit 5

## Development Features
- Spring Boot DevTools for hot reload
- Comprehensive logging throughout application (configurable levels)
- Swagger/OpenAPI documentation for all endpoints
- Support for environment variables and .env files
- Request validation with detailed error responses

## Deployment Considerations
- Runs on Ubuntu 24.04 by default
- Configured for AWS RDS MySQL
- Frontend served as static assets from Spring Boot
- JWT-based stateless authentication (suitable for cloud deployment)
- Database migrations handled by Liquibase on startup
- Configurable via environment variables for cloud deployments
