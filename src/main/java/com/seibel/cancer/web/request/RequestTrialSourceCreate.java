package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestTrialSourceCreate extends BaseRequest {

    @NotEmpty(message = "The code is required.")
    @Size(max = 50, message = "The code must be at most 50 characters.")
    private String code;

    @NotEmpty(message = "The name is required.")
    @Size(max = 255, message = "The name must be at most 255 characters.")
    private String name;

    @NotEmpty(message = "The baseUrl is required.")
    @Size(max = 255, message = "The baseUrl must be at most 255 characters.")
    private String baseUrl;
}
