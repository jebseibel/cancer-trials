# Enum Data Issue — facility_output.status

## Problem

Some `facility_output` records have `"NEW_APPLICATION"` stored in the `status` column.

`"NEW_APPLICATION"` is a `CrsTrackingAttestationStatus` constant. The `status` column maps to
`FacStatus`, which has no `NEW_APPLICATION` constant. This causes a runtime
`IllegalArgumentException` when `toResponse()` calls `FacStatus.valueOf("NEW_APPLICATION")`.

## Workaround (in place)

`FacilityOutputController.toResponse()` passes `item.getStatus()` as-is — no `valueOf()` call
is made, so no exception is thrown. The facility list loads, but affected records return
`"NEW_APPLICATION"` as their status instead of a valid `FacStatus` constant name.

## Root Cause

`FacilityReconObjectBuilder.buildFacilityFromPending()` has two bugs:

1. **Wrong field** — the `status` parameter receives `decision.crsStatus()` (a `CrsTrackingAttestationStatus`
   value e.g. `"NEW_APPLICATION"`) and sets it via `.status(status)` — writing it to `facility_output.status`
   (`FacStatus`) instead of `facility_output.crs_tracking_attestation_status`.

2. **Missing FacStatus** — no `FacStatus` is ever set on facilities created from CRS Pending.
   `buildFacilityFromApproved` has the same gap — no `FacStatus` set either.

The mapper (`FacilityOutputMapper`) is a plain `ModelMapper` with no enum conversion — it copies
the `status` string as-is. The db service `createAll` does not default `status`. So the caller
is responsible for setting it correctly, as `FacilityLoadService.resolveStatus()` does.

This is NOT a data loading issue — the CSV has no status column and `FacilityLoadService` handles
`status` correctly via `resolveStatus()`. The corruption only affects facilities created during
reconciliation.

## Fix Status

Fixed in `FacilityReconObjectBuilder` (`src/main/java/com/viro/app/service/recon/FacilityReconObjectBuilder.java`):

**`buildFacilityFromPending()`:**
- `.crsTrackingAttestationStatus(status)` — receives the CRS status string correctly
- `.status(FacStatus.LOADED.name())` — sets FacStatus correctly

**`buildFacilityFromApproved()`:**
- `.status(FacStatus.LOADED.name())` — sets FacStatus correctly

Both methods now correctly separate the two enum concerns.

## Valid FacStatus Values

| Constant | Display Value |
|----------|--------------|
| `ACTIVE` | Active |
| `FETCHED` | Fetched |
| `IMPORTED` | Imported |
| `LOADED` | Loaded |
| `MANUAL` | Manual |
| `NEW` | New |
| `RESOLVED` | Resolved |
| `STAGED` | Staged |
