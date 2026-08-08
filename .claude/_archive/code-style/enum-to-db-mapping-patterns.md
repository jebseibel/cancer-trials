# Enum-to-Database Mapping Patterns

Covers how enums are stored, retrieved, and converted across module boundaries.
For the lifecycle rules, see `enum-lifecycle-rules.md`.

---

## Pattern 1: Integer-backed enum (ActiveEnum)

Used exclusively for the `active` column on `BaseDb`. Stores `1`/`0`.
Handled by a custom JPA converter — no manual conversion needed in service code.

- Enum: `common/src/main/java/com/seibel/cancer/common/enums/ActiveEnum.java`
- Converter: `database/src/main/java/com/seibel/cancer/database/converter/ActiveEnumConverter.java`

---

## Pattern 2: String-backed domain enums (current standard)

Entity fields are plain `String`. No JPA converter. Conversion is manual at the module boundary.
**Verified 2026-08-08: there is not a single `@Enumerated` in `database/.../db/entity/`** — this
is universally the pattern here.

- DB column stores `name()` — e.g. `"RECRUITING"`, `"RULED_OUT"`
- App module receives raw `String` from the DB module domain object
- Controller `toResponse()` passes `name()` as-is to the FE — no conversion to `displayValue`
- Controller inbound receives `name()` from FE, validates with `Enum.valueOf()`, stores `name()`
- FE resolves `displayValue` for rendering

Note the last step: **`GET /api/enums/{name}` does not exist yet.** Today the frontend hardcodes
its vocabularies as `as const` arrays in `frontend/src/types/api.ts` (`TRIAL_STATUS_VALUES`,
`STAGE_VALUES`, `RECEPTOR_STATUS_VALUES`, and others) and renders them with
`.replaceAll('_', ' ')`. That is the de facto display mechanism until an enum endpoint is built.

Current String-backed enum-ish columns include `status` (on `TrialStatusDb`, `LocationDb`,
`LabResultDb`, `PatientMedicationDb`), `type` (on `InterventionDb`, `ArmGroupDb`), and `role`
(on `OverallOfficialDb`, `UserDb`).

⚠️ Because the field is a bare `String` and all 27 mappers are ModelMapper with no enum
validation, nothing prevents one enum's value being written into another's column. See
`enum-data-issue.md`.

### Staging tables vs production tables

Staging tables (`staging_raw_trial`, `staging_raw_fhir_resource`) hold payloads exactly as
received from an external source — any status-like value in them may not be a valid enum
`name()`. Use a try/catch fallback when converting these. Normalized tables (`trial`,
`trial_status`, etc.) are written by our own code and should always contain valid `name()`
values.

---

## Pattern 3: JPA @Enumerated (future — not yet implemented)

Entity field would be the enum type directly. JPA handles `name()` ↔ DB automatically.
Eliminates manual conversion entirely.

**Decision:** Deferred. Pattern 2 is the current standard. Do not migrate in isolation.

---

## Conversion Points in Pattern 2

`name()` flows through the entire system. The only boundary where `displayValue` matters is the FE rendering layer.

| Direction | Where | Method |
|-----------|-------|--------|
| DB → Controller response | Controller `toResponse()` | Pass `name()` as-is |
| FE → Controller inbound | Controller inbound | `Enum.valueOf(request.getField())` to validate, store `name()` |
| Controller → FE rendering | Frontend only | FE calls `GET /api/enums/{name}` to resolve `displayValue` |

---

## Considered and Deferred

- **Override `toString()` to return `displayValue`** — skipped. Keeps `toString()` as `name()` for consistent logging.
- **`@JsonValue` / `@JsonCreator`** — skipped. Not needed while entity fields are `String` (Pattern 2).
- **`Optional`-returning `fromString()`** — skipped. `getDefault()` fallback is simpler.

---

## Related Docs

- `enum-lifecycle-rules.md` — lifecycle rules
- `restapi-code-style.md` — controller/service/converter layering
