# UCHealth / Epic FHIR Ingestion — Implementation Plan

Design for pulling your wife's real clinical record from UCHealth (Epic/MyChart, via
"My Health Connection") into this app's database, as a second `datafetcher` source
alongside ClinicalTrials.gov. Companion to `uchealth-epic-api-notes.md` (the
research notes this plan is based on), `../../PROJECT_PLAN.md` (Phase 3 — matching/relevance),
and `.claude/_archive/datafetcher/datafetcher-module.md` (module architecture this
follows). This doc is the concrete build plan — execute top to bottom once you're ready
to start.

## Status

`UcHealthOAuthToken` and `StagingRawFhirResource` are fully scaffolded (domain, entity,
mapper, repository, dbservice, service, request/response DTOs, controller, Liquibase
changeset — changesets `018`/`019`) ahead of Epic registration, since that part of the
plan needs no real credentials.

**Steps 2–3 are now built** (OAuth client + auth controller), along with the
token-lifecycle tests from step 8:

- A properties class holds the Epic connection settings, bound from environment
  variables via `application.yml` under a `uchealth` prefix. All six values default to
  empty/localhost, so the app starts fine unconfigured and fails with a clear message
  only when the flow is actually invoked.
- A PKCE challenge store keeps the `state` → `code_verifier` pair in memory between the
  authorize redirect and the callback, with a 15-minute TTL and single-use consumption
  (a replayed callback is rejected). In-memory is deliberate — a pending authorization
  only needs to outlive the patient's trip through the login page.
- The OAuth client builds the authorization URL, exchanges the authorization code, and
  refreshes an expiring token, persisting through `UcHealthOAuthTokenDbService`. It also
  exposes an "ensure valid token" entry point (refreshes 60s ahead of expiry) — that's
  the seam the FHIR client in step 4 calls.
- The auth controller exposes `GET /api/uchealth/authorize` (302 to Epic) and
  `GET /api/uchealth/callback` (code → stored token, plain-text result for the browser),
  and handles Epic returning `error`/`error_description` instead of a code.
- Tests cover the refresh branch (expired and within-skew both refresh and persist;
  a valid token doesn't), refresh-token retention when Epic returns none, unknown-state
  rejection, the authorization URL's PKCE params, and the PKCE store's single-use and
  S256 behavior (RFC 7636 test vector).

**Decided this session:** public client + PKCE, no client secret — matching what
`uchealth-epic-api-notes.md` says Epic expects for patient-facing apps, and avoiding a
stored secret. Confirm this against Epic's actual registration form in step 1; if Epic
forces a confidential client, the token-exchange and refresh calls need a
`client_secret` added and the PKCE store becomes optional.

✅ **Endpoint security has since been re-enabled, and the callback is correctly public.**
`SecurityConfig` now ends in `.anyRequest().authenticated()` with an explicit
`.requestMatchers("/api/uchealth/callback").permitAll()` rule — Epic's redirect arrives
from the patient's browser and cannot carry a JWT. Verified 2026-08-14.

> ### Status update — 2026-08-14
>
> **Steps 1-7 are all built.** The paragraph below said steps 4-7 were not started; they
> since landed:
>
> | Step | Class | |
> | --- | --- | --- |
> | 4 | `UcHealthFhirClient` | ✅ |
> | 5 | `UcHealthIngestJob` | ✅ |
> | 6 | `EpicObservationParser` + `FhirNormalizationService` | ✅ |
> | 7 | `POST /api/ingestion/uchealth/observation` and `/uchealth/medicationrequest` | ✅ |
>
> ⚠️ **Step 7 landed as endpoints on the existing `IngestionController`, not as a separate
> `UcHealthIngestionController`.** Both CT.gov and Epic pulls are triggered from one
> controller.
>
> All five changesets are applied: `018` oauth token, `019` staging raw FHIR resource,
> `020` patient medication, `021` lab result, `022` lab result component.
>
> **What is genuinely blocked is Epic's side, not this code** — see "Open questions" and
> `../../CURRENT_STATE.md`: no `offline_access` grant so the token dies in ~1 hour with no
> refresh, `MedicationRequest` rejected pending a sub-resource grant, `DiagnosticReport`
> returning 403, and panel handling untested against real data.

The scaffolded entities' mapper/repository/dbservice tests were skipped deliberately, per
this project's usual two-pass convention (`database-restapi-template` then
`database-restapi-testing`).

