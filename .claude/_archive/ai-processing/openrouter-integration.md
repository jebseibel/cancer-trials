# OpenRouter Integration

**STATUS: IMPLEMENTED**

OpenRouter exposes an OpenAI-compatible API, so it uses `OpenAiChatModel` pointed at
OpenRouter's base URL. No new Spring AI dependency is needed — it reuses `spring-ai-openai`.

---

## What Was Built

### New Enums (common module)

**`AiProvider`** — `common/src/main/java/com/seibel/cancer/common/enums/ai/AiProvider.java`
- Values: `OPENAI`, `ANTHROPIC`, `GEMINI`, `OPENROUTER`
- Implements `InternalEnum`
- Methods: `activeValues()`, `getDefault()`, `fromString()`

**`AiModel`** — `common/src/main/java/com/seibel/cancer/common/enums/ai/AiModel.java`
- Implements `DisplayableEnum`
- Each constant carries: `provider` (AiProvider), `modelId` (the actual API string), `displayValue`
- Models: `GPT_41_MINI`, `GPT_41`, `CLAUDE_SONNET`, `CLAUDE_OPUS`, `CLAUDE_HAIKU`,
  `GEMINI_25_FLASH`, `GEMINI_25_PRO`, `GEMINI_25_FLASH_LITE`
- Methods: `forProvider(AiProvider)`, `getDefaultForProvider(AiProvider)`, `fromModelId(String)`

### Configuration Changes

**`AiConfigProperties.java`** — added `OpenRouterConfig` inner class and `openrouter` field

**`AiClientConfig.java`** — added two new beans:
- `openrouterTextChatClient` — uses `OpenAiApi` pointed at `https://openrouter.ai/api/v1`
- `openrouterVisionChatClient` — same base URL, vision model
- `selectTextClient()` and `selectVisionClient()` handle `"openrouter"` case

**`AiService.java`** — added `openrouterTextClient` and `openrouterVisionClient` fields

### New DTO and Service

**`OpenRouterModel.java`** (dto) — record with:
- `id`, `name`, `description`, `contextLength`
- `pricing` (nested record with `prompt`/`completion` cost fields)
- `supportsVision`

**`OpenRouterModelService.java`**
- Fetches OpenRouter model list at startup via `@PostConstruct`
- Caches in memory as `List<OpenRouterModel>` and `Map<id, OpenRouterModel>`
- Methods: `getModels()`, `getVisionModels()`, `findByModelId(String)`
- `findByModelId` uses fuzzy matching: strips provider prefix (e.g. `anthropic/`) and
  normalizes dots to dashes to match our model IDs like `claude-sonnet-4-6`

### New REST Endpoints (AiController)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/ai/providers` | GET | Returns `List<AiProvider>` (active providers for dropdown) |
| `/api/ai/models?provider=` | GET | Returns `List<ModelInfo>` for non-OpenRouter providers (modelId, displayValue, description, contextLength, pricing enriched from OpenRouter cache); or `List<OpenRouterModel>` for OpenRouter |
| `/api/ai/openrouter/models` | GET | Full OpenRouter model list |
| `/api/ai/openrouter/models/vision` | GET | Vision-capable OpenRouter models only |

---

## Configuration

### application.yaml

Added under `cancer.ai:`:
```yaml
openrouter:
  enabled: true
  api-key: ${OPENROUTER_API_KEY:}
  model:
    text: google/gemini-2.5-flash
    vision: google/gemini-2.5-flash
  options:
    text:
      temperature: 0.7
      max-tokens: 2048
    vision:
      temperature: 0.5
      max-tokens: 2048
```

### Environment Variable

Add to `.env`:
```
OPENROUTER_API_KEY=sk-or-v1-...
```

---

## OpenRouter Model Slugs

OpenRouter model names use the format `provider/model-name`. Examples:
- `openai/gpt-4.1-mini`
- `openai/gpt-4.1`
- `anthropic/claude-sonnet-4-5`
- `anthropic/claude-opus-4`
- `google/gemini-2.5-flash`
- `google/gemini-2.5-pro`
- `meta-llama/llama-3.3-70b-instruct`
- `mistralai/mistral-large`

Full model list: https://openrouter.ai/models

---

## Architecture Decision

### Why OpenRouter (Hybrid Approach)

OpenRouter was added as an **optional/selectable provider** alongside direct Anthropic, OpenAI, and Gemini connections — not a replacement. This is the hybrid architecture:

- **Direct providers** (Anthropic, OpenAI, Gemini) remain for production document extraction workloads
- **OpenRouter** adds access to additional models (Qwen-VL, Llama, Mistral, etc.) and enables A/B testing via a one-line model slug swap

### Does OpenRouter Handle PDFs Natively?

Yes. OpenRouter automatically uses native PDF support for providers like Gemini, Anthropic, and OpenAI. When a model does not support file input natively, OpenRouter parses the file and passes the result to the model. This is **not a blocker** for routing through OpenRouter.

### Model Comparison for PDF Vision Extraction

| Model | Price (in/out per M tokens) | Best For |
|---|---|---|
| **Gemini 2.5 Flash** | ~$0.30 / $2.50 | High-volume production extraction |
| **Gemini 2.5 Pro** | ~$1.25 / $10 | Complex/ambiguous documents |
| **Claude Sonnet 4.6** | $3 / $15 | Careful extraction where reasoning matters |
| **Qwen3-VL** | Cheaper than Claude | Non-English documents, cost-sensitive experimentation |

### What You Lose Going Through OpenRouter

- Anthropic Message Batches API (50% async discount)
- Google Gemini native File API (upload once, reference across queries)
- Feature lag: new provider features land on OpenRouter within days, not always day-one
- Extra hop in request path (OpenRouter states they don't train on user data)

### BYOK Option

OpenRouter supports Bring Your Own Key — plug existing Anthropic/Google API keys into OpenRouter for the unified SDK without changing billing relationships. First 1M BYOK requests/month free; 5% fee beyond that.

---

## Implementation Notes

- OpenRouter does not require a separate Spring AI dependency — it reuses `spring-ai-openai`
- The `model` value in `OpenAiChatOptions` must be the full OpenRouter slug (e.g. `google/gemini-2.5-flash`)
- Vision support depends on the model chosen — not all OpenRouter models support images
- OpenRouter passes through provider-native errors, so error messages will reference the
  underlying provider (OpenAI, Anthropic, etc.)
- The `OpenRouterModelService` startup cache enables the `/api/ai/models?provider=` endpoint to
  enrich our own `AiModel` enum entries with live OpenRouter metadata (description, pricing, context length)
- Fuzzy model ID matching in `findByModelId`: strips provider prefix and normalizes `.` → `-`
  so that e.g. `anthropic/claude-sonnet-4.6` matches our enum key `claude-sonnet-4-6`
