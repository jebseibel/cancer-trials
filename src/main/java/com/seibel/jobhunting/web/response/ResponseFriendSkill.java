package com.seibel.jobhunting.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseFriendSkill {
    private String extid;
    private Long friendId;
    private Long skillId;
}
