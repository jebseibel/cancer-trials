package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseTrialSource {
    private String extid;
    private String code;
    private String name;
    private String baseUrl;
}
