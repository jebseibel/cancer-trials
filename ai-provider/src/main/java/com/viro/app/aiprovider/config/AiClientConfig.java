package com.viro.app.aiprovider.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiConfigProperties.class)
public class AiClientConfig {

    private final AiConfigProperties config;

    public AiClientConfig(AiConfigProperties config) {
        this.config = config;
    }

    // ============================================================
    // OPENAI CHAT CLIENTS
    // ============================================================

    @Bean("openaiTextChatClient")
    public ChatClient openaiTextChatClient(OpenAiChatModel openAiChatModel) {
        var textOptions = config.getOpenai().getOptions().getText();

        return ChatClient.builder(openAiChatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.getOpenai().getModel().getText())
                        .temperature(toDouble(textOptions.getTemperature()))
                        .maxTokens(textOptions.getMaxTokens())
                        .topP(toDouble(textOptions.getTopP()))
                        .frequencyPenalty(toDouble(textOptions.getFrequencyPenalty()))
                        .presencePenalty(toDouble(textOptions.getPresencePenalty()))
                        .build())
                .build();
    }

    @Bean("openaiVisionChatClient")
    public ChatClient openaiVisionChatClient(OpenAiChatModel openAiChatModel) {
        var visionOptions = config.getOpenai().getOptions().getVision();

        return ChatClient.builder(openAiChatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.getOpenai().getModel().getVision())
                        .temperature(toDouble(visionOptions.getTemperature()))
                        .maxTokens(visionOptions.getMaxTokens())
                        .topP(toDouble(visionOptions.getTopP()))
                        .build())
                .build();
    }

    // ============================================================
    // ANTHROPIC CHAT CLIENTS
    // ============================================================

    @Bean("anthropicTextChatClient")
    public ChatClient anthropicTextChatClient(AnthropicChatModel anthropicChatModel) {
        var textOptions = config.getAnthropic().getOptions().getText();

        return ChatClient.builder(anthropicChatModel)
                .defaultOptions(AnthropicChatOptions.builder()
                        .model(config.getAnthropic().getModel().getText())
                        .temperature(toDouble(textOptions.getTemperature()))
                        .maxTokens(textOptions.getMaxTokens())
                        .build())
                .build();
    }

    @Bean("anthropicVisionChatClient")
    public ChatClient anthropicVisionChatClient(AnthropicChatModel anthropicChatModel) {
        var visionOptions = config.getAnthropic().getOptions().getVision();

        return ChatClient.builder(anthropicChatModel)
                .defaultOptions(AnthropicChatOptions.builder()
                        .model(config.getAnthropic().getModel().getVision())
                        .temperature(toDouble(visionOptions.getTemperature()))
                        .maxTokens(visionOptions.getMaxTokens())
                        .build())
                .build();
    }

    @Bean("anthropicOpusChatClient")
    public ChatClient anthropicOpusChatClient(AnthropicChatModel anthropicChatModel) {
        var textOptions = config.getAnthropic().getOptions().getText();
        var modelId = config.getAnthropic().getModel().getClaudeOpus();

        log.info("✅ Anthropic Opus ChatClient configured with model: {}", modelId);

        return ChatClient.builder(anthropicChatModel)
                .defaultOptions(AnthropicChatOptions.builder()
                        .model(modelId)
                        .temperature(toDouble(textOptions.getTemperature()))
                        .maxTokens(textOptions.getMaxTokens())
                        .build())
                .build();
    }

    @Bean("anthropicSonnetChatClient")
    public ChatClient anthropicSonnetChatClient(AnthropicChatModel anthropicChatModel) {
        var textOptions = config.getAnthropic().getOptions().getText();
        var modelId = config.getAnthropic().getModel().getClaudeSonnet();

        log.info("✅ Anthropic Sonnet ChatClient configured with model: {}", modelId);

        return ChatClient.builder(anthropicChatModel)
                .defaultOptions(AnthropicChatOptions.builder()
                        .model(modelId)
                        .temperature(toDouble(textOptions.getTemperature()))
                        .maxTokens(textOptions.getMaxTokens())
                        .build())
                .build();
    }

    @Bean("anthropicHaikuChatClient")
    public ChatClient anthropicHaikuChatClient(AnthropicChatModel anthropicChatModel) {
        var textOptions = config.getAnthropic().getOptions().getText();
        var modelId = config.getAnthropic().getModel().getClaudeHaiku();

        log.info("✅ Anthropic Haiku ChatClient configured with model: {}", modelId);

        return ChatClient.builder(anthropicChatModel)
                .defaultOptions(AnthropicChatOptions.builder()
                        .model(modelId)
                        .temperature(toDouble(textOptions.getTemperature()))
                        .maxTokens(textOptions.getMaxTokens())
                        .build())
                .build();
    }

    // ============================================================
    // OPENROUTER CHAT CLIENTS
    // ============================================================

    @Bean("openrouterTextChatClient")
    @Nullable
    public ChatClient openrouterTextChatClient() {
        if (config.getOpenrouter() == null || !config.getOpenrouter().isEnabled()) {
            log.warn("OpenRouter not configured or disabled.");
            return null;
        }

        var textOptions = config.getOpenrouter().getOptions().getText();

        var openRouterApi = OpenAiApi.builder()
                .baseUrl("https://openrouter.ai/api")
                .apiKey(config.getOpenrouter().getApiKey())
                .build();

        var model = OpenAiChatModel.builder()
                .openAiApi(openRouterApi)
                .build();

        log.info("✅ OpenRouter Text ChatClient configured with model: {}",
                config.getOpenrouter().getModel().getText());

        return ChatClient.builder(model)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.getOpenrouter().getModel().getText())
                        .temperature(toDouble(textOptions.getTemperature()))
                        .maxTokens(textOptions.getMaxTokens())
                        .build())
                .build();
    }

    @Bean("openrouterVisionChatClient")
    @Nullable
    public ChatClient openrouterVisionChatClient() {
        if (config.getOpenrouter() == null || !config.getOpenrouter().isEnabled()) {
            return null;
        }

        var visionOptions = config.getOpenrouter().getOptions().getVision();

        var openRouterApi = OpenAiApi.builder()
                .baseUrl("https://openrouter.ai/api")
                .apiKey(config.getOpenrouter().getApiKey())
                .build();

        var model = OpenAiChatModel.builder()
                .openAiApi(openRouterApi)
                .build();

        log.info("✅ OpenRouter Vision ChatClient configured with model: {}",
                config.getOpenrouter().getModel().getVision());

        return ChatClient.builder(model)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.getOpenrouter().getModel().getVision())
                        .temperature(toDouble(visionOptions.getTemperature()))
                        .maxTokens(visionOptions.getMaxTokens())
                        .build())
                .build();
    }

    // ============================================================
    // GEMINI CHAT CLIENTS
    // ============================================================

    @Bean("geminiTextChatClient")
    @Nullable
    public ChatClient geminiTextChatClient(@Nullable GoogleGenAiChatModel geminiChatModel) {
        if (geminiChatModel == null) {
            log.warn("Gemini ChatModel is null - Google GenAI not configured. " +
                    "Set GEMINI_API_KEY environment variable to enable Gemini.");
            return null;
        }

        if (config.getGemini() == null || config.getGemini().getOptions() == null) {
            log.warn("Gemini configuration is missing in application.yaml");
            return null;
        }

        var textOptions = config.getGemini().getOptions().getText();

        log.info("✅ Gemini Text ChatClient configured with model: {}",
                config.getGemini().getModel().getText());

        return ChatClient.builder(geminiChatModel)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(config.getGemini().getModel().getText())
                        .temperature(toDouble(textOptions.getTemperature()))
                        .maxOutputTokens(textOptions.getMaxOutputTokens())
                        .topP(toDouble(textOptions.getTopP()))
                        .topK(textOptions.getTopK())
                        .build())
                .build();
    }

    @Bean("geminiVisionChatClient")
    @Nullable
    public ChatClient geminiVisionChatClient(@Nullable GoogleGenAiChatModel geminiChatModel) {
        if (geminiChatModel == null) {
            log.warn("Gemini ChatModel is null - Google GenAI not configured. " +
                    "Set GEMINI_API_KEY environment variable to enable Gemini.");
            return null;
        }

        if (config.getGemini() == null || config.getGemini().getOptions() == null) {
            log.warn("Gemini configuration is missing in application.yaml");
            return null;
        }

        var visionOptions = config.getGemini().getOptions().getVision();

        log.info("✅ Gemini Vision ChatClient configured with model: {}",
                config.getGemini().getModel().getVision());

        return ChatClient.builder(geminiChatModel)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(config.getGemini().getModel().getVision())
                        .temperature(toDouble(visionOptions.getTemperature()))
                        .maxOutputTokens(visionOptions.getMaxOutputTokens())
                        .topP(toDouble(visionOptions.getTopP()))
                        .topK(visionOptions.getTopK())
                        .build())
                .build();
    }

    // ============================================================
    // 🧠 DEFAULT PROVIDER ALIASES
    // ============================================================

    @Bean("textChatClient")
    public ChatClient textChatClient(
            @Qualifier("openaiTextChatClient") ChatClient openaiClient,
            @Qualifier("anthropicTextChatClient") ChatClient anthropicClient,
            @Nullable @Qualifier("geminiTextChatClient") ChatClient geminiClient,
            @Nullable @Qualifier("openrouterTextChatClient") ChatClient openrouterClient) {

        return switch (config.getDefaultProvider().toLowerCase()) {
            case "anthropic" -> anthropicClient;
            case "gemini" -> geminiClient != null ? geminiClient : openaiClient;
            case "openrouter" -> openrouterClient != null ? openrouterClient : openaiClient;
            default -> openaiClient;
        };
    }

    @Bean("visionChatClient")
    public ChatClient visionChatClient(
            @Qualifier("openaiVisionChatClient") ChatClient openaiClient,
            @Qualifier("anthropicVisionChatClient") ChatClient anthropicClient,
            @Nullable @Qualifier("geminiVisionChatClient") ChatClient geminiClient,
            @Nullable @Qualifier("openrouterVisionChatClient") ChatClient openrouterClient) {

        return switch (config.getDefaultProvider().toLowerCase()) {
            case "anthropic" -> anthropicClient;
            case "gemini" -> geminiClient != null ? geminiClient : openaiClient;
            case "openrouter" -> openrouterClient != null ? openrouterClient : openaiClient;
            default -> openaiClient;
        };
    }

    // ============================================================
    // 🔧 UTILITY
    // ============================================================

    private Double toDouble(java.math.BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}