package com.seibel.cancer.sample;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Self-contained unit test that does not start Spring or require any external services.
 * Useful to demonstrate how to run individual tests with Gradle.
 */
public class SamplePureUnitTest {

    @Test
    void additionWorks() {
        int a = 2;
        int b = 3;
        assertEquals(5, a + b, "2 + 3 should equal 5");
    }
}
