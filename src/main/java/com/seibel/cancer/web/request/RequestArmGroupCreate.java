package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestArmGroupCreate extends BaseRequest {

    @NotNull(message = "The trialId is required.")
    private Long trialId;

    @NotEmpty(message = "The label is required.")
    @Size(max = 255, message = "The label must be at most 255 characters.")
    private String label;

    @Size(max = 255, message = "The type must be at most 255 characters.")
    private String type;

    private String description;
}
