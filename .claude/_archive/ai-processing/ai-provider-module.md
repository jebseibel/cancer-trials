# AI Provider Module

## Quick Reference

**Module Type:** Java Library (java-library)
**Internal API:** `AiService`, `AiWorkflowService` (Spring Beans)
**External API:** REST endpoints at `/api/ai/*`
**Lines of Code:** ~2,047 across 24 Java files

### 🔑 Key Architectural Decision

This module exposes **two distinct interfaces**:

| Consumer | Access Method | Use Case |
|----------|---------------|----------|
| **Main Application** | Java API (direct injection) | Internal document processing, batch jobs |
| **n8n / External Tools** | REST API only | Workflow automation, webhooks |

**Critical Rule:** External tools like n8n **MUST use REST API only**, never direct access to Java services.

## What It Is

The **ai-provider** module is a document intelligence and vision API library that extracts text and structured data
from documents and images. It provides unified access to multiple AI providers (OpenAI, Anthropic, Google Gemini, and OpenRouter) with:

- **Internal Java API** for main application (type-safe, zero network overhead)
- **External REST API** for n8n workflows (secured, rate-limited, versioned)

## Why

Following industry-standard architecture:
- **Provider Abstraction** - Switch between OpenAI, Anthropic, Google Gemini, and OpenRouter seamlessly for cost optimization
- **Spring AI Integration** - Leverages official Spring AI framework for reliability
- **Workflow Orchestration** - Multi-step AI pipelines and conversation management
- **AI Function Calling** - MCP-compatible tools for dynamic, context-aware responses
- **REST API First** - Designed for n8n workflows and external integrations
- **Observability** - Built-in metrics, logging, and cost tracking
- **Module Isolation** - No dependencies on fileloader or docstorage modules
- **Production Ready** - Rate limiting, error handling, audit logging

## Core Purpose

This module serves **two distinct consumer types**:

### Internal Use (Java API)
1. **Document → Text Extraction** - Extract and structure data from PDFs, images, invoices, receipts
2. **AI Workflow Orchestration** - Multi-step AI pipelines, conversation management, and function calling

### External Use (REST API)
3. **n8n Workflow Integration** - REST API endpoints for automation workflows

## Architectural Boundaries

**Internal Module Access:**
- Main application uses `AiService` and `AiWorkflowService` directly via dependency injection
- Other modules (fileloader, docstorage) do NOT access ai-provider
- Module isolation ensures clean separation of concerns

**External Integration (n8n):**
- **MUST use REST API only** (`/api/ai/*` endpoints)
- **NEVER** direct access to internal services or Java classes
- REST API provides security, versioning, rate limiting, and audit logging
- Acts as a stable public contract independent of internal implementation

**Why REST API for External Tools?**
- ✅ Authentication & authorization layer
- ✅ API versioning and contract stability
- ✅ Rate limiting and usage quotas
- ✅ Network boundary/security perimeter
- ✅ Can refactor internal modules without breaking external workflows
- ✅ Observability and audit logging for external requests

Note: the module should not talk to the docstorage module directly.
Note: the module should not talk to the fileloader module directly.

## Module Structure

```
ai-provider/
├── src/main/java/com/seibel/jobhunting/app/aiprovider/
│   ├── config/
│   │   ├── AiConfiguration.java            # Main configuration
│   │   ├── AiConfigProperties.java         # Configuration properties (includes OpenRouterConfig inner class)
│   │   ├── AiClientConfig.java             # ChatClient bean definitions (incl. openrouterTextChatClient, openrouterVisionChatClient)
│   │   ├── AiUiProperties.java             # UI-specific properties
│   │   └── OpenApiConfig.java              # Swagger/OpenAPI setup
│   ├── controller/
│   │   ├── AiController.java               # REST API endpoints (incl. /providers, /models, /openrouter/models)
│   │   └── GlobalExceptionHandler.java     # Error handling
│   ├── dto/
│   │   ├── ChatRequest.java                # Request DTOs
│   │   ├── ChatResponse.java               # Response DTOs
│   │   ├── DocumentAnalysisRequest.java
│   │   ├── DocumentAnalysisResponse.java
│   │   └── OpenRouterModel.java            # Record: id, name, description, contextLength, pricing, supportsVision
│   ├── service/
│   │   ├── AiService.java                  # Main AI service (openrouterTextClient, openrouterVisionClient fields)
│   │   ├── PromptLoaderService.java        # Loads AI prompts from the database via AiPromptDbService
│   │   ├── MetricsService.java             # Metrics wrapper
│   │   └── OpenRouterModelService.java     # Fetches + caches OpenRouter model catalog at startup (@PostConstruct)
│   ├── orchestration/
│   │   ├── AiWorkflowService.java          # Multi-step AI workflows
│   │   └── model/
│   │       ├── CombinedAnalysisResult.java
│   │       ├── ContentType.java
│   │       ├── ConversationResult.java
│   │       └── DocumentAnalysis.java
│   ├── tools/
│   │   ├── ToolRegistry.java               # AI function calling registry
│   │   └── BusinessTools.java              # Business-specific tools
│   ├── observability/
│   │   ├── AiMetricsService.java           # Prometheus metrics
│   │   ├── AiAuditLogger.java              # Audit logging
│   │   └── CostCalculator.java             # Cost tracking
└── build.gradle                             # Dependencies

common/src/main/java/com/seibel/jobhunting/common/enums/ai/
│   ├── AiProvider.java                     # Enum: OPENAI, ANTHROPIC, GEMINI, OPENROUTER. Implements InternalEnum.
│   │                                       #   activeValues(), getDefault(), fromString()
│   └── AiModel.java                        # Enum: GPT_41_MINI, GPT_41, CLAUDE_SONNET, CLAUDE_OPUS, CLAUDE_HAIKU,
│                                           #   GEMINI_25_FLASH, GEMINI_25_PRO, GEMINI_25_FLASH_LITE. Implements DisplayableEnum.
│                                           #   Each constant has provider (AiProvider), modelId (API string), displayValue.
│                                           #   forProvider(AiProvider), getDefaultForProvider(AiProvider), fromModelId(String)
```

