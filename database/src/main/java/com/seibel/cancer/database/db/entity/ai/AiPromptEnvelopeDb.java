package com.seibel.cancer.database.db.entity.ai;

import com.seibel.cancer.common.enums.ai.AiLifecycle;
import com.seibel.cancer.database.converter.AiLifecycleConverter;
import com.seibel.cancer.database.db.entity.BaseUniqueDb;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_prompt_envelope")
public class AiPromptEnvelopeDb extends BaseUniqueDb {

    private static final long serialVersionUID = 6183749205817364920L;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "sub_category", length = 128)
    private String subCategory;

    @Column(name = "result_type", length = 64)
    private String resultType;

    @Column(name = "sub_persona", columnDefinition = "TEXT")
    private String subPersona;

    @Column(name = "goal", columnDefinition = "TEXT")
    private String goal;

    @Column(name = "decision_principles", columnDefinition = "TEXT")
    private String decisionPrinciples;

    @Column(name = "tool_exclusions", length = 512)
    private String toolExclusions;

    @Column(name = "lifecycle", length = 32, nullable = false)
    @Convert(converter = AiLifecycleConverter.class)
    private AiLifecycle lifecycle;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gang_id")
    private AiPromptGangDb gang;
}