## Why this exists

The end goal (per your direction this session) is **RAG over your wife's actual medical
record** — not structured eligibility-matching against trial criteria (that's
`../../PROJECT_PLAN.md`'s Phase 3, and still on the table later, but not what this plan
builds). This plan covers **ingestion only**: getting FHIR data out of UCHealth's Epic
endpoint and into this app's database in a form that's ready to be chunked/embedded
later. The embedding/vector-store/retrieval pipeline itself is **out of scope for this
plan** — a separate plan once real data exists to design against.

## Target data (confirmed against the real My Health Connection portal)

Per a screenshot of the actual My Health Connection home screen, the portal exposes six
categories: Appointments and notes, Messages, Test results, Schedule appointment,
Billing summary, Medications. Of these, the three you want captured are **Test
results**, **Medications**, and **Messages** — but they map very differently onto
Epic's FHIR API:

- **Test results → `Observation`** (labs, vitals — the standard FHIR resource for
  discrete result values) **and possibly `DiagnosticReport`** (the report-level wrapper
  around a panel of related Observations, e.g. a full CBC). Both confirmed as
  standard, patient-accessible FHIR R4 resources.
- **Medications → `MedicationRequest`** (active/historical prescriptions) only.
  `MedicationStatement` was originally listed here as an "and/or" alternative, but
  **Epic's app-registration catalog offers it only in DSTU2 and STU3 — there is no R4
  version** (confirmed on the registration form while registering the sandbox app).
  Since this pipeline is R4 throughout, `MedicationStatement` is dropped from scope:
  no scope requested, no client method, no parser branch. `MedicationRequest` alone
  carries prescriptions, which is the medication data actually wanted. Revisit only if
  a real gap shows up in the sandbox data.
- **Messages → not confirmed available via FHIR.** Patient-provider portal messaging
  (MyChart's secure messages) generally has **no standard FHIR R4 resource** and is
  typically not exposed through Epic's patient-facing SMART on FHIR API — this is
  portal-UI-only functionality in most Epic deployments. **This needs dedicated research
  before it's buildable at all**; it may turn out to be inaccessible outside the portal
  UI itself (which would rule out an API-based approach entirely, leaving only something
  like browser automation against the portal — a very different and more fragile
  approach than the rest of this plan). Do not assume Messages ingestion is possible
  until this is confirmed one way or the other.

**This plan's build order below targets Test results and Medications only.** Messages
is out of scope until the research question above is resolved — see "Open questions."

## Decisions made this session

- **Lives in `datafetcher`, for now.** Same module as the ClinicalTrials.gov pipeline —
  reuses the module's existing shape (an API client + a staging-then-normalize path).
  If/when this grows large or its OAuth/token-lifecycle concerns start crowding out the
  CT.gov code, split it into its own module (e.g. `:patientdata` or `:fhir`) — not
  designed now, revisit if it becomes a problem.
- **Target Test results and Medications first; Messages is a research question, not yet
  in scope.** See "Target data" above. This narrows the original broader idea (pull
  everything, including narrative documents) down to what's actually confirmed
  available and wanted. If Messages later proves feasible via some FHIR-adjacent or
  alternate mechanism, it would likely need its own client/parser given how differently
  it's accessed compared to standard clinical resources — not designed here.
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
- **Trial-eligibility matching.** `../../PROJECT_PLAN.md` Phase 3's structured-profile-based
  matching is a different, still-deferred effort. This plan's normalized patient data
  could feed that later, but building the matching logic itself is not part of this
  pass.
- **Automatic/scheduled re-sync.** Like CT.gov ingestion, on-demand only — a token
  refresh flow is needed (see below) but no `@Scheduled` background sync job.
- **Multi-patient support.** This is your wife's record only. No UI for managing
  multiple patients/authorizations.
- **Messages.** Not confirmed accessible via FHIR — see "Target data" above and "Open
  questions" below. No client/parser/schema work for it in this pass.

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
│   │                                         Observation, DiagnosticReport,
│   │                                         MedicationRequest)
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
  (`"Observation"`, `"DiagnosticReport"`, or `"MedicationRequest"` for this pass), `fhir_resource_id` (Epic's id for that
  resource — the dedup key, analogous to `nct_id`), `raw_payload` (the FHIR JSON,
  `longtext`), `fetched_at`, `normalized_at`, `normalization_error`. **This table has
  already been scaffolded** (this session, ahead of Epic registration) — see
  `../../../database/src/main/resources/db/changelog/changes/019-staging-raw-fhir-resource.yaml`,
  with the composite unique constraint on `(resource_type, fhir_resource_id)` already in
  place from the start, applying the CT.gov staging-dedup lesson from day one.
