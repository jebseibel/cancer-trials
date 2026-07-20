package com.seibel.jobhunting.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestCompanyUpdate extends BaseRequest {

    @Size(max = 255, message = "The name must be at most 255 characters.")
    private String name;

    @Size(max = 1024, message = "The website must be at most 1024 characters.")
    private String website;

    @Size(max = 120, message = "The industry must be at most 120 characters.")
    private String industry;

    private String notes;
}
