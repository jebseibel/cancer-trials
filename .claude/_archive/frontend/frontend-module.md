# Frontend Module

## What It Is

The **frontend** module is a modern React-based web application that provides the user
interface for this application. Built with React 19, TypeScript,
and Vite, it communicates with the Spring Boot backend REST API to provide a responsive,
type-safe single-page application.

## Tech Stack

- **React 19** with TypeScript
- **Vite 7** — dev server (port 3000) with proxy to backend (port 8080)
- **TanStack Query** — server state, caching, mutations
- **React Hook Form + Zod** — form management and validation
- **Tailwind CSS** — utility-first styling
- **React Router** — client-side routing
- **Axios** — HTTP client
- **Recharts** — charting
- **Lucide React** — icons

## Module Structure

```
frontend/
├── public/                  # Static assets
├── src/
│   ├── main.tsx             # Entry point
│   ├── App.tsx              # Root component with routing
│   ├── index.css            # Global styles and Tailwind imports
│   ├── components/          # Reusable UI components (Layout, ProtectedRoute, ColumnFilter, ResultModal)
│   ├── pages/               # Page-level components (~59 pages)
│   ├── services/            # Axios API layer (api.ts)
│   ├── hooks/               # Custom hooks (e.g., useSessionStorage)
│   ├── types/               # TypeScript types (api.ts, enums.ts)
│   ├── lib/                 # Utilities (utils.ts)
│   ├── constants/           # Shared constants (recordStatuses.ts)
│   ├── context/             # React context (EnumContext.tsx)
│   ├── utils/               # Misc utilities (ruleCodeMapping.ts)
│   └── test/                # Test setup, mocks, test-utils
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.js
└── eslint.config.js
```

## Page Groups

Pages are organized around these functional areas:

- **Auth** — Login
- **Dashboard** — Dashboard
- **Facilities** — list, detail, form
- **Companies** — list, detail, form
- **Users** — list
- **Customer Transactions** — list
- **CRS** — Approved, Pending, Change, Mistakes (each with detail views; Pending has upload)
- **Tracking Systems** — WREGIS, MRETS, NAR, ERCOT, NCRETS, MIRECS, TsFiles
- **Retirement Upload** — upload, history, pending promotion, promoted; detail/edit pages per doc type (WREGIS, MRETS, NAR, ERCOT — 3 doc types each, view + edit)
- **Upload** — general file upload

## Architecture

Browser → React Router → Pages → TanStack Query hooks → Axios (services/api.ts) → Spring Boot REST API

## Common UI Patterns

- **Data tables**: `h-screen flex flex-col` layout, sticky headers, dual synchronized scrollbars (top + bottom)
- **Auth guard**: `ProtectedRoute` wraps all authenticated routes
- **Column filtering**: `ColumnFilter` component used across table pages
- **Enums**: Loaded via `EnumContext` — fetched once from backend, shared app-wide

## Configuration

Dev server proxies `/api/*` to `http://localhost:8080`. No `.env` file needed for local dev.

## Testing

Vitest + Testing Library configured. Test files co-located with source (`.test.tsx` / `.test.ts`).

```bash
npm run test          # watch mode
npm run test:run      # single pass
npm run test:coverage
npm run test:ui       # browser UI
```

## Current Status

- Development and production builds working
- JWT authentication with protected routes
- Full API integration with Spring Boot backend
- React 19, Vite 7, TypeScript 5.9

## Recent Changes (December 2025)

### About Modal
- Info icon in nav bar (between Profiles and Logout)
- Displays: app title, build version (from build.gradle, strips `-SNAPSHOT`), developer credits

### Retirement Certificate Uploads
- Recent uploads panel shows all 12 doc types (WREGIS, MRETS, NAR, ERCOT)
- NCRETS and MIRECS excluded (no upload functionality)
- NAR Voluntary Compliance entries shown in red semibold as warning
- ERCOT Screenshot and NAR Voluntary Compliance show AI processing warnings
- Recent uploads sorted alphabetically by doc type display name

### Display Value Mapping
- Facility table: long CRS Tracking Attestation Status values shortened for display; CSV export retains originals
- CRS Mistakes: status as plain text; CRS Change: status as colored badges
