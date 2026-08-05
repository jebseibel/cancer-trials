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
@Table(name = "lab_result_component")
public class LabResultComponentDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456822L;

    @Column(name = "lab_result_id", nullable = false)
    private Long labResultId;

    @Column(name = "component_name", length = 500, nullable = false)
    private String componentName;

    @Column(name = "loinc_code", length = 32)
    private String loincCode;

    @Column(name = "value_quantity", precision = 18, scale = 6)
    private BigDecimal valueQuantity;

    @Column(name = "value_unit", length = 64)
    private String valueUnit;

    @Column(name = "value_string", length = 1000)
    private String valueString;

    @Column(name = "interpretation", length = 128)
    private String interpretation;

    @Column(name = "reference_range_low", precision = 18, scale = 6)
    private BigDecimal referenceRangeLow;

    @Column(name = "reference_range_high", precision = 18, scale = 6)
    private BigDecimal referenceRangeHigh;

    @Column(name = "reference_range_text", length = 255)
    private String referenceRangeText;

    @Column(name = "display_text", length = 1000, nullable = false)
    private String displayText;
}
