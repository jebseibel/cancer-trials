package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestArmGroupUpdate extends BaseRequest {

    private String trialExtid;

    @Size(max = 255, message = "The label must be at most 255 characters.")
    private String label;

    @Size(max = 255, message = "The type must be at most 255 characters.")
    private String type;

    private String description;
}
