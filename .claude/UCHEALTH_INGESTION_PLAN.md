# UCHealth / Epic FHIR Ingestion — Implementation Plan

Design for pulling your wife's real clinical record from UCHealth (Epic/MyChart, via
"My Health Connection") into this app's database, as a second `datafetcher` source
alongside ClinicalTrials.gov. Companion to `.claude/uchealth-epic-api-notes.md` (the
research notes this plan is based on), `PROJECT_PLAN.md` (Phase 3 — matching/relevance),
and `.claude/_archive/datafetcher/datafetcher-module.md` (module architecture this
follows). This doc is the concrete build plan — execute top to bottom once you're ready
to start.

## Why this exists

The end goal (per your direction this session) is **RAG over your wife's actual medical
record** — not structured eligibility-matching against trial criteria (that's
`PROJECT_PLAN.md`'s Phase 3, and still on the table later, but not what this plan
builds). This plan covers **ingestion only**: getting FHIR data out of UCHealth's Epic
endpoint and into this app's database in a form that's ready to be chunked/embedded
later. The embedding/vector-store/retrieval pipeline itself is **out of scope for this
plan** — a separate plan once real data exists to design against.

## Decisions made this session

- **Lives in `datafetcher`, for now.** Same module as the ClinicalTrials.gov pipeline —
  reuses the module's existing shape (an API client + a staging-then-normalize path).
  If/when this grows large or its OAuth/token-lifecycle concerns start crowding out the
  CT.gov code, split it into its own module (e.g. `:patientdata` or `:fhir`) — not
  designed now, revisit if it becomes a problem.
- **Pull both structured resources and narrative documents.** RAG benefits most from
  free text (clinical notes, visit summaries), not just coded data — so this plan
  includes `DocumentReference`/`Binary` fetching alongside the structured FHIR
  resources, unlike the CT.gov pipeline which only ever dealt with structured JSON.
- **This is patient-authorized access, not bulk/system access.** UCHealth has no
  system-level API program — per `uchealth-epic-api-notes.md`, the only path is a
  SMART on FHIR app registered with Epic (`fhir.epic.com`), where your wife logs in with
  her My Health Connection credentials and authorizes the app against her own record.
  There is no "pull everyone's data" mode; this is inherently single-patient, matching
  the project's single-user, personal-use nature.
- **Sandbox first.** Epic provides sandbox FHIR endpoints and synthetic test patients
  through `fhir.epic.com` — build and verify the OAuth + fetch flow against sandbox data
  before ever pointing at UCHealth's real production endpoint with real credentials.

## Explicitly out of scope for this pass

- **Embedding / vector store / RAG retrieval itself.** This plan stops at "FHIR data
  lands in staging, normalized into readable rows in the DB." Chunking strategy,
  embedding model choice, vector store choice (pgvector, a dedicated vector DB,
  etc.), and retrieval/query design are a separate plan once this data exists.
- **Trial-eligibility matching.** `PROJECT_PLAN.md` Phase 3's structured-profile-based
  matching is a different, still-deferred effort. This plan's normalized patient data
  could feed that later, but building the matching logic itself is not part of this
  pass.
- **Automatic/scheduled re-sync.** Like CT.gov ingestion, on-demand only — a token
  refresh flow is needed (see below) but no `@Scheduled` background sync job.
- **Multi-patient support.** This is your wife's record only. No UI for managing
  multiple patients/authorizations.

## The access model (read this before building)

Unlike ClinicalTrials.gov's open, no-auth API, this is a **three-legged OAuth 2.0 /
SMART on FHIR flow**:

1. Register a patient-facing app at `fhir.epic.com` (free developer account). Epic
   issues a `client_id`. For a patient-facing SMART app without a hosted backend
   client secret, this is typically the "public client" / PKCE flow (confirm exact
   requirements when registering — Epic's docs cover both confidential and public
   client patterns).
2. Look up UCHealth's production FHIR R4 base URL in Epic's open endpoints directory
   (also on `fhir.epic.com`). Use Epic's **sandbox** base URL + sandbox test patients
   for all initial development.
3. Your app redirects the patient (your wife) to UCHealth's OAuth authorization
   endpoint. She logs in with her My Health Connection credentials and consents to
   share her record with your app.
4. Epic redirects back to your app's registered redirect URI with an authorization
   code; your app exchanges it for an **access token + refresh token**, scoped to her
   patient record (`patient/*.read` or specific resource scopes, per SMART on FHIR
   scope syntax).
5. Your app uses the access token as a Bearer token against UCHealth's FHIR R4
   endpoints. Access tokens are short-lived (typically ~1 hour under SMART on FHIR) —
   **a refresh-token flow is required** for anything beyond a single ingestion run in
   one sitting. Store the refresh token securely (see "Open questions" below).

This is fundamentally different from CT.gov's ingestion job (`ClinicalTrialsGovClient`
just makes an unauthenticated GET). Expect to build real OAuth handling — this is not a
thin REST client.

## Package layout (mirrors the CT.gov pipeline's shape in `datafetcher`)

