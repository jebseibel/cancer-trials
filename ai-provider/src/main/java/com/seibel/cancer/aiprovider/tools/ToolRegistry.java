package com.seibel.cancer.aiprovider.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * Tool Registry - Industry Standard Pattern
 *
 * Registers functions/tools that AI models can call using Spring AI's native function calling.
 * Compatible with both OpenAI function calling and Anthropic tool use.
 *
 * This follows the MCP (Model Context Protocol) principles of exposing tools to AI models.
 */
@Slf4j
@Configuration
public class ToolRegistry {

    // ============================================================
    // UTILITY TOOLS
    // ============================================================

    /**
     * Get current date and time
     */
    @Bean
    @Description("Get the current date and time")
    public Function<CurrentTimeRequest, CurrentTimeResponse> getCurrentTime() {
        return request -> {
            log.info("🔧 Tool called: getCurrentTime (timezone: {})", request.timezone());

            LocalDateTime now = LocalDateTime.now();
            String formatted = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            return new CurrentTimeResponse(
                formatted,
                now.toString(),
                request.timezone() != null ? request.timezone() : "UTC"
            );
        };
    }

    /**
     * Calculate simple math operations
     */
    @Bean
    @Description("Calculate simple math operations (add, subtract, multiply, divide)")
    public Function<CalculatorRequest, CalculatorResponse> calculator() {
        return request -> {
            log.info("🔧 Tool called: calculator ({} {} {})",
                request.operandA(), request.operation(), request.operandB());

            double result = switch (request.operation().toLowerCase()) {
                case "add", "+" -> request.operandA() + request.operandB();
                case "subtract", "-" -> request.operandA() - request.operandB();
                case "multiply", "*" -> request.operandA() * request.operandB();
                case "divide", "/" -> {
                    if (request.operandB() == 0) {
                        throw new IllegalArgumentException("Cannot divide by zero");
                    }
                    yield request.operandA() / request.operandB();
                }
                default -> throw new IllegalArgumentException("Unknown operation: " + request.operation());
            };

            return new CalculatorResponse(result,
                String.format("%s %s %s = %s",
                    request.operandA(), request.operation(), request.operandB(), result));
        };
    }

    // ============================================================
    // DATA STRUCTURES FOR TOOLS
    // ============================================================

    public record CurrentTimeRequest(String timezone) {}

    public record CurrentTimeResponse(
        String formattedTime,
        String isoTime,
        String timezone
    ) {}

    public record CalculatorRequest(
        double operandA,
        String operation,
        double operandB
    ) {}

    public record CalculatorResponse(
        double result,
        String explanation
    ) {}
}
