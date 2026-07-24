package com.seibel.cancer.common.domain.domain.retire;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RetirementUpload extends BaseRetDomain {

    private String batchUuid;
    private String trackingSystem;
    private String docType;
    private String customer;
    private String customerExtid;
    private String year;
    private String filename;
    private String filenameOrig;
    private String filenameClient;
    private Long storedDocumentId;
    private Integer recordCount;
    private String status;
    private String errorMessage;
    private LocalDateTime uploadedAt;
}
