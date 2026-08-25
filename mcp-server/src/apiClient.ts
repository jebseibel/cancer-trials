/**
 * Thin HTTP client over the existing Spring Boot REST API. No business logic lives here —
 * every tool handler calls one of these functions and shapes the result for the model.
 */

const BASE_URL = process.env.CANCER_API_BASE_URL ?? "http://localhost:8080";
const TOKEN = process.env.CANCER_API_TOKEN;

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: string,
  ) {
    super(`Cancer API returned ${status}: ${body}`);
  }
}

async function request<T>(path: string, params?: Record<string, string | number | boolean | undefined>): Promise<T> {
  const url = new URL(path, BASE_URL);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined) url.searchParams.set(key, String(value));
    }
  }

  const headers: Record<string, string> = { Accept: "application/json" };
  if (TOKEN) headers.Authorization = `Bearer ${TOKEN}`;

  const res = await fetch(url, { method: "GET", headers });
  return handleResponse<T>(res);
}

async function post<T>(path: string): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (TOKEN) headers.Authorization = `Bearer ${TOKEN}`;

  const res = await fetch(new URL(path, BASE_URL), { method: "POST", headers });
  return handleResponse<T>(res);
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.status === 204) return null as T;
  const text = await res.text();
  if (!res.ok) throw new ApiError(res.status, text);
  return text ? (JSON.parse(text) as T) : (null as T);
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

// --- API calls, one per tool ---

export function rankTrials(patientExtid: string, breastOnly: boolean, limit: number) {
  return request<TrialAssessment[]>(`/api/matching/rank/${patientExtid}`, { breastOnly, limit });
}

export function assessTrial(trialExtid: string, patientExtid: string) {
  return request<TrialAssessment>(`/api/matching/trial/${trialExtid}/for/${patientExtid}`);
}

export function searchTrials(
  query: string,
  maxTrials: number,
  recruitingOnly: boolean,
  criteriaOnly: boolean,
  similarityThreshold?: number,
) {
  return request<TrialSearchMatch[]>("/api/rag/search", {
    query,
    maxTrials,
    recruitingOnly,
    criteriaOnly,
    similarityThreshold,
  });
}

export function getTrial(extid: string) {
  return request<Trial>(`/api/trial/${extid}`);
}

export function listTrials(page: number, size: number) {
  return request<Page<Trial>>("/api/trial", { page, size });
}

export function runAiTrialCheck(trialExtid: string, patientExtid: string) {
  return post<AiTrialCheck>(`/api/matching/ai/trial/${trialExtid}/for/${patientExtid}`);
}

export function getLatestAiTrialCheck(trialExtid: string, patientExtid: string) {
  return request<AiTrialCheck | null>(`/api/matching/ai/trial/${trialExtid}/for/${patientExtid}`);
}
