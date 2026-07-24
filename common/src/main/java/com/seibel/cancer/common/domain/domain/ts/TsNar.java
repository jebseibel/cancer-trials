package com.seibel.cancer.common.domain.domain.ts;

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
public class TsNar extends BaseTsDomain {

    private String accountHolderCompany;
    private String trackingSystemId;
    private String state;
    private String country;
    private String facilityName;
    private String facilityOwnershipType;
    private String coFiring;
    private String resourceType;
    private String firstOperation;
    private String nameplateCapacity;
    private String mo;
    private String nc;
    private String ks;
    private String ny;
    private String il;
    private String pr;
    private String meClassI;
    private String meClassIa;
    private String meClassIi;
    private String usEpaGppEligible;
    private String lihiCertified;
}
