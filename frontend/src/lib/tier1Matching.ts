import type { PatientDiagnosis, Trial } from '../types/api';
import { ageFromDateOfBirth } from './receptorSubtype';

/**
 * Tier 1 eligibility checks - the deterministic ones.
 *
 * Per DIAGNOSIS_MATCHING_DESIGN.md section 4, these compare the diagnosis against structured
 * fields CT.gov already parsed for us (age, sex), with no text parsing of the criteria narrative.
 * They are the only checks the design allows to be presented as pass/fail; everything softer
 * belongs to Tier 2 and must be framed as "look into" / "ask about".
 *
 * The honest outcome set matters as much as the checks. "unknown" is a first-class result: a
 * missing diagnosis field or an unparseable trial value must read as "not assessed", never as a
 * pass and never as a fail. Section 5 is explicit that nothing may be auto-excluded.
 */
export type CheckOutcome = 'pass' | 'fail' | 'unknown';

export interface Tier1Check {
    label: string;
    outcome: CheckOutcome;
    /** Plain-language explanation, e.g. "Age 54 is within 18-75". */
    detail: string;
}

/**
 * Parse a CT.gov age string into whole years.
 *
 * Measured against 400 live CT.gov cancer trials (2026-08-08): 371 "N Years", 1 "N Year"
 * (singular - a "Years"-only match would silently drop it), 2 "N Months", 26 absent.
 * "N/A" also appears in our own captured fixture. Weeks and Days are accepted for the same
 * reason: CT.gov uses them on pediatric studies and they cost nothing to support.
 *
 * Returns null for absent/N/A/unrecognised input, which callers must treat as "no limit
 * stated" or "not assessable" rather than as zero.
 */
export function parseCtGovAge(value: string | undefined | null): number | null {
    if (!value) return null;
    const text = value.trim();
    if (text === '' || text.toUpperCase() === 'N/A') return null;

    const match = /^(\d+(?:\.\d+)?)\s*(year|month|week|day)s?$/i.exec(text);
    if (!match) return null;

    const amount = Number(match[1]);
    switch (match[2].toLowerCase()) {
        case 'year':
            return amount;
        case 'month':
            return amount / 12;
        case 'week':
            return amount / 52;
        case 'day':
            return amount / 365;
        default:
            return null;
    }
}

/** Render a parsed age bound back to something readable, e.g. 0.5 -> "6 months". */
function describeAge(years: number): string {
    if (Number.isInteger(years)) return `${years}`;
    const months = Math.round(years * 12);
    return months >= 1 ? `${months} months` : `${Math.round(years * 365)} days`;
}

function checkAge(diagnosis: PatientDiagnosis, trial: Trial): Tier1Check {
    const label = 'Age';
    const age = diagnosis.dateOfBirth ? ageFromDateOfBirth(diagnosis.dateOfBirth) : null;
    const min = parseCtGovAge(trial.minimumAge);
    const max = parseCtGovAge(trial.maximumAge);

    if (age === null) {
        return { label, outcome: 'unknown', detail: 'No date of birth recorded on the diagnosis.' };
    }

    // Absent bounds are the common case - 62% of trials state no maximum - and mean "no limit",
    // not "unknown". Only when BOTH are absent is there nothing to check.
    if (min === null && max === null) {
        const stated = trial.minimumAge || trial.maximumAge;
        return {
            label,
            outcome: 'unknown',
            detail: stated
                ? `Age ${age}. Trial states an age limit this app could not read ("${stated}").`
                : `Age ${age}. Trial states no age limits.`,
        };
    }

    const range = `${min !== null ? describeAge(min) : 'any'}–${max !== null ? describeAge(max) : 'any'}`;
    const tooYoung = min !== null && age < min;
    const tooOld = max !== null && age > max;

    if (tooYoung || tooOld) {
        return {
            label,
            outcome: 'fail',
            detail: `Age ${age} is outside the trial's range of ${range}.`,
        };
    }
    return { label, outcome: 'pass', detail: `Age ${age} is within the trial's range of ${range}.` };
}

function checkSex(diagnosis: PatientDiagnosis, trial: Trial): Tier1Check {
    const label = 'Sex';
    const patientSex = diagnosis.sex?.trim().toUpperCase();
    const trialSex = trial.sex?.trim().toUpperCase();

    if (!patientSex) {
        return { label, outcome: 'unknown', detail: 'No sex recorded on the diagnosis.' };
    }
    if (!trialSex) {
        return { label, outcome: 'unknown', detail: 'Trial does not state a sex requirement.' };
    }
    if (trialSex === 'ALL') {
        return { label, outcome: 'pass', detail: 'Trial accepts all sexes.' };
    }
    if (trialSex === patientSex) {
        return { label, outcome: 'pass', detail: `Trial accepts ${trialSex.toLowerCase()} participants.` };
    }
    return {
        label,
        outcome: 'fail',
        detail: `Trial accepts ${trialSex.toLowerCase()} participants only.`,
    };
}

function checkRecruiting(trial: Trial): Tier1Check {
    const label = 'Recruiting';
    const status = trial.overallStatus?.trim().toUpperCase();

    if (!status) {
        return { label, outcome: 'unknown', detail: 'Trial does not state a recruitment status.' };
    }
    const readable = status.replaceAll('_', ' ').toLowerCase();
    if (status === 'RECRUITING') {
        return { label, outcome: 'pass', detail: 'Trial is currently recruiting.' };
    }
    if (status === 'NOT_YET_RECRUITING' || status === 'ENROLLING_BY_INVITATION') {
        return { label, outcome: 'unknown', detail: `Trial is ${readable} — enrolment may still be possible.` };
    }
    return { label, outcome: 'fail', detail: `Trial is ${readable}.` };
}

/**
 * Run every Tier 1 check. Order is fixed so the UI reads consistently across trials.
 */
export function runTier1Checks(diagnosis: PatientDiagnosis, trial: Trial): Tier1Check[] {
    return [checkAge(diagnosis, trial), checkSex(diagnosis, trial), checkRecruiting(trial)];
}

/**
 * A one-line summary for list views.
 *
 * Deliberately not a score or a percentage - section 5 rules those out as false confidence
 * about a medical decision. This only counts checks, and never claims eligibility.
 */
export function summariseTier1(checks: Tier1Check[]): {
    outcome: CheckOutcome;
    text: string;
} {
    const failed = checks.filter((c) => c.outcome === 'fail');
    const unknown = checks.filter((c) => c.outcome === 'unknown');

    if (failed.length > 0) {
        return {
            outcome: 'fail',
            text: `${failed.map((c) => c.label.toLowerCase()).join(', ')} ${failed.length === 1 ? 'does' : 'do'} not match`,
        };
    }
    if (unknown.length === checks.length) {
        return { outcome: 'unknown', text: 'not assessed' };
    }
    // Name what could not be checked rather than counting what could. A bare count ("2 of 3")
    // reads as a score, which section 5 rules out - and it hides which fact is missing.
    if (unknown.length > 0) {
        return {
            outcome: 'unknown',
            text: `${unknown.map((c) => c.label.toLowerCase()).join(', ')} not assessed`,
        };
    }
    return { outcome: 'pass', text: 'basic checks match' };
}
