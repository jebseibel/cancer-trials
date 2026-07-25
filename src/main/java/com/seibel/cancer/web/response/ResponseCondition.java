package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseCondition {
    private String extid;
    private String name;
}