- **Structured clinical facts** (test results, medications) — whether these get their
  own normalized tables now (mirroring `Trial`'s children — e.g. a `LabResult` table for
  `Observation`/`DiagnosticReport`, a `PatientMedication` table for
  `MedicationRequest`) or are deferred until Phase 3 actually needs
  structured querying is an **open question, see below.** For RAG alone, it may be
  sufficient to store these as a formatted text summary per resource rather than fully
  normalized structured columns — decide once you see what the sandbox data actually
  looks like. Given Messages (narrative-heavy) is on hold pending research, this pass's
  data is likely more structured/discrete than originally scoped (test result values,
  medication names/dosages) — lean toward structured normalization unless the sandbox
  payloads suggest otherwise.
- **`clinical_message`** (or similar) — **only if/when Messages ingestion turns out to be
  feasible** per the open research question below. Not scaffolded, not designed further,
  pending that answer.

## Step-by-step build order

### 1. Register with Epic, get sandbox access
- Create a free developer account at `fhir.epic.com`.
- Register a patient-facing app (confirm public-client/PKCE vs. confidential-client
  requirements during registration).
- Note the sandbox FHIR base URL and sandbox test patient credentials Epic provides.
- **Do not look up UCHealth's production endpoint or attempt real login yet** — build
  and verify everything against sandbox first.

### 2. `UcHealthOAuthClient` (`datafetcher/uchealth/`) — **built**
- Authorization-code exchange: given a `code` from the OAuth redirect, POST to Epic's
  token endpoint, get back `access_token` + `refresh_token` + `expires_in` +
  `patient` (the patient FHIR id).
- Refresh flow: given a stored `refresh_token`, get a fresh `access_token`.
- Persists/reads tokens via a new `UcHealthOAuthTokenDbService` (`:database`, follows
  the existing `*DbService` pattern).

### 3. `UcHealthAuthController` (root module) — **built**
- `GET /api/uchealth/authorize` — builds the Epic authorization URL (client_id, redirect
  URI, scopes, state) and redirects the browser there.
- `GET /api/uchealth/callback` — receives the authorization code, calls
  `UcHealthOAuthClient` to exchange it, stores the resulting tokens.
- This is the one part of this pipeline that's browser-driven (your wife needs to
  actually log in via UCHealth's page), unlike CT.gov's fully server-side flow — needs a
  real browser round-trip, not just curl/Swagger.

### 4. `UcHealthFhirClient` (`datafetcher/uchealth/`) — **built**
- Authenticated GET against FHIR R4 resource endpoints for the target data only:
  `/Patient/{id}` (resolve the authorized patient), `/Observation?patient={id}&category=laboratory`
  (test results — confirm the right `category` search param against sandbox; Epic
  supports filtering Observation by category), `/DiagnosticReport?patient={id}`,
  `/MedicationRequest?patient={id}`, using the
  stored (and auto-refreshed if expired) access token as a Bearer header.
- FHIR search results are paginated via `Bundle.link[relation=next]` — different
  pagination shape than CT.gov's `pageToken`, handle accordingly.

