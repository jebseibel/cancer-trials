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
public class TsMirecsLoad extends BaseTsLoadDomain {
    private String accountHolderCompany;
    private String mirecsId;
    private String state;
    private String project;
    private String facilityOwnershipType;
    private String selfReporting;
    private String multiFuelIndicator;
    private String fuelProjectType;
    private String commencedOperationDate;
    private String nameplateCapacity;
    private String eiaId;
    private String mi;
    private String greenEEnergyEligible;
    private String lihiCertified;
}
