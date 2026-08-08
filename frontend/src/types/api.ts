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
    appUserExtid: string;
    status: TrialStatusValue | string;
    notes?: string;
    statusChangedAt?: string;
}

export interface TrialStatusRequest {
    trialExtid: string;
    appUserExtid: string;
    status: string;
    notes?: string;
    statusChangedAt?: string;
}

export interface AppUser {
    extid: string;
    username: string;
    displayName?: string;
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
    /** Per-trial failures. The run continues past each one. */
    errors: string[];
}

export interface IngestionResult {
    studiesFetched: number;
    stagingRowsWritten: number;
    stagingRowsSkipped: number;
    pendingRowsProcessed: number;
    trialsNormalized: number;
    ingestErrors: string[];
    normalizationErrors: string[];
}

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
