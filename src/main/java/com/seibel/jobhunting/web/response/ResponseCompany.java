package com.seibel.jobhunting.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseCompany {
    private String extid;
    private String name;
    private String website;
    private String industry;
    private String notes;
}
