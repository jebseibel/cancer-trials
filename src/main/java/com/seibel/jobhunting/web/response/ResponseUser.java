package com.seibel.jobhunting.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseUser {
    private String extid;
    private String username;
    private String email;
    private String role;
}
