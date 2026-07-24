package com.seibel.cancer.common.domain.domain.load;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Domain model for EIA Generator
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EiaGeneratorLoad extends BaseEiaDomain {

    private String utilityId;
    private String utilityName;
    private String plantCode;
    private String plantName;
    private String state;
    private String generator;
    private String technology;
    private BigDecimal nameplateCapacityMw;
    private String updateDerateYear;
    private String operatingMonth;
    private String operatingYear;
    private String multipleFuels;
    private String cofireFuels;
    private String type;
}