## What It Does

### 1. Core AI Service

**AiService** provides document intelligence operations:

**Document Analysis:**
```java
String analyzeDocument(MultipartFile file, String instruction, String provider)
```
- Extracts text from PDFs, images, documents
- OCR capabilities
- Structured data extraction

**Vision Operations:**
```java
String analyzeImage(MultipartFile image, String prompt, String provider)
String analyzeImageUrl(String imageUrl, String prompt, String provider)
```
- Image understanding and description
- Visual content analysis
- Product image analysis

**Structured Extraction:**
```java
<T> T extractStructured(String prompt, Class<T> responseType, String provider)
<T> T extractStructuredWithContext(String systemMessage, String content,
                                   Class<T> responseType, String provider)
```
- Convert unstructured content to typed Java objects
- Extract specific fields (vendor, date, amount, etc.)
- Return strongly-typed data for database storage

**Supports Multiple Providers:**
- **OpenAI** - GPT-4.1 (vision), GPT-4.1-mini (text)
- **Anthropic** - Claude Sonnet 4.6, Claude Opus 4.6, Claude Haiku 4.5
- **Google Gemini** - gemini-2.5-flash (text and vision), gemini-2.5-pro
- **OpenRouter** - Access to 100s of models via a single OpenAI-compatible API (default: google/gemini-2.5-flash)

### 2. Orchestration Layer

**AiWorkflowService** provides high-level workflow coordination for complex AI operations:

**Multi-Step Workflows:**
```java
CombinedAnalysisResult analyzeCombined(MultipartFile file, String textPrompt,
                                       String visionPrompt, String provider)
```
- Orchestrates multiple AI calls in sequence
- Combines text and vision analysis
- Handles complex document processing pipelines

**Conversation Management:**
```java
ConversationResult continueConversation(List<Message> history, String newMessage,
                                       String provider)
```
- Maintains conversation context across multiple turns
- Manages message history and state
- Enables multi-turn document Q&A

**Document Analysis Pipeline:**
```java
DocumentAnalysis analyzeDocumentWorkflow(MultipartFile file,
                                        AnalysisConfiguration config)
```
- End-to-end document processing workflow
- Configurable analysis steps
- Structured output with metadata

**Key Features:**
- **Workflow Composition** - Chain multiple AI operations together
- **State Management** - Track progress through complex pipelines
- **Error Recovery** - Handle failures in multi-step processes
- **Result Aggregation** - Combine outputs from multiple AI calls

### 3. AI Tools & Function Calling

**ToolRegistry** enables AI models to call functions during generation:

**Built-in Tools:**
```java
@Tool(description = "Get the current date and time")
public String getCurrentTime()

@Tool(description = "Perform mathematical calculations")
public double calculator(String expression)
```

**BusinessTools** provides domain-specific capabilities:
```java
@Tool(description = "Get company information")
public CompanyInfo getCompanyInfo(String companyName)

@Tool(description = "Retrieve document metadata")
public DocumentMetadata getDocumentMetadata(String documentId)
```

**MCP Compatibility:**
- Implements Model Context Protocol (MCP) patterns
- AI models can request real-time data during generation
- Enables dynamic, context-aware responses
- Extensible tool registration system

**Use Cases:**
- AI can query current date/time when analyzing time-sensitive documents
- Perform calculations while extracting financial data
- Look up company information during invoice processing
- Retrieve related document context during analysis

### 4. REST API for n8n Integration

**AiController** exposes document intelligence via REST endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/ai/analyze-document` | POST | Analyze document (PDF, image, text) |
| `/api/ai/analyze-image` | POST | Analyze uploaded image |
| `/api/ai/analyze-image-url` | POST | Analyze image from URL |
| `/api/ai/health` | GET | Health check |
| `/api/ai/providers` | GET | Returns `List<AiProvider>` (active providers for dropdown) |
| `/api/ai/models?provider=` | GET | Returns `List<ModelInfo>` for non-OpenRouter providers, or `List<OpenRouterModel>` for OpenRouter; enriched with OpenRouter metadata (description, contextLength, pricing) |
| `/api/ai/openrouter/models` | GET | Full OpenRouter model list |
| `/api/ai/openrouter/models/vision` | GET | Vision-capable OpenRouter models only |

**OpenAPI/Swagger Documentation:**
- Available at `/swagger-ui.html`
- Full API documentation with request/response schemas
- Try-it-out functionality for testing

### 5. Observability Stack

**Built-in monitoring and cost tracking:**

**Prometheus Metrics:**
```
ai.requests.total        # Total requests by operation and provider
ai.requests.duration     # Request duration
ai.tokens.used          # Token consumption
ai.cost.total           # Cost tracking in USD
ai.errors.total         # Error rate
```

**Audit Logging:**
```json
{
  "userId": "user123",
  "operation": "analyze-document",
  "provider": "openai",
  "timestamp": "2025-11-12T10:30:00Z",
  "tokenUsage": 1500,
  "cost": 0.015,
  "durationMs": 2500,
  "status": "success"
}
```

**Cost Tracking:**
- Real-time cost calculation per request
- Daily/monthly budget tracking
- Alert thresholds
- Cost optimization insights

**Available at:**
- Prometheus metrics: `/actuator/prometheus`
- Health check: `/actuator/health`
- Metrics endpoint: `/actuator/metrics`

## Architecture

```
External Tools                 Internal Application
═══════════════                ════════════════════

