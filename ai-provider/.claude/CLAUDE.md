# AI Provider Module — shelved

## Status

**This module is not part of the build.** `settings.gradle` has
`// include 'ai-provider'` commented out. The code below exists in the tree but does not
compile or run as part of the application.

Full context, including the ViroTrade cleanup needed before reviving it, is in
`.claude/_archive/ai-processing/ai-provider-module.md` at the repo root. **Read that first.**

## Module Overview

Document intelligence and vision library: extracts text and structured data from documents
and images through a unified interface over OpenAI, Anthropic, Google Gemini, and OpenRouter.
Built on Spring AI. Java library module (`java-library`) — no main class.

## Key Locations

Source root is `src/main/java/com/seibel/cancer/aiprovider/`:

- `service/AiService.java` — core AI operations
- `orchestration/AiWorkflowService.java` — multi-step workflows
- `controller/AiController.java` — REST endpoints under `/api/ai/*`
- `config/` — configuration properties and ChatClient beans
- `tools/` — AI function calling registry
- `observability/` — metrics, audit logging, cost tracking
- `build.gradle` — module dependencies

Provider enums live in `:common`, not here:
`common/src/main/java/com/seibel/cancer/common/enums/ai/`.

The module has no `src/main/resources/` and no tests.

## ⚠️ Before changing anything here

The configuration classes still bind to the **`viro.ai`** prefix, and `OpenApiConfig` still
carries Viro branding. The docs were renamed during the project rename; the code was not.
See the debt table in the archive doc linked above.

## Module Dependencies

Depends on `:common` and the Spring AI framework. Does not depend on other business modules —
`fileloader` and `docstorage` are referenced in older docs but do not exist in this project.
