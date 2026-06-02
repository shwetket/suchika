/**
 * Error handling utilities
 * Aligns with shared/src/main/java/com/suchika/shared error handling patterns
 */

import { ERROR_MESSAGES } from './constants';

/**
 * HTTP Status codes mapping
 */
const HTTP_STATUS = {
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  UNPROCESSABLE_ENTITY: 422,
  INTERNAL_SERVER_ERROR: 500,
  SERVICE_UNAVAILABLE: 503,
};

/**
 * Error codes matching backend ApplicationException
 */
const ERROR_CODES = {
  BAD_REQUEST: 'BAD_REQUEST',
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN',
  NOT_FOUND: 'NOT_FOUND',
  CONFLICT: 'CONFLICT',
  UNPROCESSABLE_ENTITY: 'UNPROCESSABLE_ENTITY',
  NOT_IMPLEMENTED: 'NOT_IMPLEMENTED',
  INTERNAL_SERVER_ERROR: 'INTERNAL_SERVER_ERROR',
  NOT_ACCEPTABLE: 'NOT_ACCEPTABLE',
};

/**
 * Custom ApplicationException matching backend pattern
 */
export class ApplicationException extends Error {
  constructor(statusCode, errorCode, message, details = null, cause = null) {
    super(message);
    this.name = 'ApplicationException';
    this.statusCode = statusCode;
    this.errorCode = errorCode;
    this.message = message;
    this.details = details;
    this.cause = cause;
    this.timestamp = new Date().getTime();
  }

  /**
   * Create ErrorResponse DTO matching backend format
   */
  toErrorResponse() {
    return {
      status: this.statusCode,
      errorCode: this.errorCode,
      message: this.message,
      details: this.details,
      timestamp: this.timestamp,
    };
  }
}

/**
 * Parse API error response (matches backend ErrorResponse DTO)
 * @param {Response|object} response - Fetch response or error data
 * @returns {ApplicationException} Parsed error
 */
export const parseErrorResponse = (response) => {
  let errorData = {
    status: HTTP_STATUS.INTERNAL_SERVER_ERROR,
    errorCode: ERROR_CODES.INTERNAL_SERVER_ERROR,
    message: ERROR_MESSAGES.SERVER_ERROR,
    details: null,
  };

  if (response?.status && response?.errorCode) {
    // Already in ErrorResponse format from backend
    errorData = response;
  } else if (response?.response?.data) {
    // Axios-style error
    errorData = response.response.data;
  } else if (response?.status && typeof response?.status === 'number') {
    // HTTP Response object
    errorData.status = response.status;
  }

  return new ApplicationException(
    errorData.status || HTTP_STATUS.INTERNAL_SERVER_ERROR,
    errorData.errorCode || ERROR_CODES.INTERNAL_SERVER_ERROR,
    errorData.message || ERROR_MESSAGES.SERVER_ERROR,
    errorData.details
  );
};

/**
 * Get user-friendly error message
 * @param {Error|ApplicationException|object} error - Error object
 * @returns {string} User-friendly error message
 */
export const getErrorMessage = (error) => {
  // Network errors
  if (!navigator.onLine) {
    return ERROR_MESSAGES.NETWORK_ERROR;
  }

  // ApplicationException
  if (error instanceof ApplicationException) {
    return error.message;
  }

  // HTTP errors
  if (error?.statusCode) {
    switch (error.statusCode) {
      case HTTP_STATUS.UNAUTHORIZED:
        return ERROR_MESSAGES.AUTH_FAILED;
      case HTTP_STATUS.FORBIDDEN:
        return ERROR_MESSAGES.UNAUTHORIZED;
      case HTTP_STATUS.NOT_FOUND:
        return ERROR_MESSAGES.NOT_FOUND;
      case HTTP_STATUS.INTERNAL_SERVER_ERROR:
      case HTTP_STATUS.SERVICE_UNAVAILABLE:
        return ERROR_MESSAGES.SERVER_ERROR;
      case HTTP_STATUS.UNPROCESSABLE_ENTITY:
      case HTTP_STATUS.BAD_REQUEST:
        return error.message || ERROR_MESSAGES.VALIDATION_ERROR;
      default:
        return error.message || ERROR_MESSAGES.SERVER_ERROR;
    }
  }

  // API response errors
  if (error?.response?.data?.message) {
    return error.response.data.message;
  }

  // Error object with message
  if (error?.message && typeof error.message === 'string') {
    return error.message;
  }

  // String errors
  if (typeof error === 'string') {
    return error;
  }

  return ERROR_MESSAGES.SERVER_ERROR;
};

/**
 * Log error with context (delegates to logger)
 * @param {string} context - Where the error occurred
 * @param {Error} error - Error object
 */
export const logError = (context, error) => {
  if (process.env.NODE_ENV === 'development') {
    console.error(`[${context}]`, error);
  }
};

/**
 * Create ApplicationException from status and message
 * @param {number} statusCode - HTTP status code
 * @param {string} errorCode - Error code
 * @param {string} message - Error message
 * @param {string} details - Optional details
 * @returns {ApplicationException}
 */
export const createApplicationException = (statusCode, errorCode, message, details = null) => {
  return new ApplicationException(statusCode, errorCode, message, details);
};

/**
 * Handle API response and throw on error
 * @param {Response} response - Fetch response object
 * @returns {Promise<object>} Response JSON data
 * @throws {ApplicationException} If response not OK
 */
export const handleAPIResponse = async (response) => {
  if (!response.ok) {
    let errorData = {};
    try {
      errorData = await response.json();
    } catch {
      // Response is not JSON, create generic error
      errorData = {
        status: response.status,
        errorCode: ERROR_CODES.INTERNAL_SERVER_ERROR,
        message: ERROR_MESSAGES.SERVER_ERROR,
      };
    }

    const error = parseErrorResponse(errorData);
    throw error;
  }

  try {
    return await response.json();
  } catch {
    return null;
  }
};

/**
 * Retry failed API calls with exponential backoff
 * @param {Function} fn - Async function to retry
 * @param {number} maxAttempts - Max retry attempts
 * @param {number} delayMs - Initial delay in milliseconds
 * @returns {Promise<any>} Result of successful call
 * @throws {ApplicationException} If all retries fail
 */
export const retryWithBackoff = async (
  fn,
  maxAttempts = 3,
  delayMs = 1000
) => {
  let lastError;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      return await fn();
    } catch (error) {
      lastError = parseErrorResponse(error);
      if (attempt < maxAttempts) {
        const delay = delayMs * (2 ** (attempt - 1));
        await new Promise((resolve) => setTimeout(resolve, delay));
      }
    }
  }

  throw lastError;
};

export { HTTP_STATUS, ERROR_CODES };
