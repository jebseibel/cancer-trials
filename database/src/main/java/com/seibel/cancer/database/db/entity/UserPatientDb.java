package com.seibel.cancer.database.db.entity;

import com.seibel.cancer.common.enums.AccessLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Which login may see which patient. See {@code com.seibel.cancer.common.domain.UserPatient}.
 *
 * <p>{@code accessLevel} is {@link EnumType#STRING}, not ordinal - an ordinal would silently
 * re-map every stored grant if a level were ever inserted into the middle of the enum, which
 * on an authorisation table means quietly handing out the wrong access.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_patient")
public class UserPatientDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456813L;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", length = 24, nullable = false)
    private AccessLevel accessLevel;

    @Column(name = "granted_by_user_id")
    private Long grantedByUserId;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    /** Null means active. Revocation writes this; the row is never deleted. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "note", length = 255)
    private String note;
}
