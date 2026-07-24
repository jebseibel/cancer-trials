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
public class TsMretsLoad extends BaseTsLoadDomain {
    private String facilityName;
    private String organizationName;
    private String mRetsId;
    private String fuelType;
    private String cod;
    private String reportingEntity;
    private String stateProvince;
    private String nameplateCapacity;
    private String capacityFactor;
    private String eligibility;
}
