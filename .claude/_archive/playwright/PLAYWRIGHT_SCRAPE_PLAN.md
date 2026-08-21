# MyChart Playwright Scraping — Plan

A second path to the patient record, alongside the Epic FHIR pipeline. Companion to
`../epic-integration/UCHEALTH_INGESTION_PLAN.md` (the API path this supplements) and
`../../PROJECT_PLAN.md` §7 (which already anticipated a Playwright source writing into
staging).

**Status as of 2026-08-14: ✅ step 1 is built; steps 2-6 are designed and not started.**
The `../../../playwright` directory at the repo root is a working standalone Gradle build —
`MyChartScraperApp`, plus `SessionManager`, `LoginService` and `ConfigLoader` under
`scraper/common/`. It is deliberately **not** in root `../../../settings.gradle`.

⚠️ `../../../playwright/.auth` is currently empty, so the next run will do a fresh login and
**MFA will fire** — the code goes to the patient's phone, so allow the 10-minute wait.

---

## Why this exists

Two separate problems with the FHIR path, and only one of them is about speed.

**The grants are slow.** `MedicationRequest` is blocked pending a sub-resource grant
that had not propagated; `DiagnosticReport` returns 403. Those may clear on their own.

**FHIR does not cover everything, and never will.** Confirmed during Epic app
registration: no messaging resource is offered in the patient-facing API catalog at all.
Appointment notes and portal messages are portal-UI-only. No amount of waiting changes
that — the data is not in the API to be granted.

Investigated and ruled out this session: **Epic has no MCP server.** Their HIMSS 2026
announcement, Agent Factory, is agent-building tooling for health-system developers, not
a server anything connects to, and not available to patients or independent developers.
The third-party FHIR MCP servers that exist (WSO2, AgentCare, `health-record-mcp`) are
MCP wrappers over the same SMART on FHIR API — same OAuth, same scopes, same blocked
grants, same missing categories. MCP would change the client, not the data.

So scraping is not a workaround for a slow grant. It is the only route to the categories
FHIR does not expose. Treat the two paths as permanently complementary, not as a
stopgap that gets deleted when the API unblocks.

---

## Decide these before building

Both are the user's calls. Neither is a technicality, and the first one changes the
approach materially.

**MFA on the account.** If My Health Connection requires a code per login, "repeatable"
degrades to "semi-automated" — a human completes each run. Check this on the real
account before committing. Storage state (below) may reduce it to occasional rather than
per-run, but that has to be observed, not assumed.

**Credentials and terms of use.** This stores a real portal password where automation
can reach it, and MyChart's terms very likely prohibit automated access. Her record, her
credentials, local machine — the most defensible version of this, but a real decision.

---

## Where it plugs in

The existing architecture absorbs this at exactly one seam. Nothing downstream changes.

- **`staging_raw_fhir_resource` is already source-agnostic.** `resource_type` +
  `fhir_resource_id` + `raw_payload`, with a unique constraint on the first two. A
  scraped row lands here as naturally as an API row. No schema change for the categories
  that map onto existing tables.
- **`FhirSourceParser` dispatches by `resource_type` string.** A parser that supports a
  scraped type slots in beside `EpicObservationParser` with no change to
  `FhirRowNormalizer` or `FhirNormalizationService`.
- **`UcHealthIngestJob.stage()` already does dedup** — skip pending, refresh normalized —
  against a list of parsed JSON nodes. Source-neutral logic worth reusing rather than
  reimplementing.
- **`StagingRawFhirResourceController` already exposes POST.** A scraper can hand rows to
  the backend over REST. This matters: it means no direct database access, honoring the
  project's standing rule.

The scraper therefore replaces exactly one component — `UcHealthFhirClient` — as a source
of staging rows. Everything from staging onward (normalize → `lab_result` → RAG backfill →
retrieval) is untouched and already proven.

---

## Runtime choice: Java Playwright, following `viro-playwright`

**Decided: Java, `com.microsoft.playwright:playwright`, mirroring
`~/projects/viro/viro-playwright`.**

An earlier draft of this plan recommended Node. That was wrong — it assumed the user's
existing Playwright work was in Node. It is not: `viro-playwright` is a standalone Gradle
Java project on Playwright 1.47.0, and its patterns are directly reusable here.

What that project already solved, worth copying rather than reinventing:

- **`LoginService`** — a generic, selector-driven login taking url/username/password/button
  selectors plus a `successSelector` to confirm the session landed. Already handles the
  iframe-vs-main-page split, which matters: MyChart login pages are not always top-level.
- **`ConfigLoader`** — selectors and URLs live in `application.yaml`, not in code, so a
  portal reskin is a config edit rather than a recompile. Exactly the right shape for the
  fragility this plan is budgeting for.
- **Credentials via `java-dotenv`** from `../../../.env`, never in the YAML or in code.
- **Logback + `@Slf4j`**, and an `outputDir` convention for captured artifacts.

