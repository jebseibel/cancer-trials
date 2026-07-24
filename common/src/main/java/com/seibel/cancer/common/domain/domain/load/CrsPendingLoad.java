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
public class CrsPendingLoad extends BaseCrsLoadDomain {
    private String facility;
    private String trackingSystem;
    private String trackingSystemId;
    private String previousExpirationDate;
}
