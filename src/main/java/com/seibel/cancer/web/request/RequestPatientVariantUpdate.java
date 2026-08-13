package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/** All fields optional - the converter rejects a request with nothing set. */
@Data
@EqualsAndHashCode(callSuper = true)
public class RequestPatientVariantUpdate extends BaseRequest {

    @Size(max = 36, message = "The patientExtid must be at most 36 characters.")
    private String patientExtid;

    @Size(max = 36, message = "The patientDiagnosisExtid must be at most 36 characters.")
    private String patientDiagnosisExtid;

    @Size(max = 16, message = "The pik3caStatus must be at most 16 characters.")
    private String pik3caStatus;

    @Size(max = 16, message = "The esr1Status must be at most 16 characters.")
    private String esr1Status;

    @Size(max = 16, message = "The tp53Status must be at most 16 characters.")
    private String tp53Status;

    @Size(max = 16, message = "The akt1Status must be at most 16 characters.")
    private String akt1Status;

    @Size(max = 16, message = "The ptenStatus must be at most 16 characters.")
    private String ptenStatus;

    @Size(max = 16, message = "The erbb2SomaticStatus must be at most 16 characters.")
    private String erbb2SomaticStatus;

    @Size(max = 16, message = "The brca1Status must be at most 16 characters.")
    private String brca1Status;

    @Size(max = 16, message = "The brca2Status must be at most 16 characters.")
    private String brca2Status;

    @Size(max = 16, message = "The palb2Status must be at most 16 characters.")
    private String palb2Status;

    @Size(max = 16, message = "The atmStatus must be at most 16 characters.")
    private String atmStatus;

    @Size(max = 16, message = "The chek2Status must be at most 16 characters.")
    private String chek2Status;

    @Size(max = 16, message = "The hrdStatus must be at most 16 characters.")
    private String hrdStatus;

    @Size(max = 16, message = "The pdl1Status must be at most 16 characters.")
    private String pdl1Status;

    @Min(value = 0, message = "The ki67Percent must be at least 0.")
    @Max(value = 100, message = "The ki67Percent must be at most 100.")
    private Integer ki67Percent;

    @Size(max = 16, message = "The germlineTestDone must be at most 16 characters.")
    private String germlineTestDone;

    @Size(max = 16, message = "The somaticTestDone must be at most 16 characters.")
    private String somaticTestDone;

    private LocalDate testDate;

    @Size(max = 255, message = "The testLab must be at most 255 characters.")
    private String testLab;

    @Size(max = 1000, message = "The otherVariants must be at most 1000 characters.")
    private String otherVariants;

    private String notes;
}
