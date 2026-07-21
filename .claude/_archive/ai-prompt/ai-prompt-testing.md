# AI Prompt Testing

## What We're Building

A way to test an `AiPrompt` record against a real PDF. The user uploads a file from the `AiPromptDetail` page — the FE parses `jsonVariablesTest` from the already-loaded `AiPrompt` record to get the upload parameters, then calls the existing retirement upload endpoint.

---

## AiPrompt Fields Involved

| Field | Purpose |
|-------|---------|
| `content` | The base system prompt sent to the AI (emitted bare, no wrapping tag) |
| `resultFormat` | Wrapped in `<result_format>...</result_format>` and appended to `content` |
| `exampleInput` | Wrapped in `<example><input>...</input></example>` and appended |
| `exampleOutput` | Wrapped in `<example><output>...</output></example>` and appended (shares the `<example>` block with `exampleInput` when both are present) |
| `provider` | Which AI company to use (e.g. `google`, `anthropic`, `openai`) |
| `model` | Which model to use (e.g. `gemini-2.5-flash`) |
| `jsonVariables` | Variable substitution placeholders (production use — not yet wired) |
| `jsonVariablesTest` | Test parameter values used when submitting to the upload endpoint |

See `ai-prompt-structure.md` → *Prompt Assembly (Runtime)* for the full assembly rules.

`jsonVariablesTest` holds all parameters needed to call the retirement upload endpoint:
- `trackingSystem` (e.g. `WREGIS`, `ERCOT`, `NAR`, `MRETS`)
- `docType` (e.g. `TransDetails`, `TransConfirm`, etc.)
- `customer` (customer extid or identifier)
- `year`

---

## Test Flow

```
User (FE)
  │  uploads PDF on AiPromptDetail page
  ▼
FE parses AiPrompt.jsonVariablesTest
  │  extracts: trackingSystem, docType, customer, year
  ▼
POST /api/retirement/upload   (multipart/form-data)
  params: trackingSystem, docType, customer, year, file
  │
RetirementService routes to tracking-system-specific service
  │
  ▼
Returns: batchUuid, recordCount, status
  │
  ▼
FE redirects to tracking-system+docType specific path
  e.g. /retire-upload/wregis-trans-details/{batchId}
       /retire-upload/ercot-email/{batchId}
  (one of 12 paths, same verification pages as the normal retirement upload flow)
```

---

## Backend — No New Endpoint Needed

The existing `POST /api/retirement/upload` endpoint handles everything.

---

## Frontend — What Needs Building

### AiPromptDetail page additions

- File drop/select input (PDF)
- "Test Prompt" button
- On submit: parse `jsonVariablesTest` JSON to extract `trackingSystem`, `docType`, `customer`, `year`
- Call the existing `retirementApi.uploadFile(...)` function from `api.ts` — no new api.ts function needed
- On success: redirect to the tracking-system+docType specific path (e.g. `/retire-upload/wregis-trans-details/{batchId}`)

---
## Future Enhancements

- The `docType` value in `jsonVariablesTest` currently uses the short form (e.g. `Screenshot`, `TransConfirm`). In the future, we should pass the full name (e.g. `ErcotScreenshot`, `MretsTransConfirm`) so the value is self-describing and unambiguous across tracking systems.

---

## Notes

- Data IS written to the database — this is a real upload, not a dry run
- `jsonVariablesTest` is already loaded on the `AiPromptDetail` page as part of the `AiPrompt` record
- The FE parses `jsonVariablesTest` client-side to extract upload parameters
- After upload, FE redirects to the tracking-system+docType specific path — identical to the normal retirement upload redirect
