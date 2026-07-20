package com.seibel.jobhunting.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseCustomer {
    private String extid;
    private String code;
    private String name;
    private String contactName;
    private String description;
    private String contactEmail;
    private String contactPhone;
}
