# Restricting Trial Processing to Admins — Change Plan

Hide "Process Trials" from non-admin users, and stop the endpoints behind it from being callable
by one. Written 2026-08-13 from a read of `SecurityConfig`, `AuthController`,
`CustomUserDetailsService`, `IngestionController`, `RagIndexController` and the frontend, rather
than from the design docs.

Companion to `MOBILE_PLAN.md`, `../CURRENT_STATE.md` (the authorization gap), and
`../hosting/DEPLOY_RUNBOOK.md`.

**Status as of 2026-08-14: ⬜ the backend guard is still NOT built** — confirmed: no
`@PreAuthorize` on `IngestionController` or `RagIndexController`, and no ingestion/rag matcher in
`SecurityConfig`. **The endpoints remain callable by any authenticated user.**

✅ **One prerequisite landed since this was written**: the frontend now stores the role.
`services/api.ts` has `saveRole`/`getRole`/`removeRole` plus an `isAdmin()` helper, added by the
access-model work. The table below is corrected accordingly.

---

## The finding that should shape this

**The request as stated — "hide it" — is the smaller half of the job, and on its own it is not
a restriction.**

Hiding a nav link removes the *route a person clicks*, not the *capability*. Today
`POST /api/ingestion/clinicaltrials` and `POST /api/rag/backfill` require **only a valid JWT**.
Neither carries a `@PreAuthorize`, and neither is named in `SecurityConfig`'s matcher list, so
both fall through to the blanket `.authenticated()` rule. Any logged-in user can start a
multi-hour corpus rebuild with a single `curl` and a token they already hold.

So the honest framing: **the menu item is the visible half; the endpoint is the actual door.**
This plan does both, and does the backend half first, because a frontend-only change would give
the appearance of a restriction without the substance.

That distinction matters more than usual here. A corpus pull on the production box is a
~14-minute MySQL pull plus hours of embedding on 1 vCPU, competing with MySQL, two Ghost
containers and the `cpss` app for 3.9GB of RAM. It is the single most expensive thing any user
can trigger.

## What is already in place, and what is missing

Better than expected — most of the machinery exists and is simply not applied here.

| Piece | State |
| --- | --- |
| `@EnableMethodSecurity` | ✅ On `SecurityConfig`, so `@PreAuthorize` is honoured |
| Roles reach Spring Security | ✅ `CustomUserDetailsService` grants `ROLE_ + user.getRole()` |
| A working `hasRole('ADMIN')` example | ✅ `/api/auth/register`, guarded two ways |
| Login returns the role to the browser | ✅ `ResponseAuth` already carries `role` |
| **Ingestion / backfill role guard** | ❌ **Missing — the gap this plan closes.** Re-confirmed 2026-08-14 |
| Frontend stores the role | ✅ **Now done** — `authHelpers.saveRole`/`getRole`/`removeRole` and `isAdmin()` in `services/api.ts`, added by the access-model work |

⚠️ **`@EnableMethodSecurity` being present is load-bearing and worth not breaking.** It was
found missing once already: without it `@PreAuthorize` is silently ignored, the method runs
anyway, and the endpoint *looks* protected while standing open. Any test of this work must
confirm a 403 actually happens rather than assuming the annotation did something.

---

## The work, in dependency order

### 1. Guard the endpoints — do this first, it is the actual restriction

Add `@PreAuthorize("hasRole('ADMIN')")` to the write endpoints that change the corpus:

- `IngestionController.ingestClinicalTrials` — `POST /api/ingestion/clinicaltrials`
- `IngestionController` — both `/uchealth/*` staging endpoints, same reasoning
- `RagIndexController.backfill` — `POST /api/rag/backfill`
- `RagIndexController.reindexTrial` — `POST /api/rag/reindex/{trialExtid}`

Follow the existing `AuthController.register` pattern exactly — it is this project's one worked
example, and consistency is worth more than a marginally different approach.

**Belt and braces, as `register` does.** Add a matching
`.requestMatchers("/api/ingestion/**", "/api/rag/backfill", "/api/rag/reindex/**")
.hasRole("ADMIN")` rule in `SecurityConfig`, placed **before** the general `.authenticated()`
rule — matcher order decides which wins.

⚠️ **`GET /api/rag/search` must stay open to authenticated users.** It is what Trial Search
runs on, and it does not modify anything. Guard the writes, not the reads. Getting this wrong
silently breaks the search page for the patient.

### ✅ 2 (DONE). Persist the role on login, so the frontend can ask

> **Already built — verified 2026-08-14.** `authHelpers` in `services/api.ts` has `saveRole`,
> `getRole`, `removeRole` and an `isAdmin()` convenience check; the role is cleared at logout
> with the token and username. This landed with the access-model work (its frontend item 3), so
> **skip this step.** The caveat below still applies to how the value may be used.

`ResponseAuth` already returns `role`; `authHelpers` currently stores only the token and
username. Add a `role` alongside them, following the existing helper pattern rather than
inventing a new storage mechanism.

