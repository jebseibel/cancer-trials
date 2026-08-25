# MCP practice server — plan

Why this project, a minimal tool list, and a skeleton to build from. Written 2026-08-25, no code
in this document per the doc-writing rule — see `mcp-server-skeleton/` (outside this repo's
Gradle build, same spirit as `playwright/`) for the actual starter code.

## Why this project is a good MCP candidate

- Every entity already follows `Controller → Service → DbService → Repository`, keyed on
  `extid`, with a predictable REST shape. An MCP server wraps that shape almost directly — no new
  backend logic needed, just a thin translation layer.
- The matching and RAG endpoints are a good mix of tool *kinds* to practice on: pure read
  (`get_trial`), ranking/assessment (`rank_trials`, `assess_trial`), semantic search
  (`search_trials`), and an action that costs money and writes a row (`run_ai_trial_check`).
  That variety is the point — a real MCP practice project shouldn't be seven copies of the same
  CRUD-GET tool.
- JWT + `UserPatient` grants means there's a real auth design question (how does the MCP server
  authenticate to the backend?), not a toy with no auth story.

## What NOT to expose, and why

- **No `get_patient` in the first pass.** `ResponsePatient` carries `fullName`, `dateOfBirth`,
  and `notes` directly — none of the de-identification the AI trial check allowlist does. An MCP
  client (Claude Desktop, etc.) calling that tool sees real PHI-adjacent fields verbatim. If
  patient identity is ever needed by a tool, allowlist fields explicitly the way
  `TrialDiagnosisMatchService` does — never pass `ResponsePatient` through as-is.
- **No write/ingestion tools in the first pass** (`POST /api/ingestion/clinicaltrials`,
  backfill, reindex). Start read-only; add actions once the read path is proven.
- **`run_ai_trial_check` is a POST that costs money and writes `ai_trial_assessment`.** Worth
  including specifically *because* it's a different risk shape from the rest — good practice for
  tool annotations (`readOnlyHint: false`) and for deciding whether an MCP client should be able
  to trigger paid calls without a confirmation step.

## Minimal tool list (first pass)

All of these map to one existing endpoint each — no new backend code required.

| Tool | Backing endpoint | Kind |
| --- | --- | --- |
| `rank_trials` | `GET /api/matching/rank/{patientExtid}?breastOnly=&limit=` | read, ranking |
| `assess_trial` | `GET /api/matching/trial/{trialExtid}/for/{patientExtid}` | read, single assessment |
| `search_trials` | `GET /api/rag/search?query=&maxTrials=&recruitingOnly=&criteriaOnly=&similarityThreshold=` | read, semantic search |
| `get_trial` | `GET /api/trial/{extid}` | read, single record |
| `list_trials` | `GET /api/trial?page=&size=&active=` | read, paginated list |
| `run_ai_trial_check` | `POST /api/matching/ai/trial/{trialExtid}/for/{patientExtid}` | action, costs money, writes a row |
| `get_latest_ai_check` | `GET /api/matching/ai/trial/{trialExtid}/for/{patientExtid}` | read, may 204 |

That's 7 tools — enough to exercise real variety (search, rank, single-record read, list,
paginated read, a costed action, a read of that action's own history) without sprawl.

**`patientExtid` is a required argument on four of these tools, not something the server
guesses.** The natural single-user shortcut ("just default to the one patient in the DB") would
quietly bake an assumption the real backend doesn't make — `CurrentUserService` already checks
grants per patient. Pass it through explicitly even though there's currently only one.

## Design decisions

**Transport: stdio, not HTTP.** For practicing with Claude Desktop or Claude Code as the client,
stdio is the standard local-server transport and avoids standing up auth for the MCP layer
itself — the server is a local child process, not a network service.

**Where it lives: outside `settings.gradle`, new top-level directory.** Same treatment as
`playwright/` — a standalone Node/TypeScript (or Python) process that calls the existing REST API
over HTTP. No reason to entangle it with the multi-module Gradle build; it's a client of the app,
not a module of it.

**Auth to the backend: a static JWT from an env var, first pass.** The MCP server process holds
one long-lived token (obtained by logging in as `jeb` once, out of band) and sends it as
`Authorization: Bearer` on every call. Good enough for single-user practice; a real multi-user
MCP deployment would need its own login/refresh flow, which is a reasonable "phase 2" to design
once the basic tool-calling loop works.

**Language: TypeScript**, using `@modelcontextprotocol/sdk`. It's the reference SDK and has the
most current documentation and examples; also keeps this fully separate from the Java build.

## Open questions to decide before/while building

- Does `rank_trials` need a result-size cap in the tool description so Claude doesn't request
  `limit=2473` (the whole corpus) by default?
- Should tool descriptions mention the no-verdicts rule explicitly ("this reports concerns and
  open questions, never eligibility"), so a client model doesn't paraphrase a CONCERN as
  disqualifying? Given how central that rule is to the rest of the app, yes — worth carrying into
  every tool description that touches `ResponseTrialAssessment` or `ResponseAiTrialCheck`.
- What happens when the backend is down (user hasn't started it)? Tools should return a clear
  error to the MCP client, not hang — worth testing deliberately.
