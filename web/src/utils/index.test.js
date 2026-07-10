import * as utils from './index';

describe('utils barrel export', () => {
  it('re-exports formatters, validators, error helpers, and constants', () => {
    expect(typeof utils.formatCurrency).toBe('function');
    expect(typeof utils.AppLogger).toBe('function');
    expect(utils.API_ENDPOINTS).toBeDefined();
  });
});
