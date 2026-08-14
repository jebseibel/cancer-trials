# Making the Frontend Mobile-Aware — Change Plan

Written 2026-08-13, from a read of every file in `frontend/src/` rather than from the design
docs. Companion to `../CURRENT_STATE.md` and `../hosting/DEPLOY_RUNBOOK.md`.

**Status: ✅ all items built and committed** as `14aadff` ("Make the app usable on a phone, and
stop it fetching everyone to find one"), verified 2026-08-14. Item 8 needed no change; item 9
turned out to be a dead-file delete (`App.css`, confirmed removed in that commit).

⚠️ **Whether this was ever confirmed on a real phone is not recorded.** Typecheck and production
build were clean and the desktop view is unchanged by construction (every change is either below
`sm` or a padding move that nets out), but no note anywhere says a device was used. `MOBILE_TESTING.md`
is the procedure; run it if you need certainty.

## Why this matters now, and not before

The app is live at `breastcancertrialfinder.com` and the patient can sign in. A phone is the
device someone actually has in a waiting room, in an infusion chair, or in the ten minutes
before an oncology appointment — which is precisely when "what should I ask about?" is the
live question. Every other page in this project has been designed around the reader rather
than the developer; the layout has not been.

## The finding that should shape the work

**The app is not broken on a phone — it is unnavigable on one.** Those are different problems
and the second is much smaller than a rewrite.

The survey found the codebase is in better shape than a mobile pass usually finds:

- **`index.html` already has the correct viewport meta tag.** No scaling hacks to undo.
- **There is not a single `<table>` in the codebase.** Tables are normally the worst mobile
  problem in a data-heavy app; this one has none. Everything is already cards and definition
  lists, which reflow.
- **Tailwind 4 is already in place**, so every fix is a class change, not a CSS architecture.
- **The two hardest pages are already responsive.** `FormControls.Section` is
  `grid-cols-1 sm:grid-cols-3`, so the ~90 fields across Diagnosis / Variants / Prior
  Treatment already stack to one column on a phone. `Dashboard` is
  `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`.

So this is a focused pass on a handful of real defects, not a redesign. **The single blocking
one is the navigation.**

---

## The work, worst first

### 1. The navigation disappears entirely below 640px — fix this first

`Layout.tsx:25` is `hidden sm:ml-6 sm:flex sm:space-x-8`. Below Tailwind's `sm` breakpoint
that entire block is hidden, **and nothing replaces it.** There is no hamburger, no drawer, no
bottom bar.

What a phone user gets today: the logo, and a logout icon. That is the whole navigation. Every
page except the Dashboard becomes unreachable except by typing a URL — including "Trials for
You", which is the page the tool exists for and is deliberately first in the nav on desktop.

**This one defect is most of the mobile problem.** It is also why the rest of the app has never
been seen on a phone: you cannot get to the pages to find out what they look like.

Recommended fix: a hamburger button visible below `sm`, toggling a stacked panel of the same
six links. Reasons to prefer it over a bottom tab bar here — it reuses the existing `Link`
list rather than duplicating it, it holds six items comfortably where a tab bar holds four or
five, and it costs one `useState` in a component that currently has none.

Three things it has to get right, each of which is a common miss:

- **The panel closes on navigation.** A React Router `Link` changes the route without
  unmounting `Layout`, so an open menu stays open over the page it just navigated to.
- **The toggle is a real `<button>` with `aria-expanded` and `aria-label`**, not a clickable
  `<div>`. The one control that reaches the whole app should be reachable by a screen reader.
- **The logout icon stays visible at all widths.** It is outside the hidden block today, and
  it should stay that way rather than being folded into the menu.

### 2. The nav bar itself overflows before the menu even matters

Independent of the hidden links: `Breast Cancer Trial Finder` at `text-xl font-bold` beside a
32px icon does not fit beside a hamburger on a 360px-wide screen. The title needs to shrink
below `sm` (`text-base sm:text-xl`), or shorten to "Trial Finder" on small screens with the
full name from `sm` up.

Worth deciding rather than defaulting: the full name is reassuring on a page someone opens
while anxious, so **prefer shrinking the type over truncating the words.**

### 3. Page padding is inverted — `sm:px-0` removes padding as the screen grows

Nine of the eleven pages wrap in `px-4 py-6 sm:px-0`. That is not a bug on its own — `main` in
`Layout.tsx:84` supplies `sm:px-6 lg:px-8`, so the padding moves from the page to the parent at
`sm`. It works.

But it means **the phone case is the one relying on the page's own `px-4`**, and any page that
forgets it has content flush against the screen edge. `RankedTrials.tsx` is exactly that case:
it wraps in `mx-auto max-w-4xl` with no horizontal padding at all, so on a phone every card
touches both edges.

Recommend moving horizontal padding to `main` unconditionally (`px-4 sm:px-6 lg:px-8`) and
dropping `px-4 ... sm:px-0` from the nine pages. One place to be right, and a new page inherits
it instead of having to remember. This is mechanical but touches nine files, so it is worth
doing as its own commit.

### 4. `RankedTrials` — the page she actually uses — has zero responsive classes

It is the only substantial page with no `sm:`/`md:` prefix anywhere, and it is the most
important page in the app. Three specific problems:

- **No horizontal padding** (above).
- **The counts row** (`{n} to check · {n} to ask about · {n} matched`) is `flex shrink-0
  gap-3` beside a `min-w-0` title block. On a narrow screen the title compresses hard while
  the counts hold their width. The header is already `flex-wrap`, so the fix is letting the
  counts wrap to their own line below the title rather than competing with it.
- **`TrialSites` city lists** — `Chicago, Illinois · Boston, Massachusetts · Las Vegas,
  Nevada and 3 more` is long, and travel is the thing the user explicitly asked to be
  visible. Verify it wraps cleanly rather than pushing the card wide; consider showing fewer
  than 4 cities before the "and N more" toggle on small screens.

### 5. Tap targets are below the accessible minimum in the places it matters most

The `why?` toggles in `RankedTrials`, the `and N more` sites link, and `What matched` are
`text-xs` inline buttons — roughly 16-18px tall. The WCAG target-size minimum is 24px, and
Apple's and Google's guidance is 44px/48px.

**These are the controls the whole no-verdicts design depends on.** A reader is supposed to be
able to check the reasoning behind every flag; if the control that reveals it is hard to hit
with a thumb, the reasoning is effectively hidden. Add vertical padding and a `min-h` to these
without changing the visual weight of the text.

Same applies to the tab bar in `PatientRecord.tsx` (`pb-3` only) and the logout icon.

### 6. The three Patient Record tabs will overflow horizontally

`PatientRecord.tsx:30` is `flex space-x-6` with `Diagnosis` / `Variants` / `Prior Treatment`,
each carrying a 16px icon. At 360px this overflows and, with no `overflow-x-auto`, the third
tab is simply unreachable — the same class of failure as the nav, on a smaller scale.

Two options: allow horizontal scroll on the tab strip, or drop the icons below `sm` and tighten
the spacing. Prefer **dropping the icons** — three tabs should fit without a scroll gesture
nobody will discover, and the labels carry the meaning.

### 7. Ingestion's `maxStudies` field and button row — done

`Ingestion.tsx:196` was a fixed `w-32` input, and the three action buttons were `flex
flex-wrap gap-3` — they wrapped, but into a ragged two-then-one arrangement, because the three
labels are very different lengths ("Pull Trials and Prepare for Search" against "Pull Trials").

Fixed: the input is `w-full sm:w-32`, and the row is `flex-col items-stretch` below `sm`,
reverting to `sm:flex-row sm:flex-wrap`. Each button gained `justify-center` so its icon and
label stay centred once the button is full-width rather than hugging its content.

**The confirmation dialog on this page needed nothing** — it already had a `px-4` backdrop,
`max-w-md w-full`, and a two-button row short enough to fit at 360px. Checked rather than
assumed, and noted here so it is not re-investigated.

### 8. `TrialDetail`'s four-column stat grid

`grid-cols-2 sm:grid-cols-4` — already responsive, and 2-up on a phone is right for four short
values. **No change needed.** Listed only so it is not re-investigated later.

### 9. Delete `App.css`

It is the untouched Vite scaffold — `#root { max-width: 1280px; padding: 2rem; text-align:
center }`, plus the spinning-logo animation. Check whether `main.tsx` still imports it; if it
does, that `#root` rule is fighting every layout in the app and the `2rem` padding is a real
mobile cost. If nothing imports it, delete it anyway.

---

## What this deliberately does not do

- **No component library, no CSS refactor.** Tailwind 4 and the existing class conventions
  are sufficient for every item above.
- **No redesign of the card layouts.** They already reflow.
- **No PWA, no install prompt, no offline support.** Real questions, different project.
- **No touch gestures** (swipe between tabs, pull to refresh). Native-feeling niceties that
  add state and failure modes to a tool whose value is being correct, not being slick.

## Suggested order

1. **Nav menu below `sm`** (item 1) — unblocks everything else, because until this ships you
   cannot reach the other pages on a phone to check them.
2. **Nav title sizing** (item 2) — same file, same commit.
3. **Padding to `main`, remove `sm:px-0` from nine pages** (item 3) — mechanical, own commit.
4. **`RankedTrials` responsive pass** (item 4) + **tap targets** (item 5) — the page that
   matters most.
5. **Patient Record tabs** (item 6).
6. **`App.css` cleanup** (item 9), **Ingestion** (item 7) — cosmetic tail.

Items 1-3 are what turn "unusable on a phone" into "usable on a phone". Items 4-6 are what
make it good.

## How to verify

**Do not verify this by resizing a desktop browser alone.** A narrow desktop window has a mouse
pointer, so every tap-target problem in item 5 stays invisible. Chrome DevTools device emulation
at 360×640 (a small Android) and 390×844 (iPhone 14) covers layout; the tap targets need a real
phone, or at minimum touch emulation enabled.

**Then open it on her actual phone against the live site.** Every failure that mattered in this
project's deploy was found by using the thing rather than testing a part — the 403 on `/login`
and the IP-only login lockout both surfaced that way. A hidden nav is exactly the class of
defect that a component test would never catch and ten seconds of real use catches immediately.

⚠️ **The live site is the only place this can be tested end to end on a phone**, and its corpus
is still empty — so "Trials for You" will render its empty state rather than the card list that
items 4 and 5 are about. Either pull the corpus first (Phase 4 of the deploy runbook), or test
those two items locally against the populated dev database and accept that the phone check
covers navigation and forms only.

## Open questions

- **Hamburger menu or bottom tab bar?** Recommended hamburger above, for six items and for
  reusing the existing link list. A bottom bar is more thumb-reachable and more app-like, at
  the cost of duplicating the nav and having to cut to four or five destinations. This is the
  one decision worth making deliberately before code is written.
- **Does the desktop nav stay unchanged?** This plan assumes yes — nothing above alters
  layout at `sm` and up, so there is no regression risk to the working desktop view.
- **Is the deployed jar rebuilt for this?** Any frontend change needs `./gradlew
  buildDeployment` and a redeploy; a plain `build` ships the stale bundled `static/`. That
  trap is already recorded in the deploy runbook.
