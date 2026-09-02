package com.seibel.cancer.service.ai.intake;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisIntakeSessionStoreTest {

    private final DiagnosisIntakeSessionStore store = new DiagnosisIntakeSessionStore();

    @Test
    @DisplayName("a session is reachable by its owner")
    void ownerCanReadTheirOwnSession() {
        DiagnosisIntakeSession created = store.create(1L, "patient-extid",
                new DiagnosisIntakeExtraction(), List.of("cancerType"));

        Optional<DiagnosisIntakeSession> found = store.get(created.getSessionId(), 1L);

        assertThat(found).isPresent();
        assertThat(found.get().getSessionId()).isEqualTo(created.getSessionId());
    }

    @Test
    @DisplayName("a different user's session id is unreachable, same as an unknown one")
    void nonOwnerCannotReadAnotherUsersSession() {
        DiagnosisIntakeSession created = store.create(1L, "patient-extid",
                new DiagnosisIntakeExtraction(), List.of("cancerType"));

        assertThat(store.get(created.getSessionId(), 2L)).isEmpty();
        assertThat(store.get("unknown-session-id", 1L)).isEmpty();
    }

    @Test
    @DisplayName("a session with no missing fields is created already complete")
    void sessionWithNoGapsStartsComplete() {
        DiagnosisIntakeSession created = store.create(1L, "patient-extid",
                new DiagnosisIntakeExtraction(), List.of());

        assertThat(created.getStatus()).isEqualTo(DiagnosisIntakeSession.SessionStatus.COMPLETE);
    }

    @Test
    @DisplayName("a session with missing fields is created awaiting an answer")
    void sessionWithGapsStartsAwaitingAnswer() {
        DiagnosisIntakeSession created = store.create(1L, "patient-extid",
                new DiagnosisIntakeExtraction(), List.of("cancerType"));

        assertThat(created.getStatus())
                .isEqualTo(DiagnosisIntakeSession.SessionStatus.AWAITING_ANSWER);
    }

    @Test
    @DisplayName("an expired session is unreachable and swept on the next eviction pass")
    void expiredSessionIsUnreachableAndSwept() throws Exception {
        DiagnosisIntakeSession created = store.create(1L, "patient-extid",
                new DiagnosisIntakeExtraction(), List.of());
        backdateLastActivity(created.getSessionId(), Instant.now().minus(31, ChronoUnit.MINUTES));

        assertThat(store.get(created.getSessionId(), 1L)).isEmpty();

        store.evictExpired();
        assertThat(sessionMap()).doesNotContainKey(created.getSessionId());
    }

    @Test
    @DisplayName("capacity eviction removes the oldest session first")
    void capacityEvictionRemovesOldestFirst() throws Exception {
        DiagnosisIntakeSession oldest = store.create(1L, "patient-extid",
                new DiagnosisIntakeExtraction(), List.of());
        backdateLastActivity(oldest.getSessionId(), Instant.now().minus(20, ChronoUnit.MINUTES));

        fillToCapacity();

        // One more create should evict the oldest (backdated) session rather than any other.
        DiagnosisIntakeSession newest = store.create(1L, "patient-extid",
                new DiagnosisIntakeExtraction(), List.of());

        assertThat(sessionMap()).doesNotContainKey(oldest.getSessionId());
        assertThat(sessionMap()).containsKey(newest.getSessionId());
    }

    @SuppressWarnings("unchecked")
    private Map<String, DiagnosisIntakeSession> sessionMap() throws Exception {
        Field field = DiagnosisIntakeSessionStore.class.getDeclaredField("sessions");
        field.setAccessible(true);
        return (Map<String, DiagnosisIntakeSession>) field.get(store);
    }

    private void backdateLastActivity(String sessionId, Instant when) throws Exception {
        DiagnosisIntakeSession session = sessionMap().get(sessionId);
        session.setLastActivityAt(when);
    }

    private void fillToCapacity() {
        // MAX_SESSIONS is 200; one already exists (the backdated "oldest"), so 199 more reaches
        // capacity and the 201st create() triggers eviction.
        for (int i = 0; i < 199; i++) {
            store.create(1L, "patient-extid", new DiagnosisIntakeExtraction(), List.of());
        }
    }
}
