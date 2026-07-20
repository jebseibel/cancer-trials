package com.seibel.jobhunting.web.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestJobPostingSkillCreate extends BaseRequest {

    @NotNull(message = "The jobPostingId is required.")
    private Long jobPostingId;

    @NotNull(message = "The skillId is required.")
    private Long skillId;
}
