package com.seibel.cancer.service.ai.intake;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A bounded, expiring, in-memory registry of diagnosis-intake sessions.
 *
 * <p>Deliberately a plain {@link ConcurrentHashMap} rather than a cache library - neither
 * Caffeine nor Spring Cache is a dependency anywhere in this project, and the scale here (a
 * personal/family-use application, not yet in production) does not call for adding one.
 *
 * <p>Every lookup is owner-checked: a session id for a different user's session is treated
 * identically to an unknown one, the same "don't confirm existence to a non-owner" discipline
 * {@code CurrentUserService} applies to patient access.
 */
@Slf4j
@Component
public class DiagnosisIntakeSessionStore {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int MAX_SESSIONS = 200;

    private final Map<String, DiagnosisIntakeSession> sessions = new ConcurrentHashMap<>();

    public DiagnosisIntakeSession create(Long userId, String patientExtid,
                                         DiagnosisIntakeExtraction draft,
                                         List<String> missingRequiredFields) {
        Instant now = Instant.now();
        DiagnosisIntakeSession session = DiagnosisIntakeSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(userId)
                .patientExtid(patientExtid)
                .draft(draft)
                .missingRequiredFields(missingRequiredFields)
                .createdAt(now)
                .lastActivityAt(now)
                .turnCount(0)
                .status(missingRequiredFields.isEmpty()
                        ? DiagnosisIntakeSession.SessionStatus.COMPLETE
                        : DiagnosisIntakeSession.SessionStatus.AWAITING_ANSWER)
                .build();

        evictOldestIfOverCapacity();
        sessions.put(session.getSessionId(), session);
        return session;
    }

    public Optional<DiagnosisIntakeSession> get(String sessionId, Long userId) {
        DiagnosisIntakeSession session = sessions.get(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Optional.empty();
        }
        if (isExpired(session)) {
            sessions.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void save(DiagnosisIntakeSession session) {
        session.setLastActivityAt(Instant.now());
        sessions.put(session.getSessionId(), session);
    }

    public void remove(String sessionId, Long userId) {
        get(sessionId, userId).ifPresent(s -> sessions.remove(sessionId));
    }

    private boolean isExpired(DiagnosisIntakeSession session) {
        return Duration.between(session.getLastActivityAt(), Instant.now()).compareTo(TTL) > 0;
    }

    private void evictOldestIfOverCapacity() {
        if (sessions.size() < MAX_SESSIONS) {
            return;
        }
        sessions.values().stream()
                .min(Comparator.comparing(DiagnosisIntakeSession::getLastActivityAt))
                .ifPresent(oldest -> sessions.remove(oldest.getSessionId()));
    }

    /** Reclaims sessions abandoned by a closed tab, independent of any request. */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void evictExpired() {
        int before = sessions.size();
        sessions.values().removeIf(this::isExpired);
        int removed = before - sessions.size();
        if (removed > 0) {
            log.info("Diagnosis intake session sweep: removed {} expired session(s)", removed);
        }
    }
}
