package com.seibel.cancer.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The single entry point for AI calls.
 *
 * <p><b>Provider isolation.</b> This class imports Spring AI interfaces only — no Anthropic types
 * appear here or anywhere outside configuration. That is what keeps the provider a dependency
 * plus a config block rather than a rewrite: moving to a local Ollama model, which would end the
 * question of clinical text leaving the machine, is a starter and two properties. Do not import a
 * provider's own types into this package.
 *
 * <p><b>Failure is expected.</b> Timeouts, rate limits, refusals and truncation are normal for an
 * LLM rather than exceptional. Everything here fails as {@link AiGenerationException} so callers
 * have one thing to catch, and so an outage never reads as the app being broken.
 *
 * <p><b>Nothing here logs prompt text.</b> Prompts on this path carry clinical detail, so only
 * lengths are logged. The API key is read once to answer "is AI configured" and never leaves the
 * constructor.
 */
@Slf4j
@Service
public class AiService {

    private final ChatClient chatClient;
    private final String configuredModel;
    private final boolean apiKeyPresent;

    public AiService(ChatClient.Builder chatClientBuilder,
                     @Value("${spring.ai.anthropic.chat.options.model:unknown}") String configuredModel,
                     @Value("${spring.ai.anthropic.api-key:}") String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.configuredModel = configuredModel;
        this.apiKeyPresent = apiKey != null && !apiKey.isBlank();

        if (!apiKeyPresent) {
            log.warn("ANTHROPIC_API_KEY is not set - the AI trial check is disabled. "
                    + "Everything else in the application is unaffected.");
        }
    }

    /**
     * Whether AI calls can be made at all.
     *
     * <p>Lets the UI hide a button rather than offer one that always fails, and distinguishes
     * "not configured" from "failed" — different problems with different fixes.
     */
    public boolean isAvailable() {
        return apiKeyPresent;
    }

    /** The model name, recorded alongside whatever it produced so output stays traceable. */
    public String getModelName() {
        return configuredModel;
    }

    /**
     * Sends a prompt and returns the structured answer the given type describes.
     *
     * <p>Structured rather than free text on purpose: the caller needs to render specific fields
     * and, more importantly, needs the model to answer a fixed set of questions rather than
     * whatever it decides to write about.
     *
     * @param systemPrompt instructions defining the role and constraints; may be null
     * @param userPrompt   the actual request — required
     * @param responseType the shape to parse into; its field names and descriptions are sent to
     *                     the model as a schema, so they read as instructions
     * @throws AiGenerationException when AI is unconfigured, the call fails, or nothing usable
     *                               comes back
     */
    public <T> T generateStructured(String systemPrompt, String userPrompt, Class<T> responseType) {
        if (!apiKeyPresent) {
            throw new AiGenerationException(
                    "AI is not configured. Set ANTHROPIC_API_KEY in the environment to enable it.");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new AiGenerationException("Cannot send an empty prompt to the AI provider.");
        }

        // Logged before the call: a request takes seconds, and without this line a stalled one is
        // indistinguishable from one that never left. Lengths, never content.
        log.info("AI call starting: model={}, prompt chars system/user={}/{}",
                configuredModel,
                systemPrompt == null ? 0 : systemPrompt.length(),
                userPrompt.length());

        long startedAt = System.currentTimeMillis();
        try {
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                spec = spec.system(systemPrompt);
            }

            T result = spec.user(userPrompt).call().entity(responseType);
            long elapsedMs = System.currentTimeMillis() - startedAt;

            if (result == null) {
                throw new AiGenerationException("AI provider returned an empty response.");
            }
            log.info("AI call ok: model={}, {}ms", configuredModel, elapsedMs);
            return result;

        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            // The provider's message can be echoed; the key travels as a header rather than in
            // the body or URL, so it cannot appear here.
            log.error("AI call failed after {}ms: {}", elapsedMs, e.getMessage());
            throw new AiGenerationException("AI request failed: " + e.getMessage(), e);
        }
    }
}
