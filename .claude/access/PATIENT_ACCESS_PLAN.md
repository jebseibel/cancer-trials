# Patients, Access and Sharing — Design and Build Plan

Replace `app_user` with a real `patient`, resolve identity from the JWT rather than the URL, and
let a patient share her record with family.

**This document supersedes two others and is the single source for this work:**

- `../_archive/patient/PATIENT_MODEL_PLAN.md` (2026-08-08) — the original multi-patient design.
  Its model and most of its reasoning survive intact here; three of its decisions were better
  than the later draft's and are preserved with attribution below.
- `ACCESS_MODEL_PLAN.md` (2026-08-13) — the sharing-focused draft, now folded in. Delete it.

Rewritten 2026-08-13 against the current code. Companion to `../CURRENT_STATE.md`,
`../frontend/ADMIN_ONLY_INGESTION_PLAN.md` (roles — a separable, much smaller change), and
`../hosting/DEPLOY_RUNBOOK.md`.

**Status: designed, nothing built.**

---

## 1. The three use cases this must serve

Stated first, because they are what the model is answerable to.

| # | Case | What it requires |
| --- | --- | --- |
| 1 | A user creates a record **for themselves** | A patient, owned by a user |
| 2 | A user creates a record for themselves, then **shares it** | Grants from that patient to other logins |
| 3 | A user creates a record **for someone they love** | **A patient with no login at all**, owned by a different user |

**Case 3 is the one that decides the schema, and it is the current situation.** This app was
built by the patient's husband; he enters the data, runs the ranks, reads the criteria. She may
or may not ever log in.

Today `PatientSeedLoader` creates an `app_user` keyed on username `jeb`, and every clinical row —
*her* diagnosis, *her* PIK3CA mutation, *her* prior treatment — hangs off it. **The row labelled
`jeb` holds his wife's medical record.** There is no field anywhere recording whose body the data
describes.

Under the target model, **cases 1 and 3 are the same operation**: creating a record makes a
`patient` and grants the creator ownership. Whether that patient is you or someone you love is a
value in `display_name`, not a different code path. Three use cases collapsing into one mechanism
is the signal the model is right.

And if she later wants a login, **the data never moves** — it is one grant row. Under today's
schema it would mean migrating her clinical rows from your `app_user` to hers.

---

## 2. What is wrong today

Three concepts exist where there should be three, but one table is doing two jobs:

| Table | What it claims to be | What it actually is |
| --- | --- | --- |
| `user` | Login identity | Login identity. **Correct, leave alone.** |
| `app_user` | Personal tracking profile | **A second login table** — own `username` and `password_hash`, no FK to `user` |
| `patient_diagnosis` etc. | Clinical facts | Correct, but hang off `app_user` |

**`app_user.password_hash` is the tell: a patient is not something that logs in.** It is
`not null`, authenticates nothing, and had to be given an invented value when the row was created.

Because there is no FK between `user` and `app_user`, they are reconciled by **string-matching
usernames**. The frontend does this in `useCurrentAppUser`, and does it badly — it fetches
*every* app_user and filters client-side:

```
appUserApi.getAll() -> content.find(u => u.username === username)
```

That is the cause of the "no app-user profile linked" state on three pages, and of the "No
AppUser-seeding UI" gap in `../CURRENT_STATE.md` — which is really "there is a second account
table nobody can create accounts in."

### Definitions, stated plainly

- **`user`** — someone who can log in. Credentials, role, session. Not a clinical concept.
- **`patient`** — a person with a diagnosis and a medical record. **Need never log in.**
- **grant** — which users may see which patients, and at what level.

### The blocking problem

**No controller reads the authenticated principal.** Verified 2026-08-08 and still true: not one
`SecurityContextHolder`, `@AuthenticationPrincipal`, or `Principal` parameter in any controller.
Every endpoint takes its target from the request and trusts it.

`JwtAuthenticationFilter` populates the `SecurityContext` already. The mechanism exists and is
unused.

---

## 3. Target model

```
   user  ──────< user_patient >──────  patient  ───1:1───  patient_diagnosis
 (login)          (access grant)      (the person)    ├──< patient_variant
                                                       ├──< patient_prior_treatment
                                                       ├──< trial_status
                                                       └──< trial_match
```

**`user`** — unchanged. Authentication only.

**`patient`** — replaces `app_user`. **No auth fields.** Dropping `username` and `password_hash`
is what removes username-matching entirely.

**`user_patient`** — many-to-many, because both directions are real: several family members may
share one patient, and one helper may follow several unrelated patients.

