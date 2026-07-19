package com.seibel.jobs.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponsePurchase {
    private String extid;
    private String customer;
    private String items;
    private String status;
}
