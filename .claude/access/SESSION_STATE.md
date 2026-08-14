# Where This Work Stands — 2026-08-13, status refreshed 2026-08-14

Written at the end of the session that built the patient/access model. Read this first when
picking the work back up; the design lives in `PATIENT_ACCESS_PLAN.md`, the step lists in
`BACKEND_PLAN.md` and `FRONTEND_PLAN.md`.

**Branch: `frontend-mobile`** (renamed from `prod-push`). ✅ **The work described here is
committed** — it landed as `c9cb30d` (the access model), `14aadff` (mobile), and `5836a0b`
(these docs).

⚠️ **None of it is on `main`, and therefore none of it is deployable.** This branch is **3 ahead
of `main` and 0 behind** — nothing to rebase, everything to merge. `main`'s tip is `0544c6a`
(the deploy commit), and these three commits sit on top of it, unmerged. **Merging this branch
is what puts the authorization model into production.** Verified 2026-08-14.

---

## ⚠️ Read this before anything else

> **Updated 2026-08-14.** The three checks below were written when nothing had been run. The work
> has since been committed and the app is deployed, so at least some of it has been exercised —
> but **whether these three specific paths were verified in a browser is not recorded anywhere.**
> If you are picking this up cold, run them; they are cheap and the failure mode is silent.

**Compiling is not working.** Every failure that mattered in this project's deploy history was
found by using the thing, not by testing a part.

**Start the backend and `npm run dev`, sign in, and check:**

- Does **Trials for You** return results? `/api/matching/rank/{patientExtid}` is a new route.
- Do the three **Patient Record** tabs load and save?
- Does the **Diagnosis** page's date-of-birth/sex save land on `patient`? It writes two tables now.

---

## What was built

### Backend — steps 1-6 of `BACKEND_PLAN.md`, all complete

| Step | What landed |
| --- | --- |
| 1 | `patient` table + full 9-file stack, changeset `028` |
| 2 | `user_patient` grants + ranked `AccessLevel` enum, changeset `029` |
| 3 | **`CurrentUserService`** — the authorisation rule, and the first code in the app to read `SecurityContextHolder` |
| 4 | `PatientDiagnosis` repointed to `patientId`; `date_of_birth`/`sex` moved onto `patient` |
| 5 | `PatientVariant`, `PatientPriorTreatment`, `TrialStatus`, `SavedTrialMatch` repointed |
| 6 | **`AppUser` deleted** — 13 files, table dropped in changeset `030` |

**The gap is closed.** Every patient endpoint resolves identity from the JWT and checks a grant.
`/api/patient/mine` names nobody. There is deliberately no "list every patient" endpoint.

**Tests: 1,058 passing, 0 failures, 1 skip** (`RetrievalEvaluation`, needs a live backend by
design — run with `-Deval.skipWithoutBackend=true`).

Two extras beyond the plan, both requested mid-session:

- **The DOB/sex gap is closed.** `PatientSeedLoader` reads `displayName`, `fullName`,
  `dateOfBirth` and `sex` from the diagnosis CSV onto the patient, and **backfills** them onto an
  existing patient where they are still null. Confirmed working.
- **`full_name` added** to `patient`, wired through every layer.

### Frontend — items 1-5 of `FRONTEND_PLAN.md`

- **`useCurrentAppUser` deleted.** It fetched *every* app_user and filtered client-side by
  username — a disclosure with any sharing at all. Replaced by `lib/PatientContext.tsx` calling
  `/api/patient/mine`.
- **API client repointed** — `appUserApi` gone, `patientApi` added, five `/by-appuser/` calls
  now `/by-patient/`.
- **Eight pages migrated**, and the "No app-user profile linked to your login" dead ends replaced
  with an actual invitation to create a record.
- **Role stored at login**, cleared at logout with the remembered patient.
- **Tier 1 changed shape**: `runTier1Checks(patient, trial)` — it no longer takes the diagnosis,
  since age and sex both moved.

Build and typecheck clean. Lint is back to the **single pre-existing `Login.tsx` `no-explicit-any`**
error that predates this work.

### Also on this branch, from earlier in the session

- **Mobile frontend work — all 9 items** of `../frontend/MOBILE_PLAN.md`, checked by the user.
- `../frontend/MOBILE_TESTING.md` — how to test at phone size.
- `.claude/settings.json` — 21 read-only Bash permissions added.

---

## What is left

### Backend step 7 — seed loader
✅ **Done.** Rewritten during step 4 and finished during the DOB/sex work; `PatientSeedLoader`
is in place and seeds the patient, the OWNER grant and the clinical rows.

