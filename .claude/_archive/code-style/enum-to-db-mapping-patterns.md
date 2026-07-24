# Enum-to-Database Mapping Patterns

Covers how enums are stored, retrieved, and converted across module boundaries.
For the lifecycle rules, see `enum-lifecycle-rules.md`.

---

## Pattern 1: Integer-backed enum (ActiveEnum)

Used exclusively for the `active` column on `BaseDb`. Stores `1`/`0`.
Handled by a custom JPA converter — no manual conversion needed in service code.

- Enum: `common/src/main/java/com/seibel/jobhunting/common/enums/ActiveEnum.java`
- Converter: `database/src/main/java/com/seibel/jobhunting/database/converter/ActiveEnumConverter.java`

---

## Pattern 2: String-backed domain enums (current standard)

All domain enums (`FacStatus`, `CrsStatus`, `CrsTrackingAttestationStatus`, etc.) use this pattern.
Entity fields are plain `String`. No JPA converter. Conversion is manual at the module boundary.

- DB column stores `name()` — e.g. `"LOADED"`, `"PENDING_REVIEW"`
- App module receives raw `String` from the DB module domain object
- Controller `toResponse()` passes `name()` as-is to the FE — no conversion to `displayValue`
- Controller inbound receives `name()` from FE, validates with `Enum.valueOf()`, stores `name()`
- FE resolves `displayValue` for rendering via `GET /api/enums/{name}`

Reference implementations:
- `src/main/java/com/seibel/jobhunting/app/web/controller/FacilityOutputController.java` — `toResponse()`, `create()`, `update()`
- `common/src/main/java/com/seibel/jobhunting/common/enums/FacStatus.java` — `valueOf()`

### Load tables vs production tables

Load tables (`facility_load`, `crs_approved`, `crs_pending`) store values exactly as received —
they may not be valid enum `name()` values. Use a try/catch fallback in `toResponse()` for these.
Production tables (`facility_output`, etc.) always contain valid `name()` values.

Reference: `src/main/java/com/seibel/jobhunting/app/web/controller/FacilityLoadController.java` — `toDisplayValue()` helper

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
