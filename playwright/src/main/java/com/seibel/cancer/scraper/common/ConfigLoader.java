package com.seibel.cancer.scraper.common;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * Loads dotted-path values out of application.yaml.
 *
 * Copied from viro-playwright's ConfigLoader. Selectors and URLs live in config rather
 * than in code on purpose: MyChart is a vendor-hosted portal that gets reskinned without
 * notice, and a reskin should be a config edit, not a recompile.
 *
 * Never put credentials here - this file is committed. Those come from .env via Dotenv.
 */
public class ConfigLoader {

    private final Map<String, Object> config;

    public ConfigLoader() {
        this("application.yaml");
    }

    public ConfigLoader(String resourceName) {
        Yaml yaml = new Yaml();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException(resourceName + " not found on the classpath");
            }
            config = yaml.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + resourceName, e);
        }
    }

    public String get(String path) {
        String[] keys = path.split("\\.");
        Object current = config;
        for (String key : keys) {
            if (!(current instanceof Map)) return null;
            current = ((Map<?, ?>) current).get(key);
        }
        return current != null ? current.toString() : null;
    }

    /** Fails loudly instead of returning null - a missing selector should not surface as a confusing timeout. */
    public String require(String path) {
        String value = get(path);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config value: " + path);
        }
        return value;
    }

    public int getInt(String path, int fallback) {
        String value = get(path);
        return (value == null || value.isBlank()) ? fallback : Integer.parseInt(value.trim());
    }

    public boolean getBoolean(String path, boolean fallback) {
        String value = get(path);
        return (value == null || value.isBlank()) ? fallback : Boolean.parseBoolean(value.trim());
    }
}
