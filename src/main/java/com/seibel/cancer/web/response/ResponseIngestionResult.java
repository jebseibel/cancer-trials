package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResponseIngestionResult {
    private int studiesFetched;
    private int stagingRowsWritten;
    private int stagingRowsSkipped;
    private int pendingRowsProcessed;
    private int trialsNormalized;
    private List<String> ingestErrors;
    private List<String> normalizationErrors;
}
