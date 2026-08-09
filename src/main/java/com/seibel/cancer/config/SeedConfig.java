package com.seibel.cancer.config;

import com.seibel.cancer.database.db.service.PatientSeedProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the patient seed properties.
 *
 * <p>{@code PatientSeedProperties} lives in :database, which is a library with no
 * {@code @SpringBootApplication} of its own, so component scan does not pick it up - the same
 * reason DatafetcherConfig and RagConfig exist in their modules.
 */
@Configuration
@EnableConfigurationProperties(PatientSeedProperties.class)
public class SeedConfig {
}
