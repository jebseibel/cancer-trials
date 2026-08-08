# CLAZZNAME Pattern Documentation

> **Status in this project (verified 2026-08-08).** Not yet used — zero occurrences of
> `CLAZZNAME` in the codebase. **Adopted going forward** (decision 2026-08-08): apply to new
> classes as they are written; do not retrofit existing ones in bulk.

## Overview
The CLAZZNAME pattern replaces hardcoded class name strings in log statements with a static final constant. This provides a single source of truth for the class identifier used in logging.

## Pattern Definition

### Declaration
Define a `private static final String` constant at the **top of the class**:

- **Variable Name:** `CLAZZNAME` (intentionally misspelled)
- **Visibility:** `private static final`
- **Position:** First constant declared in the class
- **Value:** The simple class name as a string (e.g., `"TrialRowNormalizer"`)

```java
private static final String CLAZZNAME = "TrialRowNormalizer";
```

### Usage in Log Statements
Replace hardcoded class name strings in `log` calls with `CLAZZNAME`:

Instead of:
```java
log.info("TrialRowNormalizer - processing {}", id);
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

**Not yet used in this project** — verified 2026-08-08, zero occurrences of `CLAZZNAME` in the
codebase. Adopted going forward (decision 2026-08-08); apply it to new classes as they are
written rather than retrofitting existing ones in bulk.

Where it earns its keep here: the multi-step pipeline classes whose logs interleave during a
run — `TrialRowNormalizer`, `TrialNormalizationService`, `ClinicalTrialsGovIngestJob`,
`FhirRowNormalizer`, `TrialIndexService`, `TrialBackfillService`. During a 700-trial ingestion
these all log at once, and the class prefix is what makes the output readable.

Note the existing convention in those classes is a bare method-name prefix —
`log.info("promote(): extid={}", extid)`. CLAZZNAME composes with it rather than replacing it:

```java
log.info("{} promote(): extid={}", CLAZZNAME, extid);
```
