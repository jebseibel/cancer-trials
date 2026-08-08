package com.seibel.cancer.rag.config;

import com.seibel.cancer.rag.chunk.EligibilityCriteriaChunker;
import com.seibel.cancer.rag.chunk.TrialChunker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the RAG module's beans.
 *
 * <p>:rag is a library with no {@code @SpringBootApplication}, so {@link RagProperties} needs
 * explicit registration here rather than being picked up by the app's component scan.
 *
 * <p>The chunkers are built as beans from configured values rather than annotated as
 * {@code @Component}s. That keeps them free of any Spring dependency, so their tests construct
 * them directly with explicit values and stay fast - the chunking logic is where the quality
 * risk lives, so it needs the tightest test loop.
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

    @Bean
    public EligibilityCriteriaChunker eligibilityCriteriaChunker(RagProperties props) {
        RagProperties.Chunking c = props.getChunking();
        return new EligibilityCriteriaChunker(
                c.getMaxParentPrefixLength(), c.getMinCriterionLength());
    }

    @Bean
    public TrialChunker trialChunker(RagProperties props, EligibilityCriteriaChunker eligibilityChunker) {
        return new TrialChunker(eligibilityChunker, props.getChunking().getMaxProseChunkChars());
    }
}
