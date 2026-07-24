package com.seibel.cancer.common.domain.domain;

import com.seibel.cancer.common.domain.BaseDomain;

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
public class Company extends BaseDomain {
    private String code;
    private String name;
    private String description;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String domain;
    private String portalContact;
    private String productName;
    private String estimatedMwh;
    private String program;
    private String serviceLevel;
    private String bulkProductId;
}
