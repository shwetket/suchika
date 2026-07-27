/**
 * Centralized frontend logger.
 *
 * Levels: INFO / WARN / ERROR only.
 *  - No DEBUG — redundant with INFO for a frontend logger.
 *  - No HEALTH — that's a backend service-lifecycle concept (used by the
 *    Quarkus services' shared logger), not meaningful in a browser.
 *
 * Gating:
 *  - info()  is suppressed outside development. It's low-signal
 *    progress/debug noise that has no value once shipped.
 *  - warn() and error() always log, including production. Both represent a
 *    real, actionable condition (a fallback path was taken, an API call
 *    failed) and there is no centralized client-error shipping yet (no
 *    Sentry, no analytics beacon) — the browser console is the only place
 *    these are ever visible. Suppressing them in production, as the old
 *    dev-only-gated `logError` did, means they are never visible anywhere;
 *    that's worse than the console noise.
 */

const isDevelopment = () => process.env.NODE_ENV === 'development';

/**
 * Log a low-signal informational message. Suppressed outside development.
 * @param {string} context - Where the log originated, e.g. 'AuthContext.login'
 * @param {...*} args - Additional values to log
 */
export const info = (context, ...args) => {
  if (isDevelopment()) {
    // eslint-disable-next-line no-console -- logger.js is the one sanctioned place for this
    console.info(`[${context}]`, ...args);
  }
};

/**
 * Log a warning for a recoverable/fallback condition. Always logs.
 * @param {string} context - Where the warning originated
 * @param {...*} args - Additional values to log
 */
export const warn = (context, ...args) => {
  console.warn(`[${context}]`, ...args);
};

/**
 * Log an error. Always logs, including production.
 * @param {string} context - Where the error originated
 * @param {...*} args - Additional values to log
 */
export const error = (context, ...args) => {
  console.error(`[${context}]`, ...args);
};
