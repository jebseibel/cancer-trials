package com.seibel.jobs.web.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestFriendCompanyCreate extends BaseRequest {

    @NotNull(message = "The friendId is required.")
    private Long friendId;

    @NotNull(message = "The companyId is required.")
    private Long companyId;
}