┌─────────────┐                ┌─────────────────────────────────────┐
│    n8n      │                │      MAIN APPLICATION               │
│  Workflows  │                │                                     │
└──────┬──────┘                │  Coordinates between modules:       │
       │                       │  - Receives file upload             │
       │ HTTPS/REST            │  - Calls fileloader (direct)        │
       │ (ONLY)                │  - Calls ai-provider (direct)       │
       │                       │  - Calls docstorage (direct)        │
       │                       │                                     │
       │                       └────┬──────────┬──────────┬──────────┘
       │                            │          │          │
═══════▼═════════════════    NO DIRECT DEPENDENCIES BETWEEN MODULES
                                    │          │          │
┌──────────────────────┐     ┌──────▼───┐ ┌───▼──────┐ ┌▼──────────┐
│   PUBLIC REST API    │     │fileloader│ │ai-provider│ │docstorage │
│   Security Boundary  │     │          │ │           │ │           │
│                      │     │Independent│ │Independent│ │Independent│
│ - Authentication     │     └──────────┘ └─────┬─────┘ └───────────┘
│ - Rate Limiting      │                        │
│ - API Versioning     │     INTERNAL JAVA API  │  PUBLIC REST API
│ - Audit Logging      │     (Main App Only)    │  (External Tools)
└──────────┬───────────┘                        │
           │                    ┌───────────────┴────────────────┐
           │                    │                                │
           │             ┌──────▼────────┐         ┌────────────▼─────┐
           └────────────►│ AiController  │         │ AiWorkflowService│
                         │ (REST Layer)  │         │ (Orchestration)  │
                         └──────┬────────┘         └────────┬─────────┘
                                │                           │
                                └──────────┬────────────────┘
                                           │
                                  ┌────────▼────────┐
                                  │   AiService     │◄────────┐
                                  │   (Core Layer)  │         │
                                  └────────┬────────┘         │
                                           │              ┌───┴────────┐
                                  ┌────────▼────────┐     │   Tools    │
                                  │  Spring AI      │     │  Registry  │
                                  │  Framework      │     │            │
                                  │  + Function     │────►│ - Built-in │
                                  │    Calling      │     │ - Business │
                                  └────────┬────────┘     │ - Custom   │
                                           │              └────────────┘
                                  ┌────────▼────────┐
                                  │  AI Providers   │
                                  │  - OpenAI       │
                                  │  - Anthropic    │
                                  │  - Google Gemini│
                                  │  - OpenRouter   │
                                  └─────────────────┘

                      ┌────────────────────────┐
                      │  Observability Stack   │
                      ├────────────────────────┤
                      │  AiMetricsService      │◄─── Prometheus
                      │  AiAuditLogger         │◄─── Logs
                      │  CostCalculator        │◄─── Billing
                      └────────────────────────┘

KEY:
═══ Security/Network Boundary
─── Internal Communication (Java)
```

## Configuration

### application.yaml (single config file)

All AI configuration lives in the main `src/main/resources/application.yaml`. There is no separate `application-ai.yml`. Both `jobhunting.ai.*` (bound to `AiConfigProperties`) and `spring.ai.*` (Spring AI autoconfiguration) are in this one file.

### API Keys

Each provider uses a **single API key** for all operations — this is how the provider APIs work, not a simplification. The same key handles text, vision, and any other model calls.

| Provider   | Env Var               |
|------------|-----------------------|
| OpenAI     | `OPENAI_API_KEY`      |
| Anthropic  | `ANTHROPIC_API_KEY`   |
| Gemini     | `GEMINI_API_KEY`      |
| OpenRouter | `OPENROUTER_API_KEY`  |

### Text vs Vision Models

Providers can route to different models depending on the task type. OpenAI uses a cheaper model for text and a more capable model for vision. Anthropic and Gemini use the same model for both since those models natively handle vision.

| Provider   | Text Model                   | Vision Model                 |
|------------|------------------------------|------------------------------|
| OpenAI     | `gpt-4.1-mini`               | `gpt-4.1`                    |
| Anthropic  | `claude-sonnet-4-6`          | `claude-sonnet-4-6`          |
| Gemini     | `gemini-2.5-flash`           | `gemini-2.5-flash`           |
| OpenRouter | `google/gemini-2.5-flash`    | `google/gemini-2.5-flash`    |

### Provider Configuration (bound to `AiConfigProperties` via `jobhunting.ai:` prefix)

**Provider Configuration (bound to `AiConfigProperties` via `jobhunting.ai:` prefix):**
```yaml
jobhunting:
  ai:
    default-provider: openai

    openai:
      enabled: true
      api-key: ${OPENAI_API_KEY}
      model:
        text: gpt-4.1-mini
        vision: gpt-4.1
      options:
        text:
          temperature: 0.7
          max-tokens: 2048
        vision:
          temperature: 0.5
          max-tokens: 2048

    anthropic:
      enabled: true
      api-key: ${ANTHROPIC_API_KEY}
      model:
        text: claude-sonnet-4-6
        vision: claude-sonnet-4-6
      options:
        text:
          temperature: 0.7
          max-tokens: 4096
        vision:
          temperature: 0.5
          max-tokens: 4096

    gemini:
      enabled: true
      api-key: ${GEMINI_API_KEY}
      model:
        text: gemini-2.5-flash
        vision: gemini-2.5-flash
      options:
        text:
          temperature: 0.7
          max-output-tokens: 8192
          top-k: 40
          top-p: 0.95
        vision:
          temperature: 0.5
          max-output-tokens: 8192
          top-k: 40
          top-p: 0.95

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

