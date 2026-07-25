package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseSponsor {
    private String extid;
    private String name;
    private String orgClass;
}
