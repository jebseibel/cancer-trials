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
public class TsErcotLoad extends BaseTsLoadDomain {
    private String companyName;
    private String powerGeneratingCompanyName;
    private String powerGeneratingCompanyCode;
    private String generatorSiteName;
    private String generatorSiteCode;
    private String facilityIdentificationNumber;
    private String unitContactInformation;
    private String technologyType;
    private String facilityNoncompetitiveCertificationData;
    private String capacity;
}
