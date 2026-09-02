# Wiring an AI Provider into a Spring Boot Project

A portable recipe for adding an LLM to an existing Spring Boot application using Spring AI.
Written to be reusable on any project, not just this one. Where a concrete example helps,
this project is used as the worked example and labelled as such.

No code in this document, per project convention — it describes decisions, order of work,
and traps. The code is small; the decisions are what cost time.

> **Status in this project, 2026-08-14: not wired.** `:ai-provider` exists on disk with real
> source in it (OpenRouter client, cost calculation, tool registry, audit logging) but is
> **commented out of `settings.gradle`** — "shelved until AI keys/config are ready" — so it does
> not compile as part of the build and nothing in the running app calls it. The `AiPrompt` tables
> described below *are* in the build, via `:common` and `:database`, and are unused.
>
> The advice here is provider-agnostic and does not go stale with the code. Where it says "this
> project", that is a worked example, not a description of something running.

---

## 1. The mental model

The intuition most people arrive with is correct:

> A service takes a string of text, sends it to the AI provider, gets a result back.

That is exactly what Spring AI's `ChatClient` is. You hand it a prompt, you call it, you
get content back. Everything else in this document is about the parts that intuition
leaves out — and there are only three that matter.

**One — the result usually shouldn't be a String.** Returning raw text pushes parsing onto
every caller, and LLM text output is not a stable format to parse. Spring AI can bind the
response directly to a typed object, validating against the type's schema and retrying the
model when it doesn't conform. Any time you are extracting *fields* rather than producing
*prose*, you want the typed exit, not the String exit.

**Two — the call can fail in ways ordinary code does not.** Timeouts, rate limits, refusals,
truncation, and confidently wrong answers are all normal operating conditions, not
exceptional ones. A service that only models the happy path will be rewritten.

**Three — the provider must not leak past the service.** This is the difference between
"we use Anthropic" and "we are married to Anthropic."

So the shape is: **one narrow service, two exits (text and typed), provider-agnostic
interface, explicit failure behaviour.**

---

## 2. What Spring AI gives you for free

Spring AI is a provider abstraction. You write against its interfaces; the provider is a
dependency plus configuration, not code.

| Interface | What it does | Swap cost |
|---|---|---|
| `ChatModel` / `ChatClient` | Text in, text or typed object out | Starter + config |
| `EmbeddingModel` | Text to vector | Starter + config |
| `VectorStore` | Similarity search over vectors | Starter + config |

The rule that makes the abstraction real: **never import a provider's own classes into your
business code.** Import Spring AI's interfaces only. If a provider class name appears
anywhere outside your configuration, the swap is no longer config-only.

> **Worked example.** In this project the retrieval service imports
> `org.springframework.ai.vectorstore.VectorStore` and contains zero Qdrant classes. That
> discipline is why the vector store is genuinely swappable. Apply the same rule to
> `ChatModel` and the chat provider stays swappable too.

Providers available as starters include Anthropic, OpenAI, Azure OpenAI, Google Vertex AI,
Amazon Bedrock, Mistral, and Ollama for local models. Chat and embedding providers are
chosen **independently** — they need not be the same vendor, and one vendor lacking an
embeddings API does not constrain your chat choice.

---

## 3. Decisions to make before writing anything

These are cheap now and expensive later. Answer all five before adding the dependency.

### 3.1 Which provider, and is the data allowed to leave the building?

The governing question is not price or quality — it is **what you are sending**. A cloud
provider call means the text leaves your infrastructure and may be retained.

- Public or non-sensitive text: any cloud provider is fine.
- Regulated, personal, clinical, or confidential text: this is a real decision requiring a
  real answer, not a default. Ollama runs models locally behind the same `ChatClient`
  interface, so a local model is a config change rather than a rewrite.

Decide this explicitly and write the answer down. If a project already made a
privacy-motivated decision elsewhere — for instance choosing local embeddings specifically
so text never leaves the machine — a cloud chat provider silently reverses that decision
for everything you send it. Do not let that happen by accident.

### 3.2 Where does the service live?

In a multi-module build this is the decision that bites hardest, because Gradle rejects
dependency cycles at configuration time and the fix is a refactor.

