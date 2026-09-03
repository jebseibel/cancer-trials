import type { Patient, PatientDiagnosis, PatientVariant, PatientPriorTreatment } from '../types/api';
import { VARIANT_STATUS_LABELS, TREATMENT_STATUS_LABELS } from '../types/api';

/**
 * A plain-text copy of everything on file across the three patient-record tables, built entirely
 * from data already loaded on the page - no server round-trip, nothing sent anywhere.
 *
 * <p>Unlike the AI trial check's allowlist, this withholds nothing: it is the user's own record
 * going to their own device, so free text, exact dates and all fields are included as-is.
 *
 * <p><b>Absent means absent</b> - same rule as `diagnosisSummary.ts`. A field nobody filled in is
 * left off the page entirely rather than printed as "unknown", which would read as tested and
 * indeterminate rather than simply not asked.
 */

const DIAGNOSIS_LABELS: Record<string, string> = {
    cancerType: 'Cancer type',
    diagnosisDate: 'Diagnosis date',
    stage: 'Stage',
    stageSystem: 'Staging system',
    isMetastatic: 'Metastatic',
    metastasisSites: 'Metastasis sites',
    erStatus: 'ER status',
    prStatus: 'PR status',
    her2Status: 'HER2 status',
    receptorSubtype: 'Receptor subtype',
    biomarkers: 'Biomarkers',
    priorChemoRegimens: 'Prior chemo regimens',
    lastChemoEndDate: 'Last chemo end date',
    hasMeasurableDisease: 'Measurable disease',
    priorTreatments: 'Prior treatments',
    ecogStatus: 'ECOG performance status',
    menopausalStatus: 'Menopausal status',
    notes: 'Clinical notes',
};

// Order matters here - the same order the Diagnosis form presents them in.
const DIAGNOSIS_FIELDS: (keyof PatientDiagnosis)[] = [
    'cancerType',
    'diagnosisDate',
    'stage',
    'stageSystem',
    'isMetastatic',
    'metastasisSites',
    'erStatus',
    'prStatus',
    'her2Status',
    'receptorSubtype',
    'biomarkers',
    'priorChemoRegimens',
    'lastChemoEndDate',
    'hasMeasurableDisease',
    'priorTreatments',
    'ecogStatus',
    'menopausalStatus',
    'notes',
];

const GENE_LABELS: Record<string, string> = {
    pik3caStatus: 'PIK3CA',
    esr1Status: 'ESR1',
    akt1Status: 'AKT1',
    ptenStatus: 'PTEN',
    tp53Status: 'TP53',
    erbb2SomaticStatus: 'ERBB2 (somatic)',
    brca1Status: 'BRCA1',
    brca2Status: 'BRCA2',
    palb2Status: 'PALB2',
    atmStatus: 'ATM',
    chek2Status: 'CHEK2',
    hrdStatus: 'HRD',
    pdl1Status: 'PD-L1',
};

// Same order as the Variants form.
const GENE_FIELDS: (keyof PatientVariant)[] = [
    'pik3caStatus',
    'esr1Status',
    'akt1Status',
    'ptenStatus',
    'tp53Status',
    'erbb2SomaticStatus',
    'brca1Status',
    'brca2Status',
    'palb2Status',
    'atmStatus',
    'chek2Status',
    'hrdStatus',
    'pdl1Status',
];

const VARIANT_OTHER_LABELS: Record<string, string> = {
    somaticTestDone: 'Tumor sequencing (somatic)',
    germlineTestDone: 'Germline panel (inherited)',
    testDate: 'Test date',
    testLab: 'Testing lab',
    ki67Percent: 'Ki-67 (%)',
    otherVariants: 'Other variants',
    notes: 'Notes',
};

const VARIANT_OTHER_FIELDS: (keyof PatientVariant)[] = [
    'somaticTestDone',
    'germlineTestDone',
    'testDate',
    'testLab',
    'ki67Percent',
    'otherVariants',
    'notes',
];

const DRUG_LABELS: Record<string, string> = {
    cdk46Status: 'CDK4/6 inhibitor',
    endocrineStatus: 'Endocrine therapy',
    serdStatus: 'Oral SERD',
    pi3kAktMtorStatus: 'PI3K / AKT / mTOR inhibitor',
    her2TherapyStatus: 'HER2 antibody',
    her2AdcStatus: 'HER2 antibody-drug conjugate',
    trop2AdcStatus: 'TROP2 antibody-drug conjugate',
    chemoStatus: 'Chemotherapy (any)',
    taxaneStatus: 'Taxane',
    anthracyclineStatus: 'Anthracycline',
    platinumStatus: 'Platinum',
    parpStatus: 'PARP inhibitor',
    immunotherapyStatus: 'Immunotherapy',
};

