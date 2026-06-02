/* eslint-disable no-console */
/**
 * Frontend Logger Utility
 * Mirrors backend AppLogger patterns from shared/src/main/java/com/suchika/shared/logging/AppLogger
 *
 * Usage:
 *   import { AppLogger } from '@utils/logger';
 *   AppLogger.info('User logged in', userId);
 *   AppLogger.error('Failed to fetch data', error);
 */

const LOG_LEVELS = {
  DEBUG: 0,
  INFO: 1,
  WARN: 2,
  ERROR: 3,
};

const LOG_LEVEL_NAMES = {
  0: 'DEBUG',
  1: 'INFO',
  2: 'WARN',
  3: 'ERROR',
};

/**
 * Get current log level based on environment
 */
const getCurrentLogLevel = () => {
  const env = process.env.NODE_ENV || 'development';
  if (env === 'production') return LOG_LEVELS.WARN;
  if (env === 'test') return LOG_LEVELS.ERROR;
  return LOG_LEVELS.DEBUG;
};

/**
 * Format log message with timestamp and level
 */
const formatLogMessage = (level, message, ...args) => {
  const timestamp = new Date().toISOString();
  const levelName = LOG_LEVEL_NAMES[level];
  const formattedArgs = args.length > 0 ? args : '';
  return `[${timestamp}] [${levelName}] ${message} ${formattedArgs}`.trim();
};

/**
 * AppLogger - Centralized logging utility
 * Aligns with backend logging patterns
 */
class AppLogger {
  /**
   * Log informational message
   * @param {string} message - Log message
   * @param {...any} args - Optional arguments to format into message
   */
  static info(message, ...args) {
    const currentLevel = getCurrentLogLevel();
    if (currentLevel <= LOG_LEVELS.INFO) {
      const formatted = formatLogMessage(LOG_LEVELS.INFO, message, ...args);
      console.log(formatted);
    }
  }

  /**
   * Log debug message
   * @param {string} message - Log message
   * @param {...any} args - Optional arguments
   */
  static debug(message, ...args) {
    const currentLevel = getCurrentLogLevel();
    if (currentLevel <= LOG_LEVELS.DEBUG) {
      const formatted = formatLogMessage(LOG_LEVELS.DEBUG, message, ...args);
      console.debug(formatted);
    }
  }

  /**
   * Log warning message
   * @param {string} message - Log message
   * @param {...any} args - Optional throwable or arguments
   */
  static warn(message, ...args) {
    const currentLevel = getCurrentLogLevel();
    if (currentLevel <= LOG_LEVELS.WARN) {
      const formatted = formatLogMessage(LOG_LEVELS.WARN, message);
      if (args.length === 1 && args[0] instanceof Error) {
        console.warn(formatted, args[0]);
      } else {
        console.warn(formatted, ...args);
      }
    }
  }

  /**
   * Log error message
   * @param {string} message - Log message
   * @param {...any} args - Optional throwable or arguments
   */
  static error(message, ...args) {
    const currentLevel = getCurrentLogLevel();
    if (currentLevel <= LOG_LEVELS.ERROR) {
      const formatted = formatLogMessage(LOG_LEVELS.ERROR, message);
      if (args.length === 1 && args[0] instanceof Error) {
        console.error(formatted, args[0]);
      } else {
        console.error(formatted, ...args);
      }
    }
  }

  /**
   * Log trace level (most verbose)
   * Useful for debugging API calls and state changes
   */
  static trace(message, data = null) {
    if (process.env.NODE_ENV === 'development') {
      console.debug(`[TRACE] ${message}`, data);
    }
  }

  /**
   * Log API call details
   * @param {string} method - HTTP method
   * @param {string} endpoint - API endpoint
   * @param {object} data - Optional request data (sensitive data will be masked)
   */
  static apiCall(method, endpoint, data = null) {
    if (process.env.NODE_ENV === 'development') {
      const msg = `[API] ${method} ${endpoint}`;
      if (data) {
        // Mask sensitive fields
        const masked = { ...data };
        if (masked.password) masked.password = '***';
        if (masked.token) masked.token = '***';
        if (masked.apiKey) masked.apiKey = '***';
        console.debug(msg, masked);
      } else {
        console.debug(msg);
      }
    }
  }

  /**
   * Log API response
   * @param {string} method - HTTP method
   * @param {string} endpoint - API endpoint
   * @param {number} statusCode - HTTP status code
   * @param {number} durationMs - Request duration in milliseconds
   */
  static apiResponse(method, endpoint, statusCode, durationMs = 0) {
    if (process.env.NODE_ENV === 'development') {
      const status = statusCode >= 200 && statusCode < 300 ? '✓' : '✗';
      console.debug(`[API] ${status} ${method} ${endpoint} - ${statusCode} (${durationMs}ms)`);
    }
  }

  /**
   * Log performance metrics
   * @param {string} label - Metric label
   * @param {number} durationMs - Duration in milliseconds
   */
  static performance(label, durationMs) {
    if (process.env.NODE_ENV === 'development') {
      console.debug(`[PERF] ${label}: ${durationMs.toFixed(2)}ms`);
    }
  }

  /**
   * Log user action
   * @param {string} action - Action description
   * @param {object} details - Optional details
   */
  static userAction(action, details = null) {
    const msg = `[USER] ${action}`;
    if (details) {
      this.info(msg, details);
    } else {
      this.info(msg);
    }
  }
}

export default AppLogger;
