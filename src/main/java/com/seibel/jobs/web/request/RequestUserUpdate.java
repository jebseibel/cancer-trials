package com.seibel.jobs.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestUserUpdate extends BaseRequest {

    @Size(max = 50, message = "The username must be at most 50 characters.")
    private String username;

    @Size(max = 255, message = "The password must be at most 255 characters.")
    private String password;

    @Size(max = 100, message = "The email must be at most 100 characters.")
    private String email;

    @Size(max = 20, message = "The role must be at most 20 characters.")
    private String role;
}
