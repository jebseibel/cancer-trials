# The wrong-enum-in-the-wrong-column trap

A failure mode that Pattern 2 (String-backed enum columns) cannot catch for you. Worth reading
before adding enums to this project — the conditions that allow it are all present today.

Originally written up as a specific bug in the ViroTrade project (two facility status enums
crossed). That bug is not reproduced here; the mechanism is, and that is the part worth keeping.

## The mechanism

1. Two enums exist whose values look plausible in the same slot — a record-lifecycle status and
   an external-system status, say.
2. Code writes the wrong one into the other's column. A builder call like `.status(x)` where `x`
   came from the other enum. It compiles: both are `String`.
3. The mapper copies it through. ModelMapper does a field-name-matched copy with **no enum
   validation** — a bad value is as valid as a good one.
4. The DB accepts it. The column is `varchar`, not an enum type or FK.
5. Nothing fails **until a read** calls `SomeEnum.valueOf(badValue)` and throws
   `IllegalArgumentException` — often in `toResponse()`, so an entire list endpoint 500s over
   one bad row.

The gap between the bad write and the visible failure is the dangerous part: the record that
breaks the page was written correctly-looking, hours or weeks earlier.

## Why this project is exposed

Verified 2026-08-08:

- **Zero `@Enumerated`** anywhere in `database/.../db/entity/`. Every enum-backed field is a
  plain `String` (Pattern 2 — see `enum-to-db-mapping-patterns.md`).
- **All 27 mappers are ModelMapper.** None validate enum values.
- **Nine enum-ish String columns, with repeated names** — `status` on `LocationDb`,
  `TrialStatusDb`, `LabResultDb`, `PatientMedicationDb`, `PurchaseDb`; `type` on
  `InterventionDb` and `ArmGroupDb`; `role` on `OverallOfficialDb` and `UserDb`.

Those repeated names are exactly the condition that makes a cross-wiring easy to write and hard
to spot in review. A `.status(...)` call tells you nothing about *which* status vocabulary the
value came from.

## Avoiding it

- **Set the enum, not a loose string.** `.status(TrialStatusValue.SAVED.name())` states the
  vocabulary at the call site; `.status(someOtherString)` hides it.
- **Never pass one enum's value into another's setter to "reuse" a matching constant.** If two
  enums share a constant name, that is a coincidence, not a relationship.
- **Guard reads on data you did not write.** For staging/load tables specifically, values arrive
  raw from an external source and may never have been valid — use a try/catch fallback rather
  than a bare `valueOf()`. See Rule 11 in `enum-lifecycle-rules.md`.
- **Suspect a bad write when a list endpoint throws `IllegalArgumentException` on one record.**
  The fix belongs at the write site; a catch at the read site only hides it.

The structural fix is Pattern 3 (`@Enumerated`), which makes the wrong assignment a compile
error. `enum-to-db-mapping-patterns.md` records that as deliberately deferred — so until then
this is a discipline problem, not something the type system will solve.

## Related

- `enum-to-db-mapping-patterns.md` — the three storage patterns and why Pattern 2 is current
- `enum-lifecycle-rules.md` — `fromString()` / `fromDisplayValue()` behaviour, load-table rules
