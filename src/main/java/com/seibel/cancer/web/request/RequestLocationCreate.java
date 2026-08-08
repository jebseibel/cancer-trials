package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestLocationCreate extends BaseRequest {

    @NotEmpty(message = "The trialExtid is required.")
    private String trialExtid;

    @Size(max = 255, message = "The facility must be at most 255 characters.")
    private String facility;

    @Size(max = 128, message = "The city must be at most 128 characters.")
    private String city;

    @Size(max = 128, message = "The state must be at most 128 characters.")
    private String state;

    @Size(max = 64, message = "The zip must be at most 64 characters.")
    private String zip;

    @Size(max = 128, message = "The country must be at most 128 characters.")
    private String country;

    @Size(max = 32, message = "The status must be at most 32 characters.")
    private String status;

    private BigDecimal latitude;

    private BigDecimal longitude;
}
