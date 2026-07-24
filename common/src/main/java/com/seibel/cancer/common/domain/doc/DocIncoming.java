package com.seibel.cancer.common.domain.doc;

import com.seibel.cancer.common.domain.BaseUniqueDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Domain model for incoming documents (customer uploads, OCR text, etc.)
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DocIncoming extends BaseUniqueDomain {

    private String customer;
    private String inputSource;
    private String documentType;
    private String data;
}
