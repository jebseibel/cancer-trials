package com.seibel.cancer.common.domain.domain.retire;

import com.seibel.cancer.common.domain.domain.BaseUniqueDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class BaseRetDomain extends BaseUniqueDomain {

    private String status;
}
