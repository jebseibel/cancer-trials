package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestPurchaseCreate extends BaseRequest {

    @NotEmpty(message = "The customer is required.")
    @Size(max = 50, message = "The customer must be at most 50 characters.")
    private String customer;

    @NotEmpty(message = "The items is required.")
    @Size(max = 255, message = "The items must be at most 255 characters.")
    private String items;

    @NotEmpty(message = "The status is required.")
    @Size(max = 255, message = "The status must be at most 255 characters.")
    private String status;
}