Establish the direction of your module graph first, then place the AI module **below**
everything that calls it. If a module low in the graph needs to trigger AI work, do not add
an upward dependency — invert it with a Spring application event: the low module publishes,
the AI module listens, neither references the other, and the runtime wires them together.
Declare the event type in a module both already depend on.

> **Worked example — this project's trap, and it is a repeat offender.** The graph is
> `:common ← :database ← :datafetcher ← root`, with `:rag` also sitting on `:database`.
> `:datafetcher` cannot depend on `:rag`, so re-indexing is triggered by publishing an
> event that `:rag` listens for. The same wall was hit once before: several
> `upsertByNctId`-style methods were added to root's service layer, discovered to be
> uncallable from `:datafetcher`, and are still sitting there unused. Check the graph
> before writing the class, not after.

A useful property of the event approach: if the AI module is absent or its listener is
disabled, publishing is a no-op and the rest of the system behaves identically. That is
what makes AI failure isolatable.

In a **single-module project** none of this applies — put the service in its own package
and move on.

### 3.3 Where do prompts live?

Three options, in increasing order of effort:

1. **Hardcoded constants** — fine for one or two stable prompts. Every change is a redeploy.
2. **Config files** — externalized, still redeploy-ish, no history.
3. **Database-stored with versioning and a lifecycle** — prompts become editable data with
   an audit trail and the ability to promote and roll back.

Pick deliberately. Option 1 is the honest default for a first integration; do not build
option 3 speculatively.

> **Worked example — this project should not hardcode.** *(Verified 2026-08-14.)* It already
> has live prompt infrastructure inherited from an earlier project: an `AiPrompt` domain object
> carrying `provider`, `model`, `content`, `resultFormat`, `version`, and `lifecycle`, backed by
> `AiPromptDbService`, `AiPromptGangDbService` and `AiPromptEnvelopeDbService` (changeset `004`).
> The `promote()` logic that retires the previous production prompt in one transaction lives on
> **`AiPromptGangDbService`**, and refuses to promote a `DISCREDITED` gang. It is real, it is in
> the build via `:common` and `:database`, and nothing uses it yet. There
> is no REST or UI layer over it — promotion is reachable only from Java. Wiring the AI
> service to read its prompt from that table costs little and is the reason the table
> exists.

### 3.4 Synchronous or asynchronous?

LLM calls take seconds, not milliseconds. A synchronous call inside an HTTP request ties up
a request thread and risks client timeouts.

- One-off, user-initiated, needs an answer now: synchronous is fine, but set an explicit
  timeout.
- Bulk work over many records: do it as a background job with progress reporting. Do not
  put a thousand-record loop behind a single HTTP request.

### 3.5 What happens when it fails or is wrong?

Write the answer down before you build. Minimum: a timeout, a retry policy for transient
failures, a cap on retries, and a defined behaviour when the model returns something
unusable — fall back to a deterministic path, store the record as unprocessed, or fail
loudly. Silently storing a bad AI answer as though it were verified data is the failure
mode that costs the most to unwind later.

---

## 4. Order of work

Each step is independently verifiable. Do not skip step 3.

**Step 1 — Confirm the plumbing.**
Check whether the Spring AI BOM is already imported and whether the Spring repositories are
configured. If the project already uses Spring AI for embeddings or a vector store, this is
done and the chat provider is the only missing piece.

**Step 2 — Add the chat starter and credentials.**
Add the provider's starter to the module chosen in 3.2; let the BOM supply the version so
nothing drifts. Put the API key in the environment, never in a committed file, and
reference it from configuration the same way existing secrets are referenced.

**Step 3 — Verify one round trip before building anything on top.**
Send a trivial prompt, confirm a sensible response. This separates "credentials and
networking work" from "my prompt is wrong," which are otherwise diagnosed together and
painfully. Note that some providers or local models download artifacts on first call, so
the first call may be slow.

**Step 4 — Build the service.**
One class. Injected `ChatClient`. Two public methods: one returning text, one binding to a
typed object. Timeout, retry, and failure behaviour from 3.5 live here, not in callers. No
provider classes imported.

