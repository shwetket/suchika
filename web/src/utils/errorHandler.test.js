import {
  ApplicationException,
  parseErrorResponse,
  getErrorMessage,
  logError,
  createApplicationException,
  handleAPIResponse,
  retryWithBackoff,
  HTTP_STATUS,
  ERROR_CODES,
} from './errorHandler';
import { ERROR_MESSAGES } from './constants';

describe('ApplicationException', () => {
  it('sets all fields from the constructor', () => {
    const err = new ApplicationException(404, 'NOT_FOUND', 'Account not found', { field: 'id' });
    expect(err.name).toBe('ApplicationException');
    expect(err.statusCode).toBe(404);
    expect(err.errorCode).toBe('NOT_FOUND');
    expect(err.message).toBe('Account not found');
    expect(err.details).toEqual({ field: 'id' });
    expect(err instanceof Error).toBe(true);
  });

  it('defaults details and cause to null', () => {
    const err = new ApplicationException(500, 'INTERNAL_SERVER_ERROR', 'Boom');
    expect(err.details).toBeNull();
    expect(err.cause).toBeNull();
  });

  it('toErrorResponse() returns the backend ErrorResponse DTO shape', () => {
    const err = new ApplicationException(409, 'CONFLICT', 'Duplicate', { key: 'value' });
    const dto = err.toErrorResponse();
    expect(dto).toEqual({
      status: 409,
      errorCode: 'CONFLICT',
      message: 'Duplicate',
      details: { key: 'value' },
      timestamp: err.timestamp,
    });
  });
});

describe('parseErrorResponse', () => {
  it('passes through a response already in ErrorResponse format', () => {
    const err = parseErrorResponse({
      status: 400,
      errorCode: 'BAD_REQUEST',
      message: 'Missing field',
      details: 'account_type',
    });
    expect(err).toBeInstanceOf(ApplicationException);
    expect(err.statusCode).toBe(400);
    expect(err.errorCode).toBe('BAD_REQUEST');
    expect(err.message).toBe('Missing field');
    expect(err.details).toBe('account_type');
  });

  it('unwraps an axios-style error (response.data)', () => {
    const err = parseErrorResponse({
      response: { data: { status: 422, errorCode: 'UNPROCESSABLE_ENTITY', message: 'Bad payload' } },
    });
    expect(err.statusCode).toBe(422);
    expect(err.errorCode).toBe('UNPROCESSABLE_ENTITY');
    expect(err.message).toBe('Bad payload');
  });

  it('falls back to a generic 500 for a plain Error with no recognizable shape', () => {
    const err = parseErrorResponse(new Error('network down'));
    expect(err.statusCode).toBe(HTTP_STATUS.INTERNAL_SERVER_ERROR);
    expect(err.errorCode).toBe(ERROR_CODES.INTERNAL_SERVER_ERROR);
    expect(err.message).toBe(ERROR_MESSAGES.SERVER_ERROR);
  });

  it('falls back to a generic 500 for undefined input', () => {
    const err = parseErrorResponse(undefined);
    expect(err.statusCode).toBe(HTTP_STATUS.INTERNAL_SERVER_ERROR);
    expect(err.message).toBe(ERROR_MESSAGES.SERVER_ERROR);
  });

  it('captures a numeric HTTP Response-style status with no body', () => {
    const err = parseErrorResponse({ status: 503 });
    expect(err.statusCode).toBe(503);
  });

  it('passes an already-parsed ApplicationException through unchanged', () => {
    const original = new ApplicationException(409, 'CONFLICT', 'Already exists');
    expect(parseErrorResponse(original)).toBe(original);
  });
});

describe('getErrorMessage', () => {
  const originalOnLine = navigator.onLine;

  afterEach(() => {
    Object.defineProperty(navigator, 'onLine', { value: originalOnLine, configurable: true });
  });

  it('returns the network-error message when offline', () => {
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true });
    expect(getErrorMessage(new Error('irrelevant'))).toBe(ERROR_MESSAGES.NETWORK_ERROR);
  });

  it('returns the ApplicationException message directly when online', () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    const err = new ApplicationException(404, 'NOT_FOUND', 'Custom not found message');
    expect(getErrorMessage(err)).toBe('Custom not found message');
  });

  it.each([
    [HTTP_STATUS.UNAUTHORIZED, ERROR_MESSAGES.AUTH_FAILED],
    [HTTP_STATUS.FORBIDDEN, ERROR_MESSAGES.UNAUTHORIZED],
    [HTTP_STATUS.NOT_FOUND, ERROR_MESSAGES.NOT_FOUND],
    [HTTP_STATUS.INTERNAL_SERVER_ERROR, ERROR_MESSAGES.SERVER_ERROR],
    [HTTP_STATUS.SERVICE_UNAVAILABLE, ERROR_MESSAGES.SERVER_ERROR],
  ])('maps statusCode %i to the correct friendly message', (statusCode, expected) => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    expect(getErrorMessage({ statusCode })).toBe(expected);
  });

  it('prefers the error message over the generic validation message for 400/422', () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    expect(getErrorMessage({ statusCode: HTTP_STATUS.BAD_REQUEST, message: 'account_name is required' }))
      .toBe('account_name is required');
  });

  it('falls back to the generic validation message for 400 with no message', () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    expect(getErrorMessage({ statusCode: HTTP_STATUS.BAD_REQUEST })).toBe(ERROR_MESSAGES.VALIDATION_ERROR);
  });

  it('reads an axios-style nested response.data.message', () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    expect(getErrorMessage({ response: { data: { message: 'nested message' } } })).toBe('nested message');
  });

  it('reads a plain Error.message', () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    expect(getErrorMessage(new Error('plain error'))).toBe('plain error');
  });

  it('returns a raw string error unchanged', () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    expect(getErrorMessage('already a string')).toBe('already a string');
  });

  it('falls back to the generic server error for an unrecognized shape', () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    expect(getErrorMessage({})).toBe(ERROR_MESSAGES.SERVER_ERROR);
  });
});

