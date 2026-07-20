package com.seibel.jobhunting.common.domain;

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
public class Customer extends BaseDomain {
    private String code;
    private String name;
    private String contactName;
    private String description;
    private String contactEmail;
    private String contactPhone;
}
