# AI Prompt Lifecycle

> **Status in this project (verified 2026-08-08).** The lifecycle model and the `promote()`
> logic below are **real and live** — `AiLifecycle` is in `:common` and the `promote()` methods
> are implemented in `AiPromptDbService`, `AiPromptEnvelopeDbService`, and
> `AiPromptGangDbService` in `:database`. Both modules are in the build.
>
> **But there is no REST or UI layer.** The `/promote` endpoints and the promotion modal
> described below **do not exist here** — no `Ai*Controller`, no frontend page. The promote
> logic is reachable only from Java. The endpoint paths and UI flow are retained as the
> intended design if this is ever surfaced.
>
> Inherited from the ViroTrade project along with the prompt tables (changeset
> `004-ai-tables.yaml`). Nothing in this project uses it yet.

## Lifecycle States

| State | Meaning |
|---|---|
| `CREATED` | Newly authored, not yet tested or deployed |
| `UPDATED` | Edited after initial creation, not yet in prod |
| `IN_PRODUCTION` | Actively in use by the AI service |
| `RETIRED` | Gracefully replaced, no longer active |
| `DISCREDITED` | Marked bad — removed from consideration |

---

## uniqueId — The Production Slot

`uniqueId` is a user-assigned string on every entity. It defines the "slot" — the logical identity that records compete for. Only one record with a given `uniqueId` (per entity type) can be `IN_PRODUCTION` at a time.

When a record is promoted, the backend finds the other record of the same entity type with the same `uniqueId` that is currently `IN_PRODUCTION` and moves it to `RETIRED`. This happens in a single transaction.

---

## Promotion Rules (All Entities)

- Valid "from" states for promotion: `CREATED`, `UPDATED`, `RETIRED`
- `DISCREDITED` **cannot** be promoted — the backend rejects the request
- Promoting always retires the current `IN_PRODUCTION` record with the same `uniqueId`
- Parent must be `IN_PRODUCTION` before a child can be promoted (see gate table below)
- A confirmation modal is shown before any promotion is executed

---

## AiPromptGang

### Allowed Transitions

| From | To | Notes |
|---|---|---|
| `CREATED` | `UPDATED` | Any edit after initial save |
| `CREATED` / `UPDATED` / `RETIRED` | `IN_PRODUCTION` | Retires current `IN_PRODUCTION` gang with same `uniqueId` |
| `IN_PRODUCTION` | `RETIRED` | Only if no child Envelope or Prompt is `IN_PRODUCTION` |
| `IN_PRODUCTION` | `DISCREDITED` | Only if no child Envelope or Prompt is `IN_PRODUCTION` |
| `RETIRED` | `DISCREDITED` | Manual action |
| `DISCREDITED` | `UPDATED` | Rehabilitation allowed |
| Any (except `DISCREDITED`) | `DISCREDITED` | Manual action — marks gang as bad/unusable |

### Rules
- Multiple Gangs can be `IN_PRODUCTION` simultaneously across different `uniqueId` slots
- Only one Gang per `uniqueId` can be `IN_PRODUCTION` at a time
- A Gang **cannot** move to `RETIRED` or `DISCREDITED` if any of its Envelopes or any Prompts under those Envelopes are `IN_PRODUCTION` *(not yet implemented in backend)*
- `DISCREDITED` cannot be promoted

---

## AiPromptEnvelope

### Allowed Transitions

| From | To | Notes |
|---|---|---|
| `CREATED` | `UPDATED` | Any edit after initial save |
| `CREATED` / `UPDATED` / `RETIRED` | `IN_PRODUCTION` | Parent Gang must be `IN_PRODUCTION`; retires current `IN_PRODUCTION` envelope with same `uniqueId` |
| `IN_PRODUCTION` | `RETIRED` | Only if no child Prompt is `IN_PRODUCTION` |
| `IN_PRODUCTION` | `DISCREDITED` | Only if no child Prompt is `IN_PRODUCTION` |
| `RETIRED` | `DISCREDITED` | Manual action |
| `DISCREDITED` | `UPDATED` | Rehabilitation allowed |
| Any (except `DISCREDITED`) | `DISCREDITED` | Manual action |

### Rules
- Only one Envelope per `uniqueId` can be `IN_PRODUCTION` at a time
- An Envelope **cannot** move to `IN_PRODUCTION` unless its parent Gang is `IN_PRODUCTION`
- An Envelope **cannot** move to `RETIRED` or `DISCREDITED` if any of its Prompts are `IN_PRODUCTION` *(not yet implemented in backend)*
- `DISCREDITED` cannot be promoted

---

## AiPrompt

### Allowed Transitions

| From | To | Notes |
|---|---|---|
| `CREATED` | `UPDATED` | Any edit after initial save |
| `CREATED` / `UPDATED` / `RETIRED` | `IN_PRODUCTION` | Parent Envelope must be `IN_PRODUCTION`; retires current `IN_PRODUCTION` prompt with same `uniqueId` |
| `IN_PRODUCTION` | `RETIRED` | Happens automatically when another prompt with the same `uniqueId` is promoted |
| `RETIRED` | `DISCREDITED` | Manual action |
| `DISCREDITED` | `UPDATED` | Rehabilitation allowed |
| Any (except `DISCREDITED`) | `DISCREDITED` | Manual action |

### Rules
- Only one Prompt per `uniqueId` can be `IN_PRODUCTION` at a time
- Promoting a prompt automatically moves the current `IN_PRODUCTION` prompt with the same `uniqueId` to `RETIRED`
- A Prompt **cannot** move to `IN_PRODUCTION` unless its parent Envelope is `IN_PRODUCTION`
- `DISCREDITED` cannot be promoted

---

## Promotion Flow (UI) — design only, not built here

1. User clicks "Promote to Production" on a Gang, Envelope, or Prompt detail page
2. Confirmation modal: *"This will retire the current IN_PRODUCTION record and replace it. Continue?"*
3. On confirm — backend runs in a single transaction:
   - Validates the record is not `DISCREDITED`
   - Validates the parent is `IN_PRODUCTION` (Envelope/Prompt only)
   - Finds the current `IN_PRODUCTION` record with the same `uniqueId` and moves it to `RETIRED`
   - Moves the selected record to `IN_PRODUCTION`
4. Page refreshes to reflect the new lifecycle state

**Backend endpoints — NOT present in this project.** No `Ai*Controller` exists. These are the
paths to use if the layer is ever built:
- `POST /api/ai-prompt-gang/{extid}/promote`
- `POST /api/ai-prompt-envelope/{extid}/promote`
- `POST /api/ai-prompt-template/{extid}/promote`

---

## Parent-Child Promotion Gate (Summary)

| Entity | Parent Must Be `IN_PRODUCTION` Before Child Can Promote |
|---|---|
| AiPromptGang | No parent (Soul is system-managed) |
| AiPromptEnvelope | Yes — Gang must be `IN_PRODUCTION` |
| AiPrompt | Yes — Envelope must be `IN_PRODUCTION` |

---

## Retirement Blocker (Summary)

| Entity | Cannot Retire/Discredit If... |
|---|---|
| AiPromptGang | Any child Envelope or Prompt is `IN_PRODUCTION` |
| AiPromptEnvelope | Any child Prompt is `IN_PRODUCTION` |
| AiPrompt | N/A — no children |

> **Not yet implemented:** The retirement blocker checks for Gang and Envelope are defined above as intended behavior but are NOT currently enforced in the backend. The `promote()` methods only block `DISCREDITED` from promoting — they do not check child lifecycle states before allowing retirement or discrediting.
