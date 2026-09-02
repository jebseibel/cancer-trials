# Diagnosis document-upload AI intake — status

Stopping point as of this session. Full design plan (all confirmed decisions and the reasoning
behind each) lives at `/home/jeb/.claude/plans/golden-meandering-river.md` — read that first if
picking this back up cold.

## What this feature does

On the Patient Record page, a button lets the user paste or upload a plain-text oncology
document. It is checked locally for identifying information and rejected outright if any is
found — that check never calls anywhere, so flagged text never reaches AI. If it passes, AI
extracts structured fields across Diagnosis, Variants, and Prior Treatment. If cancer type,
stage, ER/PR/HER2 status, or ECOG can't be found, a short back-and-forth asks the user directly.
When done, the draft prefills the three existing forms for the user to review and save
themselves — nothing is written to the database by this feature directly, and nothing about the
document, the extraction, or the conversation is ever persisted anywhere.

## State: implementation complete, not yet manually verified live

Everything below has been written and passes automated checks. It has **not** been exercised
against a live backend with a real Anthropic API key — that's the next step, and it needs the
backend running, which is the user's to start.

### Backend — done, tests passing

- The local PHI/PII gate (name near a header, labeled date of birth, MRN, SSN, phone, email,
  address, a demographics block of several of those together) — biased to over-flag on purpose.
- The in-memory session/conversation state — never written to any table, expires after 30
  minutes of inactivity, capped at 200 concurrent sessions and 8 clarifying turns.
- The extraction and clarifying-turn calls to the existing AI service, following the same
  pattern already used for the AI trial check elsewhere in the app.
- The new endpoints (start / answer / skip / cancel) behind the same login-required rule as
  every other endpoint.
- Automated tests: originally 29 new tests, since grown by the PHI-gate hardening passes below
  (see "Coverage gap found and closed" and "Stress-testing phone, email, and address"); includes
  one that proves the AI is never called at all when the local gate flags a document. Full
  backend suite: 200 tests, 1 pre-existing unrelated failure (see those sections for detail).

### Frontend — done, builds and lints clean

- An "Upload document to prefill" button on the Patient Record page, shown only when AI is
  configured.
- The upload/paste → accept-or-reject → question-and-answer → review screen, as a popup.
- The three tabs (Diagnosis, Variants, Prior Treatment) each pick up their share of a completed
  draft the first time they're opened after one is ready — but only into a blank record, never
  overwriting something already saved.
- Switching to a different patient record clears out any leftover draft so it can't bleed into
  the wrong person's forms.

### Button styling (2026-09-02)

The "Upload document to prefill" button on the Patient Record page originally used the same
neutral, stone-bordered style as ordinary secondary buttons elsewhere on the page, so it didn't
stand out. Restyled to a solid version of the app's brand green (the same green as the top nav
bar), with white text, so it reads as the primary action on that page. Uses the app's existing
brand-green theme color and its paired hover shade rather than a one-off color, matching the one
other place in the app that already styles a button this way (the active-mode toggle on Trial
Search). Confirmed working live by the user. Frontend lint clean (one pre-existing, unrelated
lint error remains in `Login.tsx`, untouched by this change).

## PHI/PII gate — what gets filtered

The gate runs once, on the initial document upload, before anything is sent to AI. It never runs
on the follow-up clarifying answers — those are treated as the user's own short replies, not a
new document. A single hit on any item below rejects the whole document; there's no partial-allow
or redaction, and it's deliberately biased to over-flag rather than let something through.

Full list of what it currently filters out:

1. **Patient or clinician name** — labeled ("Patient Name:", "Name:", "Pt.", "Mr./Mrs./Ms.",
   "Dr."), or unlabeled prose in any of three forms: "Patient Jane Doe presents with...", "Ms Jane
   Doe was evaluated..." (title with no punctuation), or "The patient, Jane Doe, was evaluated..."
   (comma apposition).
2. **Date of birth** — labeled ("DOB:", "date of birth", "birth date", "D.O.B.") or unlabeled
   prose ("...was born on 3/14/1965" / "born in March 1965").
