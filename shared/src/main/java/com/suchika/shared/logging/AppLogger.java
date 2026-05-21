package com.suchika.shared.logging;

import io.quarkus.logging.Log;

/**
 * Application-wide logger utility.
 * Wraps Quarkus Log for consistent logging across all domains.
 */
public class AppLogger {

    private AppLogger() {
        // Utility class, not instantiable
    }

    /**
     * Log informational message.
     */
    public static void info(String message) {
        Log.info(message);
    }

    /**
     * Log informational message with parameters.
     */
    public static void info(String message, Object... params) {
        Log.infof(message, params);
    }

    /**
     * Log debug message.
     */
    public static void debug(String message) {
        Log.debug(message);
    }

    /**
     * Log debug message with parameters.
     */
    public static void debug(String message, Object... params) {
        Log.debugf(message, params);
    }

    /**
     * Log warning message.
     */
    public static void warn(String message) {
        Log.warn(message);
    }

    /**
     * Log warning message with exception.
     */
    public static void warn(String message, Throwable throwable) {
        Log.warn(message, throwable);
    }

    /**
     * Log warning message with parameters.
     */
    public static void warn(String message, Object... params) {
        Log.warnf(message, params);
    }

    /**
     * Log error message.
     */
    public static void error(String message) {
        Log.error(message);
    }

    /**
     * Log error message with exception.
     */
    public static void error(String message, Throwable throwable) {
        Log.error(message, throwable);
    }

    /**
     * Log error message with parameters.
     */
    public static void error(String message, Object... params) {
        Log.errorf(message, params);
    }

    /**
     * Log error message with exception and parameters.
     */
    public static void error(String message, Throwable throwable, Object... params) {
        Log.errorf(message, throwable, params);
    }
}
