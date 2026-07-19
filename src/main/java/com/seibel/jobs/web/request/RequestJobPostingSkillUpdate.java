package com.seibel.jobs.web.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestJobPostingSkillUpdate extends BaseRequest {

    private Long jobPostingId;

    private Long skillId;
}
