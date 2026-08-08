# Multi-patient model — design and build plan

Replace the `app_user` concept with a real `patient`, and make a login's access to a patient
explicit and enforced. Written 2026-08-08 against the code as it stands.

Decision context: the app should support **several unrelated people, kept separate**. That is
what makes this a security boundary rather than a convenience — it is not the same problem as
"one family, several logins."

---

## 1. What is wrong today

Three concepts exist where there should be two, and one of them is a duplicate:

| Table | What it claims to be | What it actually is |
| --- | --- | --- |
| `user` | Login identity | Login identity. Correct. |
| `app_user` | Personal tracking profile | **A second login table** — it has its own `username` and `password_hash`, no FK to `user` |
| `patient_diagnosis` | Clinical facts | Correct, but hangs off `app_user` |

`app_user` carrying `password_hash` is the tell: a patient is not something that logs in.

Because there is no FK between `user` and `app_user`, the frontend reconciles them by
**string-matching usernames** (`useCurrentAppUser`). That is why three pages show "no app-user
profile linked" until a row is hand-seeded with exactly the right username.

### The blocking problem for multi-patient

**No controller reads the authenticated principal.** Verified 2026-08-08: there is not one
`SecurityContextHolder`, `@AuthenticationPrincipal`, or `Principal` parameter in any controller.
Every endpoint takes its target identity from the request — `appUserExtid` in a path or body —
and trusts it.

With one user and one patient that is harmless. With several patients kept separate it is the
entire problem: **any caller can pass any patient's extid and read that patient's record.**

`JwtAuthenticationFilter` already places the username in the `SecurityContext`, so the mechanism
exists and is simply unused. Note this is currently masked by `SecurityConfig` permitting all
requests — fixing that is a prerequisite, not a separate concern.

---

## 2. Target model

```
   user  ──────< user_patient >──────  patient  ───1:1───  patient_diagnosis
 (login)          (access grant)      (the person)         (clinical facts)
                                            │
                                            └────< trial_status
```

**`user`** — unchanged. Authentication only.

**`patient`** — the person a search is *for*. **No auth fields.** Dropping `password_hash` is
the correction that removes username-matching entirely.

**`user_patient`** — which logins may see which patients. Many-to-many because both directions
are real: two family members may share one patient, and one helper may follow several unrelated
patients. Collapsing this to `user.patient_id` only works if a login sees exactly one patient
forever, which is the assumption being relaxed.

**`patient_diagnosis`** — FK moves from `app_user_id` to `patient_id`. One current diagnosis per
patient.

**`trial_status`** — FK moves to `patient_id`. "This trial is interesting" is a fact about a
patient's search, not about who was holding the laptop; otherwise two people looking at the same
patient see different saved lists.

### Column specs

`BaseDb` supplies `id`, `extid`, `created_at`, `updated_at`, `deleted_at`, `active` — never
re-declare those.

**`patient`** (replaces `app_user`)

```
display_name               varchar(128)   not null    -- "Tina S." - what appears in the switcher
date_of_birth              date                       -- moved off patient_diagnosis, see note
sex                        varchar(16)                -- moved off patient_diagnosis, see note
notes                      varchar(1000)              -- non-clinical: relationship, context
```

**`user_patient`** (join table, no BaseDb fields per the tables-doc convention for pure links)

```
user_id                    bigint         not null    -- FK -> user.id
patient_id                 bigint         not null    -- FK -> patient.id
access_level               varchar(16)    not null    -- OWNER, EDITOR, VIEWER
                                                      -- unique (user_id, patient_id)
```

**Note on `date_of_birth` / `sex`:** these currently live on `patient_diagnosis` and are used by
Tier 1 matching. They are properties of the *person*, not of a diagnosis — and if diagnosis ever
becomes append-only history (`DIAGNOSIS_MATCHING_DESIGN.md` §9), duplicating them per row is
wrong. Move them to `patient`; Tier 1 reads them from there.

**`access_level` is deliberately included but not enforced in phase 1.** Recording it now is
free; adding the column later to a populated table is not. Phase 1 treats any grant as full
access.

---

## 3. Access control — the part that actually matters

The rule, stated once: **the patient is never taken from the request. It is resolved from the
JWT, then authorised.**

Every patient-scoped endpoint follows the same shape:

1. Read the username from the `SecurityContext` (already populated by `JwtAuthenticationFilter`).
2. Resolve `user` by username.
3. Resolve the requested `patient` by extid from the path.
4. **Verify a `user_patient` row links them.** If not — 404, not 403.
5. Proceed.

**404 rather than 403 is deliberate.** A 403 confirms the patient extid exists, which leaks the
existence of another person's record to anyone probing. A 404 says nothing.

Concretely this means `GET /api/patientdiagnosis/by-appuser/{appUserExtid}` — where the caller
names whose data it wants — is replaced by
`GET /api/patient/{patientExtid}/diagnosis`, authorised as above. The extid in the path is a
*claim*, not an authority.