3. **Medical record number** — "MRN", "medical record number/no.", or "patient ID/number".
4. **Social Security number** — a dashed number always counts; a bare 9-digit number only counts
   if "SSN" or "social security" appears nearby (so an accession/lot number isn't a false hit).
5. **Phone number** — any US-style phone number pattern, anywhere in the document. Does not catch
   a phone number spelled out in words (e.g. "five five five...") — see accepted limits below.
6. **Email address** — any standard email address pattern, anywhere in the document. Does not
   catch an obfuscated address (e.g. "jdoe [at] gmail [dot] com") — see accepted limits below.
7. **Street address** — a street-suffix pattern (Street, Avenue, Boulevard, Road, Drive, Lane,
   Way, Court, Circle, Terrace, Place, Trail, Parkway, Plaza, or an abbreviation of one of those),
   a "STATE ZIP" pattern, or the literal word "address".
8. **Demographics block** — two or more of {patient, name, DOB, MRN, address, phone} appearing as
   colon-labeled fields within a few lines of each other, even if none individually matched one
   of the rules above on its own.

Every item above has now had an adversarial pass run against it and been hardened against the
unlabeled-prose gaps that pass found, except one deliberately accepted limit: a phone number or
email address that's been spelled out or obfuscated specifically to defeat pattern matching isn't
caught, and isn't expected to be — see "Stress-testing phone, email, and address" below.

### How each item is actually detected (mechanics)

Each item above is a pattern/proximity check, not real understanding of the text — mostly it
looks for identifying-looking values sitting next to a specific marker word:

- **Name near a header** — a label like "Patient Name", "Name", "Pt", "Mr", "Mrs", "Ms", or "Dr"
  immediately followed by a colon or "#" and one to three capitalized words. Separately, a short
  label followed by a colon and two capitalized words, searched across at least the first 200
  characters of the document (or its opening third, whichever reaches further, so a long document
  isn't searched end-to-end). Separately, "Patient" or "Pt" directly followed by two capitalized
  words anywhere in the document. Separately, "Ms"/"Mr"/"Mrs" directly followed by a capitalized
  name with no punctuation required, anywhere in the document. Separately, a name set off by
  commas after "the patient," anywhere in the document.
- **Clinician name** — the literal "Dr." followed by a capitalized name, checked anywhere in the
  document.
- **Date of birth** — the words "DOB", "D.O.B.", "date of birth", or "birth date" with a
  date-shaped value found close by. Separately, the word "born" (with or without "on"/"in")
  followed nearby by anything date-shaped, with no label required at all.
- **Medical record number** — "MRN", "medical record number/no.", or "patient ID/number" followed
  by a value.
- **SSN-like** — a dashed 9-digit number always counts; a bare 9-digit number only counts if the
  words "SSN" or "social security" appear nearby.
- **Phone number** — a US-style phone number pattern.
- **Email address** — a standard email pattern.
- **Address-like** — a street-suffix pattern (Street, Avenue, Boulevard, Road, Drive, Lane, Way,
  Court, Circle, Terrace, Place, Trail, Parkway, Plaza, or an abbreviation), a "STATE ZIP" pattern,
  or the literal word "address".
- **Demographics block** — two or more of {patient, name, DOB, MRN, address, phone} all appearing
  as colon-labeled fields within a few lines of each other, even if none of them individually
  tripped one of the rules above.

### Coverage gap found and closed (2026-09-02)

A prior review found the gate missed a name or date of birth stated in ordinary prose with no
label — "Patient Jane Doe was born on 3/14/1965" tripped nothing: no "DOB"/"date of birth"/
"birth date" wording for the date rule to anchor to, and the name rule's label list didn't
include the bare word "Patient" (only "Patient Name"), so neither the name nor the date was ever
seen. This is what let a real name and DOB through during manual testing.

Two more rules were added to close it, on top of the ones above rather than loosening them:

- **Unlabeled birth phrase** — the word "born" (optionally with "on"/"in") followed within a
  short distance by anything date-shaped, with no "DOB" label required at all. Catches "born on
  3/14/1965" and "born in March 1965" alike.
- **Unlabeled patient name in prose** — the word "Patient" or "Pt" directly followed by two
  capitalized words, with no colon or "Name" label required. Catches "Patient Jane Doe presents
  with..." while still leaving plain clinical prose like "Patient presented with..." alone, since
  the word right after "Patient" there isn't capitalized.

Both are covered by new unit tests, including near-miss tests confirming ordinary clinical
phrasing ("Patient presented with...") still passes clean. All pre-existing tests still pass
unchanged. This closes the specific gap found; it does not claim to catch every possible
unlabeled phrasing, and the gate remains a heuristic scan, not a guarantee.

### Stress-testing phone, email, and address (2026-09-02)

Following the request to confirm phone/email/address are solid, a batch of adversarial cases was
run against each and turned into permanent regression tests (14 new tests, all passing; full
backend suite otherwise unaffected — one pre-existing, unrelated failure remains, a Liquibase
seed-data checksum mismatch that has nothing to do with this feature).

**Confirmed working**, including forms not previously tested: a dotted phone number
("555.123.4567"), a bare unformatted 10-digit phone number, a phone number with a country code,
an email address embedded mid-sentence, a street address embedded mid-sentence, and a bare
city/state/ZIP with no street line at all. Also confirmed clinical prose that superficially
resembles these (percentages, tumor measurements, node counts) does **not** falsely flag.

**Five new gaps were found this way. Four are now closed** (2026-09-02, same session):

- **Address suffix list was incomplete** — the street-address rule only recognized Street, Avenue,
  Boulevard, Road, Drive, Lane, Way, Court, and Circle (and their abbreviations). "742 Evergreen
  **Terrace**" alone wasn't caught. **Fixed**: added Terrace, Place, Trail, Parkway, and Plaza (and
  their abbreviations) to the suffix list.
- **A name introduced by comma apposition wasn't caught** — "The patient, Jane Doe, was evaluated
  today" was missed, because neither name rule matched a name set off by commas; both expected the
  triggering word directly adjacent to the name. **Fixed**: added a rule specifically for "the
  patient, `<Name>`," apposition.
- **A title used conversationally, with no punctuation, wasn't caught** — "Ms Jane Doe was
  evaluated in clinic today" was missed, since the labeled-header rule required a colon or "#"
  right after the title. **Fixed**: added a rule for "Ms/Mr/Mrs `<Name>`" with no punctuation
  required. ("Patient"/"Pt" were already covered by the earlier prose-name fix; "Dr" was already
  covered by the separate clinician-name rule.)
- **The labeled-name-pair rule could be truncated away in a short document** — it only searched
  the opening third of the document (to avoid false hits deep in a long one), but for a short
  document that "opening third" could be just a handful of characters, cutting off before the
  label finished. "Referring provider: Jane Doe" (29 characters) was missed because only the
  first 9 characters ("Referring") were ever searched. **Fixed**: added a 200-character floor to
  the search window on top of the existing proportional cap, so short documents are searched in
  full while long documents keep the same guard against false hits deep in the text.

**One is intentionally left open, as an accepted limit rather than a bug:**

- **Spelled-out or obfuscated contact info** — "five five five, one two three, four five six
  seven" for a phone number, or "jdoe [at] gmail [dot] com" for an email, are not caught. Closing
  this well would mean matching on ordinary words like "at"/"dot"/digit-words, which appear
  constantly in normal clinical prose and would cause heavy false-positive rejections. Not worth
  the trade for a pattern-based scan; deliberately left as documented residual risk.

All four fixes and the one accepted limit are covered by permanent regression tests (7 more new
tests on top of the 14 from the stress-testing pass — 35 total in the class). Full backend suite
otherwise unaffected: one pre-existing, unrelated failure remains, a Liquibase seed-data checksum
mismatch that has nothing to do with this feature.

### Not yet done

- **Manual end-to-end check with the backend running and a real API key.** Needs: paste a
  sample document missing something (say, HER2 status), confirm it asks about it, answer,
  confirm the Diagnosis tab picks it up correctly. Separately, paste something with a made-up
  name/date-of-birth near the top and confirm it's rejected and the log shows no AI call was
  ever made for it. (The button itself — visibility, styling, opening the popup — has been
  confirmed working live; it's the upload → extract → prefill flow behind it that still needs
  the live check.)
- Nothing has been committed to git. Everything is new, untracked files plus a handful of small
  additions to existing files (see the file list below) — nothing existing was rewritten or
  removed.

## Files touched

New backend files: the local PHI-checking class and its test, a new `intake` subfolder holding
the session/conversation logic and its tests, the new controller and its request/response
types, two new prompt text files.

Small additions to existing backend files: one new error-handling entry in the shared error
handler, one scheduling annotation added to the main application class so expired sessions get
cleaned up automatically.

New frontend files: the popup component, and a small shared helper that hands the finished
draft from the popup over to the three tabs.

Small additions to existing frontend files: the new button and popup wiring on the Patient
Record page, one new block in each of the three tab pages that picks up a waiting draft, one
line in the patient-switching logic that clears a leftover draft, plus the new types and API
calls added alongside the existing ones.

## Picking this back up

1. Read `/home/jeb/.claude/plans/golden-meandering-river.md` for the full design reasoning.
2. Start the backend with an API key set, and run the manual check described above.
3. Once that looks right, this is ready to commit.