⚠️ **This value is a UI hint and nothing more.** It lives in browser storage, so a user can
edit it and reveal the menu item. That is fine and expected — step 1 is what makes the reveal
useless, because the endpoint refuses the call regardless of what the browser believes. **The
frontend must never be the only thing standing between a user and a capability.**

Worth stating because it is the exact trap this plan exists to avoid: if step 1 were skipped,
editing one string in devtools would restore the whole capability.

### 3. Hide the nav item and the route

Two places, and both are needed:

- **`Layout.tsx`** — `NAV_ITEMS` is now a single list feeding both the desktop row and the
  mobile panel, so filtering it once hides the link in both. This is a direct benefit of the
  mobile work; before it, this would have been two edits that could drift.
- **`App.tsx`** — the `/ingestion` route itself. Hiding a link while leaving the route
  reachable means typing the URL still loads the page, which then fails confusingly at the API
  call rather than saying plainly that it is not available.

Recommend an `adminOnly: true` flag on the `NAV_ITEMS` entry rather than a separate list, so
the nav stays one array with one filter applied at render.

### 4. Decide what a non-admin sees at `/ingestion`

Three options, and this is a real choice rather than a detail:

- **Redirect to the Dashboard.** Cleanest. A page that does not exist for you simply is not
  there.
- **Render a plain "not available" message.** More honest, and it explains rather than
  bouncing someone somewhere unexpected.
- **Leave the route unguarded and let the API 403.** Worst — the failure surfaces as a broken
  page rather than an intentional boundary.

**Recommend the redirect**, matching how `ProtectedRoute` already handles an absent token.
Consistency with the one existing pattern beats a second convention.

### 5. Dashboard link check

`Dashboard.tsx` has cards that navigate. Confirm none of them link to `/ingestion`; if one
does, it needs the same treatment or it becomes a dead end for non-admins.

Cheap to check, and exactly the kind of second entry point that survives a nav-only change.

---

## Who is an admin here

Worth settling before the code, because it decides whether this is testable.

`user.role` is a plain string column, and `CustomUserDetailsService` prefixes it with `ROLE_`.
So a user with `role = "ADMIN"` becomes `ROLE_ADMIN` and passes `hasRole('ADMIN')`.

**Two accounts exist in prod: `jeb` and the unused `admin`.** Their current `role` values need
checking through the API before anything is guarded — `jeb` is the account the patient signs in
with, and if `jeb` is already `ADMIN`, then "hide from non-admins" changes nothing for the only
person using the app today, and the whole change is untested until a second account exists.

⚠️ **This is the question that decides whether the change does anything at all.** Answer it
first. If the intent is that *she* should not see Process Trials while *you* still can, then
you and she need different accounts with different roles — which is a change to how the app is
used, not just to its code, and is worth deciding deliberately.

Related, and already recorded in `../CURRENT_STATE.md`: `LOGIN_ALLOWED_USERNAMES=jeb` is
deployed as code but the property is unset, so the `admin` account is currently loginable.

---

## What this does not fix

**The authorization gap is untouched.** Any authenticated user can still read any patient's
record by passing their extid — `/api/matching/rank/{appUserExtid}`,
`/api/patientdiagnosis/by-appuser/{extid}` and friends take their target from the URL and never
compare it to the caller. This plan restricts *one capability by role*; it does not introduce
*ownership*. Those are different problems and this one is much smaller.

Recorded so the two are not confused: closing this does not close that.

**Roles remain coarse.** `ROLE_USER` / `ROLE_ADMIN` with a string column is enough for one
capability and would not scale to per-feature permissions. Fine at this size; noted so it is a
decision rather than an assumption.

---

## Suggested order

1. **Check the `role` values on `jeb` and `admin`** via `GET /api/user` — this determines
   whether anything else here is observable.
2. **Guard the endpoints** (step 1), backend only. Verify with `curl` before touching the UI.
3. **Store the role on login** (step 2).
4. **Filter `NAV_ITEMS` and guard the route** (steps 3-4).
5. **Check the Dashboard cards** (step 5).

Steps 1-2 are the restriction. Steps 3-5 are the part that stops a non-admin from being shown a
door they cannot open.

## How to verify

**Test the endpoint directly, not the menu.** Hiding a link is trivially confirmed by looking;
the guard is the part that can silently not work.

- As a **non-admin** token: `POST /api/ingestion/clinicaltrials` → expect **403**
- As a **non-admin** token: `POST /api/rag/backfill` → expect **403**
- As a **non-admin** token: `GET /api/rag/search?...` → expect **200**, still works
- As an **admin** token: both POSTs → expect **200/202**, unchanged
- **In the browser as a non-admin:** no Process Trials in the desktop nav *or* the mobile
  panel, and `/ingestion` typed directly does not render the page

⚠️ **A 200 on the first check means `@PreAuthorize` is not being honoured** — that is the
`@EnableMethodSecurity` failure mode, and it looks exactly like success from the UI side.

⚠️ **Do not verify the "admin still works" case by actually running a pull on prod.** A real
ingestion is a ~14-minute pull plus hours of backfill. Check the authorization result — that the
request is accepted rather than refused — not the job's completion.
