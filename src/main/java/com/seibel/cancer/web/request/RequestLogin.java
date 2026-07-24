package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class RequestLogin {

    @NotEmpty(message = "Username is required")
    private String username;

    @NotEmpty(message = "Password is required")
    private String password;
}
