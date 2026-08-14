# Patients and Access — Backend Build Plan

The backend half of `PATIENT_ACCESS_PLAN.md`. Read that first for the model and the reasoning;
this document is the build order, the file-by-file blast radius, and the traps.

Written 2026-08-13 from a survey of the actual code. The frontend half is a separate document
written against this plan's finished API contract.

**Status as of 2026-08-14: ✅ steps 1-7 are BUILT and committed** (`c9cb30d`). ⬜ **Step 8 —
grant and revoke endpoints — is the only one not started**, confirmed against
`PatientController`, which has `/mine` but no `share` mapping.

The build order, blast radius and traps below are kept as the record of how it was done. Read
per-step status markers rather than the prose, which is written in the future tense throughout.

---

## Roles are a separate axis, and they are already planned

**Not in this document, deliberately.** `../frontend/ADMIN_ONLY_INGESTION_PLAN.md` covers
`ROLE_ADMIN` and restricting trial ingestion and backfill to admins.

The two axes answer different questions and must not be conflated:

| Axis | Question | Mechanism |
| --- | --- | --- |
| **Roles** | What *operations* may you perform? | `user.role` → `ROLE_ADMIN` / `ROLE_USER`, checked with `@PreAuthorize` |
| **Grants** | *Whose data* may you see? | `user_patient` rows, checked by `CurrentUserService` |

Ingesting trials is a role question — trial data is shared and public, and a corpus pull is an
expensive operational act. Reading a diagnosis is a grant question. Neither substitutes for the
other: an admin has no automatic right to read a patient's record, and a record owner has no
right to trigger a corpus rebuild.

⚠️ **Do not model sharing as roles.** "Sarah is a viewer" is not a property of Sarah; it is a
property of the relationship between Sarah and one record. Encoding it as a role breaks the
moment a second patient exists.

**Sequencing:** the roles work is independent and much smaller — four `@PreAuthorize`
annotations plus a `SecurityConfig` matcher. It can land before, during, or after this plan.
Its one prerequisite is unrelated to anything here: confirming what `role` values `jeb` and
`admin` actually hold, since if the patient's own login is already `ADMIN` the restriction
changes nothing for the only person using the app.

⚠️ **One interaction worth noting.** Under this plan, `GET /api/patient/mine` returns only
patients the caller has a grant to — **including for an admin**. An admin is not implicitly
granted access to every record. That is deliberate: an operations role should not be a skeleton
key to medical data. If an admin-override is ever wanted, it should be an explicit, audited grant
rather than an implicit consequence of the role.

---

## The measured blast radius

`AppUser`/`appUser`/`app_user` appears in **89 files**: 6 in `:common`, 50 in `:database`,
33 in root. Not all need changing — many are tests that follow their subject — but the count is
the honest scale of this change.

| Group | Files | Fate |
| --- | --- | --- |
| `AppUser` stack (domain → controller + DTOs) | 11 | **Deleted**, replaced by `Patient` |
| `AppUser` tests (mapper, repository, dbservice) | 3 | Deleted with it |
| Five entities carrying `app_user_id` | 5 | FK renamed to `patient_id` |
| Their repositories / db services / services / controllers | 20 | Finders and params renamed |
| Their tests | 15 | Follow their subject |
| `DomainBuilderDatabase` | 1 | Shared builder — **append, never overwrite** |
| `PatientSeedLoader` | 1 | Creates patient + OWNER grant |
| `TrialMatchingService` + `TrialMatchingController` | 2 | `appUserId` → `patientId` throughout |

**The five entities:** `PatientDiagnosisDb` (changeset `023`), `PatientVariantDb` (`026`),
`PatientPriorTreatmentDb` (`027`), `TrialStatusDb` (`011`), `SavedTrialMatchDb` (`024`).

---

## A bug to fix while passing through — ✅ FIXED

> **Verified 2026-08-14.** `TrialStatusRepository.findByPatientId` is now a `default` method
> delegating to `findByPatientIdAndActive(patientId, ActiveEnum.ACTIVE)`, and carries a comment
> recording the `PatientDiagnosisRepository` precedent. Fixed in step 5 as planned.

⚠️ **`TrialStatusRepository.findByAppUserId` has no active filter.**

```java
List<TrialStatusDb> findByAppUserId(Long appUserId);   // returns soft-deleted rows
```

The other four all filter on `ActiveEnum.ACTIVE` via a `default` method, and two of them carry a
comment recording that `PatientDiagnosisRepository` shipped without it and had to be fixed on
2026-08-08. `../CURRENT_STATE.md` predicted this one — *"`TrialStatusRepository.findByAppUserId`
has the same shape and has not been checked"* — and it is confirmed.

**Fix it in step 5 when the finder is renamed anyway.** Do not fix it as a drive-by earlier: it
changes behaviour, and bundling it with a rename means one test run attributes both.

---

## Build order

Every step ends compiling with `:database` and root tests green. **Do not start a step before the
one above it is verified.**

### ✅ Step 1 (done) — `patient` table and full stack