```
datafetcher/src/main/java/com/seibel/cancer/datafetcher/
├── clinicaltrials/                          (existing, unchanged)
├── uchealth/
│   ├── UcHealthOAuthClient.java             authorization-code exchange, token refresh,
│   │                                         talks to UCHealth/Epic's OAuth endpoints
│   ├── UcHealthFhirClient.java               authenticated FHIR R4 client (GET Patient,
│   │                                         Condition, Observation, MedicationRequest,
│   │                                         AllergyIntolerance, DocumentReference, etc.)
│   └── UcHealthIngestJob.java                orchestrates: ensure valid token -> fetch
│                                              resources -> write staging rows
└── normalization/
    ├── (existing TrialSourceParser/ClinicalTrialsGovParser/TrialNormalizationService/
    │    TrialRowNormalizer, unchanged)
    ├── FhirSourceParser.java                 interface: raw FHIR resource JSON ->
    │                                         NormalizedClinicalRecord (parallel to
    │                                         TrialSourceParser, NOT the same interface -
    │                                         FHIR resources don't map onto Trial's shape)
    └── UcHealthFhirParser.java                implements FhirSourceParser for Epic's
                                               FHIR R4 JSON shape

src/main/java/com/seibel/cancer/web/
└── controller/
    └── UcHealthAuthController.java           GET /api/uchealth/authorize (redirect to
                                               Epic's OAuth screen), GET /api/uchealth/
                                               callback (authorization code -> token
                                               exchange, stores tokens)
    └── UcHealthIngestionController.java       POST /api/ingestion/uchealth (fetch ->
                                               stage -> normalize, using the stored token)
```

`FhirSourceParser` is a distinct interface from `TrialSourceParser`, not a reuse of it —
a `NormalizedTrial` (Trial + trial children) and a patient's clinical record are
different shapes entirely. Don't force them into one interface just because both are
"a source parser."

## Schema (new tables, additive — doesn't touch existing trial/condition/etc. tables)

Exact column lists TBD at build time (this plan intentionally doesn't over-specify
before seeing real sandbox FHIR payloads), but the table shape should be:

- **`uchealth_oauth_token`** — one row (single-patient app): `access_token`,
  `refresh_token`, `expires_at`, `patient_fhir_id` (Epic's internal patient identifier,
  returned at token-exchange time), `scope`. Standard `BaseDb` fields
  (`id`/`extid`/`created_at`/etc.) apply. **Tokens are sensitive — do not commit any
  seed data containing a real token, and treat this table like credentials.**
