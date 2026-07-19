package com.seibel.jobs.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseUserSkill {
    private String extid;
    private Long userId;
    private Long skillId;
}
