package com.seibel.jobs.web.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ResponseFriend {
    private String extid;
    private String name;
    private String relationship;
    private String email;
    private String phone;
    private String linkedinUrl;
    private LocalDate lastContactedAt;
    private String notes;
}
