package com.seibel.cancer.aiprovider.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Audit Logger - Industry Standard Pattern
 *
 * Logs all AI operations for compliance, debugging, and cost tracking.
 * In production, this would write to a database or audit log system.
 */
@Slf4j
@Component
public class AiAuditLogger {

    /**
     * Log AI request
     */
    public void logRequest(AiAuditEvent event) {
        log.info("🔍 AI Request | user={}, operation={}, provider={}, model={}",
            event.userId(), event.operation(), event.provider(), event.model());

        if (log.isDebugEnabled()) {
            log.debug("Request details: {}", event);
        }
    }

    /**
     * Log AI response with timing and token usage
     */
    public void logResponse(AiAuditEvent event, AiResponseMetrics metrics) {
        log.info("✅ AI Response | user={}, operation={}, duration={}ms, inputTokens={}, outputTokens={}, cost=${}",
            event.userId(),
            event.operation(),
            metrics.durationMs(),
            metrics.inputTokens(),
            metrics.outputTokens(),
            metrics.estimatedCost());

        if (log.isDebugEnabled()) {
            log.debug("Response details: metrics={}", metrics);
        }
    }

    /**
     * Log error
     */
    public void logError(AiAuditEvent event, String errorMessage, Throwable exception) {
        log.error("❌ AI Error | user={}, operation={}, provider={}, error={}",
            event.userId(), event.operation(), event.provider(), errorMessage, exception);
    }

    /**
     * Log tool/function call
     */
    public void logToolCall(String userId, String toolName, String provider, Map<String, Object> parameters) {
        log.info("🔧 Tool Called | user={}, tool={}, provider={}, params={}",
            userId, toolName, provider, parameters);
    }

    // ============================================================
    // DATA STRUCTURES
    // ============================================================

    /**
     * Audit event for AI operations
     */
    public record AiAuditEvent(
        String userId,
        String sessionId,
        String operation,       // "chat", "analyze-document", etc.
        String provider,        // "openai", "anthropic"
        String model,           // "gpt-4o", "claude-3-5-sonnet"
        Instant timestamp,
        Map<String, Object> metadata
    ) {
        public AiAuditEvent(String userId, String operation, String provider, String model) {
            this(userId, null, operation, provider, model, Instant.now(), Map.of());
        }
    }

    /**
     * Response metrics for tracking performance and cost
     */
    public record AiResponseMetrics(
        long durationMs,
        int inputTokens,
        int outputTokens,
        double estimatedCost,
        boolean cached,
        String status              // "success", "error", "partial"
    ) {
        public static AiResponseMetrics success(long durationMs, int inputTokens, int outputTokens, double cost) {
            return new AiResponseMetrics(durationMs, inputTokens, outputTokens, cost, false, "success");
        }

        public static AiResponseMetrics error(long durationMs, String status) {
            return new AiResponseMetrics(durationMs, 0, 0, 0.0, false, status);
        }
    }
}
