package com.seibel.jobs.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseJobPostingSkill {
    private String extid;
    private Long jobPostingId;
    private Long skillId;
}
