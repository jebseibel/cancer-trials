# UCHealth / Epic FHIR — Database Tables

Companion document to `UCHEALTH_INGESTION_PLAN.md`, broken out for iterating on the
schema independently — the same split `_archive/clinical-trials/clinical-trials-tables.md` has
from `../../PROJECT_PLAN.md`. MySQL, managed via Liquibase changelogs.

This covers the **patient clinical record** pulled from UCHealth's Epic FHIR R4
endpoint. It is additive: nothing here touches the trial/condition/sponsor tables.

## Conventions

Same as the clinical trials schema. Every core table carries the `BaseDb` fields:

```
id              bigint          not null, primary key, auto_increment
extid           varchar(36)     not null, unique                  -- UUID, external-facing id
created_at      datetime        not null, default CURRENT_TIMESTAMP
updated_at      datetime
deleted_at      datetime                                          -- soft delete
active          int             default 1                          -- ACTIVE/INACTIVE
```

Each table below lists only its *own* fields beyond these. Types are MySQL column types
as they'll appear in the Liquibase changeset (`bigint`, `varchar(n)`, `text`,
`longtext`, `datetime`, `date`, `tinyint(1)`, `int`, `decimal(x,y)`).

**Liquibase note:** decimal types must be quoted in the changeset YAML
(`type: "decimal(10,3)"`) — an unquoted comma breaks the YAML flow-mapping parser and
aborts the changelog mid-run. This has bitten this project twice.

**extid-only rule:** any endpoint exposing a cross-entity reference uses the target's
`extid`, never its numeric id — including FK-like fields. Applies to everything here
from the start.

---

## Already built (scaffolded ahead of Epic registration)

These two exist in full (domain → entity → mapper → repository → dbservice → service →
DTOs → controller), changesets `018` and `019`. Listed here for completeness.

> ✅ **All five tables in this document are now built — verified 2026-08-14.** The three under
> "Normalized tables" below shipped as changesets `020` (patient_medication), `021` (lab_result)
> and `022` (lab_result_component); their columns match this spec exactly. They are, however,
> still **empty** — the pipeline works but Epic's grants do not. See `UCHEALTH_INGESTION_PLAN.md`.
>
> ⚠️ One correction to the note above: `UcHealthOAuthTokenController` **no longer exists.** It
> exposed full CRUD over the token table, including reading refresh tokens back over HTTP, and
> was removed. The only UCHealth controller is `UcHealthAuthController`.

### `uchealth_oauth_token`
One row — this is a single-patient app. Written by the OAuth callback, read by every
FHIR call.

```
access_token        varchar(2048)   -- short-lived bearer token (~1 hour)
refresh_token       varchar(2048)   -- long-lived; requires the offline_access scope
expires_at          datetime        -- when access_token dies; refreshed 60s ahead
patient_fhir_id     varchar(64)     -- Epic's patient id, returned at token exchange
scope               varchar(1024)   -- scopes actually granted (may differ from requested)
```

**Treat this table like credentials.** Never seed it, never commit a real token, never
log its contents. See the open question below about encryption at rest before real
(non-sandbox) credentials land.

### `staging_raw_fhir_resource`
One row per fetched FHIR resource, before normalization. Mirrors
`staging_raw_trial`'s shape.

```
resource_type          varchar(64)     not null    -- "MedicationRequest", "Observation", ...
fhir_resource_id       varchar(64)     not null    -- Epic's id for the resource (dedup key)
raw_payload            longtext        not null    -- FHIR JSON exactly as returned
fetched_at             datetime        not null
normalized_at          datetime                    -- null = pending normalization
normalization_error    text                        -- populated if mapping failed
```

Unique on (`resource_type`, `fhir_resource_id`) — the dedup key, analogous to
`staging_raw_trial`'s source+nctId pair. In place from the start rather than added after
discovering duplicates.

---

## Normalized tables

