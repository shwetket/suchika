import {
  formatCurrency,
  formatDate,
  formatDateTime,
  formatPercentage,
  formatNumber,
  truncateText,
  formatDuration,
} from './formatters';

describe('formatCurrency', () => {
  it('formats USD amount with 2 decimal places', () => {
    expect(formatCurrency(1000)).toBe('$1,000.00');
  });

  it('formats with specified currency', () => {
    const result = formatCurrency(500, 'EUR');
    expect(result).toContain('500');
  });

  it('returns dash for non-number', () => {
    expect(formatCurrency('abc')).toBe('—');
    expect(formatCurrency(null)).toBe('—');
    expect(formatCurrency(undefined)).toBe('—');
  });

  it('formats zero', () => {
    expect(formatCurrency(0)).toBe('$0.00');
  });

  it('formats negative values', () => {
    expect(formatCurrency(-100)).toBe('-$100.00');
  });
});

describe('formatDate', () => {
  it('returns dash for null', () => {
    expect(formatDate(null)).toBe('—');
  });

  it('returns dash for undefined', () => {
    expect(formatDate(undefined)).toBe('—');
  });

  it('formats date in short format', () => {
    const result = formatDate('2024-01-15', 'short');
    expect(result).toContain('2024');
    expect(result).toContain('Jan');
  });

  it('formats date in long format', () => {
    const result = formatDate('2024-01-15', 'long');
    expect(result).toContain('January');
    expect(result).toContain('2024');
  });

  it('formats date in full format', () => {
    const result = formatDate('2024-01-15', 'full');
    expect(result).toContain('2024');
  });

  it('defaults to short format', () => {
    const resultDefault = formatDate('2024-06-01');
    const resultShort = formatDate('2024-06-01', 'short');
    expect(resultDefault).toBe(resultShort);
  });
});

describe('formatDateTime', () => {
  it('returns dash for null', () => {
    expect(formatDateTime(null)).toBe('—');
  });

  it('returns dash for undefined', () => {
    expect(formatDateTime(undefined)).toBe('—');
  });

  it('includes date and time parts', () => {
    const result = formatDateTime('2024-01-15T10:30:00');
    expect(result).toContain('Jan');
    expect(result).toMatch(/\d{2}:\d{2}/);
  });
});

describe('formatPercentage', () => {
  it('formats 0.5 as 50.0%', () => {
    expect(formatPercentage(0.5)).toBe('50.0%');
  });

  it('formats 1 as 100.0%', () => {
    expect(formatPercentage(1)).toBe('100.0%');
  });

  it('formats 0 as 0.0%', () => {
    expect(formatPercentage(0)).toBe('0.0%');
  });

  it('respects decimals parameter', () => {
    expect(formatPercentage(0.123, 2)).toBe('12.30%');
  });

  it('returns dash for non-number', () => {
    expect(formatPercentage('abc')).toBe('—');
    expect(formatPercentage(null)).toBe('—');
  });
});

describe('formatNumber', () => {
  it('formats integer with no decimals', () => {
    expect(formatNumber(1000)).toBe('1,000');
  });

  it('formats with specified decimals', () => {
    expect(formatNumber(1000.5, 2)).toBe('1,000.50');
  });

  it('returns dash for non-number', () => {
    expect(formatNumber('abc')).toBe('—');
    expect(formatNumber(null)).toBe('—');
  });

  it('formats zero', () => {
    expect(formatNumber(0)).toBe('0');
  });
});

describe('truncateText', () => {
  it('returns text unchanged when within limit', () => {
    expect(truncateText('hello', 10)).toBe('hello');
  });

  it('truncates and appends ellipsis when over limit', () => {
    const result = truncateText('hello world', 5);
    expect(result).toBe('hello...');
  });

  it('returns null/undefined unchanged', () => {
    expect(truncateText(null)).toBe(null);
    expect(truncateText(undefined)).toBe(undefined);
  });

  it('uses default limit of 50', () => {
    const long = 'a'.repeat(60);
    const result = truncateText(long);
    expect(result.length).toBe(53); // 50 chars + '...'
  });

  it('does not truncate when text length equals limit', () => {
    const text = 'a'.repeat(50);
    expect(truncateText(text, 50)).toBe(text);
  });
});

describe('formatDuration', () => {
  it('formats minutes only', () => {
    expect(formatDuration(30)).toBe('30m');
  });

  it('formats hours only', () => {
    expect(formatDuration(120)).toBe('2h');
  });

  it('formats hours and minutes', () => {
    expect(formatDuration(90)).toBe('1h 30m');
  });

  it('formats zero minutes', () => {
    expect(formatDuration(0)).toBe('0m');
  });

  it('returns dash for non-number', () => {
    expect(formatDuration('abc')).toBe('—');
    expect(formatDuration(null)).toBe('—');
  });

  it('returns dash for negative number', () => {
    expect(formatDuration(-1)).toBe('—');
  });
});
