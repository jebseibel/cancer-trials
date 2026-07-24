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
@Table(name = "ai_prompt_gang")
public class AiPromptGangDb extends BaseUniqueDb {

    private static final long serialVersionUID = 2917364850192837465L;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "persona", columnDefinition = "TEXT", nullable = false)
    private String persona;

    @Column(name = "gang", length = 64)
    private String gang;

    @Column(name = "subgang", length = 128)
    private String subgang;

    @Column(name = "tone", length = 120)
    private String tone;

    @Column(name = "lifecycle", length = 32)
    @Convert(converter = AiLifecycleConverter.class)
    private AiLifecycle lifecycle;

    @Column(name = "prompt_sequence", columnDefinition = "TEXT")
    private String promptSequence;

    @Column(name = "prohibitions", columnDefinition = "TEXT")
    private String prohibitions;

    @Column(name = "decision_principles", columnDefinition = "TEXT")
    private String decisionPrinciples;

    @Column(name = "memory_depth")
    private Integer memoryDepth;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "soul_id")
    private AiSoulDb soul;
}
