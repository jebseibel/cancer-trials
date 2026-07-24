# To Do

## code-style archive content review

`clazzname-pattern.md`, `enum-data-issue.md`, `enum-lifecycle-rules.md`, `enum-to-db-mapping-patterns.md`, and `restapi-code-style.md` had their package/module references fixed (`com.viro` → `com.seibel.jobhunting`, `viro-server` → `jobhunting`), but the actual content still describes features and classes from a different project (facility/CRS/retirement-certificate tracking — e.g. `FacStatus`, `FacilityReconObjectBuilder`, `EnumService`, `CrsTrackingAttestationStatus`, the `facility_output` status bug narrative). None of these classes or features exist in this project. Only the naming was corrected, not the substance — revisit later to decide whether to strip the Viro-specific examples/narratives down to the genuinely reusable patterns (e.g. enum `name()` vs `displayValue` conventions, the CLAZZNAME logging pattern) or leave as historical reference.
