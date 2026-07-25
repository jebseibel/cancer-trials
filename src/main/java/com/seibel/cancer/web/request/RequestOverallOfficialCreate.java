package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestOverallOfficialCreate extends BaseRequest {

    @NotNull(message = "The trialId is required.")
    private Long trialId;

    @NotEmpty(message = "The name is required.")
    @Size(max = 255, message = "The name must be at most 255 characters.")
    private String name;

    @Size(max = 255, message = "The affiliation must be at most 255 characters.")
    private String affiliation;

    @Size(max = 100, message = "The role must be at most 100 characters.")
    private String role;
}
