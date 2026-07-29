package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseIntervention {
    private String extid;
    private String trialExtid;
    private String type;
    private String name;
    private String description;
}
