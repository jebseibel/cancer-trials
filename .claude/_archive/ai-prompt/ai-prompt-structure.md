# AI Prompt Structure

A pure conceptual prompt and component model with no implementation details.

---

## System Overview

A prompt system composed of reusable, layered parts. Each part has a distinct role, and together they define how an AI thinks, speaks, and behaves.

---

## Core Parts

**Soul** — The universal foundation. The core values, fundamental identity, and guardrails every PromptGang inherits and never overrides. Has a name, purpose, coreValues, guardrails, and lifecycle.

**PromptGang** — A named group of PromptEnvelopes that share a common purpose. Inherits from Soul (via `soulExtid`). Owns uniqueId, persona, tone, gang, subgang, promptSequence, prohibitions, decisionPrinciples, memoryDepth, lifecycle, and version.

**PromptEnvelope** — The organizational and configuration shell for one task (e.g., Email, Screenshot, TransDetail). Lives under a PromptGang (via `gangExtid`, with denormalized `gangName`). Owns one or many Prompts and defines uniqueId, resultType, lifecycle, subPersona, goal, decisionPrinciples, toolExclusions, temperature, maxRetries, timeoutSeconds, category, subCategory, and version.

**Prompt** — The actual content of a prompt. Holds uniqueId, content, trackingSystem, provider, model, jsonVariables, jsonVariablesTest, resultFormat, exampleInput, exampleOutput, lifecycle, and version. Lives under a PromptEnvelope (via `envelopeExtid`, with denormalized `envelopeName`).


---

## Universal Fields

Every entity inherits these from `BaseDomain`. Not listed individually in the entity map.
**id**
**extid**
**createdAt**
**updatedAt**
**deletedAt**
**active**

## Near-Universal Fields

These appear on most entities but are defined per-entity, not inherited:
- **name** — Soul, PromptGang, PromptEnvelope, Prompt
- **description** — PromptGang, PromptEnvelope, Prompt (not on Soul)
- **uniqueId** — PromptGang, PromptEnvelope, Prompt (not on Soul) — the logical "slot" key used for promotion
- **lifecycle** — all four entities
- **version** — PromptGang, PromptEnvelope, Prompt (not on Soul)

---

## Supporting Concepts

**Result Type** — The target object name the result maps to (e.g., `User`). Platform agnostic. Lives on PromptEnvelope as `resultType`.

**Result Format** — The full JSON shape of the response. Lives on Prompt as `resultFormat`. Example: User {"email":"john.doe@example.com","phone":"555-123-4567","name":{"first":"John","last":"Doe"}}. At runtime this value is wrapped in `<result_format>...</result_format>` tags and appended to the system prompt (see *Prompt Assembly* below).

**Example Input / Example Output** — A paired sample showing what the AI should expect (`exampleInput`) and what it should produce (`exampleOutput`). Both live on Prompt. At runtime they are wrapped together inside a single `<example><input>...</input><output>...</output></example>` block (Anthropic-recommended XML structure, also accepted by OpenAI/Gemini/OpenRouter).

**Sub Persona** — A prompt-level persona that narrows or specializes the PromptGang's persona for a specific task. Example: You specialize on certain financial PDF docs. Lives on PromptEnvelope as `subPersona`.

**Tool Exclusions** — A comma-separated list of tools the AI is not allowed to use during this prompt execution. Lives on PromptEnvelope as `toolExclusions`.

**Goal** — The specific objective this PromptEnvelope is trying to achieve. Example: "Extract as per the result format but be sure to check the '0' and '8' as they tend to be the same." Lives on PromptEnvelope as `goal`.

**Memory Depth** — How many prior conversation turns the AI retains and uses as context. Lives on PromptGang as `memoryDepth`.

**Provider** — Which AI company delivers the model Example: OpenAI, Anthropic, Google.

**Model** — The specific model version used. Example: GPT-5, Claude Opus 4.7, Gemini 2.5 Pro

---

## Why This Works at the Form Level

- Separates what things are from how things relate — definitions first, wiring later
- Distinguishes parts from attributes — Soul, PromptGang, PromptEnvelope, and Prompt are the nouns; everything else describes them
- Avoids implementation leakage — no mention of tables, JPA, JSON, or Java
- Keeps vocabulary consistent — "Soul," "PromptGang," and "PromptEnvelope" are used the same way throughout

Once the form is settled, the implementation (entities, relationships, annotations) will almost write itself.

---

## Reading the Relationships

**Soul → PromptGang (one-to-many)** — One Soul, inherited by many PromptGangs. Every PromptGang must have a Soul.

**PromptGang → PromptEnvelope (one-to-many)** — A PromptGang owns many PromptEnvelopes (e.g., ERCOT owns Email, Screenshot, TransDetail).

