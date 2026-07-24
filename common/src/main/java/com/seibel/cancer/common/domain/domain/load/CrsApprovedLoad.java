package com.seibel.cancer.common.domain.domain.load;

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
public class CrsApprovedLoad extends BaseCrsLoadDomain {
    private String facility;

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
}
