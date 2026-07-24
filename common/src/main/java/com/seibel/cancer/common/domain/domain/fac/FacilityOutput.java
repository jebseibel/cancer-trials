package com.seibel.cancer.common.domain.domain.fac;

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
public class FacilityOutput extends BaseFacDomain {

    private String facilityName;
    private String bulkUploadLookup;
    private String trackingSystem;
    private String trackingSystemIdNumber;
    private String crsTrackingAttestationStatus;
    private String trackingAttestationStartDate;
    private String trackingAttestationExpirationDate;
    private String renewableResourceType;
    private String facilityState;
    private String eiaOrQf;
    private String eiaIdNumber;
    private String nameplateCapacityMw;
    private String firstOperational;
    private String repoweringDate;
    private String endDateOfExtendedUse;
    private String hb2021Status;
    private String nercRegion;
    private String devNotes;
    private Integer missing;
    private Integer mistake;
    private Integer duplicate;
}
