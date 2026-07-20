package com.seibel.jobhunting.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestPurchaseUpdate extends BaseRequest {

    @Size(max = 50, message = "The customer must be at most 50 characters.")
    private String customer;

    @Size(max = 255, message = "The items must be at most 255 characters.")
    private String items;

    @Size(max = 255, message = "The status must be at most 255 characters.")
    private String status;
}
