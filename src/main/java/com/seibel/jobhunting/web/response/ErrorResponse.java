package com.seibel.jobhunting.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    OffsetDateTime timestamp;
    int status;
    String error;
    String message;
    String path;
    List<FieldError> details;

    @Value
    @Builder
    public static class FieldError {
        String field;
        String message;
    }
}
