package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ResponseLocation {
    private String extid;
    private String trialExtid;
    private String facility;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String status;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
