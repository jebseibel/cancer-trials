# AI Prompt Management & Wiring

## Problem Statement
The client wants to be able to manage and update AI prompting for each of the 12 retire cert service calls. Prompts are managed through a layered hierarchy and wired into the AI services at runtime.

---

## AI Services (12 total)

WREGIS (3)
1. RetWregisTransDetails
2. RetWregisTransConfirm
3. RetWregisCertQuant

MRETS (3)
4. RetMretsTransDetails
5. RetMretsTransConfirm
6. RetMretsCertQuant

NAR (3)
7. RetNarCertSubacct
8. RetNarRetireCert
9. RetNarVolComply

ERCOT (3)
10. RetErcotTransDetail
11. RetErcotScreenshot
12. RetErcotEmail

---

## Prompt Hierarchy

Prompts are organized in a four-level hierarchy:

**Soul** — The universal foundation shared across all PromptGangs. Defines core values, identity, and guardrails. System-managed; not user-editable.

**PromptGang** — A named group of PromptEnvelopes that share a common purpose (e.g., NAR, ERCOT, MRETS, WREGIS). Inherits from Soul.

**PromptEnvelope** — The configuration shell for one specific task. Belongs to a PromptGang. Defines the goal, result type, lifecycle, temperature, retries, and other execution parameters.

**Prompt** — The actual prompt content. Belongs to a PromptEnvelope. Holds the prompt text, provider, model, result format, and example input/output.

---

## Relationships

- One Soul → many PromptGangs
- One PromptGang → many PromptEnvelopes
- One PromptEnvelope → many Prompts

---

## Wiring

Of the 12 retire-cert services, **6 use AI** and load their prompts from the managed store. The other 6 use Tabula/Apache POI and do not interact with the prompt hierarchy.

**Services that load prompts at runtime:**
- WREGIS: `RetWregisTransConfirm`
- MRETS: `RetMretsTransConfirm`
- NAR: `RetNarCertSubacct`, `RetNarRetireCert`, `RetNarVolComply`
- ERCOT: `RetErcotScreenshot`

(See `ai-processing.md` for the full extraction-method table.)

**How a service finds its prompt:**
- Each service holds a hard-coded `PROMPT_KEY` constant (e.g. `"ERCOT_SCREENSHOT"`, `"WREGIS_TRANS_CONFIRM"`).
- `PromptLoaderService.getPromptConfig(promptKey)` resolves it via `AiPromptDbService.findByUniqueId(promptKey)` — i.e. matched against `AiPrompt.uniqueId` directly. PromptEnvelope/PromptGang/Soul fields are **not** read at extraction time; they're parent containers only.
- The service receives back a `RetirementPrompt` DTO containing `provider`, `model`, and the assembled prompt text (see `ai-prompt-structure.md` → *Prompt Assembly* for how `content` + `resultFormat` + example pair are combined).

**Caching:** None. Every extraction triggers a fresh `findByUniqueId` DB lookup.

---

## Export

All three controllers (Gang, Envelope, Template) expose export endpoints:
- `GET /api/ai-prompt-gang/{extid}/export`
- `GET /api/ai-prompt-gang/export-all`
- `GET /api/ai-prompt-envelope/{extid}/export`
- `GET /api/ai-prompt-envelope/export-all`
- `GET /api/ai-prompt-template/{extid}/export`
- `GET /api/ai-prompt-template/export-all`

Exports are handled by `AiPromptExportService`.

---

## Startup Seeding

`AiPromptSeedService` runs on `ApplicationReadyEvent` and seeds the **full four-level hierarchy** from YAML files on the classpath:

| Level | Path |
|---|---|
| Soul | `classpath:db/changelog/ai-files/1-soul/*.yaml` |
| PromptGang | `classpath:db/changelog/ai-files/2-promptgang/*.yaml` |
| PromptEnvelope | `classpath:db/changelog/ai-files/3-prompt-envelopes/*.yaml` |
| Prompt | `classpath:db/changelog/ai-files/4-prompts/*.yaml` |

**Behavior:**
- Each record is keyed by `uniqueId`. If a record with that `uniqueId` already exists, it is skipped (idempotent — never overwrites).
- Parent linkage is resolved by `uniqueId`: a Gang YAML references its Soul via `soulUniqueId`; an Envelope references its Gang via `gangUniqueId`; a Prompt references its Envelope via `envelopeUniqueId`. Missing parents cause the record to be skipped with a warning.
- New `extid` values are generated (`UUID.randomUUID()`) on seed; YAMLs do not carry `extid`.