**PromptEnvelope → Prompt (one-to-many)** — A PromptEnvelope owns one or many Prompts containing the actual content.

**Category -> organizational unit for PromptEnvelope**

**Subcategory -> suborganizational unit for PromptEnvelope**

---

## Entity Map

```
┌─────────────────────┬──────────┐
│        Soul         │ Required │
│─────────────────────┼──────────│
│ name                │ true     │
│ purpose             │ true     │
│ coreValues          │ true     │
│ guardrails          │ true     │
│ lifecycle           │          │
└──────────┬──────────┴──────────┘
           │ 1
           │
           │ many
┌──────────▼──────────┬──────────┐
│     PromptGang      │ Required │
│─────────────────────┼──────────│
│ uniqueId            │          │
│ name                │          │
│ description         │          │
│ persona             │          │
│ gang                │          │
│ subgang             │          │
│ tone                │          │
│ promptSequence      │          │
│ prohibitions        │          │
│ decisionPrinciples  │          │
│ memoryDepth         │          │
│ lifecycle           │          │
│ version             │          │
│ soulExtid           │          │ ← FK to Soul
└──────────┬──────────┴──────────┘
           │
           │ 1:many
┌──────────▼──────────┬──────────┐
│    PromptEnvelope   │ Required │
│─────────────────────┼──────────│
│ uniqueId            │          │
│ name                │          │
│ description         │          │
│ category            │          │
│ subCategory         │          │
│ resultType          │          │
│ subPersona          │          │
│ goal                │          │
│ decisionPrinciples  │          │
│ toolExclusions      │          │
│ lifecycle           │          │
│ temperature         │          │
│ maxRetries          │          │
│ timeoutSeconds      │          │
│ version             │          │
│ gangExtid           │          │ ← FK to PromptGang
│ gangName            │          │ ← denormalized
└──────────┬──────────┴──────────┘
           │
           │ 1:many (PromptEnvelope owns many Prompts)
┌──────────▼──────────┬──────────┐
│       Prompt        │ Required │
│─────────────────────┼──────────│
│ uniqueId            │          │
│ name                │          │
│ description         │          │
│ trackingSystem      │          │
│ provider            │          │
│ model               │          │
│ version             │          │
│ content             │          │
│ jsonVariables       │          │
│ jsonVariablesTest   │          │
│ resultFormat        │          │
│ exampleInput        │          │
│ exampleOutput       │          │
│ lifecycle           │          │
│ envelopeExtid       │          │ ← FK to PromptEnvelope
│ envelopeName        │          │ ← denormalized
└─────────────────────┴──────────┘
```

> **Note on Required flags:** The domain classes (`AiPromptGang`, `AiPromptEnvelope`, `AiPrompt`) do not declare nullability at the Java level. Required/optional status is enforced at the database column level. For Soul, the DB columns for `name`, `purpose`, `coreValues`, and `guardrails` are `nullable=false`; the others on Soul/Gang/Envelope/Prompt do not enforce non-null in the domain model.

---

## Prompt Assembly (Runtime)

`PromptLoaderService.toRetirementPrompt(...)` builds the final system prompt sent to the model by concatenating fields from the `AiPrompt` record. Soul/PromptGang/PromptEnvelope fields are **not** currently included in the assembled prompt.

The assembled prompt uses XML tags (snake_case), following Anthropic's prompt-engineering guidance. The tags are also valid/neutral for OpenAI, Gemini, and OpenRouter-routed models.

**Order and structure:**

```
<content>                ← AiPrompt.content (no wrapper)

<result_format>
<resultFormat>
</result_format>

<example>
  <input>
<exampleInput>
  </input>
  <output>
<exampleOutput>
  </output>
</example>
```

**Rules:**
- Any section whose source field is null/blank is omitted entirely
- If both `exampleInput` and `exampleOutput` are blank, the whole `<example>` block is skipped
- If only one of `exampleInput` / `exampleOutput` is present, the `<example>` block contains only the present child
- `AiPrompt.content` is emitted bare (no wrapping tag) so legacy prompts that already contain their own structure are unaffected

**Fields NOT currently assembled into the outbound prompt:**
- `AiPrompt.jsonVariables` (variable substitution — not yet wired)
- Soul: `purpose`, `coreValues`, `guardrails`
- PromptGang: `persona`, `tone`, `promptSequence`, `prohibitions`, `decisionPrinciples`, `memoryDepth`
- PromptEnvelope: `subPersona`, `goal`, `decisionPrinciples`, `toolExclusions`, `temperature`, `maxRetries`, `timeoutSeconds`

---