// Same order as the Prior Treatment form.
const DRUG_FIELDS: (keyof PatientPriorTreatment)[] = [
    'cdk46Status',
    'endocrineStatus',
    'serdStatus',
    'pi3kAktMtorStatus',
    'her2TherapyStatus',
    'her2AdcStatus',
    'trop2AdcStatus',
    'chemoStatus',
    'taxaneStatus',
    'anthracyclineStatus',
    'platinumStatus',
    'parpStatus',
    'immunotherapyStatus',
];

const TREATMENT_OTHER_LABELS: Record<string, string> = {
    currentDrugNames: 'Taking now',
    priorDrugNames: 'Taken previously',
    linesOfTherapyMetastatic: 'Lines of therapy (metastatic)',
    currentlyOnTreatment: 'Currently on treatment',
    lastTreatmentEndDate: 'Last treatment ended',
    hadNeoadjuvant: 'Had neoadjuvant therapy',
    hadAdjuvant: 'Had adjuvant therapy',
    hadRadiation: 'Had radiation',
    hadSurgery: 'Had surgery',
    otherTreatments: 'Other treatments',
    notes: 'Notes',
};

const TREATMENT_OTHER_FIELDS: (keyof PatientPriorTreatment)[] = [
    'currentDrugNames',
    'priorDrugNames',
    'linesOfTherapyMetastatic',
    'currentlyOnTreatment',
    'lastTreatmentEndDate',
    'hadNeoadjuvant',
    'hadAdjuvant',
    'hadRadiation',
    'hadSurgery',
    'otherTreatments',
    'notes',
];

/** "Label: value", or nothing when the value is absent - never "Label: unknown". */
function line(label: string, value: unknown): string | null {
    if (value === undefined || value === null || value === '') {
        return null;
    }
    if (typeof value === 'boolean') {
        return `${label}: ${value ? 'Yes' : 'No'}`;
    }
    return `${label}: ${value}`;
}

/** Every field in `fields`, rendered "Label: value" and filtered down to the ones with a value. */
function fieldLines<T extends object>(
    row: T,
    fields: (keyof T)[],
    labels: Record<string, string>,
    statusLabels?: Record<string, string>,
): string[] {
    return fields
        .map((field) => {
            const raw = row[field];
            const label = labels[field as string];
            const value = statusLabels && typeof raw === 'string' ? statusLabels[raw] ?? raw : raw;
            return line(label, value);
        })
        .filter((l): l is string => l !== null);
}

export function buildPatientRecordText(
    // `patient` is accepted for symmetry with the record this page shows and to leave room for
    // a header line (name, DOB) later - not read yet, since PatientRecord.tsx does not surface
    // those fields today either.
    _patient: Patient | null | undefined,
    diagnosis: PatientDiagnosis | null | undefined,
    variant: PatientVariant | null | undefined,
    priorTreatment: PatientPriorTreatment | null | undefined,
): string {
    const generated = new Date().toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
    });

    const parts: string[] = [`Breast Cancer Trial Finder — Patient Record`, `Generated ${generated}`, ''];

    if (diagnosis) {
        const lines = fieldLines(diagnosis, DIAGNOSIS_FIELDS, DIAGNOSIS_LABELS);
        if (lines.length > 0) {
            parts.push('DIAGNOSIS', ...lines, '');
        }
    }

    if (variant) {
        const lines = [
            ...fieldLines(variant, GENE_FIELDS, GENE_LABELS, VARIANT_STATUS_LABELS),
            ...fieldLines(variant, VARIANT_OTHER_FIELDS, VARIANT_OTHER_LABELS),
        ];
        if (lines.length > 0) {
            parts.push('VARIANTS', ...lines, '');
        }
    }

    if (priorTreatment) {
        const lines = [
            ...fieldLines(priorTreatment, DRUG_FIELDS, DRUG_LABELS, TREATMENT_STATUS_LABELS),
            ...fieldLines(priorTreatment, TREATMENT_OTHER_FIELDS, TREATMENT_OTHER_LABELS),
        ];
        if (lines.length > 0) {
            parts.push('PRIOR TREATMENT', ...lines, '');
        }
    }

    return parts.join('\n').trimEnd() + '\n';
}
