package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "uchealth_oauth_token")
public class UcHealthOAuthTokenDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456820L;

    @Column(name = "access_token", length = 2048)
    private String accessToken;

    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "patient_fhir_id", length = 128)
    private String patientFhirId;

    @Column(name = "scope", length = 512)
    private String scope;
}
