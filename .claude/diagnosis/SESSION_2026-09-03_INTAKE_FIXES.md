# Session 2026-09-03 — export bug, PHI-gate redesign

Picks up from `DOCUMENT_INTAKE_STATUS.md`. **Nothing in this session is committed yet** —
everything below is sitting in the working tree on `docs-and-build-cleanup`, and you were
mid-manual-check when the session ended. Restart here tomorrow.

## Where things stand

- **Backend**: compiles clean, full root-module test suite passes (140+ tests), 60
  `PhiHeuristicScannerTest` cases, 7 `DiagnosisIntakeExtractionServiceTest` cases, all green.
- **Frontend**: typecheck clean, lint clean (only the pre-existing unrelated `Login.tsx` error).
- **Not done**: you were live-testing the second fix (PHI redesign) when we stopped. No git
  commit yet. One open decision below, not yet answered.

## 1. Fixed: "Download my record" was silently missing the Diagnosis section

**Root cause**: `PatientRecord.tsx` and each of the three tab pages (`Diagnosis.tsx`,
`Variants.tsx`, `PriorTreatment.tsx`) used the *same* React Query key (e.g.
`['patientDiagnosis', extid]`) but cached **different shapes** — `PatientRecord.tsx` cached the
raw array, each tab page cached `rows[0] ?? null`. Same cache slot, two shapes: whichever query
ran last silently overwrote the other's shape, so `diagnoses?.[0]` on an object-shaped cache
entry returned `undefined` with no error — the whole DIAGNOSIS section just vanished from the
export. Since Diagnosis is the first/default tab, it was the one most likely to have been
visited (and re-cached as an object) right before a download.

**Fix**: all four query sites now cache the identical raw-array shape; the three tab pages use
React Query's `select` to derive their single-row view instead of returning a different shape
from `queryFn`. Confirmed correct live by you — re-downloaded, all three sections present.

Files: `frontend/src/pages/PatientRecord.tsx`, `Diagnosis.tsx`, `Variants.tsx`,
`PriorTreatment.tsx`.

## 2. Fixed: a real `LABELED_NAME_PAIR` false positive from the export

Traced a second live rejection (`categories=[NAME_NEAR_HEADER]`) by writing a standalone pattern
tracer against the actual downloaded document. Found: `LABELED_NAME_PAIR`'s regex used `\s`
(matches newlines) both after the colon and between the two "name" tokens, so
`"Metastatic: Yes\nMetastasis sites:"` read as label `"Metastatic:"` + value tokens `"Yes"` +
`"Metastasis"` (the next line's first word) — a false positive, same bug class as this morning's
`UNLABELED_PATIENT_NAME` line-break fix, just never applied to this sibling pattern.

**Fix**: `LABELED_NAME_PAIR` now uses `[ \t]` instead of `\s`, so it can't cross a line break.
Regression tests added confirming the false positive is gone and the real case ("Referring
provider: Jane Doe" on one line) still flags.

Also added: `NAME_NEAR_HEADER` now emits a specific sub-reason alongside the umbrella category
(`NAME_HEADER_LABEL`, `UNLABELED_PATIENT_NAME`, `UNPUNCTUATED_TITLE_NAME`, `APPOSITION_NAME`, or
`LABELED_NAME_PAIR`), so a rejection log line names which of the five underlying patterns fired
— still content-free, never the matched text — instead of just the umbrella code. This is what
let the second bug get traced quickly instead of guessed at.

Files: `src/main/java/com/seibel/cancer/service/ai/PhiHeuristicScanner.java` + its test.

## 3. Redesigned: PHI gate is now per-line, not whole-document

**Your call, deliberately overriding my initial recommendation to keep whole-document gating.**
You judged the all-or-nothing design untenable in practice — one bad line (even a false
positive) made an entire otherwise-clean multi-field document unusable, three times in one day.

**What changed:**

- `PhiHeuristicScanner.scanLines(text)` — new entry point. Scans each line independently against
  every heuristic *except* `DEMOGRAPHICS_BLOCK`, which is inherently cross-line (two sparse
  labels co-occurring nearby) and was dropped rather than adapted, per your explicit choice. This
  is a real, documented, accepted gap — a demographics block made of individually-sparse lines
  (bare "Patient:" / "DOB:" / "MRN:" each on their own line) no longer trips anything under
  `scanLines`, though it still does under the original `scan()`. Test pins this on record.
- Returns `PhiLineScanResult`: `cleanedText` (flagged lines **removed**, not redacted-in-place —
  no placeholder for extraction to guess around) + `excludedLines` (1-indexed line number +
  category reasons, never the excluded text — same rule the scanner has always followed).
- `DiagnosisIntakeExtractionService.extract()` rewired: scrubs the document, and if literally
  everything gets scrubbed, **skips the AI call entirely** and returns an empty draft rather than
  spending a paid call on nothing (your choice, over "always call AI anyway").
- Returns a new `DiagnosisIntakeUpload` record (draft + excluded lines) instead of throwing
  `PhiDetectedException` — that exception class and its `GlobalExceptionHandler` entry are still
  in the codebase but now **unreachable from this feature**. Left in place rather than deleted.
- Controller (`DiagnosisIntakeController`) and `ResponseDiagnosisIntakeSession` updated to carry
  `excludedLines` back to the frontend on `/start` (never populated on `/answer`/`/skip`, since
  clarifying turns don't go through the PHI scan at all).
- Frontend: `DiagnosisIntakeModal.tsx` now shows an amber callout on the "complete" step listing
  which line numbers were cut and why, so a false positive is visible and the user can manually
  type that field in on the relevant tab instead of it silently vanishing. Upload-step copy
  updated to describe per-line screening instead of the old whole-document rejection.

**Open, unanswered**: whether to delete `PhiDetectedException` + its `GlobalExceptionHandler`
entry now that nothing throws it, or leave them as dead-but-harmless infrastructure. Asked, not
yet answered when the session ended.

## Files touched, full list

Backend:
- `service/ai/PhiHeuristicScanner.java` — `scanLines()` added, `LABELED_NAME_PAIR` line-break
  fix, `NAME_NEAR_HEADER` sub-reasons
- `service/ai/PhiLineScanResult.java` — new
- `service/ai/intake/DiagnosisIntakeExtractionService.java` — scrub-then-send rewrite
- `service/ai/intake/DiagnosisIntakeUpload.java` — new
- `web/controller/DiagnosisIntakeController.java` — carries excluded lines through
- `web/response/ResponseDiagnosisIntakeSession.java` — `excludedLines` field added
- `service/ai/PhiHeuristicScannerTest.java` — regression + new `scanLines` tests (60 total)
- `service/ai/intake/DiagnosisIntakeExtractionServiceTest.java` — rewritten for new behavior

Frontend:
- `pages/PatientRecord.tsx`, `Diagnosis.tsx`, `Variants.tsx`, `PriorTreatment.tsx` — query cache
  shape fix
- `types/api.ts` — `DiagnosisIntakeExcludedLine` type, `excludedLines` on
  `DiagnosisIntakeSession`
- `components/DiagnosisIntakeModal.tsx` — excluded-lines callout, updated copy

## Picking this back up tomorrow

1. Finish your live manual check — you were mid-check when we stopped. Confirm the second
   document (the one that hit `LABELED_NAME_PAIR`) now goes through cleanly, and that the
   excluded-lines callout renders correctly if anything does get cut.
2. Decide the open question above (delete or keep `PhiDetectedException`).
3. Nothing is committed — review the diff and commit when satisfied.
