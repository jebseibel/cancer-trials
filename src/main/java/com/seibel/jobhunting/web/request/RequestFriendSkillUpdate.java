package com.seibel.jobhunting.web.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestFriendSkillUpdate extends BaseRequest {

    private Long friendId;

    private Long skillId;
}
