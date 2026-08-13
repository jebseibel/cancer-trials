// Authentication (login accounts, distinct from AppUser personal-tracking profiles)
export interface User {
    extid: string;
    username: string;
    email?: string;
    role: string;
}

export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    password: string;
    email?: string;
}

export interface AuthResponse {
    token: string;
    username: string;
    email?: string;
    role: string;
}

// Trial
export interface Trial {
    extid: string;
    nctId?: string;
    briefTitle: string;
    officialTitle?: string;
    overallStatus?: string;
    studyType?: string;
    briefSummary?: string;
    detailedDescription?: string;
    startDate?: string;
    primaryCompletionDate?: string;
    completionDate?: string;
    lastUpdatePostedDate?: string;
    enrollmentCount?: number;
    enrollmentType?: string;
    healthyVolunteers?: boolean;
    sex?: string;
    minimumAge?: string;
    maximumAge?: string;
    eligibilityCriteria?: string;
    isPaidStudy?: boolean;
    paidAmount?: number;
    primaryTrialSourceId: number;
}

export interface TrialRequest {
    nctId?: string;
    briefTitle: string;
    officialTitle?: string;
    overallStatus?: string;
    studyType?: string;
    briefSummary?: string;
    detailedDescription?: string;
    startDate?: string;
    primaryCompletionDate?: string;
    completionDate?: string;
    lastUpdatePostedDate?: string;
    enrollmentCount?: number;
    enrollmentType?: string;
    healthyVolunteers?: boolean;
    sex?: string;
    minimumAge?: string;
    maximumAge?: string;
    eligibilityCriteria?: string;
    isPaidStudy?: boolean;
    paidAmount?: number;
    primaryTrialSourceId: number;
}

export interface TrialSource {
    extid: string;
    code: string;
    name: string;
    baseUrl?: string;
}

// Personal tracking
export const TRIAL_STATUS_VALUES = ['SAVED', 'INTERESTED', 'CONTACTED', 'RULED_OUT', 'ENROLLED'] as const;
export type TrialStatusValue = (typeof TRIAL_STATUS_VALUES)[number];

export interface TrialStatus {
    extid: string;
    trialExtid: string;
    patientExtid: string;
    status: TrialStatusValue | string;
    notes?: string;
    statusChangedAt?: string;
}

export interface TrialStatusRequest {
    trialExtid: string;
    patientExtid: string;
    status: string;
    notes?: string;
    statusChangedAt?: string;
}

/** What one login may do with one patient's record. Ranked: OWNER covers every check. */
export const ACCESS_LEVELS = ['VIEW_TRIALS', 'VIEW_RECORD', 'EDIT_RECORD', 'OWNER'] as const;
export type AccessLevel = (typeof ACCESS_LEVELS)[number];

/** A person with a medical record. Replaces AppUser, which was a second login table. */
export interface Patient {
    extid: string;
    /** Short label for the switcher and page headers. */
    displayName: string;
    /** The name the clinic holds. */
    fullName?: string;
    dateOfBirth?: string;
    sex?: string;
    notes?: string;
}

/**
 * A patient plus the level the signed-in user holds on it.
 *
 * The level is what lets the UI decide between an editable form and a read-only view without
 * discovering a refusal by attempting a save and failing.
 */
export interface PatientAccess extends Patient {
    accessLevel: AccessLevel;
}

// Trial child records (linked by trial extid, fetched via /by-trial/{trialExtid})
export interface Location {
    extid: string;
    trialExtid: string;
    facility?: string;
    city?: string;
    state?: string;
    zip?: string;
    country?: string;
    status?: string;
    latitude?: number;
    longitude?: number;
}

export interface ArmGroup {
    extid: string;
    trialExtid: string;
    label: string;
    type?: string;
    description?: string;
}

export interface Intervention {
    extid: string;
    trialExtid: string;
    type?: string;
    name: string;
    description?: string;
}

export interface Outcome {
    extid: string;
    trialExtid: string;
    outcomeType: string;
    measure: string;
    description?: string;
    timeFrame?: string;
}

export interface OverallOfficial {
    extid: string;
    trialExtid: string;
    name: string;
    affiliation?: string;
    role?: string;
}

// EligibilityRule intentionally omitted - not yet converted to extid-based
// parentRuleId/criterionId; design still open per clinical-trials-tables.md.

// Lookup entities (not yet linked to Trial via join tables - see PROJECT_PLAN.md)
export interface Condition {
    extid: string;
    name: string;
}

export interface Sponsor {
    extid: string;
    name: string;
    orgClass?: string;
}

