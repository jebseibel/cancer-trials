package com.seibel.cancer.rag.embed;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.transformers.TransformersEmbeddingModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the local ONNX embedding pipeline works end to end before anything is built on
 * top of it.
 *
 * <p>The load-bearing assertion is the vector length: the Qdrant collection
 * clinical_trial_chunks is created with 384 dimensions, and a mismatch between model and
 * collection does not fail at startup - it fails on first write, much later and much more
 * confusingly. See RAG_PLAN.md section 12.
 *
 * <p>Deliberately not a @SpringBootTest: :rag is a library with no bootable app, and this
 * needs no Spring context, no database, and no running backend. It instantiates the model
 * directly.
 *
 * <p>First run downloads and caches the ONNX model (~80MB) to
 * ${java.io.tmpdir}/spring-ai-onnx-model, so it needs network access once and is slow.
 * Subsequent runs are local and fast.
 */
class LocalEmbeddingModelTest {

    /**
     * Must match the Qdrant collection's configured vector size and the model in
     * application.yml. 384 for the default all-MiniLM-L6-v2.
     */
    private static final int EXPECTED_DIMENSIONS = 384;

    private static TransformersEmbeddingModel embeddingModel;

    @BeforeAll
    static void loadModel() throws Exception {
        embeddingModel = new TransformersEmbeddingModel();
        // Defaults to sentence-transformers/all-MiniLM-L6-v2; afterPropertiesSet() is what
        // downloads/caches the model and tokenizer.
        embeddingModel.afterPropertiesSet();
    }

    @Test
    @DisplayName("embeds text into a vector matching the Qdrant collection's dimensions")
    void embedsToExpectedDimensions() {
        float[] vector = embeddingModel.embed("Patients with stage III disease and no prior chemotherapy.");

        assertThat(vector)
                .as("vector length must equal the Qdrant collection's configured size")
                .hasSize(EXPECTED_DIMENSIONS);
        assertThat(vector).as("a real embedding is not all zeros").isNotEmpty();
        assertThat(hasNonZero(vector)).as("embedding should contain non-zero values").isTrue();
    }

    @Test
    @DisplayName("places related eligibility text closer than unrelated text")
    void relatedTextScoresHigherThanUnrelated() {
        // Not a quality benchmark - just a sanity check that the model encodes meaning
        // rather than returning noise. Real retrieval quality is measured by the
        // evaluation set in RAG_PLAN.md section 10.
        float[] query = embeddingModel.embed("prior chemotherapy is not allowed");
        float[] related = embeddingModel.embed("Patients must not have received previous chemotherapy.");
        float[] unrelated = embeddingModel.embed("The study site is located in Denver, Colorado.");

        double relatedScore = cosineSimilarity(query, related);
        double unrelatedScore = cosineSimilarity(query, unrelated);

        assertThat(relatedScore)
                .as("semantically related criterion should score above unrelated text")
                .isGreaterThan(unrelatedScore);
    }

    private static boolean hasNonZero(float[] vector) {
        for (float v : vector) {
            if (v != 0.0f) return true;
        }
        return false;
    }

    /** Cosine similarity, matching the distance metric the Qdrant collection uses. */
    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
