package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "lab_result")
public class LabResultDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456821L;

    @Column(name = "fhir_resource_id", length = 64, nullable = false, unique = true)
    private String fhirResourceId;

    @Column(name = "test_name", length = 500, nullable = false)
    private String testName;

    @Column(name = "loinc_code", length = 32)
    private String loincCode;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "effective_at")
    private LocalDateTime effectiveAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "value_quantity", precision = 18, scale = 6)
    private BigDecimal valueQuantity;

    // Nullable even when value_quantity is set - Epic returned a value with no unit.
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

    @Column(name = "is_panel")
    private Boolean isPanel;

    @Column(name = "display_text", columnDefinition = "text", nullable = false)
    private String displayText;
}
