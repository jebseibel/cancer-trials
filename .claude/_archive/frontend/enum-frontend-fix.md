# Enum Frontend Fix — Blueprint

## Status: Completed 2026-03-14

---

## Problem

The frontend was sending **display values** (e.g. `"Error"`, `"Pending Review"`) as query params
to the REST API, but the backend validates against **constant names** (e.g. `"ERROR"`, `"PENDING_REVIEW"`).

Error seen in logs:
```
400 BAD_REQUEST "Invalid status value: Error"
at CrsChangeController.getAll(CrsChangeController.java:61)
```

Root cause chain:
1. `getCounts()` returned display-value keys (`"Error"`, `"Pending Review"`)
2. Frontend stored those keys as the `statusFilter` state
3. `statusFilter` was passed directly to `?status=Error` in the API call
4. Backend `CrsStatus.isValid("Error")` → false → 400

Additionally, the frontend had **no connection to `EnumController`** — display values were
hardcoded in the TypeScript file, not fetched from the backend.

---

## Fix Pattern (Blueprint for other pages)

### Step 1 — Backend: Fix `getCounts()` map keys

Change display-value keys to constant names using `EnumName.CONSTANT.name()`:

```java
// BEFORE (broken)
return Map.of(
    "Pending Review", crsChangeService.countByStatus(CrsStatus.PENDING_REVIEW.name()),
    "Error",          crsChangeService.countByStatus(CrsStatus.ERROR.name()),
    ...
);

// AFTER (correct)
return Map.of(
    CrsStatus.PENDING_REVIEW.name(), crsChangeService.countByStatus(CrsStatus.PENDING_REVIEW.name()),
    CrsStatus.ERROR.name(),          crsChangeService.countByStatus(CrsStatus.ERROR.name()),
    ...
);
```

### Step 2 — Frontend api.ts: Fix counts interface keys

```typescript
// BEFORE
export interface CRSChangeCounts {
    'Pending Review': number;
    'Error': number;
    ...
}

// AFTER
export interface CRSChangeCounts {
    PENDING_REVIEW: number;
    ERROR: number;
    ...
}
```

### Step 3 — Frontend api.ts: Add EnumValue interface + enumApi

Add once — reusable for all enum endpoints:

```typescript
export interface EnumValue {
    value: string;        // constant name e.g. "ERROR"
    displayValue: string; // UI label e.g. "Error"
    sortOrder: number;
    noDisplay: boolean;
}

export const enumApi = {
    getCrsStatus: () => apiClient.get<EnumValue[]>('/enums/crs-status'),
    // add other enums here as needed
};
```

### Step 4 — Frontend page: Fix StatusFilter type

```typescript
// BEFORE
type StatusFilter = 'Pending Review' | 'Error' | 'Rule Accepted' | ...;

// AFTER
type StatusFilter = 'PENDING_REVIEW' | 'ERROR' | 'RULE_ACCEPTED' | ...;
```

### Step 5 — Frontend page: Fetch enums from EnumController

Add a React Query hook with `staleTime: Infinity` (fetched once per session):

```typescript
const { data: crsStatusEnums } = useQuery({
    queryKey: ['enums', 'crs-status'],
    queryFn: async () => {
        const response = await enumApi.getCrsStatus();
        return response.data as EnumValue[];
    },
    staleTime: Infinity,
});

// Build lookup map: constant name → display value
const crsStatusDisplayMap = new Map(
    (crsStatusEnums ?? []).map(e => [e.value, e.displayValue])
);
```

### Step 6 — Frontend page: Replace all hardcoded string usages

Replace every occurrence of display values with constant names:

| Before | After |
|--------|-------|
| `setStatusFilter('Pending Review')` | `setStatusFilter('PENDING_REVIEW')` |
| `statusFilter === 'Error'` | `statusFilter === 'ERROR'` |
| `counts['Pending Review']` | `counts.PENDING_REVIEW` |
| `crs.status === 'Rule Accepted'` | `crs.status === 'RULE_ACCEPTED'` |
| `"Pending Review"` (display label) | `crsStatusDisplayMap.get('PENDING_REVIEW') ?? 'Pending Review'` |

---

## Files Changed (CRS Change example)

| File | What Changed |
|------|-------------|
| `src/main/java/com/viro/app/web/controller/CrsChangeController.java` | `getCounts()` map keys → constant names |
| `frontend/src/services/api.ts` | `CRSChangeCounts` keys; added `EnumValue`; added `enumApi` |
| `frontend/src/pages/CRSChange.tsx` | `StatusFilter` type; default filter; enum query hook; all status comparisons and labels |

---

## Key Rules

- **Constant names** (`PENDING_REVIEW`, `ERROR`) are used for all API interactions, comparisons, URL params, and map keys
- **Display values** (`"Pending Review"`, `"Error"`) are used only for rendering labels to the user
- Display values come from `EnumController` — never hardcode them on the frontend
- `staleTime: Infinity` on the enum query — fetched once per session, never re-fetched
- The fallback `?? 'Pending Review'` in display map lookups is a safety net only — if the enum loads correctly it is never used
- Backend needs a restart after any Java changes to `getCounts()` or controller validation logic
