package com.suchika.shared.logging;

import io.quarkus.logging.Log;
import org.jboss.logging.Logger;

/**
 * Application-wide logger utility.
 * Wraps Quarkus Log for consistent logging across all domains.
 *
 * <p>Exactly 4 logging conventions are supported project-wide — INFO, WARNING,
 * ERROR, HEALTH — with no DEBUG level anywhere. See
 * {@code documents/LOGGING_AND_EXCEPTIONS.md} for the full convention writeup,
 * including why {@link #health(String, Object...)} is a separate log category
 * rather than a new {@code java.util.logging.Level}.
 */
public class AppLogger {

    /**
     * Dedicated category for HEALTH-convention events (service lifecycle,
     * startup/shutdown, health-probe activity) — deliberately a distinct
     * {@link Logger} instance from the one {@code io.quarkus.logging.Log}
     * resolves for the rest of this class, so HEALTH events are filterable
     * by category (%c{3.} in every application.properties log format) without
     * inventing a custom log Level. Logs at INFO severity.
     *
     * <p>Not to be confused with the unrelated "health" domain (vitals, doctor
     * visits) — this category is about service/operational health.
     */
    private static final Logger HEALTH_LOGGER = Logger.getLogger("com.suchika.health");

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
     * Log a HEALTH-convention event (service lifecycle / startup / shutdown /
     * health-probe) at INFO severity, under the dedicated "com.suchika.health"
     * category.
     */
    public static void health(String message) {
        HEALTH_LOGGER.info(message);
    }

    /**
     * Log a HEALTH-convention event with parameters.
     */
    public static void health(String message, Object... params) {
        HEALTH_LOGGER.infof(message, params);
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
        Log.errorf(throwable, message, params);
    }
}
