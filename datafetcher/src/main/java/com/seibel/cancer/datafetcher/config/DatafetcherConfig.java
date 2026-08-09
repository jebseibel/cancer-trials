package com.seibel.cancer.datafetcher.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the datafetcher module's configuration properties.
 *
 * <p>:datafetcher is a library with no {@code @SpringBootApplication}, so
 * {@code @ConfigurationProperties} classes need explicit registration here rather than being
 * picked up by the app's component scan. Same pattern as {@code RagConfig} in :rag.
 */
@Configuration
@EnableConfigurationProperties({ClinicalTrialsIngestProperties.class, ProgressTickerProperties.class})
public class DatafetcherConfig {
}