Scope note: this doc grows one resource type at a time. `MedicationRequest` was designed
first (flattest FHIR shape), but Epic's sandbox blocked it — see the status note below —
so `Observation` was built end to end first instead and is the only resource whose design
is confirmed against real payloads.

**Build status as of the first real sandbox run:**

- **`Observation` (lab results)** — fetch → staging **verified working** against Epic's
  sandbox with Camila Lopez's record: authenticated call returns 200, the payload lands
  in `staging_raw_fhir_resource` verbatim, and re-running dedups (0 written, 1 skipped).
  Normalized tables designed below, not yet scaffolded.
- **`MedicationRequest`** — normalized table designed and fully scaffolded (changeset
  `020`), but **the fetch is blocked**: Epic rejects the patient search with *"Combination
  of parameters is not valid for any authorized sub-resource"*. The app was originally
  registered for `MedicationRequest.Read/Search (Order Template Medication)`, which is the
  formulary template catalog, not a patient's prescriptions. Re-registered for
  **`Signed Medication Order` (R4)** — the correct sub-resource — but the grant had not
  propagated at time of writing. Retry the ingestion endpoint before assuming anything is
  wrong with the code.
- **`DiagnosticReport`** — returns **403** despite `.Read/.Search (Results) (R4)` being
  registered. Not investigated; not needed for either slice above.

### `lab_result`
One row per laboratory `Observation` — a test result. Deduplicated by
`fhir_resource_id`. This design is based on a **real Epic sandbox payload** (Camila
Lopez's Hemoglobin A1C, captured at
`../../../datafetcher/src/test/resources/sample-epic-observation.json`), not on the spec alone.

```
fhir_resource_id       varchar(64)     not null, unique   -- Observation.id, the natural key
test_name              varchar(500)    not null           -- display text of the test
loinc_code             varchar(32)                        -- LOINC code, when coded
status                 varchar(32)                        -- final, preliminary, amended, ...
category               varchar(64)                        -- laboratory, vital-signs, ...
effective_at           datetime                           -- when the specimen/observation applies
issued_at              datetime                           -- when the result was released
value_quantity         decimal(18,6)                      -- numeric result, when numeric
value_unit             varchar(64)                        -- NULLABLE even when a value exists
value_string           varchar(1000)                      -- non-numeric result (e.g. "Negative")
interpretation         varchar(128)                       -- H/L/N, abnormal flags
reference_range_low    decimal(18,6)
reference_range_high   decimal(18,6)
reference_range_text   varchar(255)                       -- when the range is narrative
is_panel               tinyint(1)                         -- true when component rows exist
display_text           text            not null           -- readable one-line summary
```

**`value_unit` is deliberately nullable, and this is not a guess.** Epic's real payload
returned `"valueQuantity": {"value": 5.1}` — a value with **no unit at all**. A schema
assuming value and unit travel together would have been wrong on the very first row.

**`value_quantity` vs `value_string`:** FHIR allows `valueQuantity`, `valueString`,
`valueCodeableConcept`, `valueRange`, and more. Numeric results go in `value_quantity`;
anything else is rendered into `value_string`. At most one is populated. If real data
shows `valueRange` or `valueCodeableConcept` appearing often enough to query on, they
deserve their own columns rather than being flattened into text.

**`is_panel`** is a convenience flag mirroring "this row has `lab_result_component`
children" — denormalized on purpose so a common query doesn't need a join or subquery.
The parser sets it; nothing else should write it.

### `lab_result_component`
One row per `Observation.component` entry — the individual analytes of a panel. A CBC or
metabolic panel arrives as **one** Observation carrying several components, not as
several Observations, so without this table a panel's values would be lost or crushed
into prose.

```
lab_result_id          bigint          not null    -- FK -> lab_result.id
component_name         varchar(500)    not null    -- display text of the analyte
loinc_code             varchar(32)
value_quantity         decimal(18,6)
value_unit             varchar(64)                 -- nullable, same reason as the parent
value_string           varchar(1000)
interpretation         varchar(128)
reference_range_low    decimal(18,6)
reference_range_high   decimal(18,6)
reference_range_text   varchar(255)
display_text           varchar(1000)   not null    -- readable one-line summary of this analyte
```

