# Patients and Access — Frontend Build Plan

The frontend half of `PATIENT_ACCESS_PLAN.md`, written against the API contract `BACKEND_PLAN.md`
produces. Read both first.

Written 2026-08-13 from a survey of `../../../frontend/src`. The backend dependency it named — steps 1-5 —
is satisfied; those endpoints exist.

**Status as of 2026-08-14: ✅ items 1-5 are BUILT and committed** (`c9cb30d`).
`lib/PatientContext.tsx` replaced `useCurrentAppUser`, the API client is repointed to
`/by-patient/`, and the pages are migrated.

⬜ **Items 6-8 are not started.** `lib/accessLevel.ts` exists with a `covers()` rank helper, so
item 6 has its building block, but nothing confirms pages gate on it. Item 8 remains blocked on
backend step 8, which is also not started.

The prose below is written in the future tense throughout — read the per-item status markers.

---

## The finding that shapes this

**Eight pages depend on one hook, and that hook is the entire problem.**

`lib/useCurrentAppUser.ts` is 18 lines and does something worse than its comment admits:

```ts
const response = await appUserApi.getAll();
return response.data.content.find((u) => u.username === username) ?? null;
```

**It fetches every app_user in the database and filters client-side by username string.** With one
patient that is invisible. With any sharing at all it is both a performance problem and a
disclosure — the browser receives every patient's row in order to find one.

That single call site is why this change is tractable: **replace the hook, and eight pages follow.**

| Page | Uses it for |
| --- | --- |
| `Dashboard` | trial-status counts |
| `RankedTrials` | `matchingApi.rank(appUser.extid, …)` |
| `SavedTrials` | tracked-trial list |
| `TrialSearch` | diagnosis, for Tier 1 checks |
| `TrialDetail` | reads diagnosis, **writes** trial status |
| `Diagnosis` / `Variants` / `PriorTreatment` | load and save each tab |

---

## API contract this assumes

From `BACKEND_PLAN.md`. **If any of these differ, this plan changes.**

```
GET    /api/patient/mine                              -> [{ extid, displayName, accessLevel }]
GET    /api/patient/{extid}/diagnosis                 -> PatientDiagnosis[]
GET    /api/patient/{extid}/variant                   -> PatientVariant[]
GET    /api/patient/{extid}/priortreatment            -> PatientPriorTreatment[]
GET    /api/patient/{extid}/trialstatus               -> TrialStatus[]
GET    /api/matching/rank/{extid}?breastOnly=&limit=  -> TrialAssessment[]
GET    /api/matching/trial/{trialExtid}/for/{extid}   -> TrialAssessment
POST   /api/patient/{extid}/share                     -> grant       (step 8)
DELETE /api/patient/{extid}/share/{shareExtid}        -> revoke      (step 8)
GET    /api/patient/{extid}/share                     -> who can see (step 8)
```

⚠️ **`accessLevel` on `/mine` is what makes the UI honest.** Without it the frontend cannot know
whether to render an editable form or a read-only view, and would have to discover a refusal by
attempting a save and failing.

---

## The work

### ✅ 1 (done). Replace the hook — `useCurrentPatient`

Delete `useCurrentAppUser`. New `lib/useCurrentPatient.ts` backed by `GET /api/patient/mine`.

**Returns the selected patient plus the caller's access level**, not just an extid. Every page
that renders differently for a viewer needs the level, and threading it separately from the
patient is how the two drift.

**Selection lives in a React context** (`PatientContext`), not in each page. With one patient it
auto-selects the only result and no switcher ever appears — the multi-patient case costs nothing
until it exists.

**Persist the selected extid in `localStorage`**, matching how the token and username are already
stored. On load, validate it against `/mine` and fall back to the first entry if it is stale or
revoked — a remembered extid the caller no longer has access to must not produce a wall of 404s.

### ✅ 2 (done). Rework `services/api.ts`

- **Delete `appUserApi`** entirely.
- **Add `patientApi`** — `mine()`, and the per-patient sub-resources.
- **Repoint** `patientDiagnosisApi`, `patientVariantApi`, `patientPriorTreatmentApi`,
  `trialStatusApi` from `/by-appuser/{extid}` to `/patient/{extid}/…`.
- `matchingApi.rank` keeps its shape; only the meaning of the extid changes.

**Rename the parameter from `appUserExtid` to `patientExtid` throughout.** Same string, different
concept — leaving the old name is how the old model survives its own deletion.

### ✅ 3 (done). Store the role at login

`ResponseAuth` already returns `role`; `authHelpers` keeps only token and username. Add
`saveRole`/`getRole`/`removeRole` alongside, and clear it on logout with the others.

⚠️ **This is a UI hint and nothing more.** It lives in `localStorage`, so a user can edit it and
reveal a hidden menu item. That is fine — the backend refuses the call regardless. **The frontend
must never be the only thing standing between a user and a capability.**

Needed by `ADMIN_ONLY_INGESTION_PLAN.md` for hiding Process Trials, and it is the same one-line
storage change either way.

### ✅ 4 (done). Fix the eight pages

