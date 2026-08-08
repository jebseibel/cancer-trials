# REST API Code Style

## Overview

This document defines the layered architecture and code style rules for all REST API endpoints
in the main application module. Every feature area follows the same pattern:

```
Controller → Service → (Database module)
```

Controllers handle HTTP only. Services handle business logic. Controllers never contain business logic.

> **Status (verified 2026-08-08).** The layering below is accurate and actively followed —
> every controller in `src/main/java/com/seibel/cancer/web/controller/` has its package-private
> `Converter` in the same file, and `BaseService` supplies `requireNonNull()` /
> `requireNonBlank()` in both one-arg and named-field forms.
>
> One exception to the text below: **no controller declares
> `throws DatabaseFailureException`.** `GlobalExceptionHandler` handles it centrally instead.
>
> For the full file-by-file scaffold, use the `database-restapi-template` skill — it is the
> maintained version of this pattern. This doc covers the style rules; the skill generates
> the code.

---

## Layer Responsibilities

### Controller
- HTTP routing only (`@RestController`, `@RequestMapping`, `@GetMapping`, etc.)
- Receives request, calls service, returns response
- Converts Domain → Response DTO via a package-private `Converter` class in the same file
- Converts Request DTO → Domain via the same `Converter`
- No business logic, no filtering, no sorting, no data transformation
- Lets `DatabaseFailureException` propagate to `GlobalExceptionHandler` — controllers in this
  project do **not** declare it in their `throws` clause
- Only explicit error handling: null checks and false return values

### Service
- All business logic lives here
- Validates inputs using `requireNonNull()` / `requireNonBlank()` from `BaseService`
- Logs operations with `log.info()`
- Delegates to the database module
- No HTTP concerns, no response building

### Converter (package-private, same file as Controller)
- DTO conversion only — Request → Domain, Domain → Response
- Stateless, no injected dependencies
- Uses builder pattern for all mappings
- `validateUpdateRequest()` for update requests (ensure at least one field provided)

---

## Enum endpoints — not built here

An `EnumController`/`EnumService` pair serving enum vocabularies to the frontend
(`GET /api/enums/{name}`) is assumed by `enum-lifecycle-rules.md` Rules 1, 8, and 12.
**Neither class exists in this project** — verified 2026-08-08.

Today the frontend hardcodes its vocabularies as `as const` arrays in
`frontend/src/types/api.ts` and renders them with `.replaceAll('_', ' ')`.

If that endpoint is built later, it is a special case worth noting: enum endpoints have no DB
entity, no repository, and no mapper, since the data comes entirely from Java enum constants in
`:common`. The layering rules still apply — the controller does HTTP routing only, and all
filtering (`isActive()`), sorting (`sortOrder`), and mapping to response shape belong in the
service.

---

## General Controller Rules

1. **One line per endpoint** — delegate immediately to service
2. **No `Map.of()` construction in controllers** — belongs in service
3. **No stream/filter/sort in controllers** — belongs in service
4. **No `LinkedHashMap` construction in controllers** — belongs in service or converter
5. **Converter handles all DTO mapping** — controller never touches field-by-field mapping directly
6. **No `if` blocks for data shaping** — only `if` blocks for null/false error responses

---

## Related Docs

- `restapi-template.md` — full layered architecture template with all generated file types
- `enum-lifecycle-rules.md` — enum display, serialization, and API boundary rules
- `enum-to-db-mapping-patterns.md` — DB storage patterns for enum fields
