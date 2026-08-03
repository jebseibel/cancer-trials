# Frontend Description

## Overview
The frontend is a modern React application built with TypeScript and Vite. It provides a user interface for managing customers, purchases, and users with authentication and form handling capabilities.

## Technology Stack
- **React 19.1.1**: Core UI framework with hooks
- **TypeScript 5.9.3**: Type-safe JavaScript
- **Vite 7.1.7**: Fast build tool and development server
- **React Router DOM 7.9.5**: Client-side routing and navigation
- **Tailwind CSS 4.1.16**: Utility-first CSS framework
- **TanStack React Query 5.90.6**: Server state management and data fetching
- **Axios 1.13.1**: HTTP client for API requests
- **React Hook Form 7.66.0**: Efficient form state management
- **Zod 4.1.12**: Schema validation and parsing
- **Lucide React 0.552.0**: Icon library
- **Recharts 3.3.0**: Charting and visualization library
- **clsx 2.1.1**: Conditional CSS class composition
- **tailwind-merge 3.3.1**: Tailwind CSS class merging utility
- **@hookform/resolvers 5.2.2**: Form validation resolver integration

## Project Structure

### Pages
- **Login**: Authentication page with login form
- **Dashboard**: Home page displaying overview cards and statistics
  - Shows customer count, purchase count, and user count
  - Quick action links to management pages
- **Customers**: Customer management interface for CRUD operations
- **Purchases**: Purchase management interface for CRUD operations (renamed from Orders)
- **Users**: User management interface for CRUD operations

### Components
- **Layout**: Main application layout wrapper with navigation
- **ProtectedRoute**: Route guard component for authenticated pages

### Services
- **api.ts**: Centralized API client with Axios configuration
  - Axios interceptors for JWT token attachment
  - Auto-redirect to login on 401/403 errors
  - API endpoints for Company, Customer, Purchase, and User resources
  - Authentication endpoints for login and registration
  - Auth helper functions for token management

### Types
- **api.ts**: TypeScript interfaces for all API entities
  - Profile, Company, Customer, Purchase, User types
  - Request and Response DTO interfaces
  - Authentication types (LoginRequest, RegisterRequest, AuthResponse)
  - Pagination types (PageResponse)

### Utilities
- **lib/utils.ts**: Common utility functions

## Features

### Authentication
- Login with username and password
- JWT token-based authentication stored in localStorage
- Automatic token attachment to API requests via interceptor
- Session protection via ProtectedRoute component
- Auto-logout on 401/403 responses

### Entity Management
All entities support CRUD operations through dedicated pages:
- List all entities with search/filter capabilities
- Create new entities via modal forms
- Update existing entities
- Delete entities with confirmation dialogs

### Core Entities
1. **Customers**: Code, name, contact information, email, phone
2. **Purchases**: Customer reference, items, status
3. **Users**: Username, email, role-based access
4. **Company**: Code, name, description

### Data Management
- React Query for server state caching and synchronization
- Axios for HTTP communication with the backend
- Form validation with basic input validation
- Automatic refetch and cache invalidation on mutations
- Error and success message handling

### UI/UX
- Responsive design with Tailwind CSS (mobile-first)
- Dashboard with quick action cards
- Lucide icons for visual indicators
- Professional card-based layout
- Navigation menu for main sections
- Modal forms for create/edit operations
- Real-time search and filtering
- Toast-style success/error messages
- Loading states for async operations

## Development Commands
- `npm run dev`: Start development server with Vite hot reload
- `npm run build`: TypeScript build followed by production build
- `npm run lint`: Run ESLint for code quality
- `npm run preview`: Preview production build locally

## API Integration
- Base URL configured via `VITE_API_URL` environment variable
- Defaults to `/api` for production deployments
- All requests include JWT Bearer token in Authorization header
- Response error handling with automatic login redirect on auth failures
- Paginated responses handled with content property extraction

## Authentication Flow
1. User enters credentials on Login page
2. API returns JWT token
3. Token stored in localStorage
4. Token automatically added to all subsequent API requests
5. Protected routes redirect unauthenticated users to login
6. Invalid/expired tokens trigger automatic logout and redirect

## Routing Structure
- `/login` - Public login page
- `/` - Protected dashboard (home)
- `/customers` - Customer management
- `/purchases` - Purchase management
- `/users` - User management

## Code Quality
- ESLint configuration for code linting
- TypeScript strict mode for type safety
- React hooks best practices

## State Management
- React Query for server-side data caching and synchronization
- Local state (useState) for form data and modal visibility
- localStorage for authentication token persistence
- React Router for navigation state

## Form Handling
- Modal-based forms for create and edit operations
- Inline validation with required field checks
- Form reset on successful submission or cancel
- Error messages displayed in red callout boxes
- Success notifications with auto-dismiss after 3 seconds
