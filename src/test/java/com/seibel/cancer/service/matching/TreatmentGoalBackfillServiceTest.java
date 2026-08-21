package com.seibel.cancer.service.matching;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.enums.TreatmentGoal;
import com.seibel.cancer.database.db.service.TrialDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the re-derive path that a pattern change depends on.
 *
 * <p>Ingestion skips trials whose payload has not changed, so this is the only way a corpus
 * already in the database picks up a new classification. The patterns moved twice during the
 * measurement that produced them, so this will run again.
 */
class TreatmentGoalBackfillServiceTest {

    private TrialDbService trialDbService;
    private TreatmentGoalBackfillService service;

    @BeforeEach
    void setUp() {
        trialDbService = mock(TrialDbService.class);
        service = new TreatmentGoalBackfillService(trialDbService);
    }

    private Trial trial(String extid, String summary, String storedGoal) {
        Trial t = new Trial();
        t.setExtid(extid);
        t.setNctId("NCT" + extid);
        t.setBriefTitle("A Study");
        t.setBriefSummary(summary);
        t.setTreatmentGoal(storedGoal);
        return t;
    }

    @Test
    @DisplayName("an unclassified trial gets its goal written")
    void fillsNullGoal() {
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of(
                trial("a", "SBRT to all sites of disease in 1-5 metastases", null)));

        var result = service.backfillAll();

        ArgumentCaptor<Trial> patch = ArgumentCaptor.forClass(Trial.class);
        verify(trialDbService).update(anyString(), patch.capture());
        assertThat(patch.getValue().getTreatmentGoal())
                .isEqualTo(TreatmentGoal.ABLATIVE.name());
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.unchanged()).isZero();
    }

    /**
     * Most runs follow a pattern edit that moves a handful of trials. Rewriting the rest would
     * touch updated_at across the whole corpus for nothing.
     */
    @Test
    @DisplayName("a trial already carrying the right value is not rewritten")
    void skipsUnchanged() {
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of(
                trial("a", "SBRT to all sites of disease", TreatmentGoal.ABLATIVE.name())));

        var result = service.backfillAll();

        verify(trialDbService, never()).update(anyString(), any());
        assertThat(result.unchanged()).isEqualTo(1);
        assertThat(result.updated()).isZero();
    }

    /**
     * The case this endpoint exists for: a pattern change means a stored value is now wrong, and
     * nothing in the data announces it.
     */
    @Test
    @DisplayName("a stale value is corrected when the classification has moved")
    void correctsStaleValue() {
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of(
                trial("a", "A study of drug X versus placebo", TreatmentGoal.ABLATIVE.name())));

        var result = service.backfillAll();

        ArgumentCaptor<Trial> patch = ArgumentCaptor.forClass(Trial.class);
        verify(trialDbService).update(anyString(), patch.capture());
        assertThat(patch.getValue().getTreatmentGoal())
                .isEqualTo(TreatmentGoal.NOT_STATED.name());
        assertThat(result.updated()).isEqualTo(1);
    }

    @Test
    @DisplayName("one failing trial does not abandon the run")
    void oneFailureDoesNotStopTheRun() {
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of(
                trial("bad", "SBRT to all sites of disease", null),
                trial("good", "oligometastatic breast cancer", null)));
        when(trialDbService.update(org.mockito.ArgumentMatchers.eq("bad"), any()))
                .thenThrow(new RuntimeException("boom"));

        var result = service.backfillAll();

        assertThat(result.trialsRead()).isEqualTo(2);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().getFirst()).contains("NCTbad");
    }

    /**
     * Criteria describe a patient's history, where "curative intent" means therapy someone
     * already received. Reading intent from there would invert the meaning, so the backfill must
     * read the same fields the classifier was measured against.
     */
    @Test
    @DisplayName("eligibility criteria are not read")
    void ignoresEligibilityCriteria() {
        Trial t = trial("a", "A study of drug X", null);
        t.setEligibilityCriteria("Prior local therapy delivered with curative intent");
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of(t));

        service.backfillAll();

        ArgumentCaptor<Trial> patch = ArgumentCaptor.forClass(Trial.class);
        verify(trialDbService).update(anyString(), patch.capture());
        assertThat(patch.getValue().getTreatmentGoal())
                .isEqualTo(TreatmentGoal.NOT_STATED.name());
    }

    @Test
    @DisplayName("an empty corpus reports nothing rather than failing")
    void emptyCorpus() {
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of());

        var result = service.backfillAll();

        assertThat(result.trialsRead()).isZero();
        assertThat(result.errors()).isEmpty();
    }
}
