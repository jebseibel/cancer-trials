package com.viro.app.aiprovider.controller;

import com.viro.app.aiprovider.dto.ChatRequest;
import com.viro.app.aiprovider.dto.ChatResponse;
import com.viro.app.aiprovider.dto.DocumentAnalysisResponse;
import com.viro.app.aiprovider.dto.OpenRouterModel;
import com.viro.app.aiprovider.observability.AiAuditLogger;
import com.viro.common.enums.ai.AiModel;
import com.viro.common.enums.ai.AiProvider;
import com.viro.app.aiprovider.observability.AiMetricsService;
import com.viro.app.aiprovider.observability.CostCalculator;
import com.viro.app.aiprovider.service.AiService;
import com.viro.app.aiprovider.service.OpenRouterModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI REST API - Industry Standard Pattern
 *
 * Exposes AI operations via REST endpoints for n8n and other integrations.
 * Follows RESTful conventions and includes OpenAPI documentation.
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Operations", description = "AI-powered text, vision, and analysis operations")
@Validated
public class AiController {

    private final AiService aiService;
    private final AiAuditLogger auditLogger;
    private final AiMetricsService metricsService;
    private final CostCalculator costCalculator;
    private final OpenRouterModelService openRouterModelService;

    public AiController(
            AiService aiService,
            AiAuditLogger auditLogger,
            AiMetricsService metricsService,
            CostCalculator costCalculator,
            OpenRouterModelService openRouterModelService) {
        this.aiService = aiService;
        this.auditLogger = auditLogger;
        this.metricsService = metricsService;
        this.costCalculator = costCalculator;
        this.openRouterModelService = openRouterModelService;
    }

    // ============================================================
    // TEXT CHAT OPERATIONS
    // ============================================================

