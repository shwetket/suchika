import { info, warn, error } from './logger';

describe('logger', () => {
  const originalEnv = process.env.NODE_ENV;
  let infoSpy;
  let warnSpy;
  let errorSpy;

  beforeEach(() => {
    infoSpy = jest.spyOn(console, 'info').mockImplementation(() => {});
    warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    infoSpy.mockRestore();
    warnSpy.mockRestore();
    errorSpy.mockRestore();
    process.env.NODE_ENV = originalEnv;
  });

  describe('info()', () => {
    it('logs to console.info in development', () => {
      process.env.NODE_ENV = 'development';
      info('Test.context', 'hello', { a: 1 });
      expect(infoSpy).toHaveBeenCalledWith('[Test.context]', 'hello', { a: 1 });
    });

    it('does not log outside development', () => {
      process.env.NODE_ENV = 'production';
      info('Test.context', 'hello');
      expect(infoSpy).not.toHaveBeenCalled();
    });
  });

  describe('warn()', () => {
    it('logs to console.warn in development', () => {
      process.env.NODE_ENV = 'development';
      warn('Test.context', 'careful', { a: 1 });
      expect(warnSpy).toHaveBeenCalledWith('[Test.context]', 'careful', { a: 1 });
    });

    it('still logs to console.warn in production', () => {
      process.env.NODE_ENV = 'production';
      warn('Test.context', 'careful');
      expect(warnSpy).toHaveBeenCalledWith('[Test.context]', 'careful');
    });
  });

  describe('error()', () => {
    it('logs to console.error in development', () => {
      process.env.NODE_ENV = 'development';
      const err = new Error('boom');
      error('Test.context', err);
      expect(errorSpy).toHaveBeenCalledWith('[Test.context]', err);
    });

    it('still logs to console.error in production', () => {
      process.env.NODE_ENV = 'production';
      const err = new Error('boom');
      error('Test.context', err);
      expect(errorSpy).toHaveBeenCalledWith('[Test.context]', err);
    });
  });
});
