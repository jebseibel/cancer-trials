package com.seibel.cancer.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the login throttle's two defaults to each other.
 *
 * <p>Each setting is declared twice - once in {@code application.yml} and once as the fallback
 * in the filter's {@code @Value} - and only the yaml one is normally in effect. A change to one
 * alone is silent: the app starts, logins work, and the limit is simply not what the yaml says.
 *
 * <p>The same shape of mistake broke the patient seed loader on 2026-08-21, where two defaults
 * for one path drifted apart and nothing failed. This is the cheap guard for it.
 */
class LoginRateLimitConfigTest {

    /** The default inside {@code @Value("${key:default}")} on the named field. */
    private static String annotationDefault(String fieldName) throws Exception {
        Field f = LoginRateLimitFilter.class.getDeclaredField(fieldName);
        String expression = f.getAnnotation(Value.class).value();

        Matcher m = Pattern.compile("\\$\\{[^:}]+:([^}]*)}").matcher(expression);
        assertThat(m.matches())
                .as("%s should declare a fallback default: %s", fieldName, expression)
                .isTrue();
        return m.group(1);
    }

    /** The default inside {@code ${KEY:default}} for a key under {@code security.login}. */
    @SuppressWarnings("unchecked")
    private static String yamlDefault(String key) throws Exception {
        try (InputStream in = LoginRateLimitConfigTest.class.getResourceAsStream("/application.yml")) {
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> login = (Map<String, Object>)
                    ((Map<String, Object>) root.get("security")).get("login");
            String raw = String.valueOf(login.get(key));

            Matcher m = Pattern.compile("\\$\\{[^:}]+:([^}]*)}").matcher(raw);
            return m.matches() ? m.group(1) : raw;
        }
    }

    @Test
    @DisplayName("max-attempts agrees between application.yml and the filter's fallback")
    void maxAttemptsDefaultsAgree() throws Exception {
        assertThat(annotationDefault("maxAttempts"))
                .as("two defaults for one setting - both have to move together")
                .isEqualTo(yamlDefault("max-attempts"));
    }

    @Test
    @DisplayName("lockout-minutes agrees between application.yml and the filter's fallback")
    void lockoutDefaultsAgree() throws Exception {
        assertThat(annotationDefault("lockoutMinutes"))
                .as("two defaults for one setting - both have to move together")
                .isEqualTo(yamlDefault("lockout-minutes"));
    }

    /**
     * A threshold below three locks out a real person for ordinary mistyping, and one this high
     * stops throttling anything. Not a style rule - the counter resets on success, so the number
     * only ever applies to consecutive failures.
     */
    @Test
    @DisplayName("the configured threshold stays in a range that throttles without punishing")
    void thresholdIsSane() throws Exception {
        int attempts = Integer.parseInt(yamlDefault("max-attempts"));
        assertThat(attempts).isBetween(3, 10);

        long lockout = Long.parseLong(yamlDefault("lockout-minutes"));
        assertThat(lockout).isGreaterThanOrEqualTo(5);
    }
}
