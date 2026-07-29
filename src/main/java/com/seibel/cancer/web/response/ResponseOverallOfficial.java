package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseOverallOfficial {
    private String extid;
    private String trialExtid;
    private String name;
    private String affiliation;
    private String role;
}
