# CLAZZNAME Pattern Documentation

## Overview
The CLAZZNAME pattern replaces hardcoded class name strings in log statements with a static final constant. This provides a single source of truth for the class identifier used in logging.

## Pattern Definition

### Declaration
Define a `private static final String` constant at the **top of the class**:

- **Variable Name:** `CLAZZNAME` (intentionally misspelled)
- **Visibility:** `private static final`
- **Position:** First constant declared in the class
- **Value:** The simple class name as a string (e.g., `"FacilityCrsApprovedRulesEngine"`)

```java
private static final String CLAZZNAME = "FacilityCrsApprovedRulesEngine";
```

### Usage in Log Statements
Replace hardcoded class name strings in `log` calls with `CLAZZNAME`:

Instead of:
```java
log.info("FacilityCrsApprovedRulesEngine - processing {}", id);
```

Use:
```java
log.info("{} - processing {}", CLAZZNAME, id);
```

## Key Points

- **Naming Convention:** The intentional misspelling "CLAZZNAME" makes it distinctive and searchable
- **Scope:** `private` to each class — not shared across classes
- **Type:** `String` constant (`static final`)
- **No constructor required:** This pattern does NOT use a `thisName` instance variable or constructor assignment — it is a static constant only
- **No `thisName`:** Do not add `thisName` or modify constructors as part of this pattern

## Applied Scope

The CLAZZNAME pattern has been implemented across 12 retirement certificate service classes:

- RetErcotEmailService
- RetErcotScreenshotService
- RetErcotTransDetailService
- RetMretsCertQuantService
- RetMretsTransConfirmService
- RetMretsTransDetailsService
- RetNarCertSubacctService
- RetNarRetireCertService
- RetNarVolComplyService
- RetWregisCertQuantService
- RetWregisTransConfirmService
- RetWregisTransDetailsService