⚠️ **Ownership is a grant, not a column.** Carried over from the archived plan and better than the
`owner_user_id` the later draft proposed: an `OWNER` row in `user_patient` means "myself" is not a
special case in any query or check. One code path for cases 1, 2 and 3.

### Column specs

`BaseDb` supplies `id`, `extid`, `created_at`, `updated_at`, `deleted_at`, `active` — never
re-declare those.

**`patient`** (replaces `app_user`)

```
display_name    varchar(128)   not null    -- "Alex" - what appears in the switcher
date_of_birth   date                       -- MOVED off patient_diagnosis, see below
sex             varchar(16)                -- MOVED off patient_diagnosis, see below
notes           varchar(1000)              -- non-clinical: relationship, context
```

**`user_patient`** (link table)

```
user_id         bigint        not null     -- FK -> user.id
patient_id      bigint        not null     -- FK -> patient.id
access_level    varchar(24)   not null     -- see vocabulary below
granted_by_user_id bigint                  -- who granted it, for audit
granted_at      datetime      not null
revoked_at      datetime                   -- null = active; revoke by writing, never deleting
note            varchar(255)               -- "my sister" - the patient's own words
                                           -- unique (user_id, patient_id) among active rows
```

⚠️ **`date_of_birth` and `sex` move to `patient`.** From the archived plan, and missed by the
later draft. They are properties of a *person*, not of a diagnosis — and if diagnosis ever becomes
append-only history (`../_archive/diagnosis/DIAGNOSIS_MATCHING_DESIGN.md` §9), duplicating them
per row is wrong. **Tier 1 matching reads both today** (`frontend/src/lib/tier1Matching.ts`), so
it changes with them.

⚠️ **Revoke by writing `revoked_at`, never by deleting.** Who had access to a medical record and
when is exactly the history worth keeping, and it matches the soft-delete discipline used
everywhere else in this schema.

### Access levels

```
VIEW_TRIALS | VIEW_RECORD | EDIT_RECORD | OWNER
```

- **`VIEW_TRIALS`** — the ranked trial list and saved trials, **but not the diagnosis, variants
  or treatment history**. The level most family members actually want, and the one a boolean
  cannot express: someone can help hunt for trials without reading a genomic report.
- **`VIEW_RECORD`** — the full clinical record, read-only.
- **`EDIT_RECORD`** — may also update it. For a spouse or primary caregiver.
- **`OWNER`** — full control, including granting and revoking. Created with the patient.

The archived plan proposed `OWNER / EDITOR / VIEWER`; `VIEW_TRIALS` is the genuine addition, and
it exists because of use case 2.

⚠️ **Record `access_level` from day one but enforce only OWNER-vs-not in phase 1.** From the
archived plan: the column is free now and expensive to add to a populated table later. Enforcing
the finer levels is a small follow-up in one service.

---

## 4. Access control — the part that actually matters

The rule, stated once: **the patient is never taken from the request. It is resolved from the
JWT, then authorised.**

Every patient-scoped endpoint:

1. Read the username from the `SecurityContext` (already populated by `JwtAuthenticationFilter`).
2. Resolve `user` by username.
3. Resolve the requested `patient` by extid from the path.
4. **Verify an active `user_patient` row links them at a sufficient level.** If not — **404**.
5. Proceed.

⚠️ **404, not 403.** Carried from the archived plan and the single best decision in either
document. A 403 confirms the extid exists, leaking that a given person has a record to anyone
probing. A 404 says nothing.

**`/api/patient/{patientExtid}/diagnosis` replaces
`/api/patientdiagnosis/by-appuser/{appUserExtid}`.** The extid in the path is a *claim*, not an
authority.

**One service owns this check** — `CurrentUserService` (or `AccessService`). Scattering
`SecurityContextHolder` reads across controllers is how the sixth endpoint ends up missing it.

**Out of scope, explicitly:** `/api/rag/search`, `/api/rag/backfill` and ingestion are not
patient-scoped. Trial data is shared and public; only the patient's record is private. Those are
governed by roles instead — `../frontend/ADMIN_ONLY_INGESTION_PLAN.md`.

### Roles are a separate axis

**Do not model sharing as roles.** "Sarah is a viewer" is not a property of Sarah; it is a
property of the relationship between Sarah and one record. Encoding it as a role breaks the moment
a second patient exists. `ROLE_ADMIN` governs *operational capability* (ingest, backfill,
register); grants govern *whose data you may see*.

---

## 5. Why one table rather than an authorization engine

