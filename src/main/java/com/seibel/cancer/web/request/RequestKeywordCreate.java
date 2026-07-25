package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestKeywordCreate extends BaseRequest {

    @NotEmpty(message = "The name is required.")
    @Size(max = 255, message = "The name must be at most 255 characters.")
    private String name;
}
