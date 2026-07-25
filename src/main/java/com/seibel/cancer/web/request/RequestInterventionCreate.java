package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestInterventionCreate extends BaseRequest {

    @NotNull(message = "The trialId is required.")
    private Long trialId;

    @Size(max = 64, message = "The type must be at most 64 characters.")
    private String type;

    @NotEmpty(message = "The name is required.")
    @Size(max = 255, message = "The name must be at most 255 characters.")
    private String name;

    @Size(max = 65535, message = "The description must be at most 65535 characters.")
    private String description;
}
