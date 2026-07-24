# AI Provider Module

## Current Module Context
You are currently working in the **:ai-provider** module of the Viro project.

## Module Overview
The ai-provider module is a document intelligence and vision API library that extracts text and structured data from documents and images. It provides unified access to multiple AI providers (OpenAI, Anthropic, Google Gemini, and OpenRouter) with both internal Java API and external REST API.

For detailed documentation, see `.claude/ai-provider-module.md` in the root directory.

## Module Purpose
Document intelligence and AI workflow orchestration
- Java library module (java-library)
- Extracts text and structured data from documents and images
- Provides multi-step AI workflow orchestration
- AI function calling with MCP-compatible tools
- REST API for n8n and external integrations
- Internal Java API for main application
- Supports OpenAI, Anthropic, Google Gemini, and OpenRouter providers

## Key Locations
- `build.gradle` - Module dependencies and Gradle configuration
- `src/main/java/com/viro/app/aiprovider/` - Main source code
  - `service/AiService.java` - Core AI operations
  - `orchestration/AiWorkflowService.java` - Multi-step workflows
  - `controller/AiController.java` - REST API endpoints
  - `config/` - Configuration and ChatClient beans
  - `tools/` - AI function calling registry
  - `observability/` - Metrics, audit logging, cost tracking
- AI configuration lives in the main app's `src/main/resources/application.yaml` (no separate `application-ai.yml`)

## Module Dependencies
This module depends on:
- `:common` - Shared utilities (if needed)
- Spring AI framework - OpenAI and Anthropic integration
- DOES NOT depend on `:fileloader`, `:docstorage`, or other business modules

## Module Isolation
This module is isolated from other application modules:
- Main app orchestrates interactions between modules
- Other modules (fileloader, docstorage) MUST NOT access ai-provider
- Clean separation of concerns

## API Access Patterns
**Internal Use (Main App):**
- Uses `AiService` and `AiWorkflowService` via dependency injection
- Direct Java method calls (type-safe, zero network overhead)

**External Use (n8n, webhooks):**
- MUST use REST API only (`/api/ai/*` endpoints)
- NEVER direct access to Java services
- Includes authentication, rate limiting, audit logging

## Development Notes
- Uses Spring AI framework for provider abstraction
- Built-in observability: Prometheus metrics, audit logging, cost tracking
- Supports workflow orchestration for complex multi-step AI operations
- AI function calling with MCP-compatible tools (ToolRegistry)
- OpenAPI/Swagger documentation at `/swagger-ui.html`
- ~2,047 lines of code across 23 Java files

