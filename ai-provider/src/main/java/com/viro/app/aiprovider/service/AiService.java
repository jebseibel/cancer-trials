package com.viro.app.aiprovider.service;

import com.viro.app.aiprovider.config.AiConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Simplified AI Service - Industry Standard Pattern
 *
 * Directly uses Spring AI ChatClient without custom abstraction layers.
 * Supports multiple providers (OpenAI, Anthropic) with simple selection logic.
 * Includes function/tool calling support (MCP-compatible pattern).
 */
@Slf4j
@Service
public class AiService {

    private final ChatClient openaiTextClient;
    private final ChatClient openaiVisionClient;
    private final ChatClient anthropicTextClient;
    private final ChatClient anthropicVisionClient;
    private final ChatClient anthropicOpusClient;
    private final ChatClient anthropicSonnetClient;
    private final ChatClient anthropicHaikuClient;
    private final ChatClient geminiTextClient;
    private final ChatClient geminiVisionClient;
    private final ChatClient openrouterTextClient;
    private final ChatClient openrouterVisionClient;
    private final AiConfigProperties config;
    private final ApplicationContext context;
    private final PromptLoaderService promptLoaderService;

    public AiService(
            @Qualifier("openaiTextChatClient") ChatClient openaiTextClient,
            @Qualifier("openaiVisionChatClient") ChatClient openaiVisionClient,
            @Qualifier("anthropicTextChatClient") ChatClient anthropicTextClient,
            @Qualifier("anthropicVisionChatClient") ChatClient anthropicVisionClient,
            @Qualifier("anthropicOpusChatClient") ChatClient anthropicOpusClient,
            @Qualifier("anthropicSonnetChatClient") ChatClient anthropicSonnetClient,
            @Qualifier("anthropicHaikuChatClient") ChatClient anthropicHaikuClient,
            @Nullable @Qualifier("geminiTextChatClient") ChatClient geminiTextClient,
            @Nullable @Qualifier("geminiVisionChatClient") ChatClient geminiVisionClient,
            @Nullable @Qualifier("openrouterTextChatClient") ChatClient openrouterTextClient,
            @Nullable @Qualifier("openrouterVisionChatClient") ChatClient openrouterVisionClient,
            AiConfigProperties config,
            ApplicationContext context,
            PromptLoaderService promptLoaderService) {
        this.openaiTextClient = openaiTextClient;
        this.openaiVisionClient = openaiVisionClient;
        this.anthropicTextClient = anthropicTextClient;
        this.anthropicVisionClient = anthropicVisionClient;
        this.anthropicOpusClient = anthropicOpusClient;
        this.anthropicSonnetClient = anthropicSonnetClient;
        this.anthropicHaikuClient = anthropicHaikuClient;
        this.geminiTextClient = geminiTextClient;
        this.geminiVisionClient = geminiVisionClient;
        this.openrouterTextClient = openrouterTextClient;
        this.openrouterVisionClient = openrouterVisionClient;
        this.config = config;
        this.context = context;
        this.promptLoaderService = promptLoaderService;
    }

    // ============================================================
    // TEXT OPERATIONS
    // ============================================================

    /**
     * Simple text chat with default provider
     */
    public String chat(String prompt) {
        return chat(prompt, config.getDefaultProvider());
    }

