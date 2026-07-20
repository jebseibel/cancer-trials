package com.seibel.jobhunting.web.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestFriendJobPostingCreate extends BaseRequest {

    @NotNull(message = "The friendId is required.")
    private Long friendId;

    @NotNull(message = "The jobPostingId is required.")
    private Long jobPostingId;
}