**Step 5 — Wire one real caller.**
Pick the single highest-value use case and wire only that. Resist building a general
"AI service layer" before one real path works end to end.

**Step 6 — Add observability.**
Log token usage, latency, and failures per call. Token spend is invisible until it appears
on a bill, and latency is the thing users actually feel. Log the prompt version too if
prompts are versioned.

**Step 7 — Evaluate, if output quality matters.**
See section 6.

---

## 5. Choosing the use case — the part most projects get wrong

The instinct is to point the LLM at whatever looks hard. The better question is: **is this
problem actually non-deterministic?**

An LLM is the right tool for genuinely ambiguous input: free text, inconsistent human
formatting, semantic judgment, summarization. It is the wrong tool for anything a parser
already handles correctly. Structured JSON with named keys does not need a model — it needs
a field accessor. Putting an LLM there is slower, costlier, and replaces exact behaviour
with probabilistic behaviour.

The high-value pattern is **AI as a fallback, not a replacement**: keep the deterministic
path as the primary, route only what it cannot handle to the model. This bounds cost, keeps
the common case exact, and shrinks the blast radius of a bad answer.

That pattern implies a measurement, and it is the one to take before writing any code:
**what fraction of input does the deterministic path actually fail on?** If it is a few
percent, an LLM dependency is being added to a rounding error. If it is a third, it is the
top priority. Measure first — the number decides whether the work is worth doing at all.

> **Worked example.** Here, trial field parsing is already handled deterministically from
> structured CT.gov JSON and should stay that way. The free-text eligibility criteria are
> the genuinely ambiguous part, and even those are handled by a chunker whose rules were
> derived from 50 real trials. The open question is its fallback rate on shapes it cannot
> structure — that number, not intuition, should decide whether AI parsing gets built.

---

## 6. Evaluation

If the AI output feeds anything that matters, build a small evaluation set **before**
tuning: a fixed set of realistic inputs with known-good expected outputs. Without it, every
prompt change is guesswork and "it seems better now" is the only available measure.

Keep it small and awkward on purpose — cover the failure shapes, not a representative
average. Capture the inputs from the real pipeline rather than hand-writing them;
hand-built fixtures tend to be tidier than production data and hide the cases that break
things.

> **Worked example — a trap worth stealing.** A hand-built fixture in this project used
> hyphen bullets and tidy headers. Neither shape occurs in real data: the real sample had
> 632 asterisk bullets and zero hyphens. A parser written against that fixture would match
> nothing in production. Capture fixtures from the real pipeline.

One caution on test-based evaluation: a test that **skips** reports as a successful build.
If an evaluation depends on a running backend, a live database, or credentials, confirm it
actually executed rather than trusting a green result.

---

## 7. Traps, collected

- **Provider classes leaking out of config.** The single thing that destroys portability.
- **Module cycles.** Check the dependency graph before choosing where the service lives.
- **A String-returning service for extraction work.** Use typed binding.
- **Hardcoded prompts in a project that already has prompt versioning.** Check first.
- **Cloud provider quietly reversing a privacy decision** made elsewhere in the system.
- **Bulk LLM work inside an HTTP request.** Make it a background job.
- **No timeout.** The default may be far longer than your users will wait.
- **Unbounded retries** on a paid API.
- **Storing unvalidated AI output as trusted data.** Mark provenance so it can be re-run.
- **Sending internal database ids into prompts or across API boundaries.** Use external
  identifiers, as the rest of the system does.
- **Tuning prompts against unvalidated retrieval**, in a RAG system. Fix retrieval first —
  otherwise you cannot tell which half is wrong.
- **A skipped evaluation test reading as a pass.**

---

## 8. Minimum viable integration

For a first wiring, this is the whole thing:

1. Decide provider and confirm the data is allowed to leave (3.1).
2. Add the starter; put the key in the environment (Step 2).
3. Verify one round trip (Step 3).
4. One service, injected `ChatClient`, text and typed exits, explicit timeout (Step 4).
5. One real caller (Step 5).
6. Log tokens, latency, failures (Step 6).

Everything else in this document — prompt versioning, evaluation sets, async jobs, fallback
routing — is earned later by a real need, not built up front.
