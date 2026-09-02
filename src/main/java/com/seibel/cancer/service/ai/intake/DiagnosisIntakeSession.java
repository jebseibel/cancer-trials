package com.seibel.cancer.service.ai.intake;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory state for one document-intake conversation.
 *
 * <p><b>Never persisted.</b> This class must never import anything from
 * {@code com.seibel.cancer.database} - that is the proof, checkable by inspection, that nothing
 * about an uploaded document, its extraction, or the conversation that resolves it touches a
 * table, a file, or a log line beyond category-level status. It lives only in
 * {@link DiagnosisIntakeSessionStore}'s map, for the life of the session.
 */
@Data
@Builder
public class DiagnosisIntakeSession {

    private String sessionId;
    private Long userId;
    private String patientExtid;
    private DiagnosisIntakeExtraction draft;
    private List<String> missingRequiredFields;

    @Builder.Default
    private List<ConversationTurn> transcript = new ArrayList<>();

    private Instant createdAt;
    private Instant lastActivityAt;
    private int turnCount;
    private SessionStatus status;

    public record ConversationTurn(String question, String userAnswer, Instant askedAt) {
    }

    public enum SessionStatus {
        ACTIVE, AWAITING_ANSWER, COMPLETE, ABANDONED, EXPIRED
    }
}
