package com.seibel.jobhunting.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseFriendCompany {
    private String extid;
    private Long friendId;
    private Long companyId;
}
