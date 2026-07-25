package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseArmGroup {
    private String extid;
    private Long trialId;
    private String label;
    private String type;
    private String description;
}
