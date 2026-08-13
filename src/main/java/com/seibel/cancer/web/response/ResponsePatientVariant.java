package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ResponsePatientVariant {
    private String extid;
    private String patientExtid;
    private String patientDiagnosisExtid;
    private String pik3caStatus;
    private String esr1Status;
    private String tp53Status;
    private String akt1Status;
    private String ptenStatus;
    private String erbb2SomaticStatus;
    private String brca1Status;
    private String brca2Status;
    private String palb2Status;
    private String atmStatus;
    private String chek2Status;
    private String hrdStatus;
    private String pdl1Status;
    private Integer ki67Percent;
    private String germlineTestDone;
    private String somaticTestDone;
    private LocalDate testDate;
    private String testLab;
    private String otherVariants;
    private String notes;
}
