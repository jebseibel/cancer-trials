package com.seibel.cancer.service.ai;

/**
 * Thrown when {@link PhiHeuristicScanner} flags a document before any AI call is made.
 *
 * <p>Deliberately not a subtype of {@link AiGenerationException}: this is a pre-AI refusal, not
 * an AI failure, and the two must stay visibly distinct. The message is fixed and generic -
 * never built from {@link PhiScanResult#reasons()} in a way that echoes matched content.
 */
public class PhiDetectedException extends RuntimeException {

    public PhiDetectedException(String message) {
        super(message);
    }
}
