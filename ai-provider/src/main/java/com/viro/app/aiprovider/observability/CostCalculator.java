package com.viro.app.aiprovider.observability;

import org.springframework.stereotype.Component;

/**
 * Cost Calculator - Industry Standard Pattern
 *
 * Calculates estimated costs for AI operations based on token usage.
 * Pricing as of 2024 - should be externalized to config in production.
 */
@Component
public class CostCalculator {

    // ============================================================
    // PRICING (per 1M tokens)
    // ============================================================

    // OpenAI GPT-4.1-mini
    private static final double OPENAI_GPT4O_MINI_INPUT_PRICE = 0.150;   // $0.150 per 1M input tokens
    private static final double OPENAI_GPT4O_MINI_OUTPUT_PRICE = 0.600;  // $0.600 per 1M output tokens

    // OpenAI GPT-4.1
    private static final double OPENAI_GPT4O_INPUT_PRICE = 2.50;         // $2.50 per 1M input tokens
    private static final double OPENAI_GPT4O_OUTPUT_PRICE = 10.00;       // $10.00 per 1M output tokens

    // Anthropic Claude Sonnet 4.6
    private static final double ANTHROPIC_SONNET_INPUT_PRICE = 3.00;     // $3.00 per 1M input tokens
    private static final double ANTHROPIC_SONNET_OUTPUT_PRICE = 15.00;   // $15.00 per 1M output tokens

    // Google Gemini 2.5 Flash
    private static final double GEMINI_FLASH_INPUT_PRICE = 0.50;   // $0.50 per 1M input tokens
    private static final double GEMINI_FLASH_OUTPUT_PRICE = 3.00;  // $3.00 per 1M output tokens

    // ============================================================
    // COST CALCULATION
    // ============================================================

    /**
     * Calculate cost for OpenAI operation
     */
    public double calculateOpenAiCost(String model, int inputTokens, int outputTokens) {
        double inputCost;
        double outputCost;

        if (model != null && (model.contains("gpt-4o-mini") || model.contains("gpt-4.1-mini"))) {
            inputCost = (inputTokens / 1_000_000.0) * OPENAI_GPT4O_MINI_INPUT_PRICE;
            outputCost = (outputTokens / 1_000_000.0) * OPENAI_GPT4O_MINI_OUTPUT_PRICE;
        } else {
            // Default to GPT-4o pricing
            inputCost = (inputTokens / 1_000_000.0) * OPENAI_GPT4O_INPUT_PRICE;
            outputCost = (outputTokens / 1_000_000.0) * OPENAI_GPT4O_OUTPUT_PRICE;
        }

        return inputCost + outputCost;
    }

    /**
     * Calculate cost for Anthropic operation
     */
    public double calculateAnthropicCost(String model, int inputTokens, int outputTokens) {
        // All Claude 4.x models use same pricing
        double inputCost = (inputTokens / 1_000_000.0) * ANTHROPIC_SONNET_INPUT_PRICE;
        double outputCost = (outputTokens / 1_000_000.0) * ANTHROPIC_SONNET_OUTPUT_PRICE;
        return inputCost + outputCost;
    }

    /**
     * Calculate cost for Gemini operation
     */
    public double calculateGeminiCost(String model, int inputTokens, int outputTokens) {
        // Gemini 2.5 Flash pricing
        double inputCost = (inputTokens / 1_000_000.0) * GEMINI_FLASH_INPUT_PRICE;
        double outputCost = (outputTokens / 1_000_000.0) * GEMINI_FLASH_OUTPUT_PRICE;
        return inputCost + outputCost;
    }

    /**
     * Calculate cost for any provider
     */
    public double calculateCost(String provider, String model, int inputTokens, int outputTokens) {
        if (provider == null || provider.isBlank()) {
            return 0.0;
        }

        return switch (provider.toLowerCase()) {
            case "openai", "gpt" -> calculateOpenAiCost(model, inputTokens, outputTokens);
            case "anthropic", "claude" -> calculateAnthropicCost(model, inputTokens, outputTokens);
            case "gemini", "vertex", "google" -> calculateGeminiCost(model, inputTokens, outputTokens);
            default -> 0.0;
        };
    }

    /**
     * Estimate tokens from text (rough approximation: 1 token ≈ 4 characters)
     */
    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.length() / 4;
    }

    /**
     * Format cost as currency string
     */
    public String formatCost(double cost) {
        if (cost < 0.01) {
            return String.format("$%.4f", cost);
        } else {
            return String.format("$%.2f", cost);
        }
    }
}
