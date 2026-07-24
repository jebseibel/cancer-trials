package com.seibel.cancer.common.domain.domain.crs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CrsChange extends BaseCrsDomain {

    private String version;
    private String facility;
    private String crsTrackingAttestationStatus;
    private String trackingSystem;
    private String trackingSystemId;
    private String effectiveDate;
    private String trackingAttestationExpirationDate;
    private String renewableType;
    private String state;
    private String eiaOrQf;
    private String facilityIdNum;
    private String nameplateCapacity;
    private String firstOperational;
    private String repoweringDate;
    private String extendedUseEnd;
    private String hb2021Status;

    // Explicit extid references for relationships
    private String facilityOutputExtid;
    private String crsApprovedExtid;
    private String crsPendingExtid;

    // Count of differing fields
    private Integer diffCount;

    // Comma-separated list of field names that differ
    private String diffFields;

    // Comma-separated list of field names that have recorded mistakes
    private String crsMistakeFields;

    private Integer duplicate;

    // Rule information (populated when auto-accepting via rules)
    private String ruleCode;
    private String ruleReasonSnapshot;
}