Note: OpenRouter reuses `spring-ai-openai` (no new Gradle dependency). It points `OpenAiApi`
at `https://openrouter.ai/api/v1` with the OpenRouter API key. Model slugs use the
`provider/model-name` format (e.g. `google/gemini-2.5-flash`, `anthropic/claude-sonnet-4-6`).

**Spring AI autoconfiguration (also in `application.yaml`):**
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
    google:
      genai:
        api-key: ${GEMINI_API_KEY}
        project-id: <YOUR_GCP_PROJECT_ID>
```

### Gemini Setup Notes

**Spring AI Version:** `spring-ai-starter-model-google-genai` was introduced in Spring AI **1.1.0** — it does NOT exist in 1.0.0. Both root `build.gradle` and `ai-provider/build.gradle` must use BOM version **1.1.2** or later and must be in sync. If root is on an older version the starter will not resolve even if the submodule is correct.

**Auth method:** Plain GCP API key (Generative Language API), not Vertex AI / service account. GCP Project ID: `<YOUR_GCP_PROJECT_ID>`.

**Correct Spring AI autoconfiguration namespace** is `spring.ai.google.genai.*` — NOT `spring.ai.vertex.ai.gemini.*` (that is the old Vertex AI path, now ignored):

```yaml
spring:
  ai:
    google:
      genai:
        api-key: ${GEMINI_API_KEY:}
        project-id: <YOUR_GCP_PROJECT_ID>
```

**Bean imports in `AiClientConfig`:**
- `org.springframework.ai.google.genai.GoogleGenAiChatModel`
- `org.springframework.ai.google.genai.GoogleGenAiChatOptions`

Gemini beans take `@Nullable GoogleGenAiChatModel` — if the model is null (key not set) they return null gracefully so the app starts without Gemini configured.

**Common mistakes:**

| Mistake | Result |
|---------|--------|
| BOM 1.0.0 in either module | `Could not find spring-ai-starter-model-google-genai` at compile time |
| BOM mismatch between root and submodule | `NoClassDefFoundError: GoogleGenAiChatModel` at runtime |
| Missing `spring.ai.google.genai.*` in `application.yaml` | `Google GenAI project-id must be set!` on startup |
| Using old `spring.ai.vertex.ai.gemini.*` path | Config silently ignored |
| Missing starter in root `build.gradle` | Jar not on runtime classpath |

**Rate Limits:**
```yaml
jobhunting:
  ai:
    openai:
      rate-limit:
        requests-per-minute: 500
        tokens-per-minute: 150000
        burst-size: 50

    anthropic:
      rate-limit:
        requests-per-minute: 60
        tokens-per-minute: 100000
        burst-size: 10
```

**Cost Management:**
```yaml
jobhunting:
  ai:
    cost:
      tracking-enabled: true
      budget-alerts:
        daily-limit-usd: 100.0
        monthly-limit-usd: 3000.0
        alert-threshold-percent: 80
```

**Safety & Compliance:**
```yaml
jobhunting:
  ai:
    safety:
      content-filtering: true
      max-file-size-mb: 10
      allowed-mime-types:
        - image/png
        - image/jpeg
        - application/pdf
      timeout-seconds: 30
