# BasicSpring Project Description

## Overview
BasicSpring is a full-stack web application combining a Spring Boot REST API backend with a modern React frontend. The backend is built with Java, Gradle, and MySQL, providing comprehensive CRUD operations for managing customers, purchases, and users with JWT-based authentication.

## Purpose
This project demonstrates a complete, production-ready web application architecture for managing business entities through RESTful APIs. It integrates a responsive frontend UI with a robust backend REST API, featuring Spring Security for authentication, Liquibase for database versioning, and support for AWS infrastructure deployment.

## Architecture

### Unified Single-Module Structure
The project uses a single Spring Boot application with integrated frontend:

- **Backend**: Monolithic Spring Boot application (com.seibel.basic)
- **Frontend**: React/TypeScript application with Vite build tool
- **Frontend Integration**: Built artifacts served as static assets from Spring Boot

### Backend Layered Architecture
Each entity follows a consistent 4-layer pattern:
1. **Controller Layer**: REST endpoints with DTO conversion and Swagger documentation
2. **Service Layer**: Business logic and validation with pagination and sorting
3. **Database Service Layer**: Low-level persistence operations
4. **Entity Layer**: JPA entities, repositories, and mappers

### Backend Package Structure
```
com.seibel.basic
├── common/         # Shared domain objects, enums, exceptions, utilities
├── config/         # Spring configuration
├── database/       # JPA entities, repositories, database services
├── security/       # JWT utilities and authentication
├── service/        # Business services
└── web/           # Controllers, DTOs, exception handling
```

### Frontend Structure
```
frontend/
├── src/
│   ├── pages/      # Login, Dashboard, Customers, Purchases, Users
│   ├── components/ # Layout, ProtectedRoute
│   ├── services/   # API client and utilities
│   ├── types/      # TypeScript interfaces
│   └── lib/        # Utility functions
```

## Technology Stack

### Backend
- **Java 21**: Modern Java with latest features
- **Spring Boot 3.5.7**: Web framework and dependency injection
- **Spring Security**: JWT-based authentication and authorization
- **Gradle 8.14.3**: Build automation and dependency management
- **MySQL**: Relational database (AWS RDS compatible)
- **Liquibase**: Database schema version control and migrations
- **Lombok**: Boilerplate reduction (annotations)
- **ModelMapper**: Object mapping between layers
- **Spring Data JPA**: ORM and repository pattern
- **Swagger/OpenAPI**: API documentation and testing

### Frontend
- **React 19.1.1**: UI framework with hooks
- **TypeScript 5.9.3**: Type-safe JavaScript
- **Vite 7.1.7**: Fast build tool and development server
- **React Router 7.9.5**: Client-side routing
- **Tailwind CSS 4.1.16**: Utility-first styling
- **TanStack React Query 5.90.6**: Server state management
- **Axios 1.13.1**: HTTP client
- **React Hook Form 7.66.0**: Form state management
- **Zod 4.1.12**: Schema validation
- **Lucide React 0.552.0**: Icon library
- **Recharts 3.3.0**: Data visualization

## Core Entities
1. **Customer**: Code, name, contact information, email, phone
2. **Purchase**: Customer reference, items, status (formerly Order)
3. **User**: Username, email, role-based access

## Key Features
- **Complete CRUD Operations**: Create, Read, Update, Delete for all entities
- **Soft Deletes**: Entities marked inactive with timestamps
- **UUID-Based External IDs**: Extid prevents database ID exposure
- **Pagination & Sorting**: Configurable page sizes and sort fields
- **JWT Authentication**: Stateless token-based security with 24-hour expiration
- **Responsive Frontend**: Mobile-first design with Tailwind CSS
- **Real-time Data Sync**: React Query for efficient caching and refetching
- **API Documentation**: Swagger UI for all REST endpoints
- **Input Validation**: Request DTO validation with detailed error messages
- **Exception Handling**: Typed HTTP responses with proper status codes
- **Audit Trail**: Automatic createdAt, updatedAt, deletedAt timestamps
- **Comprehensive Logging**: SLF4J with configurable levels

## Development Constraints
- Project is NOT in production
- Database schema changes made directly in Liquibase files
- No credentials committed to Git (use environment variables)
- All database interactions through REST API (no direct database access)
- Frontend uses REST API endpoints exclusively

## Development Environment
- **OS**: Ubuntu 24.04
- **Backend**: Java 21, Spring Boot, managed by user
- **Frontend**: Node.js with npm, started via `npm run dev`
- **Database**: MySQL (local or AWS RDS) configured via environment variables
- **Database Reset**: Docker webhook at `http://localhost:5678/webhook/clear-db`
- **Testing**: Playwright E2E tests available

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login with credentials
- `POST /api/auth/register` - Register new user

### Customers
- `GET /api/customer` - List (paginated)
- `GET /api/customer/{extid}` - Get details
- `POST /api/customer` - Create
- `PUT /api/customer/{extid}` - Update
- `DELETE /api/customer/{extid}` - Soft delete

### Purchases
- `GET /api/purchase` - List (paginated, sortable)
- `GET /api/purchase/{extid}` - Get details
- `POST /api/purchase` - Create
- `PUT /api/purchase/{extid}` - Update
- `DELETE /api/purchase/{extid}` - Soft delete

### Users
- `GET /api/user` - List (paginated)
- `GET /api/user/{extid}` - Get details
- `POST /api/user` - Create
- `PUT /api/user/{extid}` - Update
- `DELETE /api/user/{extid}` - Soft delete

## Build & Deployment

### Local Development
- Backend: Run Spring Boot application (port 8080)
- Frontend: Run Vite dev server (port 5173)
- Database: MySQL with automatic Liquibase migrations

### Production Deployment
- Build frontend: `npm run build` (Vite)
- Gradle build: `gradle buildDeployment` (includes frontend)
- Outputs single JAR with embedded frontend
- AWS RDS MySQL configuration via environment variables
- Spring Boot serves frontend as static assets

## Running the Application
1. **Start Backend**: Run Spring Boot (user manages this)
2. **Start Frontend**: `npm run dev` in frontend directory
3. **Reset Database**: Call webhook `http://localhost:5678/webhook/clear-db`
4. **Access Frontend**: Open browser to `http://localhost:5173`
5. **API Docs**: View Swagger at backend URL + `/swagger-ui.html`

## Design Principles
- **Layered Architecture**: Clear separation of concerns across layers
- **RESTful API**: Standard HTTP methods and status codes
- **JWT Security**: Stateless authentication suitable for cloud deployment
- **Type Safety**: TypeScript on frontend, Java with proper typing on backend
- **Responsive Design**: Mobile-first CSS with Tailwind
- **Error Handling**: Comprehensive exception handling with proper HTTP responses
- **State Management**: React Query for server state, local state for UI
- **Documentation**: Swagger API docs and inline code documentation
