package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ResponseSavedTrialMatch {
    private String extid;
    private String trialExtid;
    private String appUserExtid;
    private String patientDiagnosisExtid;
    private String searchRunId;
    private String queryText;
    private BigDecimal topScore;
    private Integer matchRank;
    private String snapshotErStatus;
    private String snapshotPrStatus;
    private String snapshotHer2Status;
    private String snapshotStage;
    private String snapshotBiomarkers;
    private LocalDateTime matchedAt;
}