// Ingestion (on-demand ClinicalTrials.gov fetch + normalize)
// Every field is optional - omitting one falls back to the backend's configured default under
// cancer.ingestion.clinicaltrials.* (condition "cancer", overallStatus RECRUITING,
// maxStudies 1000).
export interface IngestionRequest {
    condition?: string;
    term?: string;
    location?: string;
    /** CT.gov filter.overallStatus. Send 'ALL' to clear the filter and pull every status. */
    overallStatus?: string;
    maxStudies?: number;
}

/** CT.gov overall-status values worth exposing, plus the explicit opt-out. */
export const OVERALL_STATUS_OPTIONS = [
    { value: 'RECRUITING', label: 'Recruiting' },
    { value: 'NOT_YET_RECRUITING', label: 'Not yet recruiting' },
    { value: 'ACTIVE_NOT_RECRUITING', label: 'Active, not recruiting' },
    { value: 'ENROLLING_BY_INVITATION', label: 'Enrolling by invitation' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'TERMINATED', label: 'Terminated' },
    { value: 'ALL', label: 'All statuses' },
] as const;

// RAG vector-store indexing. Separate from ingestion on purpose: ingestion writes MySQL,
// backfill makes those trials searchable. See FRONTEND_JOB_TRIGGER_PLAN.md.
export interface BackfillResult {
    /** Trials that produced at least one chunk. */
    trialsIndexed: number;
    /** Total chunks embedded and written to the vector store. */
    chunksWritten: number;
    /** Trials that produced no chunks - e.g. no eligibility text. */
    trialsSkipped: number;
    /** Trials left alone because the vector store already held chunks for them. */
    trialsAlreadyIndexed: number;
    /** Per-trial failures. The run continues past each one. */
    errors: string[];
}

export interface IngestionResult {
    studiesFetched: number;
    stagingRowsWritten: number;
    stagingRowsSkipped: number;
    stagingRowsUnchanged: number;
    pendingRowsProcessed: number;
    trialsNormalized: number;
    ingestErrors: string[];
    normalizationErrors: string[];
}

// Patient diagnosis - one row per patient, compared against trial eligibility criteria.
// Vocabularies below mirror the column comments in DIAGNOSIS_MATCHING_DESIGN.md; the backend
// stores them as plain varchars, so these constrain the UI only.
export const STAGE_VALUES = ['I', 'IA', 'IB', 'II', 'IIA', 'IIB', 'III', 'IIIA', 'IIIB', 'IIIC', 'IV'] as const;
export const STAGE_SYSTEM_VALUES = ['AJCC_8', 'AJCC_7'] as const;
export const RECEPTOR_STATUS_VALUES = ['POSITIVE', 'NEGATIVE', 'UNKNOWN'] as const;
export const RECEPTOR_SUBTYPE_VALUES = [
    'HR_POSITIVE_HER2_NEGATIVE',
    'HR_POSITIVE_HER2_POSITIVE',
    'HER2_POSITIVE',
    'TRIPLE_NEGATIVE',
] as const;
export const MENOPAUSAL_STATUS_VALUES = ['PRE', 'PERI', 'POST', 'UNKNOWN'] as const;
export const SEX_VALUES = ['FEMALE', 'MALE'] as const;
export const ECOG_VALUES = [0, 1, 2, 3, 4] as const;

export type ReceptorStatus = (typeof RECEPTOR_STATUS_VALUES)[number];

export interface PatientDiagnosis {
    extid: string;
    patientExtid?: string;
    cancerType: string;
    stage?: string;
    stageSystem?: string;
    isMetastatic?: boolean;
    metastasisSites?: string;
    receptorSubtype?: string;
    erStatus?: string;
    prStatus?: string;
    her2Status?: string;
    biomarkers?: string;
    ecogStatus?: number;
    priorChemoRegimens?: number;
    lastChemoEndDate?: string;
    priorTreatments?: string;
    hasMeasurableDisease?: boolean;
    menopausalStatus?: string;
    diagnosisDate?: string;
    notes?: string;
}

// Same shape minus the server-assigned extid. Every field is optional except cancerType,
// which the backend marks @NotEmpty.
export type PatientDiagnosisRequest = Omit<PatientDiagnosis, 'extid'>;

// Patient variants - molecular and germline findings, one row per patient.
// See .claude/diagnosis/patient-variant-and-treatment-tables.md.
//
// NOT_TESTED is a distinct state from NOT_DETECTED and the distinction is load-bearing:
// "tested negative for BRCA1" may rule a trial out, while "never tested" leaves it an open
// question worth asking about. Collapsing them either hides an option or invents one.
export const VARIANT_STATUS_VALUES = [
    'DETECTED',
    'NOT_DETECTED',
    'VUS',
    'NOT_TESTED',
    'UNKNOWN',
] as const;

