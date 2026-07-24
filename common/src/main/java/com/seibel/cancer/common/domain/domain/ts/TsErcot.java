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
public class TsErcot extends BaseTsDomain {

    private String companyName;
    private String powerGeneratingCompanyName;
    private String powerGeneratingCompanyCode;
    private String facilityName;
    private String generatorSiteCode;
    private String trackingSystemId;
    private String unitContactInformation;
    private String resourceType;
    private String facilityNoncompetitiveCertificationData;
    private BigDecimal capacity;
}
