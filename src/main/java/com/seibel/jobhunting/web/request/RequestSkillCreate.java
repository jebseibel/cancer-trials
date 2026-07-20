package com.seibel.jobhunting.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestSkillCreate extends BaseRequest {

    @NotEmpty(message = "The name is required.")
    @Size(max = 120, message = "The name must be at most 120 characters.")
    private String name;
}
