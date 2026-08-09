import axios from 'axios';
import type {
    Trial,
    TrialRequest,
    TrialSource,
    TrialStatus,
    TrialStatusRequest,
    AppUser,
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
};

export const trialSourceApi = {
    getAll: () => apiClient.get<PageResponse<TrialSource>>('/trialsource', { params: { size: 100 } }),
};

export const trialStatusApi = {
    getAll: (params?: { page?: number; size?: number }) =>
        apiClient.get<PageResponse<TrialStatus>>('/trialstatus', { params: { size: 100, ...params } }),
    getByExtid: (extid: string) => apiClient.get<TrialStatus>(`/trialstatus/${extid}`),
    getByAppUserExtid: (appUserExtid: string) =>
        apiClient.get<TrialStatus[]>(`/trialstatus/by-appuser/${appUserExtid}`),
    create: (status: TrialStatusRequest) => apiClient.post<TrialStatus>('/trialstatus', status),
    update: (extid: string, status: Partial<TrialStatusRequest>) =>
        apiClient.put<TrialStatus>(`/trialstatus/${extid}`, status),
    delete: (extid: string) => apiClient.delete(`/trialstatus/${extid}`),
};

export const appUserApi = {
    getAll: () => apiClient.get<PageResponse<AppUser>>('/appuser', { params: { size: 100 } }),
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

// One diagnosis per patient in practice, so the page loads the app user's list and edits
// the first row rather than offering a list/detail flow.
export const patientDiagnosisApi = {
    getByAppUserExtid: (appUserExtid: string) =>
        apiClient.get<PatientDiagnosis[]>(`/patientdiagnosis/by-appuser/${appUserExtid}`),
    create: (diagnosis: PatientDiagnosisRequest) =>
        apiClient.post<PatientDiagnosis>('/patientdiagnosis', diagnosis),
    update: (extid: string, diagnosis: Partial<PatientDiagnosisRequest>) =>
        apiClient.put<PatientDiagnosis>(`/patientdiagnosis/${extid}`, diagnosis),
};

// One variant row per patient, same list-and-edit-the-first-row flow as the diagnosis.
export const patientVariantApi = {
    getByAppUserExtid: (appUserExtid: string) =>
        apiClient.get<PatientVariant[]>(`/patientvariant/by-appuser/${appUserExtid}`),
    create: (variant: PatientVariantRequest) =>
        apiClient.post<PatientVariant>('/patientvariant', variant),
    update: (extid: string, variant: Partial<PatientVariantRequest>) =>
        apiClient.put<PatientVariant>(`/patientvariant/${extid}`, variant),
};

// One prior-treatment row per patient, same flow again.
export const patientPriorTreatmentApi = {
    getByAppUserExtid: (appUserExtid: string) =>
        apiClient.get<PatientPriorTreatment[]>(`/patientpriortreatment/by-appuser/${appUserExtid}`),
    create: (treatment: PatientPriorTreatmentRequest) =>
        apiClient.post<PatientPriorTreatment>('/patientpriortreatment', treatment),
    update: (extid: string, treatment: Partial<PatientPriorTreatmentRequest>) =>
        apiClient.put<PatientPriorTreatment>(`/patientpriortreatment/${extid}`, treatment),
};

export const authApi = {
    login: (credentials: LoginRequest) => apiClient.post<AuthResponse>('/auth/login', credentials),
    register: (userData: RegisterRequest) => apiClient.post<AuthResponse>('/auth/register', userData),
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
};
