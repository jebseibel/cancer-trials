package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequestSavedTrialMatchCriterionUpdate extends BaseRequest {

    @Size(max = 36, message = "The trialMatchExtid must be at most 36 characters.")
    private String trialMatchExtid;

    private String chunkText;

    private BigDecimal score;

    private Boolean isExclusion;

    @Size(max = 64, message = "The source must be at most 64 characters.")
    private String source;

    private Integer ordinal;
}
