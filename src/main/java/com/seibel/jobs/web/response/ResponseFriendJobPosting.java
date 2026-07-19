package com.seibel.jobs.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseFriendJobPosting {
    private String extid;
    private Long friendId;
    private Long jobPostingId;
}
