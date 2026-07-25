package com.seibel.cancer.common.domain;

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
public class Location extends BaseDomain {
    private Long trialId;
    private String facility;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String status;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
