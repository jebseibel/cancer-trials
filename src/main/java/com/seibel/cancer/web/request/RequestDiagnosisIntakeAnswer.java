package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestDiagnosisIntakeAnswer extends BaseRequest {

    @NotEmpty(message = "The answerText is required.")
    @Size(max = 2_000, message = "The answerText must be at most 2,000 characters.")
    private String answerText;
}
