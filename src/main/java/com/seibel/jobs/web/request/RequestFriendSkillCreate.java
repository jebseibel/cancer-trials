package com.seibel.jobs.web.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestFriendSkillCreate extends BaseRequest {

    @NotNull(message = "The friendId is required.")
    private Long friendId;

    @NotNull(message = "The skillId is required.")
    private Long skillId;
}