Structural decision to make when copying: `viro-playwright` is its own Gradle project with
its own `../../../settings.gradle`. Here it should become **a directory inside this repo** — either
a fifth Gradle module (`include 'playwright'`) or a standalone Gradle build under
`../../../playwright` that is not part of the root build. Prefer **standalone**: it keeps ~300MB of
browser binaries and a `main()`-driven scraper out of `./gradlew build` and off the
deployment jar, while still living in this repo where the staging contract lives.

`jsoup` is already a `datafetcher` dependency under a "Web Scraping" heading. It stays
useful for parsing HTML fragments server-side if a scraped payload is stored as HTML, but
it is not a substitute for a browser against an authenticated JS-rendered portal.

**Session persistence is the one thing `viro-playwright` does not do.** It logs in fresh
every run with `setHeadless(false)`. This project needs Playwright's storage state
(`BrowserContext` save/load) so a session survives between runs — that is the whole point
of step 1 below, and it is genuinely new code rather than a copy.

---

## Build order

Each step is verifiable on its own. Do not start a step before the one above it works.

### 1. Login and session, and nothing else — **DONE, 2026-08-08**

Verified against the real portal with the real account:

- Login works: the form is on the main page (no iframe), credentials fill from `../../../.env`,
  submit succeeds.
- **MFA prompts on a first login from a new device**, and the code goes to the patient's
  phone — not the operator's. The wait is 10 minutes for that reason; 2 minutes was not
  enough in practice.
- **After one MFA, UCHealth trusts the device.** A later run restored the saved storage
  state and authenticated in ~3 seconds with no login and no MFA prompt:
  `Restoring saved session` → `Existing session is still valid` → `SESSION REUSED`.

**Repeatable scraping is viable.** This is what steps 2-6 were waiting on.

Still unknown: **how long the trust lasts.** Reuse is proven over minutes, not over hours
or days. The first run on a later day settles it. The code already degrades correctly —
an expired session falls back to a fresh login rather than failing.

Two implementation lessons worth keeping:

- **`successSelector` must be visible on load.** The first guess, `text=Log Out`, never
  matched: on UCHealth's MyChart, logout lives inside the account-avatar dropdown and is
  not in the DOM until opened. `text=Test results` — one of the six home-page tiles — is
  correct and unambiguous. A screenshot of the logged-in page settled this in seconds
  after two wasted MFA logins.
- **A reuse check must require that a state file was actually restored.** Without that
  guard the check also passes when Chromium simply still holds a live login, reporting
  success while proving nothing. This produced a false `FIRST RUN` result that claimed a
  valid session three seconds after logging that no session file existed.

### 2. One category, read-only, dump to disk
Pick **Test results** — it is the category with a known-good normalized target
(`lab_result`), so the scraped shape can be checked against rows the FHIR path already
produced for the same patient. Navigate, extract, write raw payloads to local files. No
posting to the backend yet.

This produces the fixtures every later step is built against, and it is the point where
selector fragility becomes concrete rather than theoretical.

**Observed on the real portal, 2026-08-08** (from screenshots of the live account):

*List page* — `Test Results`, a flat list under an `Individual Results` heading. Each row
carries test name, collection date, and ordering provider. Real oncology labs are present:
CBC ONCOLOGY, COMPREHENSIVE METABOLIC PANEL, CANCER ANTIGEN 27.29 (a breast-cancer tumor
marker), magnesium, phosphorus, urinalysis.

- **"Showing 50 of many"** — 50 is not the total. Whatever reveals the rest (infinite
  scroll, load-more, paging) must be handled explicitly, and whatever is *not* captured
  must be logged. Silently taking the first 50 would read as a complete history when it
  is not.

*Detail page* — reached by clicking a row. URL shape:
`/MyChart/app/test-results/details?pageMode=1&eorderid=<opaque>`

- **`eorderid` is the dedup key.** Epic's encrypted order id, stable per result, URL-escaped
  (`-2B`=`+`, `-3D`=`=`, `-2F`=`/`). Goes into `staging_raw_fhir_resource.fhir_resource_id`
  directly — no synthetic hash needed.
- **Unverified: does `eorderid` persist across sessions?** Epic sometimes derives these
  per-session. If it changes between logins, dedup breaks and every run re-inserts
  everything. Confirm by reopening the same result after a fresh login, **before** step 4
  writes anything to staging.
- It is an *order* id, so a panel is expected to be one `eorderid` with many analytes —
  which matches the existing `lab_result` → `lab_result_component` parent/child split.

*Fields available on a single-analyte detail page* (MAGNESIUM SERUM), mapped to `lab_result`:
test name → `test_name`; "Collected on <date> <time>" → `effective_at`; the analyte row's
display name → component name; the numeric badge → `value_quantity`; "Normal range: 1.6 -
2.6 mg/dL" → `reference_range_low`/`_high` **and** `value_unit`.

