package com.viro.app.aiprovider.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "viro.ai")
public class AiConfigProperties {

    private String defaultProvider;
    private Map<String, String> models;  // Provider name to model ID mapping
    private AnthropicConfig anthropic;
    private OpenaiConfig openai;
    private GeminiConfig gemini;
    private OpenRouterConfig openrouter;
    private RoutingConfig routing;
    private SafetyConfig safety;
    private AuditConfig audit;
    private CostConfig cost;

    // =================================================================
    // ANTHROPIC CONFIGURATION
    // =================================================================
    @Data
    public static class AnthropicConfig {
        private boolean enabled;
        private int priority;
        private String apiKey;
        private ModelConfig model;
        private OptionsConfig options;
        private CapabilitiesConfig capabilities;
        private RateLimitConfig rateLimit;
    }

    // =================================================================
    // OPENAI CONFIGURATION
    // =================================================================
    @Data
    public static class OpenaiConfig {
        private boolean enabled;
        private int priority;
        private String apiKey;
        private ModelConfig model;
        private OptionsConfig options;
        private CapabilitiesConfig capabilities;
        private RateLimitConfig rateLimit;
    }

    // =================================================================
    // OPENROUTER CONFIGURATION
    // =================================================================
    @Data
    public static class OpenRouterConfig {
        private boolean enabled;
        private int priority;
        private String apiKey;
        private ModelConfig model;
        private OptionsConfig options;
        private CapabilitiesConfig capabilities;
        private RateLimitConfig rateLimit;
    }

    // =================================================================
    // GEMINI CONFIGURATION
    // =================================================================
    @Data
    public static class GeminiConfig {
        private boolean enabled;
        private int priority;
        private String apiKey;
        private ModelConfig model;
        private OptionsConfig options;
        private CapabilitiesConfig capabilities;
        private RateLimitConfig rateLimit;
    }

    // =================================================================
    // MODEL CONFIGURATION
    // =================================================================
    @Data
    public static class ModelConfig {
        private String text;
        private String vision;
        private String claudeSonnet;
        private String claudeOpus;
        private String claudeHaiku;
        private String gpt41;
        private String gpt41Mini;
        private String embeddings;
        private String gemini25Flash;
        private String gemini25Pro;
        private String gemini25FlashLite;
    }

    // =================================================================
    // OPTIONS CONFIGURATION (Temperature, MaxTokens, etc.)
    // =================================================================
    @Data
    public static class OptionsConfig {
        private ChatOptionsConfig text;
        private ChatOptionsConfig vision;
    }

    @Data
    public static class ChatOptionsConfig {
        private BigDecimal temperature;
        private Integer maxTokens;
        private BigDecimal topP;
        private BigDecimal frequencyPenalty;
        private BigDecimal presencePenalty;
        private Integer maxOutputTokens;  // Gemini uses maxOutputTokens instead of maxTokens
        private Integer topK;              // Gemini-specific parameter
    }

    // =================================================================
    // CAPABILITIES CONFIGURATION
    // =================================================================
    @Data
    public static class CapabilitiesConfig {
        private boolean supportsVision;
        private boolean supportsStructuredOutput;
        private int maxTokens;
        private List<String> supportedFileTypes;
        private BigDecimal costPerMillionTokens;
    }

    // =================================================================
    // RATE LIMIT CONFIGURATION
    // =================================================================
    @Data
    public static class RateLimitConfig {
        private int requestsPerMinute;
        private int tokensPerMinute;
        private int burstSize;
    }

    // =================================================================
    // ROUTING CONFIGURATION
    // =================================================================
    @Data
    public static class RoutingConfig {
        private Map<String, String> rules;
        private FallbackConfig fallback;
    }

    @Data
    public static class FallbackConfig {
        private boolean enabled;
        private int maxRetries;
        private long retryDelayMs;
        private BigDecimal backoffMultiplier;
        private boolean usePriorityOrder;
    }

    // =================================================================
    // SAFETY CONFIGURATION
    // =================================================================
    @Data
    public static class SafetyConfig {
        private boolean contentFiltering;
        private java.math.BigDecimal maxFileSizeMb; // Allow fractional MB (e.g., 0.5 for 500KB)
        private List<String> allowedMimeTypes;
        private List<String> blockedKeywords;
        private int timeoutSeconds;
    }

    // =================================================================
    // AUDIT CONFIGURATION
    // =================================================================
    @Data
    public static class AuditConfig {
        private boolean enabled;
        private boolean logRequests;
        private boolean logResponses;
        private int retentionDays;
        private boolean includeMetadata;
    }

    // =================================================================
    // COST CONFIGURATION
    // =================================================================
    @Data
    public static class CostConfig {
        private boolean trackingEnabled;
        private BudgetAlertsConfig budgetAlerts;
        private boolean preferCheaperModels;
    }

    @Data
    public static class BudgetAlertsConfig {
        private BigDecimal dailyLimitUsd;
        private BigDecimal monthlyLimitUsd;
        private int alertThresholdPercent;
    }
}