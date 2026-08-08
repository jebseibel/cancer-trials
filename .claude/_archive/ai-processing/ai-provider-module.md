# AI Provider Module — shelved

**Status: SHELVED. Not built, not running, not on the classpath.**

`settings.gradle` has `// include 'ai-provider'` commented out, so this module is not part of
the Gradle build. The code is still in the tree (25 Java files under `ai-provider/`) and the
packages were renamed to `com.seibel.cancer.aiprovider`, but nothing compiles or runs it.

This file exists so the code is not a mystery if it gets revived. It is deliberately short.
The code is the source of truth; anything below that disagrees with the code is wrong.

## Why it is shelved

Carried over from the ViroTrade project. `RAG_PLAN.md` §1 records the decision to build a
fresh `:rag` module rather than revive this one, because reviving means cleanup before any
RAG work can start. That reasoning still holds.

Note the RAG work does **not** need this module: embeddings are local ONNX, and generation
(`RAG_PLAN.md` §9) is deferred with the chat provider undecided. If generation lands and
wants a multi-provider abstraction, this is the code to look at first.

## What it does

Document intelligence: extract text and structured data from PDFs and images via a unified
interface over OpenAI, Anthropic, Google Gemini, and OpenRouter. Built on Spring AI.

Entry points, if revived:

- `service/AiService.java` — core operations (`analyzeDocument`, `analyzeImage`, `extractStructured`)
- `orchestration/AiWorkflowService.java` — multi-step pipelines, conversation history
- `controller/AiController.java` — REST endpoints under `/api/ai/*`
- `config/AiClientConfig.java` — the per-provider `ChatClient` beans
- `tools/ToolRegistry.java` — function calling
- `observability/` — metrics, audit logging, cost tracking

Provider enums live in `:common`, not in this module:
`common/src/main/java/com/seibel/cancer/common/enums/ai/` — `AiProvider`, `AiModel`,
`AiLifecycle`. These **do** compile today, since `:common` is an active module.

## ⚠️ Known ViroTrade debt — fix before reviving

The docs for this module were find-replaced during the project rename. **The code was not.**
Anyone trusting a `cancer.ai.*` config example would set config that is silently ignored.

| Where | Current state | Needs to become |
| --- | --- | --- |
| `config/AiConfigProperties.java` | `@ConfigurationProperties(prefix = "viro.ai")` | `cancer.ai` |
| `config/AiUiProperties.java` | `@ConfigurationProperties(prefix = "viro.ai")` | `cancer.ai` |
| `config/OpenApiConfig.java` | "Viro AI Provider API", "Viro Support", `support@viro.com`, `https://api.viro.com` | this project's naming |
| `service/AiService.java` | javadoc references prompt key `WREGIS_TRANS_CONFIRM` | a real key, or drop the example |
| `ai-provider/.claude/CLAUDE.md` | says "the Viro project", points at `com/viro/app/aiprovider/` | fixed (see that file) |

Also note: the module has **no `src/main/resources/`** and **no tests**.

Revival checklist: fix the table above → uncomment `include 'ai-provider'` in
`settings.gradle` → confirm the Spring AI BOM version matches root (`build.gradle` pins
`springAiVersion` centrally so `:rag` and a revived `:ai-provider` cannot drift) → add the
provider API keys to `.env`.

## Configuration shape

Config binds to the `viro.ai` prefix today (see debt table). Per provider: `enabled`,
`api-key`, `model.text`, `model.vision`, and an `options` block for temperature/token limits.

API keys come from the environment: `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, `GEMINI_API_KEY`,
`OPENROUTER_API_KEY`. Never commit them.

Two Spring AI gotchas worth keeping, both cost real debugging time:

- The Gemini starter is `spring-ai-starter-model-google-genai`, introduced in Spring AI
  **1.1.0** — it does not exist in 1.0.0.
- The autoconfiguration namespace is `spring.ai.google.genai.*`. The older
  `spring.ai.vertex.ai.gemini.*` path is **silently ignored**, which reads as "config not
  working" rather than as an error.

## OpenRouter

Kept because the rationale is not recorded anywhere else.

**No new dependency.** OpenRouter exposes an OpenAI-compatible API, so it reuses
`spring-ai-openai` with `OpenAiApi` pointed at `https://openrouter.ai/api/v1`. Model slugs use
`provider/model-name` (e.g. `google/gemini-2.5-flash`).

**Why it was added as an option, not a replacement:** direct providers stay for production
workloads; OpenRouter adds access to models like Qwen-VL, Llama, and Mistral, and makes A/B
testing a one-line slug swap.

**What you give up going through it:** Anthropic's Message Batches API (50% async discount),
Gemini's native File API (upload once, reference many times), a few days' lag on new provider
features, and an extra network hop.

**PDFs work.** OpenRouter uses native PDF support where the provider has it and parses the
file itself where it does not.

`service/OpenRouterModelService.java` fetches and caches the model catalog at startup
(`@PostConstruct`), which is what lets `/api/ai/models?provider=` enrich the local `AiModel`
enum with live descriptions, pricing, and context lengths. Its `findByModelId` matches fuzzily
— strips the provider prefix and normalizes `.` to `-` — so `anthropic/claude-sonnet-4.6`
resolves to the enum key `claude-sonnet-4-6`.
