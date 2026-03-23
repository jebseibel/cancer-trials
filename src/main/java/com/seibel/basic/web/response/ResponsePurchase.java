package com.seibel.basic.web.response;

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