Mechanical once the hook lands. Each `useCurrentAppUser()` becomes `useCurrentPatient()` and each
`appUser.extid` becomes `patient.extid`.

Two that need more than a rename:

- **`TrialDetail`** is the only page that **writes** — it creates and updates trial status. It
  must respect `accessLevel`: a `VIEW_TRIALS` or `VIEW_RECORD` grantee sees the tracking control
  disabled rather than absent, so it is clear the trial *can* be tracked, just not by them.
- **`TrialSearch`** feeds `runTier1Checks` with a `PatientDiagnosis`. ⚠️ **`dateOfBirth` and `sex`
  move to `patient` in backend step 4**, so `tier1Matching.ts` needs both objects. **Pass them
  separately rather than merging** — merging hides which table owns which fact.

### ✅ 5 (done). Replace the "no app-user profile" dead end

Three pages currently render:

> *"No app-user profile linked to your login. Ask to have one seeded before entering variants."*

**This is developer vocabulary describing a schema defect**, on a page a patient reads. It exists
only because `app_user` rows had to be hand-seeded, and the new model removes that whole class of
failure — a login either has patients or it has none.

Replace with the two real cases:

- **No patients at all** → an invitation to create one. This is use case 1 and 3's entry point,
  and it is currently impossible in the UI.
- **A patient exists but this tab is empty** → the existing empty form, unchanged.

### ⬜ 6 (NOT STARTED). Access-level rendering

Per `PATIENT_ACCESS_PLAN.md` §3:

| Level | Sees |
| --- | --- |
| `VIEW_TRIALS` | Trials for You, Saved Trials, Trial Search. **Not** the Patient Record tabs |
| `VIEW_RECORD` | Everything, read-only |
| `EDIT_RECORD` | Everything, editable |
| `OWNER` | Everything, plus sharing controls |

**Filter `NAV_ITEMS` by access level**, the same mechanism the admin-only work uses. It is now a
single shared array feeding both the desktop row and the mobile panel — a direct benefit of the
mobile pass, and the reason this is one filter rather than two.

⚠️ **Hiding a nav item is not access control.** The route must also refuse, and the backend must
refuse regardless. Three layers, and only the third is load-bearing.

### ⬜ 7 (NOT STARTED). Patient switcher — only when it earns its place

In `Layout`, **rendered only when `/mine` returns more than one patient.** With one patient it
never appears, so the single-patient experience is unchanged.

⚠️ **Nav bar space is already tight below `sm`** — the title had to shrink to `text-base` to fit
beside the hamburger. A switcher does not fit there too. On mobile it belongs **inside the
hamburger panel**, above the links, not in the bar.

### ⬜ 8 (NOT STARTED). Sharing UI — last

A "Who can see my record" page, OWNER only: the list of active grants with names and levels, a
form to grant, and a revoke control.

⚠️ **Revoking is the action a person most needs to trust.** It should state plainly what happens
— access ends immediately, and what they have already seen cannot be unseen. No silent success
toast.

⚠️ **How a grantee gets a login is deferred** (`PATIENT_ACCESS_PLAN.md` §9). Registration stays
ADMIN-only, so **phase 1 of this page grants to an existing username** — the owner types it, or
picks from a list. That is a complete, usable feature for a household where the admin creates
accounts by hand. An invite flow, if it ever ships, changes how the grantee's account appears; it
does not change the grant itself.

---

## Order, and why

1. **`patientApi` + `useCurrentPatient` + context** (items 1-2)
2. **Role storage** (item 3) — one line, unblocks the admin work independently
3. **The eight pages** (item 4) — mechanical, largest diff
4. **Empty states** (item 5)
5. **Access-level rendering** (item 6)
6. **Switcher** (item 7) — only if multi-patient is in scope
7. **Sharing UI** (item 8)

Items 1-4 are the migration and must land together — the app does not build in between, because
`useCurrentAppUser` disappears while eight pages still call it. **Land them as one commit**, not
as a series.

Items 5-8 are additive and can follow separately.

---

## Verification

**Typecheck is the workhorse here.** Renaming `appUserExtid` → `patientExtid` through a typed API
client means `tsc` finds every missed call site. Run `npx tsc --noEmit` before anything else.

⚠️ **Baseline: lint has one pre-existing error** in `Login.tsx` (`no-explicit-any`). Anything
beyond that is new.

**Manual, once the backend is up:**

| Check | Expect |
| --- | --- |
| Sign in as the owner | All pages load; record editable |
| Every page in `MOBILE_TESTING.md`'s table | Still correct at 360px |
| Sign in as a `VIEW_TRIALS` grantee | Trials for You works; Patient Record not in nav |
| Type `/diagnosis` directly as that grantee | Refused, not a broken page |
| Sign in as a user with no grants | The create-a-record invitation, not an error |
| Revoke a grant while that user is signed in | Next action fails cleanly, not a blank screen |

⚠️ **Test the revoked-while-signed-in case explicitly.** The JWT stays valid and carries no
patient claim, so the session survives while every patient call starts returning 404. The app
must handle that as "access ended", not as a crash — and it is the one state that only appears
after sharing exists.