export const VARIANT_STATUS_LABELS: Record<string, string> = {
    DETECTED: 'Detected',
    NOT_DETECTED: 'Not detected',
    VUS: 'Uncertain significance (VUS)',
    NOT_TESTED: 'Not tested',
    UNKNOWN: 'Not sure',
};

export type VariantStatus = (typeof VARIANT_STATUS_VALUES)[number];

export interface PatientVariant {
    extid: string;
    patientExtid?: string;
    patientDiagnosisExtid?: string;
    pik3caStatus?: string;
    esr1Status?: string;
    tp53Status?: string;
    akt1Status?: string;
    ptenStatus?: string;
    /** Somatic ERBB2 mutation - a different test from HER2 receptor status. */
    erbb2SomaticStatus?: string;
    brca1Status?: string;
    brca2Status?: string;
    palb2Status?: string;
    atmStatus?: string;
    chek2Status?: string;
    hrdStatus?: string;
    pdl1Status?: string;
    ki67Percent?: number;
    germlineTestDone?: string;
    somaticTestDone?: string;
    testDate?: string;
    testLab?: string;
    otherVariants?: string;
    notes?: string;
}

export type PatientVariantRequest = Omit<PatientVariant, 'extid'>;

// Patient prior treatment - drug-class exposure, one row per patient.
//
// Five states rather than a checkbox because trials split into treatment-naive and
// post-progression populations: "has taken a CDK4/6 inhibitor" is true of both a patient
// currently on one and a patient who progressed off one, and they qualify for opposite
// cohorts.
export const TREATMENT_STATUS_VALUES = [
    'NEVER',
    'CURRENT',
    'PROGRESSED',
    'STOPPED_OTHER',
    'UNKNOWN',
] as const;

export const TREATMENT_STATUS_LABELS: Record<string, string> = {
    NEVER: 'Never taken',
    CURRENT: 'Taking now',
    PROGRESSED: 'Stopped - it stopped working',
    STOPPED_OTHER: 'Stopped - other reason',
    UNKNOWN: 'Not sure',
};

export type TreatmentStatus = (typeof TREATMENT_STATUS_VALUES)[number];

export interface PatientPriorTreatment {
    extid: string;
    patientExtid?: string;
    patientDiagnosisExtid?: string;
    cdk46Status?: string;
    endocrineStatus?: string;
    serdStatus?: string;
    chemoStatus?: string;
    her2TherapyStatus?: string;
    her2AdcStatus?: string;
    trop2AdcStatus?: string;
    parpStatus?: string;
    pi3kAktMtorStatus?: string;
    immunotherapyStatus?: string;
    taxaneStatus?: string;
    anthracyclineStatus?: string;
    platinumStatus?: string;
    currentDrugNames?: string;
    priorDrugNames?: string;
    linesOfTherapyMetastatic?: number;
    hadNeoadjuvant?: boolean;
    hadAdjuvant?: boolean;
    hadRadiation?: boolean;
    hadSurgery?: boolean;
    lastTreatmentEndDate?: string;
    currentlyOnTreatment?: boolean;
    otherTreatments?: string;
    notes?: string;
}

export type PatientPriorTreatmentRequest = Omit<PatientPriorTreatment, 'extid'>;

// Pagination
export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    pageable: {
        offset: number;
        pageNumber: number;
        pageSize: number;
        paged: boolean;
        unpaged: boolean;
    };
    last: boolean;
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}

// ---- Trial matching (Rank Trials) ----

export const SIGNAL_OUTCOME_VALUES = ['PASS', 'CONCERN', 'UNKNOWN', 'NOT_APPLICABLE'] as const;
export type SignalOutcome = (typeof SIGNAL_OUTCOME_VALUES)[number];

// `evidence` is the quoted criteria text that produced the signal. It is what lets a reader
// judge the reasoning instead of trusting it - a flag without it is an unexplained verdict.
export interface EligibilitySignal {
    name: string;
    outcome: SignalOutcome;
    detail: string;
    evidence?: string | null;
}

// Deliberately no fit score or percentage. The counts are what the backend can state
// honestly; a number that looks like a probability invites reliance this tool must not earn.
export interface TrialAssessment {
    trialExtid: string;
    nctId: string;
    briefTitle?: string | null;
    overallStatus?: string | null;
    signals: EligibilitySignal[];
    concernCount: number;
    unknownCount: number;
    passCount: number;
    applicableCount: number;
    breastCancer: boolean;
    // Where the trial runs. Travel decides whether a trial is reachable at all, so these are
    // first-class fields rather than something to parse out of the location signal's sentence.
    siteCities: string[];
    siteCount: number;
    hasUnitedStatesSite: boolean;
}
