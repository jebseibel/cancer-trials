# UI Design — Warmer, More Human

Written 2026-09-02. Describes where the frontend's visual design is going next, and records
the reasoning behind it. Companion to `frontend-module.md` for structure/routes; this document
is about look and voice, not architecture.

## The brief, in the user's words

> "Lean warmer and more human — softer colors, patient-story photography, plain-language copy,
> less 'dashboard,' more 'we're here to help.' Good if your audience is primarily patients
> rather than clinicians/researchers."

Scope: **the whole app, uniformly** — not just the patient-facing pages. Process Trials and the
other admin/ingestion screens get the same warmth as Trial Search and Trials for You, even
though only the user (or Researcher mode) reads them day to day. One voice throughout, rather
than a seam between "the nice part" and "the tool part."

**Added mid-session**: *"since this is a site for people who have a serious illness I want it
to be very warm and inviting. This includes the font and background."* This names font and
background as the two elements to check first, and raises the bar from "warmer than before" to
"very warm and inviting" — a stronger claim than the earlier, more general brief. §1 and the
typography note below take a firmer position accordingly, rather than leaving both as open
questions.

## Reference images

Two screenshots the user pointed to directly, both examined and weighed rather than blended in
wholesale — a reference is evidence for specific choices, not a template to copy.

**breastcancer.org homepage.** Already the reference behind the typography pass (Newsreader,
italic emphasis) and the flat tile language (uppercase eyebrow labels, borderless cards, hover
shadow, arrow-icon affordance) built earlier this session. Also the source of the "clean" and
"less dashboard" cues named in the original brief. Nothing new to add here beyond what is
already built and documented above — it remains the primary layout/card reference.

**Susanne Kaufmann skincare hero** (a spa/skincare brand, not a healthcare site). Reviewed and
weighed deliberately, not adopted wholesale — asked directly which parts of it the user meant,
because it pulls in a different direction from breastcancer.org on one major point:

