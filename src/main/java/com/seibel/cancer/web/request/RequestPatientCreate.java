package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequestPatientCreate extends BaseRequest {

    @NotEmpty(message = "The displayName is required.")
    @Size(max = 128, message = "The displayName must be at most 128 characters.")
    private String displayName;

    @Size(max = 255, message = "The fullName must be at most 255 characters.")
    private String fullName;

    private LocalDate dateOfBirth;

    @Size(max = 16, message = "The sex must be at most 16 characters.")
    private String sex;

    @Size(max = 1000, message = "The notes must be at most 1000 characters.")
    private String notes;
}
