package com.seibel.cancer.database.db.entity.ai;

import com.seibel.cancer.common.domain.enums.TrackingSystem;
import com.seibel.cancer.common.domain.enums.ai.AiLifecycle;
import com.seibel.cancer.database.converter.AiLifecycleConverter;
import com.seibel.cancer.database.converter.TrackingSystemConverter;
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
@Table(name = "ai_prompt")
public class AiPromptDb extends BaseUniqueDb {

    private static final long serialVersionUID = 7823461905312847631L;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "tracking_system", length = 32)
    @Convert(converter = TrackingSystemConverter.class)
    private TrackingSystem trackingSystem;

    @Column(name = "provider", length = 32, nullable = false)
    private String provider;

    @Column(name = "model", length = 128, nullable = false)
    private String model;

    @Column(name = "lifecycle", length = 32)
    @Convert(converter = AiLifecycleConverter.class)
    private AiLifecycle lifecycle;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "json_variables", columnDefinition = "TEXT")
    private String jsonVariables;

    @Column(name = "result_format", columnDefinition = "TEXT")
    private String resultFormat;

    @Column(name = "example_input", columnDefinition = "TEXT")
    private String exampleInput;

    @Column(name = "example_output", columnDefinition = "TEXT")
    private String exampleOutput;

    @Column(name = "json_variables_test", length = 256)
    private String jsonVariablesTest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "envelope_id")
    private AiPromptEnvelopeDb envelope;
}
