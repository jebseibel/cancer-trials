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
public class TsWregisLoad extends BaseTsLoadDomain {
    private String trackingSystemId;
    private String facilityName;
    private String organizationId;
    private String organizationName;
    private String firstOperation;
    private String nameplateCapacity;
    private String resourceType;
    private String state;
    private String originalApproval;
    private String coFiring;
    private String dgGroupIndicator;
}