This is a child table with its own identity (it carries `BaseDb` fields), not a pure
join table — each component is a real observation value, not a link between two entities.

**Camila's sandbox record has no panels** — her one lab is a flat A1C with no
`component` array. This table is designed ahead of confirmed need because real oncology
labs (CBC, CMP, liver panels) are panel-shaped, and retrofitting a child table after
rows exist means a data migration. Accepted as over-general for sandbox, correct for
production.

**Re-normalization:** when an already-normalized `lab_result` is refreshed from a newer
payload, delete and re-insert its component rows rather than diffing — the same
"clean slate" approach the trial normalizer uses for its child records.

### `patient_medication`
One row per `MedicationRequest` — a prescription written for the patient, active or
historical. Deduplicated by `fhir_resource_id`.

**Scaffolded but not yet fetchable** — see the build-status note above. The column list
below is spec-derived and has **not** been validated against a real Epic payload, unlike
`lab_result`. Treat its field mapping as provisional until a real MedicationRequest is
staged.

```
fhir_resource_id       varchar(64)     not null, unique   -- MedicationRequest.id, the natural key
medication_name        varchar(500)    not null           -- display text of the drug
rxnorm_code            varchar(32)                        -- RxNorm code when coded
status                 varchar(32)                        -- active, completed, stopped, cancelled, ...
intent                 varchar(32)                        -- order, plan, proposal
authored_on            date                               -- when the prescription was written
dosage_text            varchar(1000)                      -- full dosage instruction, free text
dose_quantity          decimal(12,3)                      -- numeric dose when discretely coded
dose_unit              varchar(64)                        -- mg, mL, tablet, ...
route                  varchar(128)                       -- oral, intravenous, ...
frequency_text         varchar(255)                       -- "twice daily", "every 8 hours"
prescriber_name        varchar(255)                       -- requester display name
reason_text            varchar(1000)                      -- why prescribed, when present
validity_start         date                               -- dispenseRequest validity period
validity_end           date
refills_allowed        int
display_text           text            not null           -- readable one-line summary, see below
```

**On `display_text`:** a human-readable rendering of the row, assembled by the parser —
e.g. *"Tamoxifen 20 mg oral tablet, once daily, active, prescribed 2025-03-14 by Dr.
Chen."* This exists because the stated end goal is **RAG over the medical record**, and
a chunk of readable prose embeds far better than a reassembled set of columns. The
structured columns above stay queryable in plain SQL; `display_text` is what a future
embedding pass consumes. Both are derived from the same payload, so they can't drift —
re-normalizing rebuilds both.

**Why no FK to a patient table:** this app stores exactly one patient's record (the
plan explicitly rules out multi-patient support). Adding a `patient_id` FK would be
dead weight. If that ever changes, it's an additive column, not a redesign.

> ⚠️ **That "if that ever changes" has happened — 2026-08-14.** A real `patient` table exists
> (changeset `028`) with `user_patient` grants deciding who may read it, and five other clinical
> tables were repointed to `patient_id`: `patient_diagnosis`, `patient_variant`,
> `patient_prior_treatment`, `trial_status`, `trial_match`.
>
> **The three tables in this document were not repointed.** `lab_result`, `lab_result_component`
> and `patient_medication` still have no `patient_id`. That is consistent — they are also still
> empty and blocked on Epic grants — but it means **the moment Epic data flows for a second
> patient, these rows have no owner** and are unreachable through the grant model that guards
> everything else. As predicted, the fix is an additive column rather than a redesign.

---

## Source field mapping reference (FHIR R4 `Observation`)

**Confirmed against a real Epic sandbox payload**, not spec-derived. Paths are relative
to the resource root.

