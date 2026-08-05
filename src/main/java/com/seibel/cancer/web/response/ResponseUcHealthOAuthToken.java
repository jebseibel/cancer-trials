package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResponseUcHealthOAuthToken {
    private String extid;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private String patientFhirId;
    private String scope;
}
