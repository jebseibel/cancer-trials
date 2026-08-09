package com.seibel.cancer.datafetcher.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgressTickerPropertiesTest {

    @Test
    void defaults_shouldMatchTheDocumentedValues() {
        ProgressTickerProperties props = new ProgressTickerProperties();

        assertEquals("true", props.getEnabled());
        assertEquals(100, props.getLineWidth());
        assertEquals(10, props.getFlushInterval());
    }

    @Test
    void default_shouldDrawTheBarWithNoConsoleAttached() {
        // The real case: ingestion triggered from the frontend runs on a request thread with
        // no console, which is what made the original "auto" default never draw.
        assertTrue(new ProgressTickerProperties().resolveEnabled(false));
    }

    @Test
    void auto_shouldFollowConsoleAttachment() {
        ProgressTickerProperties props = new ProgressTickerProperties();
        props.setEnabled("auto");

        assertTrue(props.resolveEnabled(true));
        assertFalse(props.resolveEnabled(false));
    }

    @Test
    void explicitTrue_shouldForceOnEvenWithNoConsole() {
        ProgressTickerProperties props = new ProgressTickerProperties();
        props.setEnabled("true");

        assertTrue(props.resolveEnabled(false), "true must override the no-console case");
    }

    @Test
    void explicitFalse_shouldForceOffEvenWithAConsole() {
        ProgressTickerProperties props = new ProgressTickerProperties();
        props.setEnabled("false");

        assertFalse(props.resolveEnabled(true));
    }

    @Test
    void blankOrNull_shouldFallBackToAuto() {
        ProgressTickerProperties props = new ProgressTickerProperties();

        props.setEnabled(null);
        assertTrue(props.resolveEnabled(true));
        assertFalse(props.resolveEnabled(false));

        props.setEnabled("   ");
        assertTrue(props.resolveEnabled(true));
        assertFalse(props.resolveEnabled(false));
    }

    @Test
    void enabled_shouldTolerateCasingAndSurroundingWhitespace() {
        ProgressTickerProperties props = new ProgressTickerProperties();

        props.setEnabled("AUTO");
        assertFalse(props.resolveEnabled(false), "AUTO should resolve as auto, not as false");

        props.setEnabled(" TRUE ");
        assertTrue(props.resolveEnabled(false));
    }
}
