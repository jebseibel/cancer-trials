# To Do

## code-style archive content review

`clazzname-pattern.md`, `enum-data-issue.md`, `enum-lifecycle-rules.md`, `enum-to-db-mapping-patterns.md`, and `restapi-code-style.md` have had their package/module references fixed across two passes (`com.viro` → `com.seibel.jobhunting` → `com.seibel.cancer`), but the actual content still describes features and classes from a different project (facility/CRS/retirement-certificate tracking — e.g. `FacStatus`, `FacilityReconObjectBuilder`, `EnumService`, `CrsTrackingAttestationStatus`, the `facility_output` status bug narrative). None of these classes or features exist in this project.

**Decision (2026-07-24):** naming-only fixes are the standing policy for this archive — content/examples are kept as historical reference, not stripped or rewritten to match this project's actual domain. This applies across the whole `_archive/` directory, not just here. No further action needed on this file.
