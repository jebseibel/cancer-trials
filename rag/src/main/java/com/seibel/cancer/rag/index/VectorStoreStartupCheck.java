package com.seibel.cancer.rag.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Reports at startup whether the vector store is usable.
 *
 * <p>The collection is deliberately not auto-created, so a rebuild or a fresh machine leaves it
 * missing. Without this the first symptom is a backfill that returns zero indexed trials, which
 * reads as a data problem rather than a setup step nobody ran.
 *
 * <p>Warns rather than failing startup: search is one feature, and the rest of the app - trial
 * ingestion, the patient record, Tier 1 matching - works fine without it. Blocking the boot
 * would turn a degraded feature into an outage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreStartupCheck implements ApplicationRunner {

    private final TrialIndexService indexService;

    @Override
    public void run(ApplicationArguments args) {
        String problem = indexService.checkVectorStoreReady();
        if (problem == null) {
            log.info("Vector store ready - trials can be prepared for search.");
        } else {
            log.warn("SEARCH UNAVAILABLE: {} Trial ingestion and the patient record are "
                    + "unaffected; only semantic search needs this.", problem);
        }
    }
}