    /**
     * Text chat with specific provider
     */
    public String chat(String prompt, String provider) {
        log.info("💬 Text chat request - provider: {}", provider);

        ChatClient client = selectTextClient(provider);

        return client.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * Text chat with system context
     */
    public String chatWithContext(String systemMessage, String userMessage, String provider) {
        log.info("💬 Text chat with context - provider: {}", provider);

        ChatClient client = selectTextClient(provider);

        return client.prompt()
                .system(systemMessage)
                .user(userMessage)
                .call()
                .content();
    }

    // ============================================================
    // VISION OPERATIONS
    // ============================================================

    /**
     * Analyze image from MultipartFile
     */
    public String analyzeImage(MultipartFile file, String prompt) {
        return analyzeImage(file, prompt, config.getDefaultProvider());
    }

    /**
     * Analyze image from MultipartFile with specific provider
     */
    public String analyzeImage(MultipartFile file, String prompt, String provider) {
        log.info("👁️ Image analysis request - provider: {}, file: {}", provider, file.getOriginalFilename());

        try {
            ChatClient client = selectVisionClient(provider);

            // Determine MIME type
            MimeType mimeType = MimeType.valueOf(
                file.getContentType() != null ? file.getContentType() : "image/jpeg"
            );

            Resource resource = file.getResource();

            return client.prompt()
                    .user(u -> u
                        .text(prompt)
                        .media(mimeType, resource))
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Failed to process image file", e);
            throw new RuntimeException("Failed to process image: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze image from URL
     */
    public String analyzeImageUrl(String imageUrl, String prompt, String provider) {
        log.info("👁️ Image URL analysis request - provider: {}", provider);

        try {
            ChatClient client = selectVisionClient(provider);
            URL url = new URL(imageUrl);

            return client.prompt()
                    .user(u -> u
                        .text(prompt)
                        .media(MimeType.valueOf("image/jpeg"), url))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Failed to process image URL", e);
            throw new RuntimeException("Failed to process image URL: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // STRUCTURED OUTPUT
    // ============================================================

    /**
     * Extract structured data as specific type
     */
    public <T> T extractStructured(String prompt, Class<T> responseType, String provider) {
        log.info("📊 Structured extraction request - provider: {}, type: {}",
                provider, responseType.getSimpleName());

        ChatClient client = selectTextClient(provider);

        return client.prompt()
                .user(prompt)
                .call()
                .entity(responseType);
    }

    /**
     * Extract structured data with context
     */
    public <T> T extractStructuredWithContext(
            String systemMessage,
            String content,
            Class<T> responseType,
            String provider) {

        return extractStructuredWithContext(systemMessage, content, responseType, provider, null);
    }

    /**
     * Extract structured data with context and optional model override
     *
     * @param systemMessage The system prompt
     * @param content The content to extract from
     * @param responseType The target response class
     * @param provider The AI provider to use
     * @param model Optional model override (if null, uses ChatClient's default)
     * @return The extracted structured data
     */
    public <T> T extractStructuredWithContext(
            String systemMessage,
            String content,
            Class<T> responseType,
            String provider,
            @Nullable String model) {

        log.info("📊 Structured extraction with context - provider: {}, model: {}", provider, model);

        ChatClient client = selectTextClient(provider);

        var promptSpec = client.prompt()
                .system(systemMessage)
                .user(content);

        if (model != null && !model.isBlank()) {
            String p = (provider != null) ? provider.toLowerCase() : "";
            if (p.startsWith("anthropic") || p.startsWith("claude")) {
                promptSpec = promptSpec.options(AnthropicChatOptions.builder().model(model).build());
            } else if (p.startsWith("gemini") || p.startsWith("vertex") || p.startsWith("google")) {
                promptSpec = promptSpec.options(GoogleGenAiChatOptions.builder().model(model).build());
            } else {
                promptSpec = promptSpec.options(OpenAiChatOptions.builder().model(model).build());
            }
        }

        return promptSpec.call().entity(responseType);
    }

    /**
     * Extract structured data using externally loaded prompt key.
     * Loads prompt from PromptLoaderService based on key.
     *
     * @param promptKey The prompt key (e.g., "WREGIS_TRANS_CONFIRM")
     * @param content The content to extract data from
     * @param responseType The target response class
     * @param provider The AI provider to use
     * @return The extracted structured data
     * @throws IllegalStateException if prompt is not loaded
     */
    public <T> T extractStructuredWithPromptKey(
            String promptKey,
            String content,
            Class<T> responseType,
            String provider) {

        log.info("📊 Structured extraction with prompt key - key: {}, provider: {}", promptKey, provider);

        String systemPrompt = promptLoaderService.getPrompt(promptKey);

        if (systemPrompt == null) {
            log.error("Prompt not loaded for key: {}. " +
                    "Check that prompts/retirement-prompts.yaml exists and is valid.", promptKey);
            throw new IllegalStateException(
                "Prompt not loaded for key: " + promptKey + ". " +
                "Check that classpath:prompts/retirement-prompts.yaml exists and contains '" + promptKey + "'"
            );
        }

        return extractStructuredWithContext(systemPrompt, content, responseType, provider);
    }

    /**
     * Extract structured data using prompt config from YAML (provider and model from YAML).
     * This is the recommended method for retirement certificate processing.
     *
     * @param promptKey The prompt key (e.g., "WREGIS_TRANS_CONFIRM")
     * @param content The content to extract data from
     * @param responseType The target response class
     * @return The extracted structured data
     * @throws IllegalStateException if prompt is not loaded
     */
    public <T> T extractStructuredWithPromptConfig(
            String promptKey,
            String content,
            Class<T> responseType) {

        log.info("📊 Structured extraction with prompt config - key: {}", promptKey);

        PromptLoaderService.RetirementPrompt config = promptLoaderService.getPromptConfig(promptKey);

        if (config == null) {
            log.error("Prompt config not found for key: {}", promptKey);
            throw new IllegalStateException("Prompt not found: " + promptKey);
        }

        // Extract provider and model from YAML config
        String provider = config.getProvider();
        String model = config.getModel();
        String systemPrompt = config.getPrompt();

        log.info("Using provider '{}' with model '{}' for {}", provider, model, promptKey);

        // Use the method that supports model override
        return extractStructuredWithContext(systemPrompt, content, responseType, provider, model);
    }

    /**
     * Get prompt text by key (for services that need to customize prompts).
     *
     * @param promptKey The prompt key
     * @return The prompt text, or null if not found
     */
    public String getPrompt(String promptKey) {
        String prompt = promptLoaderService.getPrompt(promptKey);
        if (prompt == null) {
            log.warn("Prompt not found for key: {}, service should use fallback", promptKey);
        }
        return prompt;
    }

    /**
     * Get access to PromptLoaderService for loading full prompt configs (provider, model, prompt text)
     */
    public PromptLoaderService getPromptLoader() {
        return promptLoaderService;
    }

    // ============================================================
    // DOCUMENT OPERATIONS
    // ============================================================

    /**
     * Analyze document (could be text or image-based)
     */
    public String analyzeDocument(MultipartFile file, String instruction, String provider) {
        return analyzeDocument(file, instruction, provider, null);
    }

    /**
     * Analyze document with optional model override
     *
     * @param file The document file to analyze
     * @param instruction The instruction/prompt for analysis
     * @param provider The AI provider to use
     * @param model Optional model override (if null, uses provider's default)
     * @return The analysis result
     */
    public String analyzeDocument(MultipartFile file, String instruction, String provider, @Nullable String model) {
        log.info("📄 Document analysis request - provider: {}, model: {}, file: {}",
                provider, model, file.getOriginalFilename());

        String contentType = file.getContentType();

        // Route based on content type
        if (contentType != null && contentType.startsWith("image/")) {
            return analyzeImageWithModel(file, instruction, provider, model);
        } else if (contentType != null && contentType.equals("application/pdf")) {
            return analyzeImageWithModel(file, instruction, provider, model); // PDFs also use vision
        } else {
            // For text-based documents, read and process as text
            try {
                String textContent = new String(file.getBytes());
                return chatWithContextAndModel(instruction, textContent, provider, model);
            } catch (IOException e) {
                log.error("Failed to read document", e);
                throw new RuntimeException("Failed to read document: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Analyze image from MultipartFile with model override
     */
    private String analyzeImageWithModel(MultipartFile file, String prompt, String provider, @Nullable String model) {
        log.info("👁️ Image analysis with model - provider: {}, model: {}, file: {}",
                provider, model, file.getOriginalFilename());

        try {
            ChatClient client = selectVisionClient(provider);

            // Determine MIME type
            MimeType mimeType = MimeType.valueOf(
                file.getContentType() != null ? file.getContentType() : "image/jpeg"
            );

            Resource resource = file.getResource();

            return client.prompt()
                    .user(u -> u
                        .text(prompt)
                        .media(mimeType, resource))
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Failed to process image file", e);
            throw new RuntimeException("Failed to process image: " + e.getMessage(), e);
        }
    }

    /**
     * Text chat with system context and optional model override
     */
    private String chatWithContextAndModel(String systemMessage, String userMessage, String provider, @Nullable String model) {
        log.info("💬 Text chat with context and model - provider: {}, model: {}", provider, model);

        ChatClient client = selectTextClient(provider);

        var prompt = client.prompt()
                .system(systemMessage)
                .user(userMessage);

        if (model != null && !model.isBlank()) {
            prompt = prompt.options(OpenAiChatOptions.builder().model(model).build());
        }

        return prompt.call().content();
    }

    // ============================================================
    // FUNCTION/TOOL CALLING - MCP-Compatible Pattern
    // ============================================================

    /**
     * Get list of available tool/function names
     *
     * Tools are registered as @Bean Function<RequestType, ResponseType> in the Spring context.
     * Spring AI automatically makes them available for function calling.
     */
    public List<String> getAvailableTools() {
        Map<String, Function> functionBeans = context.getBeansOfType(Function.class);
        log.info("📋 Available tools: {}", functionBeans.keySet());
        return new ArrayList<>(functionBeans.keySet());
    }

    /**
     * Note: Function calling in Spring AI 1.0.0
     *
     * Functions registered as @Bean are automatically available to ChatClient.
     * To enable function calling, configure it in ChatOptions when building the ChatClient.
     *
     * Example tools available in this application:
     * - getCurrentTime: Get current date/time
     * - calculator: Perform math operations
     * - getCompanyInfo: Look up company by ID
     * - searchCompanies: Search companies by name
     * - getDocumentInfo: Get document metadata
     *
     * The AI model will automatically call these tools when needed to answer user queries.
     */

    // ============================================================
    // STREAMING (Future Enhancement)
    // ============================================================

    /**
     * Stream chat response (for real-time UI)
     */
    public void streamChat(String prompt, String provider, StreamResponseHandler handler) {
        log.info("🌊 Streaming chat request - provider: {}", provider);

        ChatClient client = selectTextClient(provider);

        client.prompt()
                .user(prompt)
                .stream()
                .content()
                .subscribe(handler::onChunk, handler::onError, handler::onComplete);
    }

    // ============================================================
    // PROVIDER SELECTION (Simple Logic)
    // ============================================================


    private ChatClient selectTextClient(String provider) {
        if (provider == null || provider.isBlank()) {
            provider = config.getDefaultProvider();
        }

        log.debug("Selecting text client for provider: {}", provider);

        ChatClient client = switch (provider.toLowerCase()) {
            case "anthropic", "claude" -> anthropicTextClient;
            case "anthropic-opus", "claude-opus" -> anthropicOpusClient;
            case "anthropic-sonnet", "claude-sonnet" -> anthropicSonnetClient;
            case "anthropic-haiku", "claude-haiku" -> anthropicHaikuClient;
            case "openai", "gpt", "gpt4o-mini" -> openaiTextClient;
            case "gpt4o" -> openaiTextClient;  // Both use OpenAI client, model is configured in bean
            case "openrouter" -> {
                if (openrouterTextClient == null) {
                    log.error("OpenRouter text client requested but not configured. Check OPENROUTER_API_KEY.");
                    throw new IllegalStateException("OpenRouter AI provider is not configured. Set OPENROUTER_API_KEY.");
                }
                yield openrouterTextClient;
            }
            case "gemini", "vertex", "google" -> {
                if (geminiTextClient == null) {
                    log.error("Gemini text client requested but not configured. " +
                            "Check GOOGLE_APPLICATION_CREDENTIALS, GCP_PROJECT_ID, and Vertex AI API enablement.");
                    throw new IllegalStateException(
                        "Gemini AI provider is not configured. Required environment variables:\n" +
                        "- GOOGLE_APPLICATION_CREDENTIALS (path to service account JSON)\n" +
                        "- GCP_PROJECT_ID (your Google Cloud project ID)\n" +
                        "- GCP_LOCATION (optional, defaults to us-central1)\n" +
                        "Also verify Vertex AI API is enabled in your GCP project."
                    );
                }
                yield geminiTextClient;
            }
            default -> {
                log.warn("Unknown provider '{}', using default: {}", provider, config.getDefaultProvider());
                yield switch (config.getDefaultProvider().toLowerCase()) {
                    case "anthropic" -> anthropicTextClient;
                    case "anthropic-opus" -> anthropicOpusClient;
                    case "anthropic-sonnet" -> anthropicSonnetClient;
                    case "anthropic-haiku" -> anthropicHaikuClient;
                    case "gemini" -> {
                        if (geminiTextClient == null) {
                            log.error("Gemini text client requested but not configured.");
                            throw new IllegalStateException("Gemini AI provider is not configured.");
                        }
                        yield geminiTextClient;
                    }
                    default -> openaiTextClient;
                };
            }
        };
        return client;
    }

    private ChatClient selectVisionClient(String provider) {
        if (provider == null || provider.isBlank()) {
            provider = config.getDefaultProvider();
        }

        log.debug("Selecting vision client for provider: {}", provider);

        ChatClient client = switch (provider.toLowerCase()) {
            case "anthropic", "claude" -> anthropicVisionClient;
            case "anthropic-opus", "claude-opus" -> anthropicOpusClient;
            case "anthropic-sonnet", "claude-sonnet" -> anthropicSonnetClient;
            case "anthropic-haiku", "claude-haiku" -> anthropicHaikuClient;
            case "openai", "gpt", "gpt4o" -> openaiVisionClient;
            case "openrouter" -> {
                if (openrouterVisionClient == null) {
                    log.error("OpenRouter vision client requested but not configured. Check OPENROUTER_API_KEY.");
                    throw new IllegalStateException("OpenRouter AI provider (vision) is not configured. Set OPENROUTER_API_KEY.");
                }
                yield openrouterVisionClient;
            }
            case "gemini", "vertex", "google" -> {
                if (geminiVisionClient == null) {
                    log.error("Gemini vision client requested but not configured. " +
                            "Check GOOGLE_APPLICATION_CREDENTIALS, GCP_PROJECT_ID, and Vertex AI API enablement.");
                    throw new IllegalStateException(
                        "Gemini AI provider (vision) is not configured. Required environment variables:\n" +
                        "- GOOGLE_APPLICATION_CREDENTIALS (path to service account JSON)\n" +
                        "- GCP_PROJECT_ID (your Google Cloud project ID)\n" +
                        "- GCP_LOCATION (optional, defaults to us-central1)\n" +
                        "Also verify Vertex AI API is enabled in your GCP project."
                    );
                }
                yield geminiVisionClient;
            }
            default -> {
                log.warn("Unknown provider '{}', using default: {}", provider, config.getDefaultProvider());
                yield switch (config.getDefaultProvider().toLowerCase()) {
                    case "anthropic" -> anthropicVisionClient;
                    case "anthropic-opus" -> anthropicOpusClient;
                    case "anthropic-sonnet" -> anthropicSonnetClient;
                    case "anthropic-haiku" -> anthropicHaikuClient;
                    case "gemini" -> {
                        if (geminiVisionClient == null) {
                            log.error("Gemini vision client requested but not configured.");
                            throw new IllegalStateException("Gemini AI provider (vision) is not configured.");
                        }
                        yield geminiVisionClient;
                    }
                    default -> openaiVisionClient;
                };
            }
        };
        return client;
    }

    // ============================================================
    // UTILITY INTERFACES
    // ============================================================

    /**
     * Handler for streaming responses
     */
    public interface StreamResponseHandler {
        void onChunk(String chunk);
        void onError(Throwable error);
        void onComplete();
    }
}
