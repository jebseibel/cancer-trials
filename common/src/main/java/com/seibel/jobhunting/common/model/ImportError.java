package com.seibel.jobhunting.common.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportError {

    private long rowNumber;

    private String rawRecord;

    private String errorMessage;

    private Throwable exception;
}
