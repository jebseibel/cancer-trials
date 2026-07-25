package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestAppUserCreate extends BaseRequest {

    @NotEmpty(message = "The username is required.")
    @Size(max = 64, message = "The username must be at most 64 characters.")
    private String username;

    @NotEmpty(message = "The passwordHash is required.")
    @Size(max = 255, message = "The passwordHash must be at most 255 characters.")
    private String passwordHash;

    @Size(max = 128, message = "The displayName must be at most 128 characters.")
    private String displayName;
}
