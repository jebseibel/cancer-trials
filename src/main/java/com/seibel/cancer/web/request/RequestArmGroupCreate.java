package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestArmGroupCreate extends BaseRequest {

    @NotEmpty(message = "The trialExtid is required.")
    private String trialExtid;

    @NotEmpty(message = "The label is required.")
    @Size(max = 255, message = "The label must be at most 255 characters.")
    private String label;

    @Size(max = 255, message = "The type must be at most 255 characters.")
    private String type;

    private String description;
}
