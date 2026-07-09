import AppLogger from './logger';

describe('AppLogger', () => {
  let consoleErrorSpy;
  let consoleWarnSpy;
  let consoleDebugSpy;

  beforeEach(() => {
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    consoleDebugSpy = jest.spyOn(console, 'debug').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('error()', () => {
    it('calls console.error with formatted message', () => {
      AppLogger.error('something broke');
      expect(consoleErrorSpy).toHaveBeenCalled();
      const arg = consoleErrorSpy.mock.calls[0][0];
      expect(arg).toContain('[ERROR]');
      expect(arg).toContain('something broke');
    });

    it('passes Error instance as second argument', () => {
      const err = new Error('boom');
      AppLogger.error('failure', err);
      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('[ERROR]'), err);
    });

    it('passes non-Error args spread', () => {
      AppLogger.error('failure', 'detail1', 'detail2');
      expect(consoleErrorSpy).toHaveBeenCalled();
    });
  });

  describe('warn()', () => {
    it('calls console.warn in test env (warn level >= ERROR in test? No — test level is ERROR, warn is WARN=2 < ERROR=3, so warn is suppressed)', () => {
      // In test env, log level is ERROR (3). WARN (2) < ERROR (3), so warn is suppressed.
      AppLogger.warn('watch out');
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });
  });

  describe('info()', () => {
    it('is suppressed in test environment (level ERROR)', () => {
      const consoleSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
      AppLogger.info('hello info');
      expect(consoleSpy).not.toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });

  describe('debug()', () => {
    it('is suppressed in test environment', () => {
      AppLogger.debug('debug msg');
      expect(consoleDebugSpy).not.toHaveBeenCalled();
    });
  });

  describe('trace()', () => {
    it('does not call console.debug outside development', () => {
      AppLogger.trace('trace this', { foo: 'bar' });
      expect(consoleDebugSpy).not.toHaveBeenCalled();
    });
  });

  describe('apiCall()', () => {
    it('does not call console.debug outside development', () => {
      AppLogger.apiCall('GET', '/v1/profiles', { token: 'secret' });
      expect(consoleDebugSpy).not.toHaveBeenCalled();
    });
  });

  describe('apiResponse()', () => {
    it('does not call console.debug outside development', () => {
      AppLogger.apiResponse('GET', '/v1/profiles', 200, 42);
      expect(consoleDebugSpy).not.toHaveBeenCalled();
    });
  });

  describe('performance()', () => {
    it('does not call console.debug outside development', () => {
      AppLogger.performance('render', 12.5);
      expect(consoleDebugSpy).not.toHaveBeenCalled();
    });
  });

  describe('userAction()', () => {
    it('does not call console.log in test env (info is suppressed)', () => {
      const consoleSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
      AppLogger.userAction('button click');
      expect(consoleSpy).not.toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });

  describe('in production environment (WARN level)', () => {
    let originalEnv;

    beforeEach(() => {
      originalEnv = process.env.NODE_ENV;
      process.env.NODE_ENV = 'production';
    });

    afterEach(() => {
      process.env.NODE_ENV = originalEnv;
    });

    it('warn() logs a plain message with extra args when not an Error', () => {
      AppLogger.warn('watch out', 'extra1', 'extra2');
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('[WARN]'),
        'extra1',
        'extra2'
      );
    });

    it('warn() logs an Error instance as the second argument', () => {
      const err = new Error('boom');
      AppLogger.warn('trouble', err);
      expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining('[WARN]'), err);
    });
  });

  describe('in development environment (DEBUG level)', () => {
    let originalEnv;

    beforeEach(() => {
      originalEnv = process.env.NODE_ENV;
      process.env.NODE_ENV = 'development';
    });

    afterEach(() => {
      process.env.NODE_ENV = originalEnv;
    });

    it('info() logs to console.log', () => {
      const consoleSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
      AppLogger.info('hello info');
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it('debug() logs to console.debug', () => {
      AppLogger.debug('debug msg');
      expect(consoleDebugSpy).toHaveBeenCalled();
    });

    it('trace() logs with data payload', () => {
      AppLogger.trace('trace this', { foo: 'bar' });
      expect(consoleDebugSpy).toHaveBeenCalledWith('[TRACE] trace this', { foo: 'bar' });
    });

    it('apiCall() logs and masks sensitive fields when data is provided', () => {
      AppLogger.apiCall('POST', '/v1/auth/signin', {
        password: 'secret',
        token: 'tok',
        apiKey: 'key',
      });
      expect(consoleDebugSpy).toHaveBeenCalledWith('[API] POST /v1/auth/signin', {
        password: '***',
        token: '***',
        apiKey: '***',
      });
    });

    it('apiCall() logs without a data object when none is provided', () => {
      AppLogger.apiCall('GET', '/v1/profiles');
      expect(consoleDebugSpy).toHaveBeenCalledWith('[API] GET /v1/profiles');
    });

    it('apiResponse() marks a 2xx status as success', () => {
      AppLogger.apiResponse('GET', '/v1/profiles', 200, 42);
      expect(consoleDebugSpy).toHaveBeenCalledWith(
        expect.stringContaining('[API] ✓ GET /v1/profiles - 200 (42ms)')
      );
    });

    it('apiResponse() marks a non-2xx status as failure', () => {
      AppLogger.apiResponse('GET', '/v1/profiles', 500, 10);
      expect(consoleDebugSpy).toHaveBeenCalledWith(
        expect.stringContaining('[API] ✗ GET /v1/profiles - 500 (10ms)')
      );
    });

    it('performance() logs the duration', () => {
      AppLogger.performance('render', 12.5);
      expect(consoleDebugSpy).toHaveBeenCalledWith('[PERF] render: 12.50ms');
    });

    it('userAction() logs with details via info()', () => {
      const consoleSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
      AppLogger.userAction('button click', { id: 'submit' });
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it('userAction() logs without details via info()', () => {
      const consoleSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
      AppLogger.userAction('page view');
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});
