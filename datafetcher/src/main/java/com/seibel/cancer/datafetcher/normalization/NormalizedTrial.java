package com.seibel.cancer.datafetcher.normalization;

import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.common.domain.Trial;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * One source payload parsed into a Trial plus its child records, ready for
 * {@link TrialNormalizationService} to persist. trialId on the child records is left
 * null here - it's resolved once the Trial itself has been created/updated.
 */
@Data
@Builder
public class NormalizedTrial {
    private Trial trial;
    @Builder.Default
    private List<Location> locations = List.of();
    @Builder.Default
    private List<ArmGroup> armGroups = List.of();
    @Builder.Default
    private List<Intervention> interventions = List.of();
    @Builder.Default
    private List<Outcome> outcomes = List.of();
    @Builder.Default
    private List<OverallOfficial> overallOfficials = List.of();
    @Builder.Default
    private List<String> conditionNames = List.of();
    @Builder.Default
    private List<NormalizedSponsor> sponsors = List.of();

    @Data
    @Builder
    public static class NormalizedSponsor {
        private String name;
        private String orgClass;
    }
}
