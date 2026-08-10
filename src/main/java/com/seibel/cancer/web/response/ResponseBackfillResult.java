package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Outcome of a vector-store backfill run. */
@Data
@Builder
public class ResponseBackfillResult {

    /** Trials that produced at least one chunk. */
    private int trialsIndexed;

    /** Total chunks embedded and written to the vector store. */
    private int chunksWritten;

    /** Trials that produced no chunks - e.g. no eligibility text or child records. */
    private int trialsSkipped;

    /** Trials left alone because the vector store already held chunks for them. */
    private int trialsAlreadyIndexed;

    /** Per-trial failures. The run continues past each one rather than abandoning the corpus. */
    private List<String> errors;
}
