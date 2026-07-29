package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestOverallOfficialUpdate extends BaseRequest {

    private String trialExtid;

    @Size(max = 255, message = "The name must be at most 255 characters.")
    private String name;

    @Size(max = 255, message = "The affiliation must be at most 255 characters.")
    private String affiliation;

    @Size(max = 100, message = "The role must be at most 100 characters.")
    private String role;
}
