import {
  validateEmail,
  validatePassword,
  validateRequired,
  validateRange,
  validateURL,
  validateCurrencyAmount,
  validateForm,
} from './validators';

describe('validateEmail', () => {
  it('returns true for valid email', () => {
    expect(validateEmail('user@example.com')).toBe(true);
  });

  it('returns true with uppercase (normalised)', () => {
    expect(validateEmail('User@Example.COM')).toBe(true);
  });

  it('returns false for empty string', () => {
    expect(validateEmail('')).toBe(false);
  });

  it('returns false for whitespace only', () => {
    expect(validateEmail('   ')).toBe(false);
  });

  it('returns false when no @ symbol', () => {
    expect(validateEmail('userexample.com')).toBe(false);
  });

  it('returns false when @ is first character', () => {
    expect(validateEmail('@example.com')).toBe(false);
  });

  it('returns false when multiple @ symbols', () => {
    expect(validateEmail('a@b@c.com')).toBe(false);
  });

  it('returns false when no dot after @', () => {
    expect(validateEmail('user@examplecom')).toBe(false);
  });

  it('returns false when dot is last character', () => {
    expect(validateEmail('user@example.')).toBe(false);
  });

  it('returns false for non-string input', () => {
    expect(validateEmail(null)).toBe(false);
    expect(validateEmail(42)).toBe(false);
    expect(validateEmail(undefined)).toBe(false);
  });

  it('returns false when email contains spaces', () => {
    expect(validateEmail('user @example.com')).toBe(false);
  });
});

describe('validatePassword', () => {
  it('returns invalid for password under 8 chars', () => {
    const result = validatePassword('Ab1!');
    expect(result.isValid).toBe(false);
    expect(result.score).toBe(0);
  });

  it('returns invalid for empty string', () => {
    expect(validatePassword('').isValid).toBe(false);
  });

  it('returns invalid for null', () => {
    expect(validatePassword(null).isValid).toBe(false);
  });

  it('returns valid for strong password', () => {
    const result = validatePassword('Abcdef1!');
    expect(result.isValid).toBe(true);
    expect(result.score).toBeGreaterThanOrEqual(3);
  });

  it('adds feedback for missing lowercase', () => {
    const result = validatePassword('ABCDEF12!');
    expect(result.feedback).toContain('Add lowercase letters');
  });

  it('adds feedback for missing uppercase', () => {
    const result = validatePassword('abcdef12!');
    expect(result.feedback).toContain('Add uppercase letters');
  });

  it('adds feedback for missing numbers', () => {
    const result = validatePassword('Abcdefgh!');
    expect(result.feedback).toContain('Add numbers');
  });

  it('adds feedback for missing special characters', () => {
    const result = validatePassword('Abcdef12');
    expect(result.feedback).toContain('Add special characters');
  });

  it('score is 5 for fully strong password', () => {
    const result = validatePassword('Abcdef1!');
    expect(result.score).toBe(5);
  });
});

describe('validateRequired', () => {
  it('returns true for non-empty string', () => {
    expect(validateRequired('hello')).toBe(true);
  });

  it('returns false for empty string', () => {
    expect(validateRequired('')).toBe(false);
  });

  it('returns false for whitespace-only string', () => {
    expect(validateRequired('   ')).toBe(false);
  });

  it('returns true for non-empty array', () => {
    expect(validateRequired([1, 2])).toBe(true);
  });

  it('returns false for empty array', () => {
    expect(validateRequired([])).toBe(false);
  });

  it('returns false for null', () => {
    expect(validateRequired(null)).toBe(false);
  });

  it('returns false for undefined', () => {
    expect(validateRequired(undefined)).toBe(false);
  });

  it('returns true for number 0', () => {
    expect(validateRequired(0)).toBe(true);
  });

  it('returns true for boolean false', () => {
    expect(validateRequired(false)).toBe(true);
  });
});

describe('validateRange', () => {
  it('returns true when value within range', () => {
    expect(validateRange(5, 1, 10)).toBe(true);
  });

  it('returns true at minimum boundary', () => {
    expect(validateRange(1, 1, 10)).toBe(true);
  });

  it('returns true at maximum boundary', () => {
    expect(validateRange(10, 1, 10)).toBe(true);
  });

  it('returns false when below min', () => {
    expect(validateRange(0, 1, 10)).toBe(false);
  });

  it('returns false when above max', () => {
    expect(validateRange(11, 1, 10)).toBe(false);
  });

  it('returns false for non-number', () => {
    expect(validateRange('5', 1, 10)).toBe(false);
    expect(validateRange(null, 1, 10)).toBe(false);
  });
});

describe('validateURL', () => {
  it('returns true for valid http URL', () => {
    expect(validateURL('http://example.com')).toBe(true);
  });

  it('returns true for valid https URL', () => {
    expect(validateURL('https://example.com/path?q=1')).toBe(true);
  });

  it('returns false for invalid URL', () => {
    expect(validateURL('not-a-url')).toBe(false);
  });

  it('returns false for empty string', () => {
    expect(validateURL('')).toBe(false);
  });

  it('returns false for null', () => {
    expect(validateURL(null)).toBe(false);
  });
});

describe('validateCurrencyAmount', () => {
  it('returns true for valid positive amount', () => {
    expect(validateCurrencyAmount(10.5)).toBe(true);
  });

  it('returns true for zero', () => {
    expect(validateCurrencyAmount(0)).toBe(true);
  });

  it('returns true for 2 decimal places', () => {
    expect(validateCurrencyAmount(10.99)).toBe(true);
  });

  it('returns false for negative amount', () => {
    expect(validateCurrencyAmount(-1)).toBe(false);
  });

  it('returns false for non-number', () => {
    expect(validateCurrencyAmount('10')).toBe(false);
    expect(validateCurrencyAmount(null)).toBe(false);
  });

  it('returns false for more than 2 decimal places', () => {
    expect(validateCurrencyAmount(10.999)).toBe(false);
  });
});

describe('validateForm', () => {
  it('returns empty errors for valid data', () => {
    const errors = validateForm(
      { name: 'Alice', email: 'alice@example.com' },
      { name: { required: true }, email: { required: true, email: true } }
    );
    expect(errors).toEqual({});
  });

  it('returns error for missing required field', () => {
    const errors = validateForm({ name: '' }, { name: { required: true } });
    expect(errors.name).toBeTruthy();
  });

  it('returns error for invalid email', () => {
    const errors = validateForm({ email: 'notvalid' }, { email: { email: true } });
    expect(errors.email).toBeTruthy();
  });

  it('returns error for minLength violation', () => {
    const errors = validateForm({ username: 'ab' }, { username: { minLength: 5 } });
    expect(errors.username).toBeTruthy();
  });

  it('returns error for maxLength violation', () => {
    const errors = validateForm({ bio: 'a'.repeat(201) }, { bio: { maxLength: 200 } });
    expect(errors.bio).toBeTruthy();
  });

  it('returns error for pattern violation', () => {
    const errors = validateForm(
      { code: 'abc' },
      { code: { pattern: /^\d+$/, patternMessage: 'Digits only' } }
    );
    expect(errors.code).toBe('Digits only');
  });

  it('uses default pattern message when patternMessage missing', () => {
    const errors = validateForm({ code: 'abc' }, { code: { pattern: /^\d+$/ } });
    expect(errors.code).toBe('Invalid format');
  });
});
