import type { AccessLevel } from '../types/api';

/**
 * Access levels are ranked, matching the backend enum: OWNER covers every check below it.
 *
 * Kept out of PatientContext.tsx so that file exports only components - a file mixing the two
 * breaks Vite's fast refresh.
 */
const RANK: Record<AccessLevel, number> = {
    VIEW_TRIALS: 10,
    VIEW_RECORD: 20,
    EDIT_RECORD: 30,
    OWNER: 40,
};

/**
 * True when a held level satisfies a requirement.
 *
 * Ranked comparison, not equality - an OWNER covers VIEW_TRIALS, so no call site has to
 * enumerate which levels qualify.
 *
 * ⚠️ This is for rendering decisions only. The backend re-checks every request, and a user can
 * edit localStorage; the frontend must never be the only thing standing between a user and a
 * capability.
 */
export function covers(held: AccessLevel | null, required: AccessLevel): boolean {
    if (!held) return false;
    return RANK[held] >= RANK[required];
}
