package com.batch.android.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.SmallTest;
import com.batch.android.LoggerLevel;
import org.junit.Test;

// Tests that log level cascading works
@SmallTest
public class LoggerLevelTest {

    @Test
    public void testInternal() {
        LoggerLevel configuredLevel = LoggerLevel.INTERNAL;

        assertTrue(configuredLevel.canLog(LoggerLevel.INTERNAL));
        assertTrue(configuredLevel.canLog(LoggerLevel.VERBOSE));
        assertTrue(configuredLevel.canLog(LoggerLevel.INFO));
        assertTrue(configuredLevel.canLog(LoggerLevel.WARNING));
        assertTrue(configuredLevel.canLog(LoggerLevel.ERROR));
    }

    @Test
    public void testVerbose() {
        LoggerLevel configuredLevel = LoggerLevel.VERBOSE;

        assertFalse(configuredLevel.canLog(LoggerLevel.INTERNAL));
        assertTrue(configuredLevel.canLog(LoggerLevel.VERBOSE));
        assertTrue(configuredLevel.canLog(LoggerLevel.INFO));
        assertTrue(configuredLevel.canLog(LoggerLevel.WARNING));
        assertTrue(configuredLevel.canLog(LoggerLevel.ERROR));
    }

    @Test
    public void testInfo() {
        LoggerLevel configuredLevel = LoggerLevel.INFO;

        assertFalse(configuredLevel.canLog(LoggerLevel.INTERNAL));
        assertFalse(configuredLevel.canLog(LoggerLevel.VERBOSE));
        assertTrue(configuredLevel.canLog(LoggerLevel.INFO));
        assertTrue(configuredLevel.canLog(LoggerLevel.WARNING));
        assertTrue(configuredLevel.canLog(LoggerLevel.ERROR));
    }

    @Test
    public void testWarning() {
        LoggerLevel configuredLevel = LoggerLevel.WARNING;

        assertFalse(configuredLevel.canLog(LoggerLevel.INTERNAL));
        assertFalse(configuredLevel.canLog(LoggerLevel.VERBOSE));
        assertFalse(configuredLevel.canLog(LoggerLevel.INFO));
        assertTrue(configuredLevel.canLog(LoggerLevel.WARNING));
        assertTrue(configuredLevel.canLog(LoggerLevel.ERROR));
    }

    @Test
    public void testError() {
        LoggerLevel configuredLevel = LoggerLevel.ERROR;

        assertFalse(configuredLevel.canLog(LoggerLevel.INTERNAL));
        assertFalse(configuredLevel.canLog(LoggerLevel.VERBOSE));
        assertFalse(configuredLevel.canLog(LoggerLevel.INFO));
        assertFalse(configuredLevel.canLog(LoggerLevel.WARNING));
        assertTrue(configuredLevel.canLog(LoggerLevel.ERROR));
    }
}
