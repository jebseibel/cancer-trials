package com.seibel.jobs.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestSkillUpdate extends BaseRequest {

    @Size(max = 120, message = "The name must be at most 120 characters.")
    private String name;
}
