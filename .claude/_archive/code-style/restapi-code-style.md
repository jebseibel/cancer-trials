# REST API Code Style

## Overview

This document defines the layered architecture and code style rules for all REST API endpoints
in the jobhunting main module. Every feature area follows the same pattern:

```
Controller → Service → (Database module)
```

Controllers handle HTTP only. Services handle business logic. Controllers never contain business logic.

---

## Layer Responsibilities

### Controller
- HTTP routing only (`@RestController`, `@RequestMapping`, `@GetMapping`, etc.)
- Receives request, calls service, returns response
- Converts Domain → Response DTO via a package-private `Converter` class in the same file
- Converts Request DTO → Domain via the same `Converter`
- No business logic, no filtering, no sorting, no data transformation
- Declares `throws DatabaseFailureException` and lets Spring handle it
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

## Enum-Specific Pattern

Enum endpoints are a special case — they have no DB entity, no repository, no mapper.
Their data comes entirely from Java enum constants in `:common`.

### Rule: No Business Logic in EnumController

`EnumController` must delegate all logic to `EnumService`. The controller does HTTP routing only.

`EnumController` is fully refactored — all filtering, sorting, and mapping live in `EnumService`. Each controller endpoint is a one-liner delegating to the service.

### EnumService

**Location:** `src/main/java/com/seibel/jobhunting/app/service/EnumService.java`
**Package:** `com.seibel.jobhunting.app.service`

Responsibilities:
- Filtering (`isActive()`)
- Sorting by `sortOrder`
- Mapping enum constants → response maps
- Special-field handling (e.g. `promotable` on `RetCertEligibilityStatus`)

**Pattern:**
```java
@Service
public class EnumService {

    public List<Map<String, Object>> toResponse(DisplayableEnum[] values) {
        return Arrays.stream(values)
                .filter(DisplayableEnum::isActive)
                .sorted(Comparator.comparingInt(DisplayableEnum::getSortOrder))
                .map(v -> Map.<String, Object>of(
                        "name", v.name(),
                        "displayValue", v.getDisplayValue(),
                        "sortOrder", v.getSortOrder(),
                        "preferred", v.preferred(),
                        "displayable", v.isDisplayable()
                ))
                .toList();
    }

    public List<Map<String, Object>> toResponseEligibility() {
        return Arrays.stream(RetCertEligibilityStatus.values())
                .filter(RetCertEligibilityStatus::isActive)
                .sorted(Comparator.comparingInt(RetCertEligibilityStatus::getSortOrder))
                .map(v -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("displayValue", v.getDisplayValue());
                    map.put("sortOrder", v.getSortOrder());
                    map.put("preferred", v.preferred());
                    map.put("displayable", v.isDisplayable());
                    map.put("promotable", v.isPromotable());
                    return map;
                })
                .toList();
    }

    public Map<String, Object> getAll() {
        return Map.of(
                "crsStatus", toResponse(CrsStatus.values()),
                "crsTrackingAttestationStatus", toResponse(CrsTrackingAttestationStatus.values()),
                "facStatus", toResponse(FacStatus.values()),
                "facNercRegion", toResponse(FacNercRegion.values()),
                "facRenewableType", toResponse(FacRenewableType.values()),
                "retCertRecordStatus", toResponse(RetCertRecordStatus.values()),
                "retCertUploadStatus", toResponse(RetCertUploadStatus.values()),
                "eligibilityStatus", toResponseEligibility()
        );
    }
}
```

**EnumController (current implementation):**
```java
@RestController
@RequestMapping("/api/enums")
public class EnumController {

    private final EnumService enumService;

    public EnumController(EnumService enumService) {
        this.enumService = enumService;
    }

    @GetMapping("/all")
    public Map<String, Object> getAll() {
        return enumService.getAll();
    }

    @GetMapping("/crs-status")
    public List<Map<String, Object>> getCrsStatus() {
        return enumService.toResponse(CrsStatus.values());
    }

    // ... etc — each endpoint is a one-liner delegating to enumService
}
```

**Status:** Implemented. `EnumService` exists at `src/main/java/com/seibel/jobhunting/app/service/EnumService.java`. `EnumController` delegates all logic to `EnumService`.

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
