package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseAppUser {
    private String extid;
    private String username;
    private String displayName;
}
