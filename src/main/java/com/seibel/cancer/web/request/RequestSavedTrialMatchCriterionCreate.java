package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequestSavedTrialMatchCriterionCreate extends BaseRequest {

    @NotEmpty(message = "The trialMatchExtid is required.")
    @Size(max = 36, message = "The trialMatchExtid must be at most 36 characters.")
    private String trialMatchExtid;

    @NotEmpty(message = "The chunkText is required.")
    private String chunkText;

    @NotNull(message = "The score is required.")
    private BigDecimal score;

    private Boolean isExclusion;

    @Size(max = 64, message = "The source must be at most 64 characters.")
    private String source;

    private Integer ordinal;
}
