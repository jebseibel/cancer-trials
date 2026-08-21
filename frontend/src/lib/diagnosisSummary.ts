import type { PatientDiagnosis, PatientVariant } from '../types/api';

/**
 * The one-line clinical picture: what an oncologist would say in a sentence.
 *
 * <p>Built from fields that are already on the page rather than stored, so it cannot drift from
 * the forms below it. Nothing here is new information — it is the same record read in the order
 * a clinician thinks about it: what the cancer is, where it has spread, what drives it, and how
 * well she is.
 *
 * <p><b>Absent means absent.</b> A field that is not recorded contributes nothing rather than
 * "unknown" — a line of the word "unknown" repeated is noise, and it would also read as though
 * something had been tested and come back indeterminate. The same distinction the five-state
 * vocabularies exist to preserve.
 */

/** Genes worth naming in a summary, in the order they matter for this disease. */
const SUMMARY_GENES: Array<{ key: keyof PatientVariant; label: string }> = [
    { key: 'pik3caStatus', label: 'PIK3CA' },
    { key: 'esr1Status', label: 'ESR1' },
    { key: 'akt1Status', label: 'AKT1' },
    { key: 'ptenStatus', label: 'PTEN' },
    { key: 'brca1Status', label: 'BRCA1' },
    { key: 'brca2Status', label: 'BRCA2' },
    { key: 'palb2Status', label: 'PALB2' },
    { key: 'atmStatus', label: 'ATM' },
    { key: 'chek2Status', label: 'CHEK2' },
    { key: 'tp53Status', label: 'TP53' },
    { key: 'erbb2SomaticStatus', label: 'ERBB2' },
];

const MENOPAUSAL_LABELS: Record<string, string> = {
    PRE: 'premenopausal',
    PERI: 'perimenopausal',
    POST: 'postmenopausal',
};

/** ER+ / PR− / HER2−, using the symbols a pathology report uses. */
function receptorLine(diagnosis: PatientDiagnosis): string | null {
    const symbol = (status?: string) =>
        status === 'POSITIVE' ? '+' : status === 'NEGATIVE' ? '−' : null;

    const parts = [
        ['ER', symbol(diagnosis.erStatus)],
        ['PR', symbol(diagnosis.prStatus)],
        ['HER2', symbol(diagnosis.her2Status)],
    ].filter(([, s]) => s !== null);

    return parts.length === 0 ? null : parts.map(([name, s]) => `${name}${s}`).join(' / ');
}

/**
 * Genes that were tested and found, with VUS kept distinct.
 *
 * <p>A variant of uncertain significance is not a detected driver and not a negative result, and
 * a trial's criteria can turn on the difference — so it is labelled rather than folded in.
 * NOT_TESTED and NOT_DETECTED contribute nothing: neither is a finding.
 */
export function summaryVariants(variant?: PatientVariant | null): string[] {
    if (!variant) {
        return [];
    }
    return SUMMARY_GENES.flatMap(({ key, label }) => {
        const status = variant[key];
        if (status === 'DETECTED') return [label];
        if (status === 'VUS') return [`${label} (uncertain)`];
        return [];
    });
}

/**
 * The summary line, or null when there is not enough recorded to say anything.
 *
 * <p>Null rather than a placeholder: a page for someone who has not filled in their record yet
 * should invite them to, not show them an empty sentence.
 */
export function buildDiagnosisSummary(
    diagnosis?: PatientDiagnosis | null,
    variant?: PatientVariant | null,
): string | null {
    if (!diagnosis) {
        return null;
    }

    const segments: string[] = [];

    // What it is, and how far along - "Stage IV invasive ductal carcinoma".
    const stage = diagnosis.stage ? `Stage ${diagnosis.stage}` : null;
    const type = diagnosis.cancerType?.trim() || null;
    const headline = [stage, type].filter(Boolean).join(' ');
    if (headline) {
        segments.push(headline);
    }

    // Where it has spread. Only when it has - "no metastases" is a different claim from a
    // record that simply does not say.
    if (diagnosis.isMetastatic && diagnosis.metastasisSites?.trim()) {
        segments.push(`spread to ${diagnosis.metastasisSites.trim()}`);
    } else if (diagnosis.isMetastatic) {
        segments.push('metastatic');
    }

    const receptors = receptorLine(diagnosis);
    if (receptors) {
        segments.push(receptors);
    }

    const variants = summaryVariants(variant);
    if (variants.length > 0) {
        segments.push(variants.join(', '));
    }

    const menopausal = diagnosis.menopausalStatus
        ? MENOPAUSAL_LABELS[diagnosis.menopausalStatus]
        : null;
    if (menopausal) {
        segments.push(menopausal);
    }

    // Performance status is a number a clinician reads instantly and nobody else does, so it is
    // labelled rather than left bare.
    if (diagnosis.ecogStatus !== undefined && diagnosis.ecogStatus !== null) {
        segments.push(`ECOG ${diagnosis.ecogStatus}`);
    }

    return segments.length === 0 ? null : segments.join(' · ');
}