| Core table / column | Source FHIR path |
|---|---|
| `lab_result.fhir_resource_id` | `id` |
| `lab_result.test_name` | `code.text`, else the LOINC coding's `display` |
| `lab_result.loinc_code` | `code.coding[]` where `system` == `http://loinc.org` → `.code` |
| `lab_result.status` | `status` |
| `lab_result.category` | `category[].coding[]` where `system` ends `observation-category` → `.code` |
| `lab_result.effective_at` | `effectiveDateTime` (or `effectivePeriod.start`) |
| `lab_result.issued_at` | `issued` |
| `lab_result.value_quantity` | `valueQuantity.value` |
| `lab_result.value_unit` | `valueQuantity.unit` — **often absent, see below** |
| `lab_result.value_string` | `valueString`, else rendered from `valueCodeableConcept.text` |
| `lab_result.interpretation` | `interpretation[0].text`, else `.coding[0].code` |
| `lab_result.reference_range_low` / `_high` | `referenceRange[0].low.value` / `.high.value` |
| `lab_result.reference_range_text` | `referenceRange[0].text` |
| `lab_result.is_panel` | derived: true when `component[]` is non-empty |
| `lab_result.display_text` | assembled by the parser |
| `lab_result_component` (rows) | `component[]` |
| `lab_result_component.component_name` | `component[].code.text` |
| `lab_result_component.loinc_code` | `component[].code.coding[]` where `system` == LOINC |
| `lab_result_component.value_quantity` / `_unit` | `component[].valueQuantity.value` / `.unit` |

Three things the **real payload** established that the spec alone would not have:

1. **`valueQuantity.unit` can be absent.** Epic returned `{"value": 5.1}` with no unit
   for the A1C. Never assume a unit accompanies a value.
2. **`code.coding[]` holds more than one coding.** The A1C carried both LOINC
   (`4548-4`) and an Epic-internal OID coding (`urn:oid:1.2.840.114350...`). **Select by
   `system`, never by position** — `coding[0]` is not reliably LOINC.
3. **Search Bundles carry a trailing `OperationOutcome` entry.** Epic appends a
   "this may not be the complete record" warning as a Bundle entry. It is *not* clinical
   data — the client filters entries by `resourceType` before staging. This was a real
   bug caught by the first live call, not a hypothetical.

## Source field mapping reference (FHIR R4 `MedicationRequest`)

For when normalizing staging rows into `patient_medication`. Paths are relative to the
resource root (the object stored in `staging_raw_fhir_resource.raw_payload`).

| Core table / column | Source FHIR path |
|---|---|
| `patient_medication.fhir_resource_id` | `id` |
| `patient_medication.medication_name` | `medicationCodeableConcept.text`, else `medicationCodeableConcept.coding[0].display` |
| `patient_medication.rxnorm_code` | `medicationCodeableConcept.coding[]` where `system` is the RxNorm URI → `.code` |
| `patient_medication.status` | `status` |
| `patient_medication.intent` | `intent` |
| `patient_medication.authored_on` | `authoredOn` |
| `patient_medication.dosage_text` | `dosageInstruction[0].text` |
| `patient_medication.dose_quantity` | `dosageInstruction[0].doseAndRate[0].doseQuantity.value` |
| `patient_medication.dose_unit` | `dosageInstruction[0].doseAndRate[0].doseQuantity.unit` |
| `patient_medication.route` | `dosageInstruction[0].route.text`, else `.coding[0].display` |
| `patient_medication.frequency_text` | `dosageInstruction[0].timing.code.text`, else derived from `timing.repeat` |
| `patient_medication.prescriber_name` | `requester.display` |
| `patient_medication.reason_text` | `reasonCode[0].text`, else `reasonCode[0].coding[0].display` |
| `patient_medication.validity_start` | `dispenseRequest.validityPeriod.start` |
| `patient_medication.validity_end` | `dispenseRequest.validityPeriod.end` |
| `patient_medication.refills_allowed` | `dispenseRequest.numberOfRepeatsAllowed` |
| `patient_medication.display_text` | assembled by the parser from the above |

