package com.seibel.jobs.web.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestUserSkillUpdate extends BaseRequest {

    private Long userId;

    private Long skillId;
}
