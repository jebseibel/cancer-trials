# Frontend

React + TypeScript + Vite single-page app in `frontend/`, served standalone in dev and bundled
into the Spring Boot jar for deployment.

> **Verified 2026-08-08** against `frontend/src` and `package.json`.

## Technology Stack

React 19.1, TypeScript, Vite, React Router DOM 7.9, Tailwind CSS 4.1, TanStack React Query 5.90,
Axios 1.13, Lucide icons.

Also declared but **not currently used** by any page: React Hook Form + `@hookform/resolvers`,
Zod, Recharts. The Diagnosis form is hand-rolled `useState`, not React Hook Form — worth knowing
before assuming a form library is in play.

## Structure

```
frontend/src/
├── App.tsx                      routes
├── main.tsx
├── components/
│   ├── Layout.tsx               nav bar + <Outlet/>, logout button
│   ├── ProtectedRoute.tsx       redirects to /login when no token
│   └── JobResultModal.tsx       shared job-result modal (title + label/value lines)
├── pages/
│   ├── Login.tsx
│   ├── Dashboard.tsx
│   ├── TrialSearch.tsx          filter trials already in the DB
│   ├── TrialDetail.tsx          full record + personal tracking + Tier 1 checks
│   ├── SavedTrials.tsx          the user's tracked trials, filtered by status
│   ├── Diagnosis.tsx            single-page patient diagnosis form
│   └── Ingestion.tsx            triggers CT.gov ingestion
├── lib/
│   ├── useCurrentAppUser.ts     matches login User -> AppUser by username
│   ├── receptorSubtype.ts       derives receptor subtype; derives age from DOB
│   ├── tier1Matching.ts         deterministic age/sex/recruiting eligibility checks
│   └── utils.ts
├── services/api.ts              axios client + all endpoint groups
└── types/api.ts                 response/request interfaces + enum value arrays
```

## Routes

| Route | Page | Access |
| --- | --- | --- |
| `/login` | Login | public |
| `/` | Dashboard | protected |
| `/trials` | Trial Search | protected |
| `/trials/:extid` | Trial Detail | protected |
| `/saved-trials` | Saved Trials | protected |
| `/diagnosis` | Diagnosis | protected |
| `/ingestion` | Ingestion | protected |

Everything except `/login` is wrapped in `ProtectedRoute` inside a `Layout`.

## API layer

`services/api.ts` exports one object per resource group: `trialApi`, `trialSourceApi`,
`trialStatusApi`, `appUserApi`, `locationApi`, `armGroupApi`, `interventionApi`, `outcomeApi`,
`overallOfficialApi`, `conditionApi`, `sponsorApi`, `patientDiagnosisApi`, `ingestionApi`,
`ragApi`, `authApi`, plus `authHelpers`.

- Base URL from `VITE_API_URL`, defaulting to `/api`.
- Request interceptor attaches `Authorization: Bearer <token>` from `localStorage`.
- Response interceptor clears the token and redirects to `/login` on 401/403 — **except** on
  `/auth/` endpoints, so the login form can show its own error.

**Everything on the wire is an `extid`.** No numeric id ever crosses the API boundary, including
FK-like references (`trialExtid`, `appUserExtid`). Controllers resolve extid → internal id.

## Two things that surprise people

**`User` and `AppUser` are different tables with no FK.** Login identity is `user`; personal
tracking is `app_user`. `useCurrentAppUser` fetches `/api/appuser` and matches **by username**.
Every login account needs a matching `AppUser` row seeded manually — there is no UI for it.
Without one, Trial Detail tracking, Saved Trials, and Diagnosis all show "no app-user profile
linked to your login."

**Enum vocabularies are hardcoded on the frontend.** There is no `GET /api/enums/{name}`
endpoint. `types/api.ts` declares `as const` arrays (`TRIAL_STATUS_VALUES`, `STAGE_VALUES`,
`RECEPTOR_STATUS_VALUES`, `MENOPAUSAL_STATUS_VALUES`, and others) and pages render them with
`.replaceAll('_', ' ')`. Adding a value to a backend enum will **not** surface it in the UI —
the array must be updated too. See `../code-style/enum-lifecycle-rules.md`.

## Commands

- `npm run dev` — Vite dev server (only start this if explicitly asked)
- `npm run build` — `tsc -b && vite build`
- `npm run lint` — ESLint
- `./gradlew buildDeployment` (repo root) — builds the frontend and bundles it into the jar

Note `npm run lint` currently reports one pre-existing error in `Login.tsx`
(`@typescript-eslint/no-explicit-any`).

## Related

- `../../CURRENT_STATE.md` — overall status
- `../diagnosis/DIAGNOSIS_MATCHING_DESIGN.md` — what the Diagnosis page and Tier 1
  checks implement, and why no eligibility verdict is ever rendered
- `../clinical-trials-rag/FRONTEND_JOB_TRIGGER_PLAN.md` — job-trigger button patterns
