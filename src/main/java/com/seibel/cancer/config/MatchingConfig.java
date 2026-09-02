package com.seibel.cancer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link MatchingProperties}, the same way {@link SeedConfig} registers
 * {@code PatientSeedProperties} - explicit {@code @EnableConfigurationProperties} rather than
 * relying on component scan, so the binding is easy to find in one place.
 */
@Configuration
@EnableConfigurationProperties(MatchingProperties.class)
public class MatchingConfig {
}
