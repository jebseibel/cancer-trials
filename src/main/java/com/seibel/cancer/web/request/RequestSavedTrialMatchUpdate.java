package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequestSavedTrialMatchUpdate extends BaseRequest {

    @Size(max = 36, message = "The trialExtid must be at most 36 characters.")
    private String trialExtid;

    @Size(max = 36, message = "The appUserExtid must be at most 36 characters.")
    private String appUserExtid;

    @Size(max = 36, message = "The patientDiagnosisExtid must be at most 36 characters.")
    private String patientDiagnosisExtid;

    @Size(max = 36, message = "The searchRunId must be at most 36 characters.")
    private String searchRunId;

    private String queryText;

    private BigDecimal topScore;

    private Integer matchRank;

    @Size(max = 16, message = "The snapshotErStatus must be at most 16 characters.")
    private String snapshotErStatus;

    @Size(max = 16, message = "The snapshotPrStatus must be at most 16 characters.")
    private String snapshotPrStatus;

    @Size(max = 16, message = "The snapshotHer2Status must be at most 16 characters.")
    private String snapshotHer2Status;

    @Size(max = 16, message = "The snapshotStage must be at most 16 characters.")
    private String snapshotStage;

    @Size(max = 1000, message = "The snapshotBiomarkers must be at most 1000 characters.")
    private String snapshotBiomarkers;

    private LocalDateTime matchedAt;
}