```

## REST API Security (External Access)

**Important:** The REST API is designed for external tools like n8n. Implement these security measures:

### Authentication & Authorization

```yaml
# Spring Security Configuration
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-auth-server.com
```

```java
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/ai/**").authenticated()
                .requestMatchers("/actuator/health").permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        return http.build();
    }
}
```

### Rate Limiting

```yaml
# Rate limiting per API key
spring:
  cloud:
    gateway:
      routes:
        - id: ai-provider-api
          uri: lb://ai-provider
          predicates:
            - Path=/api/ai/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter:
                  replenishRate: 10  # requests per second
                  burstCapacity: 20
                key-resolver: "#{@apiKeyResolver}"
```

### API Key Management

```java
// Example: API Key Authentication for n8n
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) {
        String apiKey = request.getHeader("X-API-Key");

        if (apiKey != null && isValidApiKey(apiKey)) {
            // Set authentication context
            SecurityContextHolder.getContext()
                .setAuthentication(new ApiKeyAuthentication(apiKey));
        }

        filterChain.doFilter(request, response);
    }
}
```

### Audit Logging for External Requests

All REST API calls should be logged with:
- API key / user ID
- Endpoint called
- Request size
- Response status
- IP address
- Timestamp

```java
@Component
public class ExternalApiAuditLogger {

    public void logExternalRequest(HttpServletRequest request,
                                   String userId,
                                   String operation) {
        log.info("External API Call: user={}, operation={}, ip={}, endpoint={}",
            userId, operation, request.getRemoteAddr(), request.getRequestURI());
    }
}
```

### CORS Configuration

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://your-n8n-instance.com"));
        config.setAllowedMethods(List.of("POST", "GET"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/ai/**", config);
        return source;
    }
}
```

### Best Practices for External API Access

1. ✅ **Always require authentication** - Never allow anonymous access
2. ✅ **Rate limit per API key** - Prevent abuse
3. ✅ **Log all external requests** - Audit trail and security
4. ✅ **Use HTTPS only** - No HTTP endpoints
5. ✅ **Validate all inputs** - File size, MIME types, content
6. ✅ **Return generic errors** - Don't leak internal details
7. ✅ **Set timeouts** - Prevent long-running requests
8. ✅ **Monitor usage patterns** - Detect anomalies
9. ✅ **Version your API** - `/api/v1/ai/*` for stability
10. ✅ **Document clearly** - OpenAPI/Swagger for consumers

## Dependencies

### Spring AI

```gradle
dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:1.1.2"
    }
}

dependencies {
    // AI Providers
    implementation "org.springframework.ai:spring-ai-starter-model-openai"
    implementation "org.springframework.ai:spring-ai-starter-model-anthropic"
    implementation "org.springframework.ai:spring-ai-starter-model-google-genai"
    // NOTE: OpenRouter requires NO new dependency — it reuses spring-ai-openai,
    //       pointing OpenAiApi at https://openrouter.ai/api/v1

    // Spring Framework
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Observability
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // API Documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
}
```

### What is Spring AI?

**Spring AI** is Spring's official framework for AI integration:
- Unified abstraction over multiple LLM providers
- Structured output parsing
- Retry logic and error handling
- Observability hooks
- Production-ready patterns

## Usage Examples

### 1. Main Application - Document Processing Flow

```java
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final FileLoaderService fileLoaderService;
    private final AiService aiService;
    private final DocStorageService docStorageService;

    public ProcessedDocument processUploadedDocument(MultipartFile file) {
        // Step 1: Store file (fileloader module)
        SavedDocument savedDoc = fileLoaderService.processUpload(
            new DocIncomingSubmit(...),
            file.getInputStream()
        );

        // Step 2: Extract data with AI (ai-provider module)
        String extractedData = aiService.analyzeDocument(
            file,
            "Extract vendor name, invoice number, date, and total amount",
            "anthropic"
        );

        // Step 3: Save metadata (docstorage module)
        docStorageService.saveDocumentMetadata(
            savedDoc.getId(),
            extractedData
        );

        return new ProcessedDocument(savedDoc, extractedData);
    }
}
```

**Key Point:** The main app orchestrates. Modules don't talk to each other.

### 2. Invoice Data Extraction

```java
@Service
@RequiredArgsConstructor
public class InvoiceProcessor {

    private final AiService aiService;

    public InvoiceData extractInvoiceData(MultipartFile invoice) {
        // Define the structure we want
        String prompt = """
            Extract invoice information from this document.
            Return JSON with fields: vendor, invoiceNumber, date,
            lineItems (array), subtotal, tax, total.
            """;

        // AI extracts and returns typed data
        return aiService.extractStructured(
            prompt + " Document: " + invoice.getOriginalFilename(),
            InvoiceData.class,
            "openai"
        );
    }

    public record InvoiceData(
        String vendor,
        String invoiceNumber,
        LocalDate date,
        List<LineItem> lineItems,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total
    ) {}

    public record LineItem(
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal total
    ) {}
}
```

### 3. OCR and Text Extraction

```java
@Service
@RequiredArgsConstructor
public class OcrService {

    private final AiService aiService;

    public String extractTextFromImage(MultipartFile image) {
        return aiService.analyzeImage(
            image,
            "Extract all text from this image. Maintain formatting and structure.",
            "anthropic"  // Claude excels at OCR
        );
    }

    public String extractTextFromPdf(MultipartFile pdf) {
        return aiService.analyzeDocument(
            pdf,
            "Extract all text content from this PDF document.",
            "anthropic"
        );
    }
}
```

### 4. Product Image Analysis

```java
@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final AiService aiService;

    public ProductDescription analyzeProductImage(String imageUrl) {
        String description = aiService.analyzeImageUrl(
            imageUrl,
            """
            Describe this product image for an e-commerce catalog.
            Include: product type, color, key features, condition,
            and suggested keywords for search.
            """,
            "openai"
        );

        return new ProductDescription(imageUrl, description);
    }
}
```

### 5. Multi-Step Workflow Orchestration

```java
@Service
@RequiredArgsConstructor
public class ComplexDocumentProcessor {

    private final AiWorkflowService workflowService;

    public CombinedAnalysisResult analyzeComplexDocument(MultipartFile document) {
        // Orchestrate multiple AI operations in sequence
        return workflowService.analyzeCombined(
            document,
            "Extract all text and identify key entities",
            "Analyze any charts, graphs, or visual elements",
            "anthropic"
        );
    }

    public ConversationResult interactiveDocumentQA(
        MultipartFile document,
        List<String> questions
    ) {
        List<Message> history = new ArrayList<>();

        // Start with document context
        history.add(new Message("system",
            "You are analyzing a document. Answer questions about its content."));

        ConversationResult result = null;
        for (String question : questions) {
            result = workflowService.continueConversation(
                history,
                question,
                "openai"
            );
            history = result.getUpdatedHistory();
        }

        return result;
    }
}
```

### 6. AI Function Calling with Tools

```java
@Service
@RequiredArgsConstructor
public class SmartInvoiceProcessor {

    private final AiService aiService;
    private final ToolRegistry toolRegistry;

    public InvoiceAnalysis processInvoiceWithTools(MultipartFile invoice) {
        // AI can call registered tools during analysis
        String prompt = """
            Analyze this invoice. Use available tools to:
            1. Get current date to check if invoice is overdue
            2. Calculate totals and verify amounts
            3. Look up company information for the vendor
            4. Retrieve any related document metadata

            Provide a complete analysis with payment status.
            """;

        // AI will automatically call getCurrentTime(), calculator(),
        // getCompanyInfo(), and getDocumentMetadata() as needed
        return aiService.extractStructuredWithTools(
            prompt,
            InvoiceAnalysis.class,
            toolRegistry.getRegisteredTools(),
            "anthropic"
        );
    }
}

public record InvoiceAnalysis(
    String vendor,
    CompanyInfo vendorDetails,
    LocalDate invoiceDate,
    LocalDate dueDate,
    boolean isOverdue,
    BigDecimal calculatedTotal,
    String paymentStatus,
    List<String> relatedDocuments
) {}
```

### 7. Custom Business Tools

```java
@Service
public class CustomBusinessTools {

    @Tool(description = "Check customer credit status")
    public CreditStatus checkCustomerCredit(String customerId) {
        // AI can call this during invoice processing
        return creditService.getCreditStatus(customerId);
    }

    @Tool(description = "Get historical pricing for a product")
    public PriceHistory getProductPricing(String productId, int months) {
        // AI can verify invoice pricing against historical data
        return pricingService.getHistory(productId, months);
    }

    @Tool(description = "Validate tax calculation for a region")
    public TaxValidation validateTax(String region, BigDecimal amount) {
        // AI can verify tax calculations on invoices
        return taxService.validate(region, amount);
    }
}
```

**Registering Custom Tools:**
```java
@Configuration
public class ToolConfiguration {

    @Bean
    public ToolRegistry toolRegistry(CustomBusinessTools customTools) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(customTools);
        return registry;
    }
}
```

### 8. n8n Workflow Integration

**n8n HTTP Request Node - Invoice Processing:**

```json
{
  "method": "POST",
  "url": "https://your-app.com/api/ai/analyze-document",
  "authentication": "headerAuth",
  "headerParameters": {
    "parameters": [
      {
        "name": "Authorization",
        "value": "Bearer {{$credentials.apiToken}}"
      }
    ]
  },
  "sendBody": true,
  "bodyParameters": {
    "parameters": [
      {
        "name": "file",
        "value": "={{$binary.data}}"
      },
      {
        "name": "instruction",
        "value": "Extract vendor, date, invoice number, and total amount"
      },
      {
        "name": "provider",
        "value": "anthropic"
      }
    ]
  }
}
```

**n8n Workflow Example - Automated Invoice Processing:**

```
1. Watch Folder (Trigger)
   ↓
2. HTTP Request → /api/ai/analyze-document
   ↓
3. Extract Data (JSON Parse)
   ↓
4. PostgreSQL (Insert invoice data)
   ↓
5. Send Email (Notification)
```

### 9. cURL Examples for Testing

```bash
# Analyze document (multipart form data)
curl -X POST http://localhost:8080/api/ai/analyze-document \
  -H "Content-Type: multipart/form-data" \
  -F "file=@invoice.pdf" \
  -F "instruction=Extract vendor name and total amount" \
  -F "provider=anthropic"

# Analyze image from URL
curl -X POST http://localhost:8080/api/ai/analyze-image-url \
  -H "Content-Type: application/json" \
  -d '{
    "imageUrl": "https://example.com/product.jpg",
    "prompt": "Describe this product image",
    "provider": "openai"
  }'

# Health check
curl http://localhost:8080/api/ai/health
```

## Key Features

✅ **Document Intelligence** - Extract text and data from PDFs, images, documents
✅ **Vision AI** - Image analysis and understanding
✅ **Structured Extraction** - Convert unstructured content to typed data
✅ **Multi-Provider Support** - OpenAI, Anthropic, Google Gemini, and OpenRouter with easy switching
✅ **Workflow Orchestration** - Multi-step AI pipelines and conversation management
✅ **AI Function Calling** - MCP-compatible tool registry for dynamic capabilities
✅ **REST API First** - Designed for n8n and external integrations
✅ **Module Isolation** - No dependencies on other application modules
✅ **OpenAPI Docs** - Swagger UI for API exploration
✅ **Observability** - Prometheus metrics, audit logs, cost tracking
✅ **Rate Limiting** - Configurable per-provider limits
✅ **Error Handling** - Comprehensive error handling and retries
✅ **Cost Management** - Budget tracking and alerts
✅ **Safety Controls** - Content filtering, file size limits
✅ **Production Ready** - Comprehensive monitoring and logging

## Cost Management

**Real-time cost tracking:**

| Provider | Model | Input Token Cost | Output Token Cost |
|----------|-------|-----------------|-------------------|
| OpenAI | gpt-4.1-mini | $0.15 / 1M | $0.60 / 1M |
| OpenAI | gpt-4.1 | $2.50 / 1M | $10.00 / 1M |
| Anthropic | claude-sonnet-4-6 | $3.00 / 1M | $15.00 / 1M |
| Anthropic | claude-haiku-4-5 | $0.80 / 1M | $4.00 / 1M |
| Google | gemini-2.5-flash | $0.50 / 1M | $3.00 / 1M |

**Budget Management:**
- Daily limit: $100
- Monthly limit: $3,000
- Alert at 80% threshold
- Automatic cost tracking per request

**Cost Optimization Tips:**
- Use GPT-4.1-mini for simple text extraction
- Use Claude Haiku 4.5 for cost-effective document analysis
- Use GPT-4.1 or Claude Sonnet 4.6 for complex vision tasks
- Use Gemini 2.5 Flash for cost-effective large context processing
- Monitor token usage via Prometheus metrics

## Common Patterns

### Pattern 1: Smart Provider Selection

```java
public String extractText(MultipartFile file) {
    // Use cheaper model for simple OCR
    if (isSimpleDocument(file)) {
        return aiService.analyzeDocument(file, "Extract all text", "openai");
    }
    // Use more capable model for complex documents
    return aiService.analyzeDocument(file, "Extract all text", "anthropic");
}
```

### Pattern 2: Structured Data Extraction

```java
public ReceiptData extractReceiptData(MultipartFile receipt) {
    String instruction = """
        Extract receipt information.
        Return JSON with: merchantName, date, items[], subtotal, tax, total.
        """;

    return aiService.extractStructured(
        instruction,
        ReceiptData.class,
        "openai"
    );
}
```

### Pattern 3: Error Handling

```java
public String extractWithFallback(MultipartFile file, String instruction) {
    try {
        // Try primary provider
        return aiService.analyzeDocument(file, instruction, "anthropic");
    } catch (RateLimitException e) {
        log.warn("Anthropic rate limited, falling back to OpenAI");
        return aiService.analyzeDocument(file, instruction, "openai");
    }
}
```

## Best Practices

1. **Choose the right provider** - Anthropic for documents, OpenAI for cost optimization
2. **Set appropriate temperature** - Lower (0.3) for extraction, higher (0.7) for descriptions
3. **Monitor costs** - Track token usage and set budget alerts
4. **Audit all requests** - Log for compliance and debugging
5. **Handle errors gracefully** - Implement retries and fallback providers
6. **Cache when possible** - Cache results for frequently processed documents
7. **Set max tokens** - Prevent runaway costs
8. **Use structured extraction** - Get typed data instead of parsing text
9. **Test with Swagger UI** - Use built-in API documentation for testing
10. **Monitor metrics** - Use Prometheus for cost and performance tracking
11. **Use orchestration for complex workflows** - Chain multiple operations with AiWorkflowService
12. **Register custom tools** - Extend AI capabilities with domain-specific functions
13. **Leverage function calling** - Let AI access real-time data during analysis

## Testing

**Note:** Test files are not currently implemented for this module.

**Recommended test coverage:**
- Unit tests for AiService, AiWorkflowService, ToolRegistry
- Integration tests with mocked AI providers
- Contract tests for REST API endpoints
- End-to-end tests with real providers (use test API keys)

```bash
# Run all tests (when implemented)
./gradlew :ai-provider:test

# Test with real API calls (requires API keys)
export OPENAI_API_KEY="sk-..."
export ANTHROPIC_API_KEY="sk-ant-..."
./gradlew :ai-provider:test

# Build module
./gradlew :ai-provider:build
```

## Module Type

**java-library** - Reusable library module
- Provides AI services to main app
- No main class (not a standalone application)
- Components registered via `@Configuration`, `@Service`
- Consumed by main application, not other modules

## Integration with Other Modules

### Module Isolation Architecture

**This module:**
- Does NOT depend on fileloader
- Does NOT depend on docstorage
- Does NOT depend on any business modules
- Only depends on `:common` for shared DTOs if needed

### Internal Consumers (Java API)

**Main Application Only:**
- Uses `AiService` and `AiWorkflowService` via `@Autowired` dependency injection
- Direct method calls within same JVM
- Type-safe Java interfaces
- No network overhead

```java
@Service
class DocumentProcessor {
    @Autowired
    private AiService aiService; // ✅ Internal use - OK
}
```

**Other modules (fileloader, docstorage):**
- ❌ MUST NOT inject or call ai-provider services
- Module isolation ensures clean boundaries

### External Consumers (REST API)

**n8n and External Tools:**
- ✅ MUST use REST API (`/api/ai/*`) ONLY
- ❌ NEVER direct access to Java classes/services
- Authentication via API keys/tokens
- Rate limited and monitored

```bash
# ✅ Correct: n8n uses REST API
curl -X POST https://api.yourapp.com/api/ai/analyze-document \
  -H "Authorization: Bearer $API_KEY" \
  -F "file=@invoice.pdf"
```

### What the Module Exposes

**For Internal Use (Main App):**
- `AiService` - Core AI operations (Spring Bean)
- `AiWorkflowService` - Orchestration (Spring Bean)
- Java interfaces with type safety

**For External Use (n8n, webhooks, etc):**
- `POST /api/ai/analyze-document` - Document analysis
- `POST /api/ai/analyze-image` - Image analysis
- `POST /api/ai/analyze-image-url` - URL-based image analysis
- `GET /api/ai/health` - Health check
- `GET /api/ai/providers` - Active AI providers list (for dropdowns)
- `GET /api/ai/models?provider=` - Models for a given provider
- `GET /api/ai/openrouter/models` - Full OpenRouter model catalog
- `GET /api/ai/openrouter/models/vision` - Vision-capable OpenRouter models
- OpenAPI/Swagger documentation at `/swagger-ui.html`

## Use Cases

### Use Case 1: Invoice Processing System (Internal)

**Flow in Main Application:**
```
1. User uploads invoice PDF
   ↓
2. Main App calls FileLoader.store() → returns file ID (internal Java call)
   ↓
3. Main App calls AiService.analyzeDocument() → extract data (internal Java call)
   ↓
4. Main App calls DocStorage.save() → store metadata (internal Java call)
   ↓
5. Return success to user

✅ All communication via Java method calls within JVM
✅ Type-safe, no network overhead
✅ Main app orchestrates - modules don't talk to each other
```

### Use Case 2: n8n Document Automation (External)

**n8n Workflow via REST API:**
```
1. Email trigger (new invoice received)
   ↓
2. Download attachment
   ↓
3. HTTP POST → /api/ai/analyze-document (REST API with auth)
   ↓
4. Parse JSON response
   ↓
5. HTTP POST → /api/documents (your document API)
   ↓
6. Send notification

✅ n8n uses ONLY REST API endpoints
✅ Authentication, rate limiting, audit logging
✅ Network boundary enforced
❌ n8n NEVER calls internal Java services
```

**Security Boundary:**
```
n8n (External) ─ HTTPS/REST ─► AiController ─► AiService (Internal)
                    │
                    └─► Authentication
                    └─► Rate Limiting
                    └─► Audit Logging
```

### Use Case 3: Product Catalog Image Processing (Internal)

**Flow in Main Application:**
```
1. Batch job finds new product images (internal service)
   ↓
2. For each image URL:
   - Call AiService.analyzeImageUrl() → extract description (internal Java call)
   - Extract keywords from response
   ↓
3. Update product database (internal repository)
   ↓
4. Generate SEO metadata

✅ Internal batch job uses Java API directly
✅ No REST overhead for internal operations
```

## Observability Stack

### Prometheus Metrics

```
# Request metrics
ai_requests_total{operation="analyze-document",provider="anthropic"} 1523
ai_requests_duration_seconds{operation="analyze-document",provider="anthropic"} 2.5

# Token usage
ai_tokens_used_total{provider="openai",model="gpt-4.1-mini"} 45230
ai_tokens_used_total{provider="anthropic",model="claude-3.5-sonnet"} 123450

# Cost tracking
ai_cost_usd_total{provider="openai"} 12.45
ai_cost_usd_total{provider="anthropic"} 38.67

# Error rate
ai_errors_total{operation="analyze-document",provider="openai",error_type="rate_limit"} 3
```

### Grafana Dashboard Queries

Create dashboards to visualize:
- Request volume by operation and provider
- Average response time
- Token consumption trends
- Daily/monthly cost burn rate
- Error rates by provider
- Cost per operation

## Future Enhancements

### Potential Additions
- **Comprehensive Test Suite** - Unit, integration, and end-to-end tests
- **Batch Processing** - Process multiple documents in parallel
- **Embeddings** - Vector search for document similarity
- **Document Classification** - Auto-categorize documents by type
- **Multi-language Support** - OCR in multiple languages
- **Table Extraction** - Extract tables from PDFs
- **Form Field Detection** - Identify and extract form fields
- **Quality Scores** - Confidence scores for extractions
- **Workflow Templates** - Pre-built orchestration patterns for common use cases
- **Advanced Tool Support** - Database access, API calls, file system operations
- **Streaming Responses** - Real-time streaming for long-running operations

### Additional Providers
- **AWS Bedrock** - Add Amazon models
- **Azure OpenAI** - Enterprise OpenAI deployment

### Advanced Orchestration
- **Workflow Persistence** - Save and resume long-running workflows
- **Conditional Branching** - Dynamic workflow paths based on AI analysis
- **Parallel Execution** - Run multiple AI operations concurrently
- **Error Recovery Strategies** - Automatic retry and fallback patterns

## Build Status

✅ **BUILD SUCCESSFUL** - All classes compile
✅ **Module integrated** - Added to settings.gradle
✅ **Production ready** - Used in main application

## Files Overview

**Total: 23 Java files + 1 config (~2,047 lines of code)**

### Core Services (2 files)
- `AiService.java` - Main AI operations (321 lines)
- `MetricsService.java` - Metrics wrapper with fallback handling (54 lines)

### REST API (2 files)
- `AiController.java` - HTTP endpoints (260 lines)
- `GlobalExceptionHandler.java` - Error handling (74 lines)

### Configuration (5 files)
- `AiConfiguration.java` - Main config (9 lines)
- `AiConfigProperties.java` - Properties binding (162 lines)
- `AiClientConfig.java` - ChatClient beans (121 lines)
- `AiUiProperties.java` - UI-specific properties (65 lines)
- `OpenApiConfig.java` - Swagger setup (60 lines)

### Orchestration (5 files)
- `AiWorkflowService.java` - Multi-step AI workflows (180 lines)
- `CombinedAnalysisResult.java` - Workflow result model (5 lines)
- `ContentType.java` - Content type enum (6 lines)
- `ConversationResult.java` - Conversation result model (5 lines)
- `DocumentAnalysis.java` - Document analysis model (6 lines)

### Tools & Function Calling (2 files)
- `ToolRegistry.java` - AI function calling registry (101 lines)
- `BusinessTools.java` - Business-specific tools (139 lines)

### Observability (3 files)
- `AiMetricsService.java` - Prometheus metrics (177 lines)
- `AiAuditLogger.java` - Audit logging (104 lines)
- `CostCalculator.java` - Cost tracking (98 lines)

### DTOs (4 files)
- `ChatRequest.java` - Chat request DTO (24 lines)
- `ChatResponse.java` - Chat response DTO (25 lines)
- `DocumentAnalysisRequest.java` - Document request DTO (25 lines)
- `DocumentAnalysisResponse.java` - Document response DTO (26 lines)

## Why This Module Exists

The ai-provider module provides a **production-ready document intelligence API** that:

1. **Extracts data from documents** - PDFs, images, invoices, receipts
2. **Orchestrates complex workflows** - Multi-step AI pipelines with conversation management
3. **Enables AI function calling** - MCP-compatible tools for dynamic capabilities
4. **Provides REST API** - For n8n workflows and external integrations
5. **Abstracts AI providers** - Switch between OpenAI/Anthropic for cost optimization
6. **Tracks costs** - Monitor and control AI spending
7. **Ensures isolation** - Independent module, main app orchestrates
8. **Production ready** - Metrics, logging, error handling, rate limits

**Instead of:**
```java
// Direct AI API calls scattered throughout app
OpenAiApi api = new OpenAiApi(apiKey);
VisionRequest request = new VisionRequest(...);
// No metrics, no cost tracking, no error handling, no orchestration...
```

**We have:**
```java
// Simple API with full observability and provider abstraction
InvoiceData data = aiService.extractStructured(prompt, InvoiceData.class, "openai");

// Complex workflows with orchestration
CombinedAnalysisResult result = workflowService.analyzeCombined(
    file, textPrompt, visionPrompt, "anthropic"
);

// AI with real-time function calling
InvoiceAnalysis analysis = aiService.extractStructuredWithTools(
    prompt, InvoiceAnalysis.class, toolRegistry.getRegisteredTools(), "openai"
);
```

That's the power of Spring AI + clean module design + advanced orchestration!
