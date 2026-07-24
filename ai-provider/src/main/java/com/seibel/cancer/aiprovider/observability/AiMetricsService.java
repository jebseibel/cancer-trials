package com.seibel.cancer.aiprovider.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics Service - Industry Standard Pattern
 *
 * Tracks AI operation metrics using Micrometer (industry standard).
 * Metrics are exposed via /actuator/metrics and can be scraped by Prometheus.
 */
@Slf4j
@Service
public class AiMetricsService {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, AtomicLong> tokenUsage;
    private final ConcurrentHashMap<String, AtomicLong> costTracking;

    public AiMetricsService(MeterRegistry registry) {
        this.registry = registry;
        this.tokenUsage = new ConcurrentHashMap<>();
        this.costTracking = new ConcurrentHashMap<>();

        log.info("🔢 AI Metrics Service initialized");
    }

    // ============================================================
    // REQUEST TRACKING
    // ============================================================

    /**
     * Record AI request
     */
    public void recordRequest(String operation, String provider) {
        Counter.builder("ai.requests.total")
            .description("Total AI requests")
            .tag("operation", operation)
            .tag("provider", provider)
            .register(registry)
            .increment();
    }

    /**
     * Record request duration
     */
    public void recordDuration(String operation, String provider, long durationMs) {
        Timer.builder("ai.requests.duration")
            .description("AI request duration in milliseconds")
            .tag("operation", operation)
            .tag("provider", provider)
            .register(registry)
            .record(java.time.Duration.ofMillis(durationMs));
    }

    /**
     * Record success
     */
    public void recordSuccess(String operation, String provider) {
        Counter.builder("ai.requests.success")
            .description("Successful AI requests")
            .tag("operation", operation)
            .tag("provider", provider)
            .register(registry)
            .increment();
    }

    /**
     * Record error
     */
    public void recordError(String operation, String provider, String errorType) {
        Counter.builder("ai.requests.error")
            .description("Failed AI requests")
            .tag("operation", operation)
            .tag("provider", provider)
            .tag("error_type", errorType)
            .register(registry)
            .increment();
    }

    // ============================================================
    // TOKEN TRACKING
    // ============================================================

    /**
     * Record token usage
     */
    public void recordTokenUsage(String provider, int inputTokens, int outputTokens) {
        // Input tokens
        Counter.builder("ai.tokens.input")
            .description("Input tokens used")
            .tag("provider", provider)
            .register(registry)
            .increment(inputTokens);

        // Output tokens
        Counter.builder("ai.tokens.output")
            .description("Output tokens generated")
            .tag("provider", provider)
            .register(registry)
            .increment(outputTokens);

        // Update local tracking
        String key = provider + ".input";
        tokenUsage.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(inputTokens);

        key = provider + ".output";
        tokenUsage.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(outputTokens);
    }

    /**
     * Get total tokens used for a provider
     */
    public long getTotalTokens(String provider) {
        long input = tokenUsage.getOrDefault(provider + ".input", new AtomicLong()).get();
        long output = tokenUsage.getOrDefault(provider + ".output", new AtomicLong()).get();
        return input + output;
    }

    // ============================================================
    // COST TRACKING
    // ============================================================

    /**
     * Record cost
     */
    public void recordCost(String provider, double cost) {
        Counter.builder("ai.cost.total")
            .description("Total AI cost in USD")
            .tag("provider", provider)
            .baseUnit("USD")
            .register(registry)
            .increment(cost);

        // Update local tracking
        costTracking.computeIfAbsent(provider, k -> new AtomicLong())
            .addAndGet((long)(cost * 10000)); // Store as 1/10000th of a cent for precision
    }

    /**
     * Get total cost for a provider (in USD)
     */
    public double getTotalCost(String provider) {
        return costTracking.getOrDefault(provider, new AtomicLong()).get() / 10000.0;
    }

    /**
     * Get total cost across all providers (in USD)
     */
    public double getTotalCostAllProviders() {
        return costTracking.values().stream()
            .mapToLong(AtomicLong::get)
            .sum() / 10000.0;
    }

    // ============================================================
    // TOOL/FUNCTION TRACKING
    // ============================================================

    /**
     * Record tool call
     */
    public void recordToolCall(String toolName, String provider) {
        Counter.builder("ai.tools.calls")
            .description("AI tool/function calls")
            .tag("tool", toolName)
            .tag("provider", provider)
            .register(registry)
            .increment();
    }
}
