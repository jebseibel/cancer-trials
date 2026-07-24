package com.seibel.cancer.common.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ImportResult<T> {

    @Builder.Default
    private List<T> successful = new ArrayList<>();

    @Builder.Default
    private List<ImportError> errors = new ArrayList<>();

    private int totalProcessed;

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public double getSuccessRate() {
        if (totalProcessed == 0) return 0.0;
        return (successful.size() * 100.0) / totalProcessed;
    }

    public String getSummary() {
        return String.format("Imported %d/%d records successfully (%.1f%%), %d errors",
                successful.size(), totalProcessed, getSuccessRate(), errors.size());
    }
}