    @PostMapping("/chat")
    @Operation(summary = "Text chat", description = "Send a text prompt to the AI and get a response")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(schema = @Schema(implementation = ChatResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String provider = request.getProvider() != null ? request.getProvider() : "openai";
        String userId = request.getUserId() != null ? request.getUserId() : "anonymous";

        // Audit logging
        var auditEvent = new AiAuditLogger.AiAuditEvent(userId, "chat", provider, "auto");
        auditLogger.logRequest(auditEvent);

        // Metrics
        metricsService.recordRequest("chat", provider);

        long startTime = System.currentTimeMillis();

        try {
            String content;
            if (request.getSystemMessage() != null && !request.getSystemMessage().isBlank()) {
                content = aiService.chatWithContext(
                    request.getSystemMessage(),
                    request.getPrompt(),
                    provider
                );
            } else {
                content = aiService.chat(request.getPrompt(), provider);
            }

            long processingTime = System.currentTimeMillis() - startTime;

            // Estimate tokens and cost
            int inputTokens = costCalculator.estimateTokens(request.getPrompt());
            int outputTokens = costCalculator.estimateTokens(content);
            double cost = costCalculator.calculateCost(provider, "auto", inputTokens, outputTokens);

            // Record metrics
            metricsService.recordDuration("chat", provider, processingTime);
            metricsService.recordSuccess("chat", provider);
            metricsService.recordTokenUsage(provider, inputTokens, outputTokens);
            metricsService.recordCost(provider, cost);

            // Audit log response
            var metrics = AiAuditLogger.AiResponseMetrics.success(processingTime, inputTokens, outputTokens, cost);
            auditLogger.logResponse(auditEvent, metrics);

            ChatResponse response = ChatResponse.builder()
                .content(content)
                .provider(provider)
                .model("auto")
                .processingTimeMs(processingTime)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.recordDuration("chat", provider, processingTime);
            metricsService.recordError("chat", provider, e.getClass().getSimpleName());
            auditLogger.logError(auditEvent, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/chat")
    @Operation(summary = "Simple text chat (GET)", description = "Quick chat with query parameter")
    public ResponseEntity<ChatResponse> chatGet(
            @RequestParam String prompt,
            @RequestParam(required = false, defaultValue = "openai") String provider) {

        log.info("GET /api/ai/chat - prompt: {}, provider: {}", prompt, provider);

        long startTime = System.currentTimeMillis();
        String content = aiService.chat(prompt, provider);
        long processingTime = System.currentTimeMillis() - startTime;

        ChatResponse response = ChatResponse.builder()
            .content(content)
            .provider(provider)
            .model("auto")
            .processingTimeMs(processingTime)
            .build();

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // DOCUMENT ANALYSIS OPERATIONS
    // ============================================================

    @PostMapping(value = "/analyze-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze document", description = "Analyze a document (image, PDF, or text file)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful analysis",
            content = @Content(schema = @Schema(implementation = DocumentAnalysisResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or request"),
        @ApiResponse(responseCode = "500", description = "Processing error")
    })
    public ResponseEntity<DocumentAnalysisResponse> analyzeDocument(
            @RequestParam("file") @Parameter(description = "Document file to analyze") MultipartFile file,
            @RequestParam(name = "instruction", required = false, defaultValue = "Analyze this document and extract key information") String instruction,
            @RequestParam(name = "provider", required = false, defaultValue = "anthropic") String provider,
            @RequestParam(name = "model", required = false) String model) {

        log.info("POST /api/ai/analyze-document - file: {}, provider: {}, model: {}",
            file.getOriginalFilename(), provider, model);

        long startTime = System.currentTimeMillis();
        String extractedText = aiService.analyzeDocument(file, instruction, provider, model);
        long processingTime = System.currentTimeMillis() - startTime;

        DocumentAnalysisResponse response = DocumentAnalysisResponse.builder()
            .extractedText(extractedText)
            .summary("Analysis complete")
            .provider(provider)
            .model(model != null ? model : "auto")
            .processingTimeMs(processingTime)
            .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze image", description = "Analyze an image and describe its contents")
    public ResponseEntity<ChatResponse> analyzeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "Describe this image in detail") String prompt,
            @RequestParam(required = false, defaultValue = "anthropic") String provider) {

        log.info("POST /api/ai/analyze-image - file: {}, provider: {}",
            file.getOriginalFilename(), provider);

        long startTime = System.currentTimeMillis();
        String content = aiService.analyzeImage(file, prompt, provider);
        long processingTime = System.currentTimeMillis() - startTime;

        ChatResponse response = ChatResponse.builder()
            .content(content)
            .provider(provider)
            .model("auto")
            .processingTimeMs(processingTime)
            .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze-image-url")
    @Operation(summary = "Analyze image from URL", description = "Analyze an image from a URL")
    public ResponseEntity<ChatResponse> analyzeImageUrl(
            @RequestParam String imageUrl,
            @RequestParam(required = false, defaultValue = "Describe this image") String prompt,
            @RequestParam(required = false, defaultValue = "anthropic") String provider) {

        log.info("POST /api/ai/analyze-image-url - url: {}, provider: {}", imageUrl, provider);

        long startTime = System.currentTimeMillis();
        String content = aiService.analyzeImageUrl(imageUrl, prompt, provider);
        long processingTime = System.currentTimeMillis() - startTime;

        ChatResponse response = ChatResponse.builder()
            .content(content)
            .provider(provider)
            .model("auto")
            .processingTimeMs(processingTime)
            .build();

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // OPENROUTER MODEL DISCOVERY
    // ============================================================

    @GetMapping("/openrouter/models")
    @Operation(summary = "List OpenRouter models", description = "Get list of all available models from OpenRouter")
    public ResponseEntity<List<OpenRouterModel>> getOpenRouterModels() {
        log.info("GET /api/ai/openrouter/models");
        return ResponseEntity.ok(openRouterModelService.getModels());
    }

    @GetMapping("/openrouter/models/vision")
    @Operation(summary = "List OpenRouter vision models", description = "Get list of vision-capable models from OpenRouter")
    public ResponseEntity<List<OpenRouterModel>> getOpenRouterVisionModels() {
        log.info("GET /api/ai/openrouter/models/vision");
        return ResponseEntity.ok(openRouterModelService.getVisionModels());
    }

    // ============================================================
    // PROVIDER & MODEL DISCOVERY
    // ============================================================

    @GetMapping("/providers")
    @Operation(summary = "List AI providers", description = "Get list of all active AI providers")
    public ResponseEntity<List<AiProvider>> getProviders() {
        log.info("GET /api/ai/providers");
        return ResponseEntity.ok(AiProvider.activeValues());
    }

    @GetMapping("/models")
    @Operation(summary = "List models for provider", description = "Get models for a specific provider. Use 'openrouter' to get live OpenRouter models.")
    public ResponseEntity<?> getModelsForProvider(
            @RequestParam("provider") String provider) {
        log.info("GET /api/ai/models - provider: {}", provider);

        if ("openrouter".equalsIgnoreCase(provider)) {
            return ResponseEntity.ok(openRouterModelService.getModels());
        }

        AiProvider aiProvider = AiProvider.fromString(provider);
        var models = AiModel.forProvider(aiProvider).stream()
                .map(m -> {
                    var or = openRouterModelService.findByModelId(m.getModelId());
                    return new ModelInfo(
                            m.getModelId(),
                            m.getDisplayValue(),
                            or.map(OpenRouterModel::description).orElse(null),
                            or.map(OpenRouterModel::contextLength).orElse(0),
                            or.map(OpenRouterModel::pricing).orElse(null));
                })
                .toList();
        return ResponseEntity.ok(models);
    }

    // ============================================================
    // TOOL/FUNCTION DISCOVERY
    // ============================================================

    @GetMapping("/tools")
    @Operation(summary = "List available tools", description = "Get list of all available AI tools/functions")
    public ResponseEntity<List<String>> listTools() {
        log.info("GET /api/ai/tools");
        List<String> tools = aiService.getAvailableTools();
        return ResponseEntity.ok(tools);
    }

    // ============================================================
    // HEALTH CHECK
    // ============================================================

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if AI service is operational")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("ok", "AI service is operational"));
    }

    // ============================================================
    // DATA STRUCTURES
    // ============================================================

    public record HealthResponse(String status, String message) {}
    public record ModelInfo(String modelId, String displayValue, String description, int contextLength, OpenRouterModel.Pricing pricing) {}
}