This is ReBAC — the Google Zanzibar model — and `user_patient` is a Zanzibar tuple
(subject, relation, resource) in all but name.

| Considered | Verdict |
| --- | --- |
| **OpenFGA / SpiceDB** | Built for billions of checks over trillions of tuples. This is one patient and perhaps five grants, on a 1 vCPU box already sharing 3.9GB with MySQL, two Ghost containers and `cpss`. Operational cost against a problem one table solves |
| **`spring-security-acl`** | Closest first-party fit; four prescribed tables, a bitmask permission model, and the least-maintained corner of Spring Security. More machinery than the thing it replaces |
| **Keycloak** | Would replace the half that already works. Authentication here is done and verified |
| **Casbin** | Embeddable Java, no service to run, declarative policy. **Worth a look for §4's checks** — it does not help with §3's schema work |

**Keeping the tuple shape means a later migration to a real engine is a data move, not a
redesign.** That is the reason to follow the pattern rather than invent one.

And the decisive point: **no library fixes the actual blocker.** `user` and `app_user` reconciled
by string comparison, with nothing owning anything, is identical work whatever engine follows.

---

## 6. What changes, by layer

The archived plan counted 48 files referencing `app_user`. **That count has grown** — it predates
`patient_variant`, `patient_prior_treatment` and `trial_match`.

**Delete outright** — the whole `AppUser` stack: domain, `AppUserDb`, mapper, repository,
dbservice, service, controller, `RequestAppUserCreate`/`Update`, `ResponseAppUser`, and their
tests. Replaced by `Patient` equivalents via the `entity-full-stack` skill.

**New** — `Patient` (full stack), `UserPatient` (repository + db service only, no CRUD
controller), and `CurrentUserService` in root.

**Changed FKs — five tables, not two.** `appUserId` → `patientId` through domain, entity, mapper,
repository finders, db service, service, controller converter, DTOs and changeset, for:

- `PatientDiagnosisDb` (changeset `023`) — plus `date_of_birth`/`sex` moving to `patient`
- `PatientVariantDb` (`026`)
- `PatientPriorTreatmentDb` (`027`)
- `TrialStatusDb` (`011`)
- `SavedTrialMatchDb` (`024`)

**Five `by-appuser` endpoints** become `/api/patient/{extid}/...`, plus
`/api/matching/rank/{appUserExtid}` and `/api/matching/trial/{trialExtid}/for/{appUserExtid}`.

**`PatientSeedLoader`** — keys on username and calls `resolveOrCreateAppUser(username)` per CSV
row. It must create a `patient` plus an `OWNER` grant to the matching `user`, or seeded rows
arrive orphaned.

**Frontend** — delete `useCurrentAppUser` (and its fetch-everything-and-filter approach); add
`useCurrentPatient` backed by `GET /api/patient/mine`, plus a selected-patient context. Dashboard,
PatientRecord, SavedTrials, TrialDetail, TrialSearch and RankedTrials read the selected patient. A
patient switcher goes in `Layout` — where `NAV_ITEMS` is now a single shared list, so it renders
once for both desktop and mobile.

**Tier 1 matching** — `tier1Matching.ts` takes a `PatientDiagnosis`; it needs
`date_of_birth`/`sex` from `patient`. **Pass both objects rather than merging** — merging hides
which table owns which fact.

---

## 7. Build order

Each step ends compiling with tests green.

- ~~**Step 0 — restore endpoint security.**~~ ✅ **Done 2026-08-11.** Every endpoint requires a
  JWT; `/api/uchealth/callback` correctly stays public; `JWT_SECRET` is an env var. The archived
  plan's prerequisite is satisfied.
- **Step 1 — `patient` table + full stack** via `entity-full-stack`. New changeset, not an edit to
  `006-app-user.yaml`.
- **Step 2 — `user_patient` link table.** Repository and db service only.
- **Step 3 — `CurrentUserService` + `GET /api/patient/mine`.** The authorisation rule in one
  place, **with a test for the negative case**: a login must not reach an unlinked patient, and
  must get 404 rather than 403.
- **Step 4 — repoint `PatientDiagnosis`**, and move `date_of_birth`/`sex` to `patient`.
- **Step 5 — repoint `PatientVariant`, `PatientPriorTreatment`, `TrialStatus`,
  `SavedTrialMatch`.**
- **Step 6 — delete the `AppUser` stack**, once nothing references it.
- **Step 7 — frontend.** `useCurrentPatient` + context, then the pages, then the switcher.
- **Step 8 — sharing.** Grant/revoke endpoints and a "who can see my record" page. **Everything
  above is worth doing even if this is never built.**
