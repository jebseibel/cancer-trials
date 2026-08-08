# Enum Lifecycle Rules

## Overview

Defines how enums flow through the system. For implementation patterns and code examples,
see `enum-to-db-mapping-patterns.md`.

> **Status (verified 2026-08-08).** `DisplayableEnum` and `InternalEnum` are real, live
> interfaces in `:common`, and `DisplayableEnum`'s javadoc cites these rule numbers directly —
> the doc and the code are coupled, so keep them in step.
>
> **The `GET /api/enums/{name}` endpoint referenced in Rules 1, 8, and 12 does not exist yet.**
> No `EnumController` or `EnumService` is present. Those rules describe the intended contract
> for when enum-driven dropdowns are added; nothing serves them today.
>
> Only three enums currently implement `DisplayableEnum`: `TrackingSystem`, `AiModel`, and
> `AiLifecycle`.

---

## Enum Types

### Type 1: ActiveEnum (Integer-backed)
Used exclusively for the `active` column on `BaseDb`. Stores `1`/`0`.
`common/src/main/java/com/seibel/cancer/common/enums/ActiveEnum.java`

### Type 2: DisplayableEnum (Domain enums)
Domain enums that appear in the UI implement `DisplayableEnum`
(`common/src/main/java/com/seibel/cancer/common/enums/DisplayableEnum.java`), which supplies
`normalize()` and `matchesDisplayValue()` and requires `isActive()`.

Current implementors: `TrackingSystem`, `AiModel`, `AiLifecycle`.

Note that most status-like values in this project are **not** yet enums — they are plain
`String` columns (`Trial.overallStatus`, `TrialStatus.status`, `Location.status`, and others)
with the vocabulary enforced only by convention, or by `as const` arrays on the frontend. Adding
`DisplayableEnum` implementations for those is the natural next step, and these rules are what
they should follow.

#### Enums with similar values — do not confuse

When two enums carry overlapping-looking constants, keep straight which column each belongs to.
Because entity fields are plain `String` (Pattern 2), nothing in the compiler, the mapper, or
the database will stop a value from one being written into the other's column — the failure
surfaces later as an `IllegalArgumentException` from `valueOf()` on read.

See `enum-data-issue.md` for the full mechanism and how to avoid it.

### Type 3: Simple enums
Internal processing flags. Do not implement `DisplayableEnum`. Not subject to these rules.

---
## Rule 1: Frontend-Only Presentation — displayValue
`displayValue` is used **only in the Frontend** when rendering enum values for the user.
It must never appear in API response bodies, service logic, or any backend layer.
The FE resolves `displayValue` from the enum list endpoint (`GET /api/enums/{name}`) using the `name()` it received.

## Rule 2: Database Storage — name()
Enum values are stored in the DB as their Java constant name (uppercase). See `enum-to-db-mapping-patterns.md`.

## Rule 3: name() Is the System-Wide Identifier
`name()` is used everywhere in the system: database storage, service logic, controller request/response bodies, and logs.
**The only place `displayValue` appears is in the Frontend rendering layer. No exceptions.**

For the code pattern, see `enum-to-db-mapping-patterns.md`.

## Rule 4: Ordering — sortOrder
When presenting enum values in ordered UI elements, sort by `sortOrder` ascending. Use `displayableValues()`.

## Rule 5: Visibility — displayable
Only constants where `displayable=true` appear in UI dropdowns. `displayableValues()` filters automatically.

## Rule 6: Default Value — preferred
Exactly one constant per enum has `preferred=true`. Used for pre-selection in dropdowns. `getDefault()` returns it.

## Rule 7: Display Values Are Unique
Every `displayValue` within an enum must be unique. Required because `fromDisplayValue()` lookups must be unambiguous.

## Rule 8: Enum List Response Shape
Each entry in the enum list endpoint response must include: `name`, `displayValue`, `sortOrder`, `preferred`, `displayable`.
The FE uses `name` as the value it sends back on form submissions, and `displayValue` for rendering only.
`active=false` constants excluded.

## Rule 9: Never Return Inactive Values in Enum Lists
Constants with `active=false` are excluded from all enum list responses and dropdowns.
A record that already has a retired value stored may still return it in its own data response — that is acceptable historical data.

- `displayable=false` — live value, hidden from UI selection only
- `active=false` — fully retired; excluded from all enum lists

## Rule 10: fromString() — Throw on Unknown, Default on Null/Blank

Input is always `name()` (the Java constant name). The FE sends `name()` on form submissions.

| Input | Result |
|-------|--------|
| `null` or blank | return `getDefault()` |
| unknown value | throw `IllegalArgumentException` |
| known but `active=false` | return the constant — caller checks `isActive()` |

## Rule 11: Load Table Preservation — Store Raw, Transform to Canonical
- **Load table** — store the value exactly as received. No conversion. Values may be displayValues (e.g. `"Solar"`, `"Wind"`), not constant names.
- **Transform step** — convert from displayValue to `name()` using `fromDisplayValue()`, then save `name()` to the production table.
- **Production table** — always contains `name()`.

The transform step must use `fromDisplayValue()`, not `valueOf()`, because load tables store displayValues from external sources.

See `enum-to-db-mapping-patterns.md` for the code pattern.

## Rule 12: Enum List API Endpoint
`GET /api/enums/{name}` returns `name`, `displayValue`, `sortOrder`, `preferred`, `displayable` per entry. `active=false` constants excluded.
The FE uses `name` as the value for form submissions and API calls; `displayValue` is for rendering only.

## Rule 14: fromDisplayValue() — Normalize Before Matching
`fromDisplayValue()` must not use `equalsIgnoreCase()` directly. Instead use `matchesDisplayValue()` inherited from `DisplayableEnum`.

`DisplayableEnum` provides:
- `normalize(String s)` — static method: lowercases and strips all whitespace
- `matchesDisplayValue(String input)` — default method: normalizes both sides before comparing

This handles case differences and spacing variations from external sources (load tables, CSV imports) without each enum implementing its own logic.

## Rule 13: API Transport — Always the Uppercase Constant Name
All enum values passed between FE and BE (in request bodies, response bodies, and query parameters)
must be the uppercase constant name — e.g. `"ACTIVE"`, `"PENDING_REVIEW"`, `"SOLAR"`.

**Exceptions (only two):**
- **Display to users** — the FE resolves `displayValue` from the enum cache for rendering only; it is never sent back to the BE
- **Load tables** — ingest values exactly as received from the source; they may not be valid constant names

---

## Related Docs

- `enum-to-db-mapping-patterns.md` — patterns, code examples, module boundary details
- `restapi-code-style.md` — controller/service/converter layering
