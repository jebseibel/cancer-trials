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
public class TsNcretsLoad extends BaseTsLoadDomain {
    private String accountHolderCompany;
    private String trackingSystemId;
    private String state;
    private String facilityName;
    private String coFiring;
    private String resourceType;
    private String ncDocket;
    private String firstOperation;
    private String nameplateCapacity;
    private String nc;
    private String s886Classification;
    private String lihiCertified;
}
