package com.seibel.basic.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestCustomerUpdate extends BaseRequest {

    @Size(max = 8, message = "The code must be at most 8 characters.")
    private String code;

    @Size(max = 120, message = "The name must be at most 120 characters.")
    private String name;

    @Size(max = 255, message = "The contactName must be at most 255 characters.")
    private String contactName;

    @Size(max = 255, message = "The description must be at most 255 characters.")
    private String description;

    @Size(max = 255, message = "The contactEmail must be at most 255 characters.")
    private String contactEmail;

    @Size(max = 255, message = "The contactPhone must be at most 255 characters.")
    private String contactPhone;
}
