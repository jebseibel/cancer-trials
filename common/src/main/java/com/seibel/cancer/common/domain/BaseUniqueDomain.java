package com.seibel.cancer.common.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class BaseUniqueDomain extends BaseDomain {

    private String uniqueId;
    private String version;
    private String status;
}
