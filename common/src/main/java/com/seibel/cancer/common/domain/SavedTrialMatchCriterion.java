package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * One matching chunk - the evidence for why a trial surfaced. A SavedTrialMatch with three
 * matching criteria has three of these.
 *
 * isExclusion is the important field: it lets a high-scoring exclusion render as a concern
 * rather than a fit, per DIAGNOSIS_MATCHING_DESIGN.md section 5 - no verdicts, no
 * auto-exclusion. A match against an exclusion criterion is exactly what a human should be
 * prompted to ask about, and must never silently remove a trial from a list.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SavedTrialMatchCriterion extends BaseDomain {
    private Long trialMatchId;
    private String chunkText;
    private BigDecimal score;
    private Boolean isExclusion;
    private String source;
    private Integer ordinal;
}
