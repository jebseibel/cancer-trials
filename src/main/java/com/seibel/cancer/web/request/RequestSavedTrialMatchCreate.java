package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequestSavedTrialMatchCreate extends BaseRequest {

    @NotEmpty(message = "The trialExtid is required.")
    @Size(max = 36, message = "The trialExtid must be at most 36 characters.")
    private String trialExtid;

    @NotEmpty(message = "The appUserExtid is required.")
    @Size(max = 36, message = "The appUserExtid must be at most 36 characters.")
    private String appUserExtid;

    @Size(max = 36, message = "The patientDiagnosisExtid must be at most 36 characters.")
    private String patientDiagnosisExtid;

    @NotEmpty(message = "The searchRunId is required.")
    @Size(max = 36, message = "The searchRunId must be at most 36 characters.")
    private String searchRunId;

    @NotEmpty(message = "The queryText is required.")
    private String queryText;

    @NotNull(message = "The topScore is required.")
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

    @NotNull(message = "The matchedAt is required.")
    private LocalDateTime matchedAt;
}
