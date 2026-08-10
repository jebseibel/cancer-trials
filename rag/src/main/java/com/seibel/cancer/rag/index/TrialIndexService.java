package com.seibel.cancer.rag.index;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.database.db.service.InterventionDbService;
import com.seibel.cancer.database.db.service.OutcomeDbService;
import com.seibel.cancer.database.db.service.TrialDbService;
import com.seibel.cancer.rag.chunk.TrialChunk;
import com.seibel.cancer.rag.chunk.TrialChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Indexes trials into the vector store: chunk, embed, write.
 *
 * <p>Calls {@code *DbService} classes in :database directly rather than the root module's
 * service layer - root depends on :rag, so the reverse would be a circular dependency. Same
 * constraint the ingestion pipeline hit (RAG_PLAN.md section 3).
 *
 * <p>Written against Spring AI's {@link VectorStore} interface, never Qdrant classes, so the
 * store can be swapped by changing a dependency and config.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialIndexService {

    private final VectorStore vectorStore;
    private final TrialDbService trialDbService;
    private final InterventionDbService interventionDbService;
    private final OutcomeDbService outcomeDbService;

    /** Injected so it carries the configured cancer.rag.chunking.* values. */
    private final TrialChunker chunker;

    /**
     * Re-indexes one trial: deletes its existing chunks, then writes the current ones.
     *
     * <p>Delete-then-insert rather than upsert, mirroring the shape
     * {@code TrialRowNormalizer} already uses for child records. This makes re-indexing
     * idempotent <em>and</em> correct when the chunk count shrinks - a chunking change that
     * produces fewer chunks would otherwise leave stale ones behind, which upserting by id
     * cannot fix.
     *
     * @return number of chunks written
     */
    public int reindexTrial(String trialExtid) {
        Trial trial = trialDbService.findByExtid(trialExtid);
        if (trial == null) {
            log.warn("reindexTrial: no trial for extid={}", trialExtid);
            return 0;
        }

        deleteChunksFor(trialExtid);

        List<TrialChunk> chunks = chunker.chunk(
                trial,
                interventionDbService.findByTrialId(trial.getId()),
                outcomeDbService.findByTrialId(trial.getId()));

        if (chunks.isEmpty()) {
            log.info("reindexTrial: extid={} nctId={} produced no chunks", trialExtid, trial.getNctId());
            return 0;
        }

        // Embedding happens inside VectorStore.add() - one call per batch, not per chunk.
        vectorStore.add(chunks.stream().map(this::toDocument).toList());

        log.debug("reindexTrial: extid={} nctId={} chunks={}", trialExtid, trial.getNctId(), chunks.size());
        return chunks.size();
    }

    /**
     * Confirms the vector store can be queried before a long job starts.
     *
     * <p>The collection is not auto-created ({@code initialize-schema: false}), deliberately -
     * letting Spring AI create it would infer vector dimensions at first write, so a
     * model/collection mismatch would surface as bad search results instead of a clear error.
     * The cost of that choice is that a missing collection has to be reported well, which is
     * what this is for.
     *
     * @return null when the store is usable, otherwise a message naming the actual problem
     */
    public String checkVectorStoreReady() {
        try {
            vectorStore.similaritySearch(SearchRequest.builder().query("readiness check").topK(1).build());
            return null;
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.toString() : e.getMessage();
            if (detail.contains("doesn't exist") || detail.contains("NOT_FOUND")) {
                return "The search index has not been set up: the vector store collection is "
                        + "missing. It must be created with 384 dimensions and Cosine distance "
                        + "before trials can be prepared for search.";
            }
            return "The vector store is not reachable: " + detail;
        }
    }

    /** Removes a trial's chunks from the vector store, by trialExtid metadata. */
    public void deleteChunksFor(String trialExtid) {
        try {
            vectorStore.delete("trialExtid == '" + trialExtid + "'");
        } catch (Exception e) {
            // A missing collection or an empty match is not an error worth failing over -
            // the caller is about to write the current chunks regardless.
            log.debug("deleteChunksFor({}) no-op: {}", trialExtid, e.getMessage());
        }
    }

    private Document toDocument(TrialChunk chunk) {
        return Document.builder()
                .id(chunk.id())
                .text(chunk.text())
                .metadata(chunk.metadata())
                .build();
    }

    /** Chunk count currently in the store, for verification. */
    public int countChunksFor(String trialExtid) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                        .query("*")
                        .topK(1000)
                        .filterExpression("trialExtid == '" + trialExtid + "'")
                        .build())
                .size();
    }
}
