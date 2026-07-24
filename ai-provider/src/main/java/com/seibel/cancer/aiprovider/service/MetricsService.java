package com.seibel.cancer.aiprovider.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MetricsService safely wraps Micrometer usage.
 * If no MeterRegistry bean exists, it falls back to an internal SimpleMeterRegistry
 * to avoid startup errors in environments without monitoring configured.
 */
@Slf4j
@Service
public class MetricsService {

    private final MeterRegistry registry;

    @Autowired
    public MetricsService(@Autowired(required = false) MeterRegistry registry) {
        if (registry != null) {
            this.registry = registry;
            log.info("Using provided MeterRegistry: {}", registry.getClass().getSimpleName());
        } else {
            this.registry = new SimpleMeterRegistry();
            log.warn("No MeterRegistry bean found — using SimpleMeterRegistry fallback.");
        }
    }

    // ----------------------------------------------------------------------
    // Example usage helpers (you can expand these later)
    // ----------------------------------------------------------------------

    public void incrementCounter(String name, String... tags) {
        try {
            registry.counter(name, tags).increment();
        } catch (Exception e) {
            log.debug("Metrics increment failed for counter '{}': {}", name, e.getMessage());
        }
    }

    public void recordLatency(String timerName, long millis, String... tags) {
        try {
            registry.timer(timerName, tags).record(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.debug("Metrics recording failed for timer '{}': {}", timerName, e.getMessage());
        }
    }

    public MeterRegistry getRegistry() {
        return registry;
    }
}