### Backend step 8 — sharing endpoints
`POST/GET/DELETE /api/patient/{extid}/share`. ⬜ **Still not started — confirmed 2026-08-14**,
no `share` mapping exists on `PatientController`. Everything before it is worth having
regardless; this is the sharing feature itself.

### Frontend items 6-8
- **6 — access-level rendering.** Hide the Patient Record tabs from a `VIEW_TRIALS` grantee;
  disable tracking for a read-only one. ⚠️ `lib/accessLevel.ts` exists with a `covers()` rank
  check, so the *helper* is built — what is unconfirmed is whether pages actually gate on it.
- **7 — patient switcher**, only when `/mine` returns more than one. ⚠️ On mobile it belongs
  *inside* the hamburger panel — the nav bar is already tight below `sm`.
- **8 — sharing UI.** Blocked on backend step 8.

**None of 6-8 matter with one patient and one login.** They become real when a second person has
an account.

### Separate and unblocked
`../frontend/ADMIN_ONLY_INGESTION_PLAN.md` — restricting trial ingestion to admins. Four
`@PreAuthorize` annotations plus a `SecurityConfig` matcher. Independent of everything above.
⬜ **Still not started — confirmed 2026-08-14**, no `@PreAuthorize` on `IngestionController` or
`RagIndexController`.

---

## Decisions made this session, so they are not re-litigated

- **Three concepts, not two**: `user` logs in, `patient` has a record and need never log in,
  `user_patient` says who may see what. Driven by the third use case — creating a record for
  someone you love — which the old schema could not express.
- **Ownership is a grant** (`access_level = OWNER`), not a column on `patient`. "My own record"
  is therefore not a special case anywhere in the code.
- **Refusals are 404, never 403.** A 403 confirms the extid names a real person's record.
  ⚠️ The message is identical for "no such patient" and "not allowed" — `GlobalExceptionHandler`
  puts the message in the body, so a differing message would leak what the status code hides.
  There is a test for this.
- **Four access levels**, ranked: `VIEW_TRIALS < VIEW_RECORD < EDIT_RECORD < OWNER`.
  `VIEW_TRIALS` exists so a family member can help hunt trials **without** reading a genomic
  report — the reason this is not a boolean.
- **Zanzibar's model, not its infrastructure.** One grant table shaped as subject-relation-resource.
  OpenFGA/SpiceDB solve a scale problem this project does not have; the tuple shape means moving
  to one later is a data move, not a redesign.
- **Invite flow deferred.** Registration stays ADMIN-only. It blocks only step 8's UI, and grants
  work fine against admin-created accounts. Reasoning recorded in `PATIENT_ACCESS_PLAN.md` §9.

---

## Traps worth remembering

⚠️ **Do not run Gradle while the backend is running.** `spring-boot-devtools` hot-restarts
against a half-written classpath. It cost time twice this session, once surfacing as a bogus
"bad class file / cannot access ...Builder" error that looked like a real compile fault.

⚠️ **Editing an applied changeset needs a database rebuild** (n8n `clear-db`), because
`drop-first` is false. This session needed three. **A new changeset does not** — `030-drop-app-user`
applied with no rebuild, which is why new numbers beat edits.

⚠️ **`BUILD SUCCESSFUL` does not mean tests ran.** Read counts from
`{module}/build/test-results/test/TEST-*.xml`. Also: `--tests "*Foo*"` **fails the build** in any
module with no match, which looks like a test failure and is not.

⚠️ **The migration path is rebuild-and-reseed**, and it is proven now — `PatientSeedLoader`
recreated the patient, the OWNER grant and all three clinical rows from the gitignored CSVs.
Verified by the user.

---

## Timing, unchanged and still important

⚠️ **Prod's Qdrant corpus was empty as of 2026-08-11**, which is what made this migration's
rebuilds nearly free. After Phase 4 of `../hosting/DEPLOY_RUNBOOK.md` a rebuild costs hours of
re-embedding on 1 vCPU. **Do the schema work before the corpus pull, not after.** ⚠️ *Whether the
pull has since happened is not recorded — check the server, not this document.*

`LOGIN_ALLOWED_USERNAMES=jeb`: ✅ the allowlist shipped in `1b663cb`. Whether the property is
actually set in `/opt/cancer/.env` is a deployment question — check the box.

---

## Git

✅ **Committed 2026-08-14.** The work described here landed as three commits, close to the split
this section recommended: `14aadff` (mobile), `c9cb30d` (the access model), `5836a0b` (the docs).

⚠️ **The branch is 3 ahead of `main` and 0 behind** — this work is unmerged, so merging is what
ships it.

⚠️ **Before any commit, confirm no patient file is in the index**: `.claude/patient-data/` is
gitignored at directory level and has nearly been committed twice before.
