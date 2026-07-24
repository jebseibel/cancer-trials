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
public class TsWregis extends BaseTsDomain {

    private String trackingSystemId;
    private String facilityName;
    private String organizationId;
    private String organizationName;
    private String firstOperation;
    private BigDecimal nameplateCapacity;
    private String resourceType;
    private String state;
    private String originalApproval;
    private String coFiring;
    private String dgGroupIndicator;
}