- **The unit is only present inside the reference-range string**, not attached to the
  value. It has to be parsed out of that text. Same lesson as the sandbox payload where
  Epic returned a value with no unit at all - never assume value and unit travel together.

**Two gaps this raises:**

- **The provider's comment has no column.** The detail page carries a narrative note from
  the ordering provider (e.g. reassurance that no medication change is needed). It is
  clinically meaningful, embeds well for RAG, and has **no FHIR equivalent in the
  patient-facing API** — one of the categories this whole path exists to reach. `lab_result`
  has nowhere to put it. Needs a column or a related table; decide when the parser is written.
**Panels — observed 2026-08-08 (CBC, ONCOLOGY).** The parent/child design is confirmed
correct against real data: **one `eorderid` carries many analytes** (WBC, RBC, hemoglobin,
hematocrit, MCV, MCH, MCHC, platelets, RDW, NRBC, and differential percentages). This is
the first real validation of `lab_result_component`, which until now was designed from the
FHIR spec alone because the sandbox patient had no panels.

Three extraction hazards the single-analyte page did not reveal:

- **Card layout is not uniform within one page.** Most analytes render as name +
  "Normal range: X - Y unit" + a numeric badge on a range bar. But *Neutrophil Percent*
  has no range bar at all — a bare `%` line and `Value 58.4`. A parser keyed only on the
  badge-and-bar shape drops these silently. Extraction must handle at least: two-sided
  range with bar, one-sided range, and bare value with no range.
- **The unit appears in three different places** — inside a two-sided range
  ("4.0 - 11.0 10*9/L"), inside a one-sided range ("0 10*9/L or below"), or alone on its
  own line (Neutrophil's `%`). Note `10*9/L` is Epic's ASCII rendering of ×10⁹/L; store it
  verbatim rather than normalizing it.
- **One-sided ranges do not fit low/high columns.** "Normal range: 0.0 % or below" has no
  low/high pair. These belong in the existing `reference_range_text` column — the second
  field in this schema (after nullable `value_unit`) that was added defensively and turns
  out to be load-bearing against real data.

**The provider comment is per-encounter, not per-result.** The identical note, with the
identical timestamp, appears on both the MAGNESIUM SERUM and CBC ONCOLOGY pages from the
same collection. Storing it on `lab_result` would duplicate it across every result in a
batch. It belongs on whatever represents the encounter/result-release, or in its own table
keyed by provider + timestamp. Decide before the parser is written.

### 3. Decide the payload shape
With real scraped output in hand, decide whether scraped rows are shaped into
FHIR-like JSON (so an existing parser can be reused) or stored in their own shape with a
dedicated parser. **Do not decide this in advance** — the same lesson `lab_result` learned
from the real Epic payload, where the spec would have produced a wrong schema.

Also decide the `fhir_resource_id` equivalent: scraped rows need a stable natural key for
dedup. If the portal exposes no stable id, a deterministic hash of identifying fields is
the fallback. Getting this wrong means duplicate rows on every run.

### 4. Post to staging
Scraper posts to `StagingRawFhirResourceController` with a distinct `resource_type` so
scraped and API rows never collide in the unique constraint. Verify the dedup path holds
on a second run — the same check that caught the CT.gov bug.

### 5. Parser and normalization
A `FhirSourceParser` implementation for the scraped type, following
`EpicObservationParser`. Fixture-based tests from step 2's captured payloads, same pattern
as `ClinicalTrialsGovParserTest`.

### 6. The categories FHIR cannot reach
Only after the above works end to end for one category. Messages and appointment notes
have no existing normalized target and need their own table design — a separate schema
decision, informed by real scraped payloads.

---

## Open questions

- **Does storage state survive long enough to matter?** Determines whether this is
  repeatable or merely semi-automated. Answered by step 1.
- **Is there a stable per-result identifier in the portal DOM?** Determines the dedup key.
  Answered by step 2.
- **How much does the DOM change between visits?** Unknowable up front. Budget for
  selectors breaking, and prefer text/role-based selectors over structural ones.
- **Where do portal credentials live?** `../../../.env` is gitignored and is where the project's
  other secrets go. Whether a real portal password warrants more than that is the same
  unresolved question already open for the OAuth refresh token.
- **Does scraped data need provenance marking?** A `lab_result` row from a scrape and one
  from FHIR are not equally trustworthy — the API payload is structured and coded, the
  scrape is parsed from rendered text. If both can populate the same table, the source
  should be distinguishable.

---

## Explicitly out of scope

- **Writing anything back to the portal.** Read-only, always.
- **Scheduled/background scraping.** On demand only, matching every other ingestion path
  in this project.
- **Replacing the FHIR pipeline.** The API path stays and stays preferred where it works —
  coded, structured data beats parsed text.
- **Multi-patient support.** Unchanged from the rest of the project.