Use the `entity-full-stack` skill. Inputs, so it runs uninterrupted:

- **Entity:** `Patient`
- **Source:** the column spec in `PATIENT_ACCESS_PLAN.md` §3
- **Base class:** `BaseDomain`
- **FK fields to keep:** none
- **Module:** `:database`

```
display_name    varchar(128)  not null
date_of_birth   date
sex             varchar(16)
notes           varchar(1000)
```

**New changeset** (next free number — note `014` is already used twice, so check the directory).
Do **not** edit `006-app-user.yaml`; a fresh number reads more clearly in the changelog.

⚠️ **No `username`, no `password_hash`.** Their absence is the whole point — a patient does not
log in.

### ✅ Step 2 (done) — `user_patient` link table

Repository and db service only. **No controller** — grants are managed through a purpose-built
endpoint in step 8, not generic CRUD.

```
user_id             bigint       not null   -- FK -> user.id
patient_id          bigint       not null   -- FK -> patient.id
access_level        varchar(24)  not null   -- OWNER | EDIT_RECORD | VIEW_RECORD | VIEW_TRIALS
granted_by_user_id  bigint
granted_at          datetime     not null
revoked_at          datetime
note                varchar(255)
```

Unique on `(user_id, patient_id)` among active rows. Finders needed:
`findByUserIdAndRevokedAtIsNull`, `findByPatientIdAndRevokedAtIsNull`,
`findByUserIdAndPatientIdAndRevokedAtIsNull`.

**`AccessLevel` belongs in `:common` as an enum**, not a bare string — it is compared in
authorisation logic, and a typo in a string comparison fails open. Follow the existing
`ActiveEnum` pattern. The DB column stays `varchar(24)`, consistent with the rest of the schema.

### ✅ Step 3 (done) — `CurrentUserService` and `GET /api/patient/mine`

**The single most important step. Everything else is plumbing.**

In root, `service/`. One class owns the rule:

```
resolveCurrentUser()                              -- SecurityContext -> UserDb
patientsFor(currentUser)                          -- active grants -> List<Patient>
requireAccess(patientExtid, AccessLevel minimum)  -- throws if not permitted
```

⚠️ **`requireAccess` must throw a 404-mapping exception, never 403.** A 403 confirms the extid
exists, leaking that a given person has a record to anyone probing. `ResourceNotFoundException`
already maps to 404 in `GlobalExceptionHandler` — reuse it rather than inventing a type.

⚠️ **This is the first code in the app to read `SecurityContextHolder`.** Verified: no controller
or service references it today. `JwtAuthenticationFilter` populates it already, so the mechanism
works and is simply unused — but that means there is **no existing example to copy**, and it must
be tested directly rather than assumed.

**Tests are the deliverable here, not the code:**

- Owner reaches their own patient ✅
- A grantee at a sufficient level reaches it ✅
- A grantee at an insufficient level gets **404** ✅
- A user with **no** grant gets **404**, not 403 ✅
- A **revoked** grant gets 404 ✅
- No authentication at all → 401 from the filter chain, before this runs ✅

`GET /api/patient/mine` returns the patients this login may see, each with the caller's own
access level — the frontend needs the level to decide what to render.

### ✅ Step 4 (done) — repoint `PatientDiagnosis`, and move `date_of_birth` / `sex`

`appUserId` → `patientId` through domain, entity, mapper, repository finders, db service, service,
controller converter, both request DTOs, the response DTO, and changeset `023`.

**Also move `date_of_birth` and `sex` off `patient_diagnosis` onto `patient`.** They are
properties of a person, not of a diagnosis. Use the `database-column-change` skill for the drops.

⚠️ **Tier 1 matching reads both fields** (`frontend/src/lib/tier1Matching.ts` takes a
`PatientDiagnosis`). The response DTO for diagnosis loses two fields, so **this step breaks the
frontend until its own plan lands.** Expected and acceptable — but know it before running it, and
do not interpret the resulting frontend failure as a defect in this step.

New path: `GET /api/patient/{patientExtid}/diagnosis`, authorised through `CurrentUserService`.

### ✅ Step 5 (done) — repoint the other four

`PatientVariant`, `PatientPriorTreatment`, `TrialStatus`, `SavedTrialMatch`. Same shape as step 4,
no field moves.

**Fix `TrialStatusRepository.findByAppUserId`'s missing active filter here** — see above.

`TrialMatchingService.loadPatientRecord(Long appUserId)` and `assessAll(..., Long appUserId)`
become `patientId`, as do `TrialMatchingController`'s two paths:

- `GET /api/matching/rank/{patientExtid}`
- `GET /api/matching/trial/{trialExtid}/for/{patientExtid}`

Both gain a `requireAccess(..., VIEW_TRIALS)` check — the lowest level, since ranking is exactly
what `VIEW_TRIALS` exists to permit.

### ✅ Step 6 (done) — delete the `AppUser` stack

Only once nothing references it. Eleven main files plus three test classes:

