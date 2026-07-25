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
@Table(name = "staging_raw_trial")
public class StagingRawTrialDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456800L;

    @Column(name = "trial_source_id", nullable = false)
    private Long trialSourceId;

    @Column(name = "source_trial_id", length = 255, nullable = false)
    private String sourceTrialId;

    @Column(name = "raw_payload", columnDefinition = "longtext")
    private String rawPayload;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "normalized_at")
    private LocalDateTime normalizedAt;

    @Column(name = "normalization_error", columnDefinition = "text")
    private String normalizationError;
}
