package com.seibel.jobs.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestContactCreate extends BaseRequest {

    private Long companyId;

    private Long jobPostingId;

    @NotEmpty(message = "The name is required.")
    @Size(max = 255, message = "The name must be at most 255 characters.")
    private String name;

    @Size(max = 120, message = "The role must be at most 120 characters.")
    private String role;

    @Size(max = 255, message = "The email must be at most 255 characters.")
    private String email;

    @Size(max = 32, message = "The phone must be at most 32 characters.")
    private String phone;

    private String notes;
}
