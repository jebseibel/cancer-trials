/**
 * Thin HTTP client over the existing Spring Boot REST API. No business logic lives here —
 * every tool handler calls one of these functions and shapes the result for the model.
 *
 * The bearer token is per-instance, not a module-level constant: the stdio entry point (one
 * user, one env var) and the HTTP entry point (one JWT per connected session, forwarded from
 * that caller's own Authorization header) both need their own token, and instances must never
 * share one another's — that is the whole point of Phase 2's per-user auth.
 */

const BASE_URL = process.env.CANCER_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: string,
  ) {
    super(`Cancer API returned ${status}: ${body}`);
  }
}

// --- Response shapes (trimmed to the fields tools actually use) ---

export interface EligibilitySignal {
  name: string;
  outcome: "PASS" | "CONCERN" | "UNKNOWN" | "NOT_APPLICABLE";
  detail: string;
  evidence: string;
}

export interface TrialAssessment {
  trialExtid: string;
  nctId: string;
  briefTitle: string;
  overallStatus: string;
  signals: EligibilitySignal[];
  concernCount: number;
  unknownCount: number;
  passCount: number;
  applicableCount: number;
  breastCancer: boolean;
  siteCities: string[];
  siteCount: number;
  hasUnitedStatesSite: boolean;
}

export interface AiTrialCheck {
  rulesPatientOut: boolean | null;
  exclusionCriterion: string | null;
  summary: string;
  criteriaSheAppearsToMeet: string[];
  openQuestions: string[];
  concerns: string[];
  model: string;
  assessedAt: string;
}

export interface TrialSearchMatch {
  trialExtid: string;
  nctId: string;
  briefTitle: string;
  overallStatus: string;
  topScore: number;
  matches: Array<{ text: string; source: string; ordinal: number; score: number; isExclusion: boolean }>;
}

export interface Trial {
  extid: string;
  nctId: string;
  briefTitle: string;
  officialTitle: string;
  overallStatus: string;
  studyType: string;
  treatmentGoal: string;
  diseaseStage: string;
  siteCount: number | null;
  hasUnitedStatesSite: boolean | null;
  briefSummary: string;
  eligibilityCriteria: string;
  sex: string;
  minimumAge: string;
  maximumAge: string;
}

interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

/**
 * One instance per caller identity. Construct with the JWT that identifies whoever is
 * actually asking — never a shared/global token once more than one person can connect.
 */
export class CancerApiClient {
  constructor(private readonly token: string | undefined) {}

  private async request<T>(path: string, params?: Record<string, string | number | boolean | undefined>): Promise<T> {
    const url = new URL(path, BASE_URL);
    if (params) {
      for (const [key, value] of Object.entries(params)) {
        if (value !== undefined) url.searchParams.set(key, String(value));
      }
    }

    const headers: Record<string, string> = { Accept: "application/json" };
    if (this.token) headers.Authorization = `Bearer ${this.token}`;

    const res = await fetch(url, { method: "GET", headers });
    return this.handleResponse<T>(res);
  }

  private async post<T>(path: string): Promise<T> {
    const headers: Record<string, string> = { Accept: "application/json" };
    if (this.token) headers.Authorization = `Bearer ${this.token}`;

    const res = await fetch(new URL(path, BASE_URL), { method: "POST", headers });
    return this.handleResponse<T>(res);
  }

  private async handleResponse<T>(res: Response): Promise<T> {
    if (res.status === 204) return null as T;
    const text = await res.text();
    if (!res.ok) throw new ApiError(res.status, text);
    return text ? (JSON.parse(text) as T) : (null as T);
  }

  rankTrials(patientExtid: string, breastOnly: boolean, limit: number) {
    return this.request<TrialAssessment[]>(`/api/matching/rank/${patientExtid}`, { breastOnly, limit });
  }

  assessTrial(trialExtid: string, patientExtid: string) {
    return this.request<TrialAssessment>(`/api/matching/trial/${trialExtid}/for/${patientExtid}`);
  }

  searchTrials(
    query: string,
    maxTrials: number,
    recruitingOnly: boolean,
    criteriaOnly: boolean,
    similarityThreshold?: number,
  ) {
    return this.request<TrialSearchMatch[]>("/api/rag/search", {
      query,
      maxTrials,
      recruitingOnly,
      criteriaOnly,
      similarityThreshold,
    });
  }

  getTrial(extid: string) {
    return this.request<Trial>(`/api/trial/${extid}`);
  }

  listTrials(page: number, size: number) {
    return this.request<Page<Trial>>("/api/trial", { page, size });
  }

  runAiTrialCheck(trialExtid: string, patientExtid: string) {
    return this.post<AiTrialCheck>(`/api/matching/ai/trial/${trialExtid}/for/${patientExtid}`);
  }

  getLatestAiTrialCheck(trialExtid: string, patientExtid: string) {
    return this.request<AiTrialCheck | null>(`/api/matching/ai/trial/${trialExtid}/for/${patientExtid}`);
  }
}

/** Convenience for the stdio entry point: one process, one token, from its own env. */
export function defaultApiClient(): CancerApiClient {
  return new CancerApiClient(process.env.CANCER_API_TOKEN);
}
