package com.seibel.cancer.aiprovider.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AiConfigProperties.class, AiUiProperties.class})
public class AiConfiguration {
    // Configuration is now enabled
}