describe('logError', () => {
  const originalEnv = process.env.NODE_ENV;
  let consoleErrorSpy;

  beforeEach(() => {
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
    process.env.NODE_ENV = originalEnv;
  });

  it('logs to console.error in development', () => {
    process.env.NODE_ENV = 'development';
    const err = new Error('dev error');
    logError('Accounts.load', err);
    expect(consoleErrorSpy).toHaveBeenCalledWith('[Accounts.load]', err);
  });

  it('does not log in production', () => {
    process.env.NODE_ENV = 'production';
    logError('Accounts.load', new Error('prod error'));
    expect(consoleErrorSpy).not.toHaveBeenCalled();
  });
});

describe('createApplicationException', () => {
  it('builds an ApplicationException from primitives', () => {
    const err = createApplicationException(409, 'CONFLICT', 'Already exists', 'email');
    expect(err).toBeInstanceOf(ApplicationException);
    expect(err.statusCode).toBe(409);
    expect(err.errorCode).toBe('CONFLICT');
    expect(err.message).toBe('Already exists');
    expect(err.details).toBe('email');
  });

  it('defaults details to null when omitted', () => {
    const err = createApplicationException(500, 'INTERNAL_SERVER_ERROR', 'Boom');
    expect(err.details).toBeNull();
  });
});

describe('handleAPIResponse', () => {
  it('returns parsed JSON when the response is ok', async () => {
    const response = { ok: true, json: jest.fn().mockResolvedValue({ id: '123' }) };
    await expect(handleAPIResponse(response)).resolves.toEqual({ id: '123' });
  });

  it('returns null when an ok response has no JSON body', async () => {
    const response = { ok: true, json: jest.fn().mockRejectedValue(new Error('no body')) };
    await expect(handleAPIResponse(response)).resolves.toBeNull();
  });

  it('throws a parsed ApplicationException when the response is not ok and has a JSON error body', async () => {
    const response = {
      ok: false,
      status: 404,
      json: jest.fn().mockResolvedValue({ status: 404, errorCode: 'NOT_FOUND', message: 'Account not found' }),
    };
    await expect(handleAPIResponse(response)).rejects.toMatchObject({
      statusCode: 404,
      errorCode: 'NOT_FOUND',
      message: 'Account not found',
    });
  });

  it('throws a generic ApplicationException when the error response body is not valid JSON', async () => {
    const response = {
      ok: false,
      status: 500,
      json: jest.fn().mockRejectedValue(new Error('not json')),
    };
    await expect(handleAPIResponse(response)).rejects.toMatchObject({
      statusCode: 500,
      errorCode: ERROR_CODES.INTERNAL_SERVER_ERROR,
    });
  });
});

describe('retryWithBackoff', () => {
  it('returns the result immediately on first success, without retrying', async () => {
    const fn = jest.fn().mockResolvedValue('ok');
    await expect(retryWithBackoff(fn, 3, 1)).resolves.toBe('ok');
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it('retries on failure and succeeds on a later attempt', async () => {
    const fn = jest
      .fn()
      .mockRejectedValueOnce(new Error('fail 1'))
      .mockResolvedValueOnce('recovered');
    await expect(retryWithBackoff(fn, 3, 1)).resolves.toBe('recovered');
    expect(fn).toHaveBeenCalledTimes(2);
  });

  it('throws the last error, parsed via parseErrorResponse, after exhausting all attempts', async () => {
    // retryWithBackoff runs every caught error through parseErrorResponse, which does not
    // recognize a plain Error's .message field, so a plain Error becomes the generic 500 —
    // this asserts that real (if perhaps surprising) behavior, not a hypothetical pass-through.
    const fn = jest.fn().mockRejectedValue(new Error('always fails'));
    await expect(retryWithBackoff(fn, 2, 1)).rejects.toMatchObject({
      statusCode: HTTP_STATUS.INTERNAL_SERVER_ERROR,
      errorCode: ERROR_CODES.INTERNAL_SERVER_ERROR,
      message: ERROR_MESSAGES.SERVER_ERROR,
    });
    expect(fn).toHaveBeenCalledTimes(2);
  });

  it('preserves the real ApplicationException shape (status/code/message) when the failing call already throws one', async () => {
    const original = new ApplicationException(409, 'CONFLICT', 'Already exists');
    const fn = jest.fn().mockRejectedValue(original);
    await expect(retryWithBackoff(fn, 2, 1)).rejects.toMatchObject({
      statusCode: 409,
      errorCode: 'CONFLICT',
      message: 'Already exists',
    });
  });
});
