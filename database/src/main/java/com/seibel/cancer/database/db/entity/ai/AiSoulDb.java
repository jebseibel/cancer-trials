package com.seibel.cancer.database.db.entity.ai;

import com.seibel.cancer.common.domain.enums.ai.AiLifecycle;
import com.seibel.cancer.database.converter.AiLifecycleConverter;
import com.seibel.cancer.database.db.entity.BaseUniqueDb;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_soul")
public class AiSoulDb extends BaseUniqueDb {
    private static final long serialVersionUID = 4819273650123847291L;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "purpose", columnDefinition = "TEXT", nullable = false)
    private String purpose;

    @Column(name = "core_values", columnDefinition = "TEXT", nullable = false)
    private String coreValues;

    @Column(name = "guardrails", columnDefinition = "TEXT", nullable = false)
    private String guardrails;

    @Column(name = "lifecycle", length = 32)
    @Convert(converter = AiLifecycleConverter.class)
    private AiLifecycle lifecycle;
}
