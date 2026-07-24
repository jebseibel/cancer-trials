package com.seibel.cancer.common.domain.domain.types;

import com.seibel.cancer.common.domain.domain.BaseUniqueDomain;
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
public class FacilityReport extends BaseUniqueDomain {

    private String facilityName;
    private String trackingSystem;
    private String trackingSystemId;
    private String resourceType;
    private String state;
    private String eiaCode;
    private String otherId;
    private BigDecimal nameplateCapacityMw;
    private String firstOperation;
    private String repoweredDate;
    private String coFiring;
    private String endDateOfExtendedUse;
    private String nercRegion;
    private String mwTypePorg;
}
