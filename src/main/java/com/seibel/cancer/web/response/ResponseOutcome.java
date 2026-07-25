package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseOutcome {
    private String extid;
    private Long trialId;
    private String outcomeType;
    private String measure;
    private String description;
    private String timeFrame;
}
