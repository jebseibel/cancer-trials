package com.seibel.cancer.common.domain.domain.ts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TsMirecs extends BaseTsDomain {

    private String accountHolderCompany;
    private String trackingSystemId;
    private String state;
    private String facilityName;
    private String facilityOwnershipType;
    private String selfReporting;
    private String coFiring;
    private String fuelProjectType;
    private String firstOperation;
    private BigDecimal nameplateCapacity;
    private String eiaId;
    private String mi;
    private String greenEEligible;
    private String lihiCertified;
}
