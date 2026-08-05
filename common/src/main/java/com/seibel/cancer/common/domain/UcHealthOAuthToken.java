package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UcHealthOAuthToken extends BaseDomain {
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private String patientFhirId;
    private String scope;
}
