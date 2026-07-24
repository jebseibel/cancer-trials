package com.seibel.cancer.common.domain.domain.crs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Domain object for CRS mistakes identified by users during CrsChange review.
 * Captures a snapshot of the erroneous CRS data for audit/reporting purposes.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CrsMistake extends BaseCrsDomain {

    private String columnsInError;
    private Integer diffCount;

    private String facility;
    private String crsTrackingAttestationStatus;
    private String trackingSystem;
    private String trackingSystemId;
    private String effectiveDate;
    private String expirationDate;
    private String renewableType;
    private String state;
    private String eiaOrQf;
    private String facilityIdNum;
    private String nameplateCapacity;
    private String firstOperational;
    private String repoweringDate;
    private String extendedUseEnd;
    private String hb2021Status;
    private String memo;
    private LocalDateTime reportedAt;
    private Integer missing;
    private Integer mistake;
    private Integer duplicate;
}
