package com.seibel.jobs.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseContact {
    private String extid;
    private Long companyId;
    private Long jobPostingId;
    private String name;
    private String role;
    private String email;
    private String phone;
    private String notes;
}
