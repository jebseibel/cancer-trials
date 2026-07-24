package com.seibel.cancer.common.domain.domain.load;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Domain object for CRS mistakes loaded from CSV.
 * Raw data from production that will be processed into CrsMistake.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CrsMistakeLoad extends BaseCrsLoadDomain {

    private String columnsInError;
    private Integer diffCount;
    private String facility;
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
    private String reportedAt;  // Raw string from CSV, will be parsed during transformation
    private Integer missing;
    private Integer mistake;
    private Integer duplicate;
}
