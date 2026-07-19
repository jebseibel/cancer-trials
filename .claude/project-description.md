# Spring Multimodule Project Description

## Overview
Spring Multimodule is a full-stack web application combining a Spring Boot REST API backend with a modern React frontend. The backend is built with Java, Gradle, and MySQL, providing comprehensive CRUD operations for managing customers, purchases, and users with JWT-based authentication.

## Purpose
This project demonstrates a multi-module Spring Boot architecture for managing business entities through RESTful APIs. It integrates a responsive frontend UI with a robust backend REST API, featuring Spring Security for authentication, Liquibase for database versioning, and support for AWS infrastructure deployment.

## Architecture

### Multi-Module Structure
The project uses a Gradle multi-module build:

- **Root module**: Spring Boot application — web layer (controllers, security, config)
- **:common** — Shared domain objects, enums, exceptions, utilities
- **:database** — JPA entities, repositories, mappers, db services, Liquibase migrations
- **Frontend**: React/TypeScript application with Vite build tool, served as static assets from Spring Boot

### Backend Layered Architecture
Each entity follows a consistent 4-layer pattern:
1. **Controller Layer**: REST endpoints with DTO conversion and Swagger documentation
2. **Service Layer**: Business logic and validation with pagination and sorting
3. **Database Service Layer**: Low-level persistence operations
4. **Entity Layer**: JPA entities, repositories, and mappers

### Backend Package Structure
```
com.seibel.jobs
├── common/         # Shared domain objects, enums, exceptions, utilities  (:common module)
├── database/       # JPA entities, repositories, db services, Liquibase   (:database module)
├── config/         # Spring configuration
├── security/       # JWT utilities and authentication
├── service/        # Business services
└── web/            # Controllers, DTOs, exception handling
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
- **React 19**: UI framework with hooks
- **TypeScript**: Type-safe JavaScript
- **Vite**: Fast build tool and development server
- **React Router**: Client-side routing
- **Tailwind CSS**: Utility-first styling
- **TanStack React Query**: Server state management
- **Axios**: HTTP client
- **React Hook Form**: Form state management
- **Zod**: Schema validation
- **Recharts**: Data visualization

## Core Entities
1. **Customer**: Code, name, contact information, email, phone
2. **Purchase**: Customer reference, items, status
3. **User**: Username, email, role-based access

## Key Features
- **Complete CRUD Operations**: Create, Read, Update, Delete for all entities
- **Soft Deletes**: Entities marked inactive with timestamps
- **UUID-Based External IDs**: Extid prevents database ID exposure
- **Pagination & Sorting**: Configurable page sizes and sort fields
- **JWT Authentication**: Stateless token-based security
- **String Cleanup**: Empty strings/whitespace automatically converted to NULL via Liquibase stored procedure and JPA entity listener

## Development Environment
- **OS**: Ubuntu 24.04
- **Backend**: Java 21, Spring Boot, managed by user
- **Frontend**: `npm run dev` in `frontend/` directory
- **Database Reset**: Docker n8n webhook at `http://localhost:5678/webhook/clear-db`

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
- Database: MySQL with automatic Liquibase migrations on startup

### Production Deployment
- Build frontend: `npm run build` (Vite)
- Gradle build: `./gradlew buildDeployment` (includes frontend)
- Outputs single JAR with embedded frontend
- AWS RDS MySQL configuration via environment variables