- **`staging_raw_fhir_resource`** — mirrors `staging_raw_trial`'s shape: `resource_type`
  (e.g. `"Condition"`, `"Observation"`, `"DocumentReference"`), `fhir_resource_id`
  (Epic's id for that resource — the dedup key, analogous to `nct_id`), `raw_payload`
  (the FHIR JSON, `longtext`), `fetched_at`, `normalized_at`, `normalization_error`. Add
  the same composite unique constraint pattern learned from the CT.gov staging bug this
  session: unique on `(resource_type, fhir_resource_id)` from the start, not bolted on
  after finding duplicates.
- **`clinical_document`** (or similar) — normalized narrative documents:
  `fhir_resource_id`, `document_type` (from `DocumentReference.type`), `title`,
  `content_text` (extracted plain text — Epic's `DocumentReference`/`Binary` content is
  often base64-encoded and may be RTF/PDF/XML under the hood; extraction approach is a
  build-time decision, not decided here), `document_date`, plus `BaseDb` fields. This is
  the table RAG chunking will eventually read from.
- **Structured clinical facts** (conditions, medications, allergies, observations) —
  whether these get their own normalized tables now (mirroring `Trial`'s children) or
  are deferred until Phase 3 actually needs structured querying is an **open
  question, see below.** For RAG alone, it may be sufficient to store these as
  additional narrative-ish rows (e.g. a formatted text summary per resource) rather than
  fully normalized structured columns — decide once you see what the sandbox data
  actually looks like.

## Step-by-step build order

### 1. Register with Epic, get sandbox access
- Create a free developer account at `fhir.epic.com`.
- Register a patient-facing app (confirm public-client/PKCE vs. confidential-client
  requirements during registration).
- Note the sandbox FHIR base URL and sandbox test patient credentials Epic provides.
- **Do not look up UCHealth's production endpoint or attempt real login yet** — build
  and verify everything against sandbox first.

### 2. `UcHealthOAuthClient` (`datafetcher/uchealth/`)
- Authorization-code exchange: given a `code` from the OAuth redirect, POST to Epic's
  token endpoint, get back `access_token` + `refresh_token` + `expires_in` +
  `patient` (the patient FHIR id).
- Refresh flow: given a stored `refresh_token`, get a fresh `access_token`.
- Persists/reads tokens via a new `UcHealthOAuthTokenDbService` (`:database`, follows
  the existing `*DbService` pattern).

### 3. `UcHealthAuthController` (root module)
- `GET /api/uchealth/authorize` — builds the Epic authorization URL (client_id, redirect
  URI, scopes, state) and redirects the browser there.
- `GET /api/uchealth/callback` — receives the authorization code, calls
  `UcHealthOAuthClient` to exchange it, stores the resulting tokens.
- This is the one part of this pipeline that's browser-driven (your wife needs to
  actually log in via UCHealth's page), unlike CT.gov's fully server-side flow — needs a
  real browser round-trip, not just curl/Swagger.

### 4. `UcHealthFhirClient` (`datafetcher/uchealth/`)
- Authenticated GET against FHIR R4 resource endpoints (`/Patient/{id}`,
  `/Condition?patient={id}`, `/Observation?patient={id}`,
  `/MedicationRequest?patient={id}`, `/AllergyIntolerance?patient={id}`,
  `/DocumentReference?patient={id}`, etc.), using the stored (and auto-refreshed if
  expired) access token as a Bearer header.
- FHIR search results are paginated via `Bundle.link[relation=next]` — different
  pagination shape than CT.gov's `pageToken`, handle accordingly.

### 5. `UcHealthIngestJob` (`datafetcher/uchealth/`)
- Ensures a valid (non-expired, refreshed-if-needed) token exists.
- Fetches each resource type for the authorized patient, writes
  `staging_raw_fhir_resource` rows — same "raw payload preserved as-is" principle as
  `ClinicalTrialsGovIngestJob`, and same dedup-before-insert pattern (skip pending
  duplicates, refresh already-normalized ones) that fixed this session's CT.gov bug —
  build this pipeline with that dedup logic from day one instead of discovering the gap
  again.

### 6. `UcHealthFhirParser` (`datafetcher/normalization/`)
- Parses each `staging_raw_fhir_resource` row's JSON per its `resource_type`.
- For `DocumentReference`: resolve and extract the actual document content (may require
  a separate authenticated fetch to a `Binary` endpoint referenced by the
  `DocumentReference`, and text-extraction from whatever format comes back) into
  `clinical_document.content_text`.
- For structured resources: normalize into whatever table shape gets decided in the
  schema step above.

### 7. `UcHealthIngestionController` (root module)
- `POST /api/ingestion/uchealth` — triggers fetch → stage → normalize synchronously
  (same shape as `POST /api/ingestion/clinicaltrials`), using the already-stored token
  (no patient interaction needed for this call — only the initial `/authorize` /
  `/callback` round-trip requires the browser).

### 8. Tests
- `UcHealthFhirParserTest` — fixture-based, using **Epic's sandbox sample payloads**
  (captured once sandbox access exists), same pattern as `ClinicalTrialsGovParserTest`.
- Mock-based tests for the OAuth token refresh branch (expired-token-triggers-refresh
  logic) — this is the one genuinely new kind of logic vs. the CT.gov pipeline and
  deserves direct test coverage.
- Skip live-network tests against sandbox/production in the automated suite, same as
  CT.gov — manual verification covers that.

## Open questions to resolve before/during the build

- **Where do tokens live?** A DB table is the simplest fit with this project's existing
  patterns, but a refresh token is a long-lived credential for your wife's real medical
  record — is a plain DB column acceptable for personal/local-only use, or does it
  warrant encryption at rest even in a non-production app? Decide before building
  `UcHealthOAuthTokenDbService`.
- **Structured data depth.** As noted in the schema section — full normalized tables
  per resource type (mirroring Trial's children) vs. lighter-weight
  text-summary-per-resource rows, given the actual target is RAG, not structured
  querying. Recommend deciding after seeing real sandbox payload shapes, not
  up front.
- **Document content extraction.** `DocumentReference`/`Binary` content types vary
  (plain text, RTF, PDF, XML/CDA). Needs a concrete decision on what to extract from
  and how, once real sandbox documents are available to inspect.
- **Redirect URI / local dev.** Epic's OAuth flow needs a real registered redirect URI.
  For local dev this likely means registering `http://localhost:8080/api/uchealth/callback`
  (or similar) with Epic — confirm Epic's sandbox app registration allows a localhost
  redirect before assuming this works out of the box.

## Verification checklist before calling this done

- [ ] Sandbox app registered at fhir.epic.com, sandbox FHIR base URL confirmed reachable.
- [ ] `/api/uchealth/authorize` → sandbox login → `/api/uchealth/callback` round-trip
      successfully stores a token.
- [ ] `POST /api/ingestion/uchealth` against sandbox pulls a sandbox test patient's
      Condition/Observation/MedicationRequest/DocumentReference data into staging and
      normalizes it.
- [ ] Re-running the same call updates/dedups rather than duplicating (apply this
      session's staging-dedup lesson from the start, verify it holds).
- [ ] An expired access token triggers a successful automatic refresh mid-flow, without
      requiring the patient to re-authenticate.
- [ ] Only after all of the above pass on sandbox: look up UCHealth's real production
      FHIR endpoint and do one real, deliberate authorization with your wife's actual
      My Health Connection login — treat this as a separate, careful step, not a
      natural continuation of sandbox testing.
