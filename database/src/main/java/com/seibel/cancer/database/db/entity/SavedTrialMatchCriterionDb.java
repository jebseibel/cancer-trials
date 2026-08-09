package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "trial_match_criterion")
public class SavedTrialMatchCriterionDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456825L;

    @Column(name = "trial_match_id", nullable = false)
    private Long trialMatchId;

    @Column(name = "chunk_text", columnDefinition = "text", nullable = false)
    private String chunkText;

    @Column(name = "score", precision = 6, scale = 4, nullable = false)
    private BigDecimal score;

    @Column(name = "is_exclusion")
    private Boolean isExclusion;

    @Column(name = "source", length = 64)
    private String source;

    @Column(name = "ordinal")
    private Integer ordinal;
}