- **Step 9 — seed.** A `patient` row and an `OWNER` grant in `PatientSeedLoader`, so a rebuilt
  database is immediately usable and the manual AppUser step disappears.

Steps 1-6 are backend and independently testable. Step 7 is the largest single piece.

---

## 8. The cost, corrected

⚠️ **The archived plan's central cost claim has expired, and this is the most important
correction in this document.**

It states: *"all three affected tables are currently empty (verified 2026-08-08: 0 appuser,
0 trialstatus, 0 patientdiagnosis rows), so there is no data to migrate — this is the cheapest
this change will ever be."*

**That is no longer true.** It predates the patient data landing on 2026-08-09. Those tables now
hold a real medical record — diagnosis, variants, prior treatment — both locally and on
production. "No data to migrate" has become "migrate one real medical record, carefully."

**Mitigating this: `PatientSeedLoader` rebuilds all four rows from the three gitignored CSVs.**
So the migration path is *rebuild and re-seed* rather than *write a data migration* — provided
the seed loader is updated in the same change (step 9). Verify the CSV vocabulary against
`frontend/src/types/api.ts` first; that check has caught silent drift before.

⚠️ **Still far cheaper now than later. Prod's Qdrant corpus is empty today.** A rebuild
invalidates the collection, and re-embedding after Phase 4 of the deploy runbook is hours on
1 vCPU. **This is a real argument for doing this before the corpus pull, not after.**

Also: `spring.liquibase.drop-first` is `false`, so changeset edits need a rebuild via the n8n
`clear-db` webhook. New changesets apply normally.

---

## 9. Decisions still open

- ~~**How does a grantee get a login?**~~ **Deferred 2026-08-13, deliberately.** It blocks only
  backend step 8 and frontend item 8 (the sharing UI); everything before that is worth building
  regardless. Revisit when step 8 is reached, with the grant model actually in front of you.

  **Why the current lock exists, and why it stops mattering.** `/api/auth/register` is ADMIN-only
  because on 2026-08-11 it was anonymous — a stranger could POST, get a valid JWT, and read the
  whole record. **Proven: HTTP 201 with a working token.** But the danger was not that strangers
  could create accounts; it was that *any* account could read everything. Registration was the
  entry point; the authorization gap was the damage.

  Once grants exist, **a new account is worthless by default** — no grants, so `/api/patient/mine`
  returns nothing and every patient endpoint 404s. The lock is currently compensating for the
  missing authorization model, and this plan removes that need.

  ⚠️ **Do not open registration before the grant model ships.** Doing so restores the
  2026-08-11 hole exactly.

  The three options when it is revisited: open registration (simple; anyone may create an account
  on a host holding a real medical record, seeing nothing but existing); **single-use invite
  tokens** (the consumer-health norm, and the only one where "invite my sister" is one action
  rather than a coordination exercise); or **admin creates every account by hand** (zero new code,
  works today, awkward only in that the owner chooses the grantee's password — and for one sister
  that may simply be the answer).
- **Is `VIEW_TRIALS` enforced in phase 1, or recorded only?** Recommend recorded-only, per the
  archived plan's reasoning about `access_level`. But it is the level with the most privacy value,
  so enforcing it early has an argument.
- **What happens to `trial_status` written by a helper when their grant is revoked?** Nothing
  under this model — status belongs to the patient, not the user. Worth confirming that is
  intended.
- **Does the patient switcher persist across sessions?** localStorage matches how the JWT is
  already stored.
- **Does `patient` need soft-delete semantics distinct from `active`?** Removing a patient a
  helper no longer works with is arguably a `user_patient` revocation, not a patient deletion.
- **Is multi-patient in scope for the UI, or only the schema?** The schema becomes
  multi-patient-capable either way. `../BREAST_FOCUS_PLAN.md` treats multi-patient as a cost of
  narrowing to breast cancer.

---

## 10. What this does not solve

**No encryption at rest, and no audit log of who read what.** Both accepted knowingly by the user
for a single-patient tool on his own host — see `../hosting/DEPLOY_RUNBOOK.md`. **Sharing changes
that calculus**: once several people can read a record, "who read what and when" stops being
hypothetical. `user_patient` records who *may* read; it does not record who *did*.

**Existing JWTs survive a revoked grant.** Tokens are stateless and carry no patient claim, but a
revoked grant only takes effect on the next check — which is every request, so this is fine. Worth
stating so it is not assumed to be a hole.
