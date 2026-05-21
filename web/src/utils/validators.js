/**
 * Data validation utilities
 */

/**
 * Validate email format
 * @param {string} email - Email to validate
 * @returns {boolean} True if valid email
 */
export const validateEmail = (email) => {
  if (typeof email !== 'string') return false;

  const normalized = email.trim().toLowerCase();
  if (normalized.length === 0) return false;
  if (normalized.includes(' ')) return false;

  const atIndex = normalized.indexOf('@');
  if (atIndex <= 0) return false;
  if (normalized.slice(atIndex + 1).includes('@')) return false;

  const dotIndex = normalized.indexOf('.', atIndex + 2);
  if (dotIndex === -1 || dotIndex === normalized.length - 1) return false;

  return true;
};

/**
 * Validate password strength
 * @param {string} password - Password to validate
 * @returns {object} Validation result with score and feedback
 */
export const validatePassword = (password) => {
  if (!password || password.length < 8) {
    return { isValid: false, score: 0, feedback: 'Password must be at least 8 characters' };
  }

  let score = 1;
  const feedback = [];

  if (/[a-z]/.test(password)) score += 1;
  else feedback.push('Add lowercase letters');

  if (/[A-Z]/.test(password)) score += 1;
  else feedback.push('Add uppercase letters');

  if (/\d/.test(password)) score += 1;
  else feedback.push('Add numbers');

  if (/[^a-zA-Z0-9]/.test(password)) score += 1;
  else feedback.push('Add special characters');

  return {
    isValid: score >= 3,
    score,
    feedback,
  };
};

/**
 * Validate required field
 * @param {any} value - Value to validate
 * @returns {boolean} True if value is not empty
 */
export const validateRequired = (value) => {
  if (typeof value === 'string') return value.trim().length > 0;
  if (Array.isArray(value)) return value.length > 0;
  return value !== null && value !== undefined;
};

/**
 * Validate number range
 * @param {number} value - Value to validate
 * @param {number} min - Minimum value
 * @param {number} max - Maximum value
 * @returns {boolean} True if within range
 */
export const validateRange = (value, min, max) => {
  return typeof value === 'number' && value >= min && value <= max;
};

/**
 * Validate URL format
 * @param {string} url - URL to validate
 * @returns {boolean} True if valid URL
 */
export const validateURL = (url) => {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
};

/**
 * Validate currency amount
 * @param {number} amount - Amount to validate
 * @returns {boolean} True if valid positive number with max 2 decimals
 */
export const validateCurrencyAmount = (amount) => {
  if (typeof amount !== 'number' || amount < 0) return false;
  return Number(amount.toFixed(2)) === amount;
};

/**
 * Validate form data
 * @param {object} data - Form data to validate
 * @param {object} rules - Validation rules
 * @returns {object} Validation errors
 */
export const validateForm = (data, rules) => {
  const errors = {};

  Object.entries(rules).forEach(([field, rule]) => {
    const value = data[field];

    if (rule.required && !validateRequired(value)) {
      errors[field] = `${field} is required`;
    }

    if (rule.email && value && !validateEmail(value)) {
      errors[field] = 'Invalid email format';
    }

    if (rule.minLength && value && value.length < rule.minLength) {
      errors[field] = `Must be at least ${rule.minLength} characters`;
    }

    if (rule.maxLength && value && value.length > rule.maxLength) {
      errors[field] = `Must be at most ${rule.maxLength} characters`;
    }

    if (rule.pattern && value && !rule.pattern.test(value)) {
      errors[field] = rule.patternMessage || 'Invalid format';
    }
  });

  return errors;
};
