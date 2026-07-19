package com.seibel.jobs.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestFriendUpdate extends BaseRequest {

    @Size(max = 255, message = "The name must be at most 255 characters.")
    private String name;

    @Size(max = 255, message = "The relationship must be at most 255 characters.")
    private String relationship;

    @Size(max = 255, message = "The email must be at most 255 characters.")
    private String email;

    @Size(max = 32, message = "The phone must be at most 32 characters.")
    private String phone;

    @Size(max = 1024, message = "The linkedinUrl must be at most 1024 characters.")
    private String linkedinUrl;

    private LocalDate lastContactedAt;

    private String notes;
}