- **Confirms and sharpens §1's background direction.** Its warm beige/stone hero background is
  real evidence that a genuinely warm neutral (not `stone`'s desaturated gray) reads as
  "inviting" the way this document is asking for — independent confirmation, from a second
  reference, of the direction already taken in §1 before this screenshot arrived.
- **Its minimal, restrained nav treatment is worth carrying over** — small-caps or all-caps
  wordmark, few nav items, no visual clutter — as a lighter-weight companion to the green nav
  bar already built, not a replacement for it.
- **Its large, tight-cropped real human photography (a face, a hand holding product) is
  explicitly declined.** Asked directly, and confirmed: the user is not asking for real
  photography of people. §2's illustration-only direction and its PHI/consent reasoning stand
  as written, unchanged by this reference.
- **Its dense, edge-to-edge, card-free hero layout is not adopted.** This app is a data-heavy
  tool with many list pages (Trial Search, Saved Trials, Trials for You); breastcancer.org's
  card-based layout already fits that shape better than a single full-bleed image ever could,
  and remains the layout reference.

## Where this picks up from

Two design passes already landed this session and are not being undone:

- **Palette**: green as primary/accent (already consistent app-wide — nav, buttons, focus
  rings, active states), a warm-neutral `stone` scale replacing plain gray everywhere
  (267 occurrences swapped across 15 files), and a green nav bar with inverted light text.
- **Typography (superseded — see §5)**: Newsreader for headings, italic used for emphasis
  inside a title rather than bold; Source Sans 3 for body and UI text, app-wide default. This
  pairing was judged to hold up against "very warm and inviting" when that bar was raised, and
  it does read as gentle/editorial rather than luxury or clinical — but it was chosen against
  general warmth/trust criteria, not against the specific accessibility needs of a reader going
  through cancer treatment (vision changes are common, and font weight/size/line-height matter
  more than usual). §5 below, working from a more targeted reference, supersedes this with
  Lora + Inter and a set of concrete accessibility minimums this earlier choice never checked
  against. Left in this list for the record of what was tried, not as the current direction.
- **Flat tile language**: the Dashboard's stat and quick-action tiles moved from bordered white
  cards with a permanent shadow to flat `stone`-filled tiles, shadow only on hover, an uppercase
  eyebrow label, and — on the action tiles — an arrow-icon affordance that only animates in on
  hover. Modeled on breastcancer.org's homepage article cards, which is the reference the user
  pointed to for "clean" and for what "less dashboard" should look like in practice.

This document extends that direction into softer color, plain-language copy, and imagery — the
three pieces the brief named that haven't been addressed yet — and into every remaining
dashboard-shaped surface in the app.

**Searched and not found**: neither this pass nor the earlier palette pass found a database
match for "green primary + warm-neutral background" as a combination in the design-search
tool's healthcare/nonprofit/wellness palette results — every match defaults to blue, cyan,
lavender, or indigo as primary, with green appearing only as a secondary accent. That gap is
now moot for the two anchor colors — dark warm green and beige are user-decided, named below,
not tool-matched — but it is worth keeping on record that this combination sits outside what
the reference database considers a typical healthcare palette.

## 1. The palette: dark warm green + beige

**Decided by the user, mid-session**: *"Our colors are dark warm green and beige."* This
replaces the two open color questions below with a named, two-color palette rather than a
direction to interpret — recorded here as the actual answer, superseding the "softer sage"
guess an earlier draft of this section made before the user named the palette directly.

### The problem with the current palette

Green-700 nav, `stone-100` background, white cards — functional and legible, but two things
fall short of "very warm and inviting" specifically:

- **The current green is a cool, saturated, high-chroma green** (Tailwind's `green-600`/
  `green-700`), which reads as *action* and *system status* (the same shade family used for
  "PASS", confirmations, and primary buttons) rather than *warm*. "Dark warm green" is a
  different color, not just a darker or lighter version of what is there now — it leans toward
  olive/forest/moss rather than the current emerald-leaning hue, with a visibly warmer,
  yellow-shifted undertone. Confirming this is a hue change, not only a value change, matters
  for implementation: swapping to a darker shade of the *same* green would not satisfy this.
- **The background is barely warm at all.** `stone-100` is Tailwind's most neutral warm gray —
  a half-step off pure white, chosen last time specifically because `stone-50` "read as barely
  off-white" and this was the fix. Against the original, more general brief that was enough.
  Against *"very warm and inviting,"* it is not: `stone` is a desaturated neutral, not a warm
  color, and the whole page currently sits on something closer to "not-quite-gray" than to any
  color a reader would call warm. **Beige is now the named answer** — a genuinely warm,
  yellow/tan-leaning neutral, not `stone`'s low-saturation gray-brown.

### Direction

**Two roles, not one green doing both jobs**, now that dark warm green is named specifically:

- **Dark warm green is the anchor/interactive color** — nav bar, primary buttons,
  active/selected states, focus rings. It replaces the current `green-600`/`green-700` outright
  rather than sitting alongside it; this is the app's one green from here, not a second shade
  added on top. Being *dark* rather than bright is itself part of what "warm and inviting" is
  asking for here — a deep, quiet green reads calmer than a bright emerald, closer in register
  to breastcancer.org's own dark announcement-bar color than to a SaaS-dashboard accent green.
- **Beige is the background**, replacing `stone-100` on the page and — a step paler, not
  pure white — on cards. This is the single highest-leverage change in this document for the
  "warm and inviting" bar: the background is what every other element sits on, so it sets the
  room's temperature before any card, button, or headline is read. Confirmed independently by
  the Susanne Kaufmann reference above, which used a comparably warm beige for the same reason.
- **A lighter, softer green tint remains useful as a third role**, distinct from the dark
  anchor green — for illustration accents, empty-state messaging, and amber-adjacent
  "we're here to help" callouts, so those moments do not have to borrow the same dark,
  high-commitment green used for buttons and the nav. Named here as a role that still needs a
  value (see "Open questions"), not as a second interactive color competing with the first.

One more softening, once the palette itself has moved:

- **Warmer cards.** Cards are currently pure `white` against the page background. Once the
  page background is beige rather than near-white `stone-100`, cards need to sit a clear,
  deliberate step lighter than the page — not pure white, which would now read as a cold,
  clinical surface floating on a warm one, but a paler version of the same beige.
  The app should have no pure white left except on text/icons that specifically need maximum
  contrast (button labels on a colored background).
- **Rounder, softer corners and shadows.** Current tiles use `rounded-lg` with sharp-edged
  shadows on hover. A slightly larger radius and a softer, more diffuse shadow (larger blur,
  lower opacity, warm-tinted rather than pure black at low opacity) read as gentler without
  changing the layout at all.

### What does not change

- Amber for the no-verdicts warning callouts, red/destructive for errors, blue for status
  badges, purple for the AI-generated content markers. These are semantic and already
  documented (`project-description.md`'s "Hard rules", `CURRENT_STATE.md`'s AI-check section)
  — a color audit is not a reason to touch meaning that already works.
- The Patient/Researcher toggle's own visual treatment (white pill on the green nav) — it is
  new, tested, and not part of what the user flagged as needing warmth.

## 2. Patient-story photography → no identifiable person, not a photography ban

**Revised mid-session** (originally "illustration only, no photography of people at all" —
see below for what changed and why the earlier version is kept here rather than deleted).

The brief asked for "patient-story photography," modeled on breastcancer.org's hero image of a
real, identifiable person. **This app still never uses that** — a photo presented as "a
patient" on a tool that is *about* one real patient invites exactly the confusion the project's
existing carefulness exists to prevent (`CURRENT_STATE.md`'s repeated notes on
`_archive/patient-data/` never being tracked, the AI trial check's de-identification allowlist):
is this her? That reasoning is unchanged and still governs anything presented as depicting the
patient or a stand-in for her.

**What changed: the line is "identifiable person," not "contains a human at all."** The
original version of this section banned photography of people outright, reasoned from that
same PHI/consent concern — but free stock photography where no face or distinguishing feature
is in frame (hands, shoulders, a gesture, deliberately cropped) carries none of that ambiguity.
There is no "is this her?" question to ask about a photo where no one is identifiable in the
first place. Confirmed with the user directly before revising: `two-ladies-with-ribbons.jpg`,
added to Trials for You, is free stock photography with both women's faces cropped out of
frame — exactly the case this revision draws the line around.

**The actual test for any photo of a person, going forward: would a reasonable viewer be able
to identify who this is?** If yes — a recognizable face, a distinguishing feature, anything
that invites "is this her?" — it does not go in this app, full stop, for the reasoning above.
If no — faces out of frame, hands/gesture only, generic and anonymous by construction — a real
photo is fine on the same footing as illustration.

**Direction, updated:**

- Illustration remains a fine, always-safe choice and needs no per-image judgment call — soft
  shapes, hands, light, plants, gentle line-art. Still the right default when in doubt.
- A real photo is acceptable specifically when no one in it is identifiable, per the test
  above. Each such photo is still a one-at-a-time judgment call, not a blanket license — this
  section describes the test, not a pre-cleared image library.
- A hero illustration or photo on the Dashboard and a supporting image on Trials for You
  (`two-ladies-with-ribbons.jpg`, illustrating "this is a starting point" — see §4) both now
  exist, added this session.
- Small supporting illustrations on empty states (no saved trials yet, no patient record yet)
  remain a good direction, not yet built.

Sourcing continues to be a per-image decision made when an image is actually added, not
something this document pre-approves in bulk.

**The test was applied against the full asset library, not just the one image being added.**
`frontend/src/assets/images/` held eleven candidate images at the time of this revision; two —
clearly identifiable individuals, full faces, direct eye contact with the camera — failed the
test outright and were deleted from the project rather than left sitting unused, on the user's
own instruction. A third (`pexels-thirdman-7659367.jpg`, a profile shot through a window, small
and dim but still a discernible face) was judged too close to the line to use without asking
first, and was left in place, unused, pending that call. The remaining photos in the folder are
all safe by the test — no person in frame at all (fruit, handwriting, scrabble tiles, the
`find-a-cure` card from two angles) — and stayed untouched.

**The original text, kept for the record rather than deleted:**

> *This app does not use real patient photography.* Two reasons, both already load-bearing
> elsewhere in the project: this tool holds one real patient's actual medical record, and a
> stock or AI-generated photo presented as "a patient" invites exactly the confusion the
> project's existing safeguards exist to prevent; and there is no sourcing or consent question
> to manage if illustration is used instead. *No photography of people anywhere in the app* was
> stated as a hard line. That blanket line is what this revision narrows — the underlying
> reasoning about identifiable/depicted patients is what carried forward, not the literal
> "no humans in any photo" reading of it.

## 3. Plain-language copy

Distinct from the friendly-title feature (which rewrites *trial titles*) — this is about the
app's own voice: labels, empty states, button text, section headers.

### Audit findings — current copy that reads clinical/dashboard rather than human

| Where | Current | Reads as |
| --- | --- | --- |
| Dashboard stat tile | "Trials in database" | System/inventory language |
| Dashboard stat tile | "Saved/tracked trials" | Slash-separated field name |
| Dashboard action tile | "Search Trials" / "View Saved Trials" / "Process Trials" | Menu items, not sentences |
| Nav item | "Process Trials" | Meaningless to a patient — this is an admin page |
| Trial Search | "No trials match your search." | Terse, system-error register |
| Saved Trials | "No tracked trials match this filter." | Same |
| Empty patient record | "No patient record yet. Create one to start tracking trials." | Instructional, not welcoming |

### Direction

- **Stat tiles become sentences, not field labels.** "Trials in database" → something closer to
  "trials we're watching for you." The number stays the same size and position; the label
  around it should sound like a person said it.
- **Empty states get a sentence of reassurance before the instruction**, not just the
  instruction. "No saved trials yet" reads very differently from "You haven't saved any trials
  yet — when you find one worth a second look, save it here and it'll show up on this page."
  The second is longer, and that is fine: this is not a page anyone scans quickly.
- **"Process Trials" gets an audience-aware label.** Per the whole-app-uniform scope decision,
  this page keeps its plain, low-ceremony admin language for what it actually is (pulling and
  indexing a corpus) rather than being dressed up in patient-facing warmth it doesn't need —
  but the *nav label* a patient sees should not be a page they'd never open. Worth deciding
  alongside the Researcher-mode work already in the app: this may belong behind
  Researcher-mode-only nav visibility rather than a copy rewrite. Flagged here, not resolved —
  see "Open questions" below.
- **Numbers get a sentence of context**, matching the "counts, never a percentage" no-verdicts
  rule already in place for trial matching (`project-description.md`) — the same instinct
  extends naturally to "12 trials waiting for you to look at" reading warmer than a bare stat
  tile, without inventing a score where the app deliberately has none.

### What does not change

- **No-verdicts language stays exactly as documented.** "Concerns," "open questions," "what
  matched" — this vocabulary is load-bearing (`project-description.md`'s "Hard rules": no
  eligibility verdicts, no fit score). A warmth pass rewrites tone, never the epistemic
  carefulness underneath it.
- **The AI trial check's own copy** ("nothing here rules you out," never "you match") is
  already exactly the plain-language, human register this whole document is asking for
  elsewhere. It is the existing high-water mark to write the rest of the app toward, not
  something that needs revisiting.

## 4. Less "dashboard," more "we're here to help"

This is the structural piece, not just a color/font/copy pass — some of it will mean rebuilding
sections, not just re-skinning them.

### What currently reads as "dashboard"

- The Dashboard page itself: a headline, three stat tiles, three action tiles. Information
  architecture borrowed directly from admin/analytics tooling — numbers first, a grid of
  equal-weight boxes, no narrative.
- Trial Search and Saved Trials: list-of-rows-in-cards, each row structurally identical,
  optimized for scanning many records rather than for a reader looking for one thing that
  matters to them.
- Process Trials: explicitly an admin page (pull/normalize/index a corpus) and, per the scope
  decision above, allowed to stay closer to this register than the rest of the app — but still
  due the same font/color/spacing treatment as everywhere else, so it doesn't look like a
  different, older product bolted onto the side.

### Direction

- **The Dashboard becomes a landing moment, not a stats panel.** A short, warm opening
  statement (the "we're here to help" line the brief names directly) ahead of anything
  numeric — the illustration from §2 belongs here. Stats and actions still exist below it,
  in the flat-tile language already built, but they are no longer the first thing on the page.
- **Trials for You (`RankedTrials`) is the one page already closest to this voice** — it opens
  with a plain-language explanation, uses counts instead of scores, and has an amber callout
  stating plainly that this is a starting point for conversations, not a verdict
  (`CURRENT_STATE.md`, "Trials for You — the page she actually uses"). It is the model to bring
  the rest of the app toward, not a page that itself needs rework.
- **Card density comes down.** List pages currently pack a title, badges, a summary, and
  signals into one dense card. Nothing here proposes removing information — the no-verdicts
  rule means every flag has to stay visible and explainable — but more breathing room between
  elements, and a clearer visual break between "the trial" and "what we found," would read as
  less like a data table wearing a card's clothing.

### What does not change

- **No information is hidden or removed to look softer.** Concerns, open questions, and the
  quoted criteria behind every flag stay exactly as visible as they are today. "Less
  dashboard" is a request about density and voice, not about the no-verdicts rule's own
  transparency requirement — a warmer page that hides its reasoning would be a worse page.
- **Process Trials stays functionally an admin page.** It gets the same fonts, colors, and
  tile language as everywhere else (uniform per the scope decision), but its actual content
  — pull, normalize, index, backfill — does not get softened into something it isn't. A
  patient does not need this page rewritten to sound reassuring; she needs it to not be the
  first thing she sees.

## 5. Typography, revised: Lora + Inter, with accessibility minimums

**Supersedes the Newsreader/Source Sans 3 pairing from "Where this picks up from" above.** The
user supplied a second, more targeted typography reference mid-session — one reasoned
specifically from the needs of a reader undergoing cancer treatment, not from general
warmth/trust criteria. That is a better-grounded brief than the one the earlier choice was
made against, so it wins rather than being weighed as one option among equals.

### Why this supersedes rather than supplements the earlier choice

The earlier Newsreader/Source Sans 3 pick was reasoned against "reads as gentle/editorial,
holds up against a warmth bar" — a real consideration, but not the one that matters most for
this specific audience. The new reference reasons from **vision changes during cancer
treatment** (a concrete, documented effect of some treatments and of age, not a generic
accessibility platitude) and sets minimums accordingly: 400 weight floor for body text, 16px+
base size, 1.5–1.6 line-height, and a requirement to test the final choice at 200% browser
zoom. Newsreader/Source Sans 3 were never checked against these — they were chosen for tone,
not verified for legibility under the conditions this app's actual reader may be in. That gap
is reason enough to replace rather than layer on top of the earlier pick.

### The chosen pairing: Lora (headings) + Inter (body)

The user's own top recommendation from the reference, and the reasoning holds up on
inspection:

- **Lora** is warm, human, and editorial rather than cold or corporate — but calmer and more
  restrained than Newsreader, which leaned further toward a literary/magazine register (the
  italic-emphasis technique that worked well on the Dashboard headline). Lora keeps some serif
  warmth without asking every heading to carry that much personality.
- **Inter** is the most neutral, extremely legible option on the reference list, "widely used
  in health/tech products" per the user's own note — a safer choice for a body font that
  carries eligibility criteria, diagnosis fields, and trial descriptions than a font chosen
  primarily for a particular warmth register. Source Sans 3 (the earlier pick) is also on the
  reference's list of reasonable candidates, but Inter is specifically the one named in the
  winning pairing.
- Both are free via Google Fonts, matching how the app already loads its fonts (a `<link>` in
  `index.html`, `--font-sans`/`--font-heading` tokens in the Tailwind theme) — no new loading
  mechanism needed, only new font names.

### Accessibility minimums — new to this document, apply app-wide

None of these existed as stated requirements before this reference. They are not
typeface-specific and should hold regardless of which pairing is chosen, so record them as
standing minimums rather than notes attached only to Lora/Inter:

- **400 weight minimum for all body text.** No light/thin weights anywhere text is meant to be
  read, not skimmed as a label. This affects more than the font choice — it is a check against
  every existing `font-light`/`font-thin` usage in the app, if any exist.
  Headings may use heavier weights for hierarchy; this floor is about body copy only.
- **16px minimum base size.** Below this, and combined with any vision change, text stops being
  reliably readable rather than merely small.
  Small print (timestamps, metadata) can go smaller than body text, but body text itself should
  not.
- **1.5–1.6 line-height** on body text. Tighter line-height reads faster for an unimpaired
  reader and worse for one with any visual difficulty — the tradeoff should resolve toward the
  more careful reader, not the faster one, given who this app is for.
- **Test at 200% browser zoom before calling any typography change finished.** Named explicitly
  in the reference as "a real thing older or vision-impaired patients will do" — not a
  theoretical edge case. This is a verification step for implementation, not a design decision,
  but it belongs in this document because skipping it would silently undo the whole point of
  this section.

### What to avoid — also new, and worth stating as a standing constraint

The reference names specific typefaces/styles that read as "engineering tool" rather than
"care," which lines up with and sharpens what "less dashboard" in §4 is already asking for:

- **No condensed typefaces**, anywhere — condensed forms exist to fit more into less space,
  which is a data-density optimization exactly opposed to what §4 is asking the whole app to
  move away from.
- **No heavy all-caps treatments.** This is a direct conflict with a choice already built this
  session: the flat-tile eyebrow labels (`Trials in database`, `Action`, "ARTICLE"-style
  labels on the Dashboard tiles) currently use `uppercase tracking-wide` — small, restrained
  all-caps, not "heavy." Worth a deliberate check against this new guidance rather than an
  assumption it is fine, since the reference calls out all-caps specifically. Flagged, not
  resolved — see "Open questions."
- **No monospace or sharp geometric sans** (the reference names Space Grotesk specifically) —
  not currently used anywhere in the app, so this is a constraint to keep in mind going
  forward rather than a change to make now.

### What does not change

- The italic-for-emphasis technique on the Dashboard headline (`Breast Cancer` in "Breast
  Cancer Trial Finder") — Lora supports italics the same way Newsreader did, so the technique
  carries over; only the typeface underneath it changes.
- Every place `font-heading` is already applied (page titles across Trial Search, Saved
  Trials, Trials for You, Process Trials, Patient Diagnosis, Login, Trial Detail, the nav
  brand) — the token-based approach built earlier this session means swapping the typeface is
  a one-line change to what `--font-heading` resolves to, not a per-page rewrite. The same is
  true of `--font-sans` for the body-font swap.

## Open questions

Recorded rather than resolved, so nothing here is silently decided by omission:

1. **Should "Process Trials" (and any other admin-only surface) be hidden from the nav in
   Patient mode** now that the Patient/Researcher toggle exists, rather than only recopied?
   The toggle already exists for exactly this kind of audience-shaped difference — this may be
   the more honest fix than better labeling.
2. **Illustration sourcing** — which set, generator, or license. Out of scope here by design;
   the direction (illustration, not photography of people) is decided, the source is not.
3. **Exact dark-warm-green and beige values** — the hues are named by the user, not left to
   guess, but the specific hex/oklch triples (nav green, button green, page beige, card beige)
   are still implementation work, not a design decision this document makes on its own. Should
   be checked for contrast (white nav text on the new dark green; body text on the new beige)
   before being finalized, the same 4.5:1 bar applied to the current green nav.
4. **Exact value for the third, lighter green-tint role** (illustration accents, empty-state
   messaging, warm callouts) named in §1 — still open in the way item 3 used to be, since the
   user's answer named the two anchor colors (dark warm green, beige) but not this third,
   softer role.
5. **Whether the palette needs its own semantic token** (distinct roles for nav/button green
   vs. the lighter accent green, vs. page beige vs. card beige) or can stay one-off per
   component — a real decision, deferred to implementation rather than guessed at here.
6. **Do the flat-tile eyebrow labels' small `uppercase tracking-wide` treatment conflict with
   §5's "no heavy all-caps" guidance?** They are restrained (small, not bold-heavy, not a full
   line of caps) rather than what the reference is warning against, but the reference names
   all-caps specifically enough that this deserves a deliberate check rather than an assumed
   pass. If it needs to change, the eyebrow-label pattern itself (not just its font) would need
   a second look — sentence case with a different visual treatment (color, size, spacing) could
   carry the same "this is a label, not the headline" job without relying on caps at all.
7. **Audit body text for any existing light/thin font weights** against §5's 400-weight floor —
   not checked yet; this document names the rule, verifying the app already meets it (or finding
   where it doesn't) is implementation work.

**Resolved, not open**: the two anchor palette colors (dark warm green, beige — see §1,
superseding the earlier "softer sage" guess and the undecided background hue), and typography
— **twice resolved**, first to Newsreader/Source Sans 3, then superseded by §5 to Lora + Inter
plus the accessibility minimums. §5's answer is the current one; the Newsreader pick is kept
in "Where this picks up from" only as a record of what was tried and why it was replaced.

## What this document does not cover

Implementation order, specific Tailwind classes, or exact hex/oklch values — those belong to
the work itself, not the plan, and putting code in a `.claude`-area document is against this
project's own documentation convention. This document is the "what and why"; the next
`/ui-ux-pro-max` pass or direct implementation session is the "how."