**Prerequisite:** `SecurityConfig` currently ends `.anyRequest().permitAll()`. None of this is
real until that is restored. Sequence it first (see §5, step 0). On restore,
`/api/uchealth/callback` must stay `permitAll` — Epic's OAuth redirect cannot carry a JWT.

**Out of scope, stated explicitly:** the RAG endpoints (`/api/rag/search`, `/backfill`) and
ingestion are not patient-scoped. Trial data is shared and public; only the patient's own record
is private.

---

## 4. What changes, by layer

48 files reference `app_user`/`AppUser`/`appUser` today. Grouped:

**Delete outright** — `AppUser` domain, `AppUserDb`, `AppUserMapper`, `AppUserRepository`,
`AppUserDbService`, `AppUserService`, `AppUserController`, `RequestAppUserCreate`,
`RequestAppUserUpdate`, `ResponseAppUser`, and their three test classes. Replaced by the
`Patient` equivalents, which is the `entity-full-stack` skill's job.

**New** — `Patient` (full stack via `entity-full-stack`), `UserPatient` (thin link entity;
repository + db service only, no controller), and a `CurrentUserService` in root that resolves
username → `user` → authorised patients. That service is the single place the authorisation rule
lives; scattering it across controllers is how one endpoint ends up missing the check.

**Changed FKs** — `TrialStatus` and `PatientDiagnosis`: `appUserId` → `patientId` through
domain, entity, mapper, repository finders, db service, service, controller converter, DTOs, and
changesets `011` / `023`. Plus `date_of_birth` and `sex` move off `patient_diagnosis`.

**Frontend** — delete `useCurrentAppUser`; add `useCurrentPatient` backed by a new
`GET /api/patient/mine` (patients this login may see) plus a selected-patient context. Then
Dashboard, Diagnosis, SavedTrials, TrialDetail, and TrialSearch each read the selected patient
instead of matching usernames. A patient switcher goes in `Layout`.

**Tier 1 matching** — `tier1Matching.ts` takes a `PatientDiagnosis` today; it will need
`date_of_birth`/`sex` from `patient`. Smallest change is to pass both objects, or to have
`/api/patient/{extid}/diagnosis` return them merged. Prefer passing both — merging hides which
table owns which fact.

---

## 5. Build order

Each step ends compiling with tests green. **All three affected tables are currently empty
(verified 2026-08-08: 0 appuser, 0 trialstatus, 0 patientdiagnosis rows), so there is no data to
migrate — this is the cheapest this change will ever be.**

- **Step 0 — restore endpoint security.** Uncomment the JWT rule set in `SecurityConfig`, keep
  `/api/uchealth/callback` public, move the JWT secret to `JWT_SECRET` in `.env`. Nothing below
  is real without this.
- **Step 1 — `patient` table + full stack.** Run `entity-full-stack`. New changeset (do not edit
  `006-app-user.yaml`; a fresh number is clearer in the log).
- **Step 2 — `user_patient` link table.** Repository and db service only. Unique constraint on
  `(user_id, patient_id)`.
- **Step 3 — `CurrentUserService` + `GET /api/patient/mine`.** The authorisation rule, in one
  place, with tests covering the negative case: a login must not reach an unlinked patient.
- **Step 4 — repoint `PatientDiagnosis`.** FK, DTOs, controller path
  (`/api/patient/{extid}/diagnosis`), and move `date_of_birth`/`sex` to `patient`.
- **Step 5 — repoint `TrialStatus`.** Same shape.
- **Step 6 — delete the `AppUser` stack.** Only once nothing references it.
- **Step 7 — frontend.** `useCurrentPatient` + selected-patient context, then the five pages,
  then the switcher in `Layout`.
- **Step 8 — seed.** A `patient` row and a `user_patient` grant in `100-load-init-data.yaml`, so
  a rebuilt database is immediately usable. This is what makes the current manual AppUser-seeding
  step disappear.

Steps 1–6 are backend and independently testable. Step 7 is the largest single piece.

Note: `spring.liquibase.drop-first` is off, so changeset edits need a DB rebuild (n8n `clear-db`)
to take effect. New changesets apply normally.

---

## 6. Decisions still open

- **Does a VIEWER need to be prevented from editing a diagnosis?** Phase 1 records
  `access_level` but does not enforce it. Enforcing it means checking the level in
  `CurrentUserService`, which is a small change — but decide before the column has meaningful
  values.
- **What happens to `trial_status` written by user A when user B is removed from the patient?**
  Nothing, under the model above, since status belongs to the patient rather than the user. Worth
  confirming that is what you want.
- **Should the patient switcher persist across sessions?** localStorage is enough and matches how
  the JWT is already stored.
- **Does `patient` need soft-delete semantics distinct from `active`?** Removing a patient a
  helper no longer works with is arguably a `user_patient` deletion, not a patient deletion.

---

## 7. What this does not solve

The app still holds real clinical data with **no encryption at rest, no audit log of who read
what, and no HTTPS** in the current setup. Separating patients by login is necessary for
"several people I help, kept separate" — it is not sufficient to make that arrangement
responsible. See the application-blocker checklist in `../hosting/qa-setup.md` before this runs
anywhere but localhost.
