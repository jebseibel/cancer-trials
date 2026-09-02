package com.seibel.cancer.service.ai;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.config.MatchingProperties;
import com.seibel.cancer.database.db.service.TrialDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unlike {@code TrialClassificationBackfillServiceTest}, "already correct" is not a concept
 * here — every generation is a paid call, so the only question is whether a value exists yet.
 */
class TrialFriendlyTitleBackfillServiceTest {

    private TrialDbService trialDbService;
    private TrialFriendlyTitleService friendlyTitleService;
    private TrialFriendlyTitleBackfillService backfillService;

    @BeforeEach
    void setUp() {
        trialDbService = mock(TrialDbService.class);
        friendlyTitleService = mock(TrialFriendlyTitleService.class);
        // A real, plain-object properties instance rather than a mock - it is a POJO with no
        // behavior worth stubbing. The bar's own enabled/disabled rendering is the ticker's
        // concern, not this service's; "false" here keeps the test run's console clean.
        MatchingProperties matchingProperties = new MatchingProperties();
        matchingProperties.getProgress().setEnabled("false");
        backfillService = new TrialFriendlyTitleBackfillService(
                trialDbService, friendlyTitleService, matchingProperties);
    }

    private Trial trial(String extid, String friendlyTitle) {
        Trial t = new Trial();
        t.setExtid(extid);
        t.setNctId("NCT" + extid);
        t.setFriendlyTitle(friendlyTitle);
        return t;
    }

    @Test
    @DisplayName("a trial with no friendly title is generated")
    void generatesWhenMissing() {
        Trial t = trial("a", null);
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of(t));

        var result = backfillService.backfillAll();

        verify(friendlyTitleService).generate(t);
        assertThat(result.generated()).isEqualTo(1);
        assertThat(result.alreadyPresent()).isZero();
    }

    /**
     * The whole reason this backfill differs from the free treatment-goal one: regenerating a
     * trial that already has a title would re-pay for an answer nobody asked to change.
     */
    @Test
    @DisplayName("a trial that already has a friendly title is skipped, not regenerated")
    void skipsWhenPresent() {
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of(
                trial("a", "Stage IV - Disease Control - X - Y")));

        var result = backfillService.backfillAll();

        verify(friendlyTitleService, never()).generate(any());
        assertThat(result.alreadyPresent()).isEqualTo(1);
        assertThat(result.generated()).isZero();
    }

    @Test
    @DisplayName("a blank friendly title still counts as missing")
    void blankTitleCountsAsMissing() {
        Trial t = trial("a", "   ");
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of(t));

        var result = backfillService.backfillAll();

        verify(friendlyTitleService).generate(t);
        assertThat(result.generated()).isEqualTo(1);
    }

    @Test
    @DisplayName("one failing trial does not abandon the run")
    void oneFailureDoesNotStopTheRun() {
        Trial bad = trial("bad", null);
        Trial good = trial("good", null);
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of(bad, good));
        when(friendlyTitleService.generate(bad)).thenThrow(new RuntimeException("rate limited"));

        var result = backfillService.backfillAll();

        assertThat(result.trialsRead()).isEqualTo(2);
        assertThat(result.generated()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().getFirst()).contains("NCTbad");
        verify(friendlyTitleService, times(1)).generate(good);
    }

    @Test
    @DisplayName("an empty corpus reports nothing rather than failing")
    void emptyCorpus() {
        when(trialDbService.findByActive(ActiveEnum.ACTIVE)).thenReturn(List.of());

        var result = backfillService.backfillAll();

        assertThat(result.trialsRead()).isZero();
        assertThat(result.errors()).isEmpty();
    }
}
