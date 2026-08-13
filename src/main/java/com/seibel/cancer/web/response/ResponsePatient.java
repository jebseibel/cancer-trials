package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ResponsePatient {

    private String extid;
    private String displayName;
    private String fullName;
    private LocalDate dateOfBirth;
    private String sex;
    private String notes;
}
