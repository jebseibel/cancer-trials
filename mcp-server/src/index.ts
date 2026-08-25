#!/usr/bin/env node
/**
 * MCP server for the cancer trial finder. Wraps the existing REST API's matching, RAG,
 * and trial-read endpoints as tools — no new backend logic, this is a translation layer.
 *
 * Deliberately excluded from this first pass (see .claude/mcp-server/MCP_PRACTICE_PLAN.md):
 *   - get_patient — ResponsePatient carries name/DOB/notes verbatim, none of the
 *     de-identification the AI trial check allowlist does.
 *   - ingestion / backfill / reindex — write and cost real time; add once read tools work.
 */

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import * as api from "./apiClient.js";
import { ApiError } from "./apiClient.js";

const server = new McpServer({
  name: "cancer-trial-mcp",
  version: "0.1.0",
});

/**
 * Wraps a tool handler so a backend failure (down, 4xx, 5xx) comes back as a normal MCP tool
 * error the client model can react to, instead of crashing the request or the whole process.
 */
function safe<Args extends unknown[]>(
  fn: (...args: Args) => Promise<{ content: Array<{ type: "text"; text: string }> }>,
) {
  return async (...args: Args) => {
    try {
      return await fn(...args);
    } catch (err) {
      const message =
        err instanceof ApiError
          ? `Backend returned ${err.status}: ${err.body}`
          : `Could not reach the backend at ${process.env.CANCER_API_BASE_URL ?? "http://localhost:8080"} — is it running? (${String(err)})`;
      return { content: [{ type: "text" as const, text: message }], isError: true };
    }
  };
}

// Shared reminder appended to every tool touching an assessment, so a client model doesn't
// paraphrase a CONCERN as a disqualification. This is the app's central rule — see
// project-description.md "Hard rules": no eligibility verdicts, ever.
const NO_VERDICTS_NOTE =
  "This reports concerns and open questions to raise with an oncology team — it never reports " +
  "eligibility, a fit score, or a verdict. A CONCERN or UNKNOWN flag does not mean a patient " +
  "cannot join a trial.";

server.registerTool(
  "rank_trials",
  {
    title: "Rank trials for a patient",
    description:
      `Rank the trial corpus against a patient's record, best matches first. ${NO_VERDICTS_NOTE} ` +
      "Defaults to breast-cancer trials only; set breastOnly=false to rank the whole corpus " +
      "(slower, and mostly irrelevant diseases for a breast-cancer patient).",
    inputSchema: {
      patientExtid: z.string().describe("The patient's external id (UUID-style)."),
      breastOnly: z.boolean().default(true).describe("Restrict to breast-cancer trials."),
      limit: z.number().int().min(1).max(200).default(50).describe("Max trials to return."),
    },
    annotations: { readOnlyHint: true },
  },
  safe(async ({ patientExtid, breastOnly, limit }) => {
    const results = await api.rankTrials(patientExtid, breastOnly, limit);
    return { content: [{ type: "text", text: JSON.stringify(results, null, 2) }] };
  }),
);

server.registerTool(
  "assess_trial",
  {
    title: "Assess one trial for a patient",
    description: `Run the seven-signal assessment of a single trial against a patient's record. ${NO_VERDICTS_NOTE}`,
    inputSchema: {
      trialExtid: z.string().describe("The trial's external id."),
      patientExtid: z.string().describe("The patient's external id."),
    },
    annotations: { readOnlyHint: true },
  },
  safe(async ({ trialExtid, patientExtid }) => {
    const result = await api.assessTrial(trialExtid, patientExtid);
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
  }),
);

server.registerTool(
  "search_trials",
  {
    title: "Semantic search over trial eligibility text",
    description:
      "Search indexed trial text by meaning (vector similarity), not keyword matching. " +
      "Set criteriaOnly=true to search only eligibility-criteria text rather than trial-design " +
      "prose (titles, phase descriptions) — usually what you want when the question is " +
      "'who can join'.",
    inputSchema: {
      query: z.string().describe("Natural-language query, e.g. a patient's profile line."),
      maxTrials: z.number().int().min(1).max(50).default(5),
      recruitingOnly: z.boolean().default(false),
      criteriaOnly: z.boolean().default(false),
      similarityThreshold: z.number().min(0).max(1).optional(),
    },
    annotations: { readOnlyHint: true },
  },
  safe(async ({ query, maxTrials, recruitingOnly, criteriaOnly, similarityThreshold }) => {
    const results = await api.searchTrials(query, maxTrials, recruitingOnly, criteriaOnly, similarityThreshold);
    return { content: [{ type: "text", text: JSON.stringify(results, null, 2) }] };
  }),
);

server.registerTool(
  "get_trial",
  {
    title: "Get one trial's full record",
    description: "Fetch a single trial's full normalized record by its external id, including eligibility criteria text.",
    inputSchema: {
      extid: z.string().describe("The trial's external id."),
    },
    annotations: { readOnlyHint: true },
  },
  safe(async ({ extid }) => {
    const trial = await api.getTrial(extid);
    return { content: [{ type: "text", text: JSON.stringify(trial, null, 2) }] };
  }),
);

server.registerTool(
  "list_trials",
  {
    title: "List trials, paginated",
    description: "List trials in the corpus without any matching or search — plain pagination, sorted by title.",
    inputSchema: {
      page: z.number().int().min(0).default(0),
      size: z.number().int().min(1).max(100).default(20),
    },
    annotations: { readOnlyHint: true },
  },
  safe(async ({ page, size }) => {
    const result = await api.listTrials(page, size);
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
  }),
);

server.registerTool(
  "run_ai_trial_check",
  {
    title: "Run a fresh AI reading of one trial against a patient",
    description:
      "Sends a de-identified subset of the patient's record and one trial's criteria to Claude " +
      "for a fresh reading, catching phrasing the deterministic signals cannot (e.g. a carve-out " +
      `inside an exclusion). Costs money per call and stores the result. ${NO_VERDICTS_NOTE} ` +
      "Prefer get_latest_ai_check first to see if a recent reading already exists.",
    inputSchema: {
      trialExtid: z.string().describe("The trial's external id."),
      patientExtid: z.string().describe("The patient's external id."),
    },
    annotations: { readOnlyHint: false, destructiveHint: false },
  },
  safe(async ({ trialExtid, patientExtid }) => {
    const result = await api.runAiTrialCheck(trialExtid, patientExtid);
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
  }),
);

server.registerTool(
  "get_latest_ai_check",
  {
    title: "Get the most recent stored AI reading, if any",
    description: "Fetch the last AI trial check stored for this trial/patient pair, without running a new (paid) one.",
    inputSchema: {
      trialExtid: z.string().describe("The trial's external id."),
      patientExtid: z.string().describe("The patient's external id."),
    },
    annotations: { readOnlyHint: true },
  },
  safe(async ({ trialExtid, patientExtid }) => {
    const result = await api.getLatestAiTrialCheck(trialExtid, patientExtid);
    if (result === null) {
      return { content: [{ type: "text", text: "No AI check has been run yet for this trial/patient pair." }] };
    }
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
  }),
);

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("cancer-trial-mcp running on stdio");
}

main().catch((err) => {
  console.error("Fatal error starting cancer-trial-mcp:", err);
  process.exit(1);
});