**These paths are provisional.** They follow the FHIR R4 spec, but Epic populates
resources unevenly in practice and the sandbox may differ from UCHealth production.
Verify each against a real sandbox payload before trusting the mapping — capture one as
a test fixture (same pattern as `sample-clinicaltrials-study.json`) and let the actual
JSON settle any disagreement with this table.

`medicationCodeableConcept` vs. `medicationReference` is the most likely surprise: FHIR
allows either, and if Epic returns a reference to a contained `Medication` resource
rather than an inline concept, the name/code mapping above needs a second branch that
resolves it.

---

## Open questions

- **Does Epic's sandbox return `medicationCodeableConcept` or `medicationReference`?**
  Determines whether the parser needs a reference-resolution branch. Answer from a real
  payload, not from the spec.
- **Is `dosageInstruction[0]` enough?** The field is an array — a tapering schedule or
  split dose can carry several entries. Taking only the first would silently drop
  information. If real data shows multiple entries with any regularity, this needs a
  child table (`patient_medication_dosage`) rather than flat columns.
- **Encryption at rest for `uchealth_oauth_token`.** Currently plain columns, which is
  fine for sandbox synthetic patients. Decide before the one real authorization against
  a live My Health Connection account — a refresh token is a long-lived credential to a
  real medical record. Related: the scaffolded `UcHealthOAuthTokenController` exposes
  full CRUD including reading tokens back over HTTP; consider removing or restricting it
  before real credentials land.
- **Indexing.** At minimum `patient_medication.fhir_resource_id` (unique, the dedup
  lookup), `patient_medication.status`,
  `staging_raw_fhir_resource.(resource_type, fhir_resource_id)` (already unique), plus
  `lab_result.fhir_resource_id` (unique), `lab_result.loinc_code` and
  `lab_result.effective_at` (the natural "this test over time" query), and
  `lab_result_component.lab_result_id`.

- **No refresh token — the biggest open issue.** Epic granted
  `patient/*.read fhirUser launch/patient openid` but **dropped `offline_access`**, even
  though it was requested. So no refresh token is issued, the access token dies in about
  an hour, and every ingestion run past that needs a fresh interactive browser login.
  The refresh code path exists and is unit-tested, but has never run against Epic because
  there has been nothing to refresh. This is tolerable for sandbox testing and **not**
  tolerable against a real record — it defeats the "trigger ingestion whenever" goal.
  Resolve before the production authorization: check whether Epic's app registration has
  a persistent-access option that must be enabled, or whether `offline_access` needs
  requesting differently.

- **Does `drop-first` stay off?** `application.yml` had `spring.liquibase.drop-first: true`,
  which rebuilt the schema on every restart and **deleted the stored OAuth token every
  time** — meaning a re-login per restart. Turned off so tokens survive. Trade-off: edits
  to an already-applied changeset no longer take effect on startup; rebuild the DB (n8n
  `clear-db` webhook) to pick those up. Revisit if schema iteration gets painful.

- **Panels are unverified.** `lab_result_component` is designed from the FHIR spec, not
  from observed data — Camila's sandbox record contains exactly one lab and it has no
  `component` array. The parent/child split is the right shape for real oncology labs,
  but the component field mapping stays provisional until a real panel is staged.

- **Sandbox data is thin.** One lab result total. Enough to prove the pipeline, not
  enough to characterize what real payloads look like. Don't over-fit the parser to it.
- **`display_text` length.** `text` holds ~64KB, far beyond a one-line summary. Sized
  generously on purpose — a future chunking strategy may want more than one line, and
  widening a column later is a migration.
- **Messages remain out of scope.** Confirmed during Epic app registration: no
  messaging resource is offered in the patient-facing API catalog at all. MyChart secure
  messages would require browser automation against the portal UI — a fundamentally
  different approach, not to be started without a separate deliberate decision.
- **`MedicationStatement` is unavailable in R4.** Epic's catalog offers it only in
  DSTU2/STU3. Dropped from scope; `MedicationRequest` alone carries prescriptions.