```
common/domain/AppUser.java
database/db/{entity/AppUserDb, mapper/AppUserMapper, repository/AppUserRepository,
             service/AppUserDbService}.java
src/service/AppUserService.java
src/web/controller/AppUserController.java
src/web/request/{RequestAppUserCreate, RequestAppUserUpdate}.java
src/web/response/ResponseAppUser.java
database/src/test/.../{AppUserMapperTest, AppUserRepositoryTest, AppUserDbServiceTest}.java
```

**Drop `app_user` in a new changeset**, not by editing `006`. Editing an applied changeset breaks
its checksum, and `drop-first` is `false`.

`DomainBuilderDatabase` loses its AppUser builders and gains Patient ones — **append to that file,
never overwrite it**; it is shared by every `:database` test.

### ✅ Step 7 (done) — `PatientSeedLoader`

Currently calls `resolveOrCreateAppUser(username)` per CSV row, keyed on the username column.

New behaviour, per row: resolve the `user` by username → create the `patient` if absent, using
`display_name` → create an `OWNER` grant linking them.

⚠️ **This is what makes the migration cheap** — see "Migration" below. It must land with steps 4-6,
not after, or a rebuild produces an app with no patient and no grants.

**Keep all four existing rules:** seed-if-absent and never sync; keyed on username, not extid; a
missing file is not an error; a malformed row never blocks startup.

⚠️ **The CSVs have a `username` column that now means "the owning login", not "the patient".**
Worth a comment in the loader, because the same string is doing a different job.

### ⬜ Step 8 (NOT STARTED) — grant and revoke endpoints

`POST /api/patient/{patientExtid}/share` — grant, OWNER only.
`DELETE /api/patient/{patientExtid}/share/{shareExtid}` — revoke by writing `revoked_at`.
`GET /api/patient/{patientExtid}/share` — who can see this record, OWNER only.

**Everything above is worth building even if this step never is.** Steps 1-7 close the
authorisation gap; step 8 is the sharing feature.

⚠️ **How a grantee gets a login is deferred** (`PATIENT_ACCESS_PLAN.md` §9). `/api/auth/register`
stays ADMIN-only for now, so grants can only be issued to accounts an admin has already created by
hand. **That is enough to build and test this step** — the grant endpoints do not care how the
grantee's account came to exist. Decide the invite mechanism when you get here.

---

## Migration

⚠️ **The archived plan's "these tables are empty, this is free" claim expired on 2026-08-09.**
`patient_diagnosis`, `patient_variant` and `patient_prior_treatment` now hold a real medical
record, locally and on production.

**The migration path is rebuild-and-reseed, not a data migration**, because `PatientSeedLoader`
recreates all four rows from the three gitignored CSVs in `.claude/patient-data/`. That is only
true if step 7 lands with steps 4-6.

Sequence:

1. Land steps 1-7 locally, tests green.
2. Rebuild the local DB via the n8n `clear-db` webhook (**user's action**).
3. Confirm the seed loader recreated patient + grant + the three clinical rows.
4. Verify CSV vocabulary against `frontend/src/types/api.ts` first — that check has caught silent
   drift before, and the backend stores plain varchars that accept a wrong value silently.

⚠️ **Do this before the production corpus pull.** A rebuild invalidates the Qdrant collection,
and re-embedding is hours on prod's 1 vCPU. Prod's corpus is **empty today**, so the rebuild is
nearly free right now. After Phase 4 of the deploy runbook it is not.

⚠️ **`trial_match` is the one table the seed loader does not recreate.** One stored run exists
locally (`search_run_id 22ccb562…`, 15 matches, 77 criterion rows). It will be lost in the
rebuild. Judged acceptable — it was written through the REST API as a design exercise and is
reproducible — but decide deliberately rather than discovering it afterwards.

---

## Verification

**Per step:** `./gradlew :database:test` and `./gradlew test`, with the backend stopped.

⚠️ **Do not run Gradle while the backend is running.** `spring-boot-devtools` watches build
output and hot-restarts against a half-written classpath; it surfaces as
`Not a managed type: class ...UserDb` or a `ClassNotFoundException`, both on a `restartedMain`
thread, and both look like real configuration faults. This has cost more time than any actual bug
in this project.

⚠️ **`BUILD SUCCESSFUL` does not mean tests ran.** Read counts from
`{module}/build/test-results/test/TEST-*.xml`.

**Baseline before starting:** `:database` 820, root 71, `:common` 12, `:datafetcher` 55,
`:rag` 35 — 0 skipped, 0 failures.

**End-to-end, after step 5**, with two accounts and one patient:

| Check | Expect |
| --- | --- |
| Owner reads their patient's diagnosis | 200 |
| Second user, no grant, reads it by extid | **404** (not 403) |
| Second user with `VIEW_TRIALS` calls `/api/matching/rank/{extid}` | 200 |
| Same user reads `/diagnosis` | **404** — the level is insufficient |
| Revoke the grant, retry | **404** |
| `GET /api/patient/mine` as each user | Only their own patients |

⚠️ **The 404-not-403 check is the one most likely to be got wrong**, and it is invisible from the
UI — both render as "not found". Test it at the HTTP level.
