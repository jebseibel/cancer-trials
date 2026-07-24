package com.seibel.cancer.common.domain.domain.load;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Domain model for EIA Plant
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EiaPlantLoad extends BaseEiaDomain {

    private String plantCode;
    private String plantName;
    private String streetAddress;
    private String city;
    private String state;
    private String zipcode;
    private String nercRegion;
}
