package com.seibel.cancer.service.ai;

/**
 * Thrown when an AI call cannot produce usable output.
 *
 * <p>Deliberately distinct from {@code ServiceException}: AI failure is a normal operating
 * condition, not a bug. A timeout, a rate limit, or a missing API key means "no assessment this
 * time" — it must not read as the application being broken, and the rest of the trial page must
 * still render.
 */
public class AiGenerationException extends RuntimeException {

    public AiGenerationException(String message) {
        super(message);
    }

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
