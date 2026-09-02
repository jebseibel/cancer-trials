import axios from 'axios';
import type {
    Trial,
    TrialRequest,
    TrialSource,
    TrialStatus,
    TrialStatusRequest,
    Patient,
    PatientAccess,
    Location,
    ArmGroup,
    Intervention,
    Outcome,
    OverallOfficial,
    Condition,
    Sponsor,
    LoginRequest,
    RegisterRequest,
    AuthResponse,
    PageResponse,
    IngestionRequest,
    IngestionResult,
    BackfillResult,
    PatientDiagnosis,
    PatientDiagnosisRequest,
    PatientVariant,
    PatientVariantRequest,
    PatientPriorTreatment,
    PatientPriorTreatmentRequest,
    TrialAssessment,
    TrialSearchMatch,
    TreatmentGoalBackfillResult,
    FriendlyTitleBackfillResult,
    AiTrialCheck,
    AiStatus,
    DiagnosisIntakeSession,
    DiagnosisIntakeStartRequest,
    DiagnosisIntakeAnswerRequest,
} from '../types/api';

// API Configuration
// Use relative path for production, localhost for development
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Axios interceptor to attach JWT token to requests
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Axios interceptor to handle 403 responses and redirect to login
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        // Don't redirect on auth endpoints (login/register) - let the form handle errors
        const isAuthEndpoint = error.config?.url?.includes('/auth/');

        if ((error.response?.status === 403 || error.response?.status === 401) && !isAuthEndpoint) {
            // Clear invalid token
            localStorage.removeItem('token');
            // Redirect to login page
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export interface TrialSearchParams {
    page?: number;
    size?: number;
    sort?: string;
    active?: string;
}

// API Endpoints
export const trialApi = {
    getAll: (params?: TrialSearchParams) => apiClient.get<PageResponse<Trial>>('/trial', { params }),
    getByExtid: (extid: string) => apiClient.get<Trial>(`/trial/${extid}`),
    create: (trial: TrialRequest) => apiClient.post<Trial>('/trial', trial),
    update: (extid: string, trial: Partial<TrialRequest>) => apiClient.put<Trial>(`/trial/${extid}`, trial),
    delete: (extid: string) => apiClient.delete(`/trial/${extid}`),
    // Asks a model to rewrite this trial's title in plain language and stores it, always
    // overwriting whatever was there - a deliberate single press, the same contract as the AI
    // trial check's "Check again". Trial-only: no patient data is read or sent.
    generateFriendlyTitle: (extid: string) =>
        apiClient.post<Trial>(`/trial/${extid}/generate-friendly-title`),
};

// Semantic search over trial text, as opposed to trialApi.getAll's substring filtering.
//
// criteriaOnly restricts matching to eligibility criteria, dropping summaries, descriptions,
// interventions and outcomes. Measured 2026-08-21: on a whole-profile query 15 of the top 25
// hits were trial-design prose ("first-in-human, open-label, phase I/Ib...") repeated across
// unrelated trials, crowding out the criteria that decide whether someone qualifies.
//
// Off by default on purpose - prose is the right answer to "what is this trial testing", so
// the restriction is the caller's explicit choice, never a silent one.
export const ragSearchApi = {
    search: (params: {
        query: string;
        maxTrials?: number;
        recruitingOnly?: boolean;
        excludeExclusionCriteria?: boolean;
        criteriaOnly?: boolean;
        similarityThreshold?: number;
    }) => apiClient.get<TrialSearchMatch[]>('/rag/search', { params }),
};

// Ranks the whole corpus against the patient record already on file, so nobody has to know
// what to type into a search box. Slow by nature - it assesses thousands of trials per call.
export const matchingApi = {
    rank: (patientExtid: string, params?: { breastOnly?: boolean; limit?: number }) =>
        apiClient.get<TrialAssessment[]>(`/matching/rank/${patientExtid}`, { params }),
    assessTrial: (trialExtid: string, patientExtid: string) =>
        apiClient.get<TrialAssessment>(`/matching/trial/${trialExtid}/for/${patientExtid}`),
    // Re-derives the treatment-goal column for every trial. Needed because ingestion skips
    // trials whose ClinicalTrials.gov payload has not changed, so a re-pull cannot pick up a
    // change to the code that reads that payload. ADMIN-only.
    // ⚠️ The only call that sends clinical text off the machine. The backend builds a
    // de-identified payload from an explicit allowlist - no name, no date of birth, no
    // free-text notes, dates coarsened to a year.
    aiCheck: (trialExtid: string, patientExtid: string) =>
        apiClient.post<AiTrialCheck>(`/matching/ai/trial/${trialExtid}/for/${patientExtid}`),
    // The stored reading, if there is one. 204 when this trial has never been checked.
    latestAiCheck: (trialExtid: string, patientExtid: string) =>
        apiClient.get<AiTrialCheck | ''>(`/matching/ai/trial/${trialExtid}/for/${patientExtid}`),
    aiStatus: () => apiClient.get<AiStatus>('/matching/ai/status'),
    backfillTreatmentGoals: () =>
        apiClient.post<TreatmentGoalBackfillResult>('/matching/backfill-treatment-goals'),
    // Generates a friendly title for every trial that does not have one yet. Unlike the
    // treatment-goal backfill this is not free - it is a paid AI call per trial missing a
    // title - so it skips trials that already have one rather than re-checking them. ADMIN-only.
    backfillFriendlyTitles: () =>
        apiClient.post<FriendlyTitleBackfillResult>('/matching/backfill-friendly-titles'),
};

export const trialSourceApi = {
    getAll: () => apiClient.get<PageResponse<TrialSource>>('/trialsource', { params: { size: 100 } }),
};

export const trialStatusApi = {
    getAll: (params?: { page?: number; size?: number }) =>
        apiClient.get<PageResponse<TrialStatus>>('/trialstatus', { params: { size: 100, ...params } }),
    getByExtid: (extid: string) => apiClient.get<TrialStatus>(`/trialstatus/${extid}`),
    getByPatientExtid: (patientExtid: string) =>
        apiClient.get<TrialStatus[]>(`/trialstatus/by-patient/${patientExtid}`),
    create: (status: TrialStatusRequest) => apiClient.post<TrialStatus>('/trialstatus', status),
    update: (extid: string, status: Partial<TrialStatusRequest>) =>
        apiClient.put<TrialStatus>(`/trialstatus/${extid}`, status),
    delete: (extid: string) => apiClient.delete(`/trialstatus/${extid}`),
};

/**
 * Patients the signed-in user may see.
 *
 * `/mine` names nobody - the server resolves the caller from the token - so there is no extid
 * for a caller to substitute. It replaces fetching every app_user and filtering by username
 * client-side, which returned other people's rows to the browser to find one.
 */
export const patientApi = {
    mine: () => apiClient.get<PatientAccess[]>('/patient/mine'),
    getByExtid: (patientExtid: string) => apiClient.get<Patient>(`/patient/${patientExtid}`),
    create: (patient: Partial<Patient>) => apiClient.post<Patient>('/patient', patient),
    update: (patientExtid: string, patient: Partial<Patient>) =>
        apiClient.put<Patient>(`/patient/${patientExtid}`, patient),
};

export const locationApi = {
    getByTrialExtid: (trialExtid: string) => apiClient.get<Location[]>(`/location/by-trial/${trialExtid}`),
};

export const armGroupApi = {
    getByTrialExtid: (trialExtid: string) => apiClient.get<ArmGroup[]>(`/armgroup/by-trial/${trialExtid}`),
};

export const interventionApi = {
    getByTrialExtid: (trialExtid: string) =>
        apiClient.get<Intervention[]>(`/intervention/by-trial/${trialExtid}`),
};

export const outcomeApi = {
    getByTrialExtid: (trialExtid: string) => apiClient.get<Outcome[]>(`/outcome/by-trial/${trialExtid}`),
};

export const overallOfficialApi = {
    getByTrialExtid: (trialExtid: string) =>
        apiClient.get<OverallOfficial[]>(`/overallofficial/by-trial/${trialExtid}`),
};

export const conditionApi = {
    getAll: () => apiClient.get<PageResponse<Condition>>('/condition', { params: { size: 100 } }),
};

export const sponsorApi = {
    getAll: () => apiClient.get<PageResponse<Sponsor>>('/sponsor', { params: { size: 100 } }),
};

export const ingestionApi = {
    runClinicalTrials: (request: IngestionRequest) =>
        apiClient.post<IngestionResult>('/ingestion/clinicaltrials', request),
};

// Vector-store indexing. Deliberately separate from ingestion: ingestion writes to MySQL,
// backfill chunks and embeds those trials so they become searchable. Ingesting alone leaves
// search returning nothing for the new trials.
export const ragApi = {
    /** Chunk, embed, and index trials already in the database. Idempotent - safe to re-run. */
    backfill: () => apiClient.post<BackfillResult>('/rag/backfill'),
    /** Re-index a single trial by extid. */
    reindexTrial: (trialExtid: string) =>
        apiClient.post<BackfillResult>(`/rag/reindex/${trialExtid}`),
};

// One diagnosis per patient in practice, so the page loads the patient's list and edits
// the first row rather than offering a list/detail flow.
export const patientDiagnosisApi = {
    getByPatientExtid: (patientExtid: string) =>
        apiClient.get<PatientDiagnosis[]>(`/patientdiagnosis/by-patient/${patientExtid}`),
    create: (diagnosis: PatientDiagnosisRequest) =>
        apiClient.post<PatientDiagnosis>('/patientdiagnosis', diagnosis),
    update: (extid: string, diagnosis: Partial<PatientDiagnosisRequest>) =>
        apiClient.put<PatientDiagnosis>(`/patientdiagnosis/${extid}`, diagnosis),
};

// One variant row per patient, same list-and-edit-the-first-row flow as the diagnosis.
export const patientVariantApi = {
    getByPatientExtid: (patientExtid: string) =>
        apiClient.get<PatientVariant[]>(`/patientvariant/by-patient/${patientExtid}`),
    create: (variant: PatientVariantRequest) =>
        apiClient.post<PatientVariant>('/patientvariant', variant),
    update: (extid: string, variant: Partial<PatientVariantRequest>) =>
        apiClient.put<PatientVariant>(`/patientvariant/${extid}`, variant),
};

// One prior-treatment row per patient, same flow again.
export const patientPriorTreatmentApi = {
    getByPatientExtid: (patientExtid: string) =>
        apiClient.get<PatientPriorTreatment[]>(`/patientpriortreatment/by-patient/${patientExtid}`),
    create: (treatment: PatientPriorTreatmentRequest) =>
        apiClient.post<PatientPriorTreatment>('/patientpriortreatment', treatment),
    update: (extid: string, treatment: Partial<PatientPriorTreatmentRequest>) =>
        apiClient.put<PatientPriorTreatment>(`/patientpriortreatment/${extid}`, treatment),
};

// AI-assisted document intake: paste/upload text, get a draft to review across the three
// diagnosis-adjacent tables. Nothing here is persisted server-side - the session lives only in
// backend memory for the life of the conversation.
export const diagnosisIntakeApi = {
    start: (request: DiagnosisIntakeStartRequest) =>
        apiClient.post<DiagnosisIntakeSession>('/diagnosisintake/start', request),
    answer: (sessionId: string, request: DiagnosisIntakeAnswerRequest) =>
        apiClient.post<DiagnosisIntakeSession>(`/diagnosisintake/${sessionId}/answer`, request),
    skip: (sessionId: string) =>
        apiClient.post<DiagnosisIntakeSession>(`/diagnosisintake/${sessionId}/skip`),
    cancel: (sessionId: string) => apiClient.delete<void>(`/diagnosisintake/${sessionId}`),
};

export const authApi = {
    login: (credentials: LoginRequest) => apiClient.post<AuthResponse>('/auth/login', credentials),
    register: (userData: RegisterRequest) => apiClient.post<AuthResponse>('/auth/register', userData),
};

// The signed-in user's own account. Distinct from authApi: these calls require an existing
// token rather than producing one.
export const accountApi = {
    changePassword: (currentPassword: string, newPassword: string) =>
        apiClient.post<string>('/auth/change-password', { currentPassword, newPassword }),
};

// Auth helper functions
export const authHelpers = {
    saveToken: (token: string) => localStorage.setItem('token', token),
    getToken: () => localStorage.getItem('token'),
    removeToken: () => localStorage.removeItem('token'),
    isAuthenticated: () => !!localStorage.getItem('token'),
    saveUsername: (username: string) => localStorage.setItem('username', username),
    getUsername: () => localStorage.getItem('username'),
    removeUsername: () => localStorage.removeItem('username'),
    // A UI hint only. It lives in localStorage, so a user can edit it and reveal a hidden
    // menu item - the backend refuses the call regardless. The frontend must never be the
    // only thing standing between a user and a capability.
    saveRole: (role: string) => localStorage.setItem('role', role),
    getRole: () => localStorage.getItem('role'),
    removeRole: () => localStorage.removeItem('role'),
    isAdmin: () => localStorage.getItem('role') === 'ADMIN',
};
