package com.seibel.jobs.web.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestFriendJobPostingUpdate extends BaseRequest {

    private Long friendId;

    private Long jobPostingId;
}