### 5. `UcHealthIngestJob` (`datafetcher/uchealth/`) — **built**
- Ensures a valid (non-expired, refreshed-if-needed) token exists.
- Fetches each resource type for the authorized patient, writes
  `staging_raw_fhir_resource` rows — same "raw payload preserved as-is" principle as
  `ClinicalTrialsGovIngestJob`, and same dedup-before-insert pattern (skip pending
  duplicates, refresh already-normalized ones) that fixed this session's CT.gov bug —
  build this pipeline with that dedup logic from day one instead of discovering the gap
  again.

### 6. `UcHealthFhirParser` (`datafetcher/normalization/`) — **built as `EpicObservationParser` + `FhirNormalizationService`**
- Parses each `staging_raw_fhir_resource` row's JSON per its `resource_type`
  (`Observation`/`DiagnosticReport`/`MedicationRequest`) into
  whatever normalized table shape gets decided in the schema step above.

### 7. `UcHealthIngestionController` (root module) — **built as endpoints on `IngestionController`**
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

- **Can Messages be accessed at all, via any means?** Research needed: check Epic's FHIR
  documentation and open endpoints info for any Messages-related resource (there is no
  standard FHIR Communication-resource guarantee for MyChart secure messages across all
  Epic deployments — confirm UCHealth's specific configuration once sandbox/production
  access exists). If FHIR genuinely doesn't expose it, the only remaining path would be
  something like authenticated browser automation against the portal UI itself — a
  fundamentally different, more fragile approach than everything else in this plan, and
  not something to build without a deliberate separate decision. Do not start that work
  without first confirming FHIR access is truly a dead end.
- **Where do tokens live? — still open, currently plain columns.** As built, access and
  refresh tokens sit in plain `uchealth_oauth_token` columns, matching the project's
  existing patterns. That's fine against Epic's sandbox (synthetic patients, throwaway
  credentials), but it is **not** a decision about the real thing: before the one real
  authorization against your wife's My Health Connection account, decide whether a
  long-lived refresh token to her actual medical record warrants encryption at rest even
  in a local-only app. Changing this later touches only the DbService and the changeset —
  nothing in the OAuth client depends on how the columns are stored. Related: the
  scaffolded `UcHealthOAuthTokenController` exposes full CRUD over this table, including
  reading tokens back out over HTTP — consider removing or restricting it before real
  credentials land.
- **Structured data depth.** As noted in the schema section — full normalized tables
  per resource type (mirroring Trial's children) vs. lighter-weight
  text-summary-per-resource rows, given the actual target is RAG, not structured
  querying. Recommend deciding after seeing real sandbox payload shapes, not
  up front.
- **Redirect URI / local dev.** Epic's OAuth flow needs a real registered redirect URI.
  For local dev this likely means registering `http://localhost:8080/api/uchealth/callback`
  (or similar) with Epic — confirm Epic's sandbox app registration allows a localhost
  redirect before assuming this works out of the box.

## Verification checklist before calling this done

- [ ] Sandbox app registered at fhir.epic.com, sandbox FHIR base URL confirmed reachable.
- [ ] `/api/uchealth/authorize` → sandbox login → `/api/uchealth/callback` round-trip
      successfully stores a token.
- [ ] `POST /api/ingestion/uchealth` against sandbox pulls a sandbox test patient's
      Observation/DiagnosticReport/MedicationRequest data into
      staging and normalizes it.
- [ ] Messages research question (above) resolved one way or the other — feasible via
      FHIR, feasible via some other means, or confirmed not accessible — before deciding
      whether any Messages work gets added to this plan.
- [ ] Re-running the same call updates/dedups rather than duplicating (apply this
      session's staging-dedup lesson from the start, verify it holds).
- [ ] An expired access token triggers a successful automatic refresh mid-flow, without
      requiring the patient to re-authenticate.
- [ ] Only after all of the above pass on sandbox: look up UCHealth's real production
      FHIR endpoint and do one real, deliberate authorization with your wife's actual
      My Health Connection login — treat this as a separate, careful step, not a
      natural continuation of sandbox testing.
