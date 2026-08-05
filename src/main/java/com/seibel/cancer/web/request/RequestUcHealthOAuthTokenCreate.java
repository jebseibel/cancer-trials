package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestUcHealthOAuthTokenCreate extends BaseRequest {

    @Size(max = 2048, message = "The accessToken must be at most 2048 characters.")
    private String accessToken;

    @Size(max = 2048, message = "The refreshToken must be at most 2048 characters.")
    private String refreshToken;

    private LocalDateTime expiresAt;

    @Size(max = 128, message = "The patientFhirId must be at most 128 characters.")
    private String patientFhirId;

    @Size(max = 512, message = "The scope must be at most 512 characters.")
    private String scope;
}
