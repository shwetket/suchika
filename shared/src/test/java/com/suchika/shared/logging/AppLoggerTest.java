package com.suchika.shared.logging;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * AppLogger is a pure static delegation wrapper around io.quarkus.logging.Log.
 * Log's static methods require Quarkus bytecode transformation (augmentation) to
 * resolve to a real org.jboss.logging.Logger instance -- calling them outside an
 * augmented runtime throws IllegalStateException. Hence @QuarkusTest here, purely
 * as a smoke test that every delegation path routes through without error.
 */
@QuarkusTest
class AppLoggerTest {

    @Test
    void info_plain_message_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.info("plain info message"));
    }

    @Test
    void info_with_params_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.info("info with %s", "param"));
    }

    @Test
    void debug_plain_message_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.debug("plain debug message"));
    }

    @Test
    void debug_with_params_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.debug("debug with %s", "param"));
    }

    @Test
    void warn_plain_message_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.warn("plain warn message"));
    }

    @Test
    void warn_with_throwable_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.warn("warn with cause", new RuntimeException("boom")));
    }

    @Test
    void warn_with_params_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.warn("warn with %s", "param"));
    }

    @Test
    void error_plain_message_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.error("plain error message"));
    }

    @Test
    void error_with_throwable_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.error("error with cause", new RuntimeException("boom")));
    }

    @Test
    void error_with_params_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.error("error with %s", "param"));
    }

    @Test
    void error_with_throwable_and_params_does_not_throw() {
        assertDoesNotThrow(() -> AppLogger.error("error %s with cause", new RuntimeException("boom"), "param"));
    }
}
