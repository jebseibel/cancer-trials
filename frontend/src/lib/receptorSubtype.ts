import type { ReceptorStatus } from '../types/api';

/**
 * Derive the receptor subtype from the three individual receptors.
 *
 * Per DIAGNOSIS_MATCHING_DESIGN.md section 7, the subtype and the three receptors are stored
 * redundantly on purpose - criteria are written both ways - but they must never disagree, so
 * this is the single place the subtype is produced and the field is read-only in the UI.
 *
 * Returns null when the receptors don't determine a subtype (any of them unknown or unset),
 * which is honest rather than guessing at one.
 */
export function deriveReceptorSubtype(er: string, pr: string, her2: string): string | null {
    const known = (v: string): v is ReceptorStatus => v === 'POSITIVE' || v === 'NEGATIVE';
    if (!known(er) || !known(pr) || !known(her2)) return null;

    const hormonePositive = er === 'POSITIVE' || pr === 'POSITIVE';
    if (her2 === 'POSITIVE') {
        return hormonePositive ? 'HR_POSITIVE_HER2_POSITIVE' : 'HER2_POSITIVE';
    }
    return hormonePositive ? 'HR_POSITIVE_HER2_NEGATIVE' : 'TRIPLE_NEGATIVE';
}

/** Age in whole years, derived rather than stored - a stored age is wrong within a year. */
export function ageFromDateOfBirth(dob: string): number | null {
    if (!dob) return null;
    const birth = new Date(dob);
    if (Number.isNaN(birth.getTime())) return null;
    const now = new Date();
    let age = now.getFullYear() - birth.getFullYear();
    const monthDelta = now.getMonth() - birth.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < birth.getDate())) age -= 1;
    return age >= 0 ? age : null;
}
