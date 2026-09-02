package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestDiagnosisIntakeStart extends BaseRequest {

    @NotEmpty(message = "The patientExtid is required.")
    @Size(max = 36, message = "The patientExtid must be at most 36 characters.")
    private String patientExtid;

    @NotEmpty(message = "The documentText is required.")
    @Size(max = 50_000, message = "The documentText must be at most 50,000 characters.")
    private String documentText;
}
