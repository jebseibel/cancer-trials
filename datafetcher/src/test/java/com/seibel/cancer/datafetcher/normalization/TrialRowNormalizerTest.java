package com.seibel.cancer.datafetcher.normalization;

import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.database.db.service.ArmGroupDbService;
import com.seibel.cancer.database.db.service.ConditionDbService;
import com.seibel.cancer.database.db.service.InterventionDbService;
import com.seibel.cancer.database.db.service.LocationDbService;
import com.seibel.cancer.database.db.service.OutcomeDbService;
import com.seibel.cancer.database.db.service.OverallOfficialDbService;
import com.seibel.cancer.database.db.service.SponsorDbService;
import com.seibel.cancer.database.db.service.StagingRawTrialDbService;
import com.seibel.cancer.database.db.service.TrialDbService;
import com.seibel.cancer.database.db.service.TrialSourceDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrialRowNormalizerTest {

    @Mock private StagingRawTrialDbService stagingRawTrialDbService;
    @Mock private TrialSourceDbService trialSourceDbService;
    @Mock private TrialDbService trialDbService;
    @Mock private LocationDbService locationDbService;
    @Mock private ArmGroupDbService armGroupDbService;
    @Mock private InterventionDbService interventionDbService;
    @Mock private OutcomeDbService outcomeDbService;
    @Mock private OverallOfficialDbService overallOfficialDbService;
    @Mock private ConditionDbService conditionDbService;
    @Mock private SponsorDbService sponsorDbService;
    @Mock private TrialSourceParser parser;

    private TrialRowNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new TrialRowNormalizer(
                List.of(parser), stagingRawTrialDbService, trialSourceDbService, trialDbService,
                locationDbService, armGroupDbService, interventionDbService, outcomeDbService,
                overallOfficialDbService, conditionDbService, sponsorDbService);
    }

    private StagingRawTrial stagingRow() {
        StagingRawTrial staging = new StagingRawTrial();
        staging.setExtid("staging-extid-1");
        staging.setTrialSourceId(9L);
        staging.setSourceTrialId("NCT01234567");
        staging.setRawPayload("{}");
        return staging;
    }

    private TrialSource trialSource() {
        TrialSource source = new TrialSource();
        source.setId(9L);
        source.setCode("CLINICALTRIALS_GOV");
        return source;
    }

    @Test
    void normalize_newTrial_shouldCreateTrialAndChildrenWithoutDeletingAnything() {
        StagingRawTrial staging = stagingRow();
        when(trialSourceDbService.findById(9L)).thenReturn(trialSource());
        when(parser.supports("CLINICALTRIALS_GOV")).thenReturn(true);

        Trial incomingTrial = Trial.builder().nctId("NCT01234567").briefTitle("New Trial").build();
        Location location = Location.builder().facility("Some Facility").build();
        NormalizedTrial normalized = NormalizedTrial.builder()
                .trial(incomingTrial)
                .locations(List.of(location))
                .conditionNames(List.of("Some Condition"))
                .build();
        when(parser.parse("{}")).thenReturn(normalized);

        when(trialDbService.findByNctId("NCT01234567")).thenReturn(null);
        Trial createdTrial = Trial.builder().nctId("NCT01234567").briefTitle("New Trial").build();
        createdTrial.setId(42L);
        createdTrial.setExtid("trial-extid-1");
        when(trialDbService.create(incomingTrial)).thenReturn(createdTrial);

        when(conditionDbService.findByName("Some Condition")).thenReturn(null);

        normalizer.normalize(staging);

        verify(trialDbService).create(incomingTrial);
        verify(trialDbService, never()).update(any(), any());
        // no existing trial found -> no child deletion pass
        verify(locationDbService, never()).findByTrialId(anyLong());
        verify(locationDbService).create(argThat(l -> l.getTrialId().equals(42L)));
        verify(conditionDbService).create("Some Condition");
        verify(stagingRawTrialDbService).update(eq("staging-extid-1"), eq(null), eq(null), eq(null), eq(null),
                any(), eq(null));
    }

    @Test
    void normalize_existingTrial_shouldDeleteOldChildrenBeforeInsertingNew() {
        StagingRawTrial staging = stagingRow();
        when(trialSourceDbService.findById(9L)).thenReturn(trialSource());
        when(parser.supports("CLINICALTRIALS_GOV")).thenReturn(true);

        Trial incomingTrial = Trial.builder().nctId("NCT01234567").briefTitle("Updated Trial").build();
        NormalizedTrial normalized = NormalizedTrial.builder().trial(incomingTrial).build();
        when(parser.parse("{}")).thenReturn(normalized);

        Trial existingTrial = Trial.builder().nctId("NCT01234567").build();
        existingTrial.setId(42L);
        existingTrial.setExtid("trial-extid-1");
        when(trialDbService.findByNctId("NCT01234567")).thenReturn(existingTrial);

        Trial updatedTrial = Trial.builder().nctId("NCT01234567").briefTitle("Updated Trial").build();
        updatedTrial.setId(42L);
        updatedTrial.setExtid("trial-extid-1");
        when(trialDbService.update("trial-extid-1", incomingTrial)).thenReturn(updatedTrial);

        ArmGroup oldArmGroup = ArmGroup.builder().build();
        oldArmGroup.setExtid("old-armgroup-extid");
        when(armGroupDbService.findByTrialId(42L)).thenReturn(List.of(oldArmGroup));

        normalizer.normalize(staging);

        verify(trialDbService).update("trial-extid-1", incomingTrial);
        verify(trialDbService, never()).create(any());
        verify(armGroupDbService).findByTrialId(42L);
        verify(armGroupDbService).delete("old-armgroup-extid");
    }

    @Test
    void normalize_unknownTrialSource_shouldThrow() {
        StagingRawTrial staging = stagingRow();
        when(trialSourceDbService.findById(9L)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> normalizer.normalize(staging));
    }

    @Test
    void markFailed_shouldWriteNormalizationErrorOnly() {
        StagingRawTrial staging = stagingRow();

        normalizer.markFailed(staging, "boom");

        verify(stagingRawTrialDbService).update("staging-extid-1", null, null, null, null, null, "boom");
    }
}
