package com.seibel.jobs.web.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestUserSkillCreate extends BaseRequest {

    @NotNull(message = "The userId is required.")
    private Long userId;

    @NotNull(message = "The skillId is required.")
    private Long skillId;
}
