package com.seibel.jobhunting.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestCustomerCreate extends BaseRequest {

    @NotEmpty(message = "The code is required.")
    @Size(max = 8, message = "The code must be at most 8 characters.")
    private String code;

    @NotEmpty(message = "The name is required.")
    @Size(max = 120, message = "The name must be at most 120 characters.")
    private String name;

    @NotEmpty(message = "The contactName is required.")
    @Size(max = 255, message = "The contactName must be at most 255 characters.")
    private String contactName;

    @NotEmpty(message = "The description is required.")
    @Size(max = 255, message = "The description must be at most 255 characters.")
    private String description;

    @NotEmpty(message = "The contactEmail is required.")
    @Size(max = 255, message = "The contactEmail must be at most 255 characters.")
    private String contactEmail;

    @NotEmpty(message = "The contactPhone is required.")
    @Size(max = 255, message = "The contactPhone must be at most 255 characters.")
    private String contactPhone;
}
