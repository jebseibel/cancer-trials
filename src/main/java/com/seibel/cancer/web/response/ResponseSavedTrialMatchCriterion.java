package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ResponseSavedTrialMatchCriterion {
    private String extid;
    private String trialMatchExtid;
    private String chunkText;
    private BigDecimal score;
    private Boolean isExclusion;
    private String source;
    private Integer ordinal;
}
