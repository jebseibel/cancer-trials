package com.seibel.jobs.web.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestFriendCompanyUpdate extends BaseRequest {

    private Long friendId;

    private Long companyId;
}
