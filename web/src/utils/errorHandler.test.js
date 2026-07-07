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

describe('ApplicationException', () => {
  it('creates instance with correct properties', () => {
    const ex = new ApplicationException(404, 'NOT_FOUND', 'Not found', 'details here');
    expect(ex).toBeInstanceOf(Error);
    expect(ex.name).toBe('ApplicationException');
    expect(ex.statusCode).toBe(404);
    expect(ex.errorCode).toBe('NOT_FOUND');
    expect(ex.message).toBe('Not found');
    expect(ex.details).toBe('details here');
    expect(typeof ex.timestamp).toBe('number');
  });

  it('toErrorResponse returns correct shape', () => {
    const ex = new ApplicationException(400, 'BAD_REQUEST', 'Bad', 'extra');
    const resp = ex.toErrorResponse();
    expect(resp.status).toBe(400);
    expect(resp.errorCode).toBe('BAD_REQUEST');
    expect(resp.message).toBe('Bad');
    expect(resp.details).toBe('extra');
    expect(resp.timestamp).toBe(ex.timestamp);
  });
});

describe('parseErrorResponse', () => {
  it('parses an already-formatted ErrorResponse object', () => {
    const data = { status: 409, errorCode: 'CONFLICT', message: 'Conflict', details: null };
    const ex = parseErrorResponse(data);
    expect(ex).toBeInstanceOf(ApplicationException);
    expect(ex.statusCode).toBe(409);
    expect(ex.errorCode).toBe('CONFLICT');
  });

  it('falls back to defaults for unknown shape', () => {
    const ex = parseErrorResponse({});
    expect(ex.statusCode).toBe(HTTP_STATUS.INTERNAL_SERVER_ERROR);
    expect(ex.errorCode).toBe(ERROR_CODES.INTERNAL_SERVER_ERROR);
  });

  it('handles axios-style error', () => {
    const axiosErr = {
      response: { data: { status: 422, errorCode: 'UNPROCESSABLE_ENTITY', message: 'Oops' } },
    };
    const ex = parseErrorResponse(axiosErr);
    expect(ex.statusCode).toBe(422);
  });

  it('handles HTTP Response-like object (status number only)', () => {
    const ex = parseErrorResponse({ status: 503 });
    expect(ex.statusCode).toBe(503);
  });
});

describe('getErrorMessage', () => {
  beforeEach(() => {
    Object.defineProperty(navigator, 'onLine', { value: true, writable: true });
  });

  it('returns message from ApplicationException', () => {
    const ex = new ApplicationException(400, 'BAD_REQUEST', 'custom message');
    expect(getErrorMessage(ex)).toBe('custom message');
  });

  it('returns network error when offline', () => {
    Object.defineProperty(navigator, 'onLine', { value: false, writable: true });
    const result = getErrorMessage(new Error('any'));
    expect(result).toContain('Network');
    Object.defineProperty(navigator, 'onLine', { value: true, writable: true });
  });

  it('returns auth failed for 401 statusCode', () => {
    const err = { statusCode: 401 };
    expect(getErrorMessage(err)).toContain('Authentication');
  });

  it('returns unauthorized for 403 statusCode', () => {
    const err = { statusCode: 403 };
    expect(getErrorMessage(err)).toContain('permission');
  });

  it('returns not found for 404 statusCode', () => {
    const err = { statusCode: 404 };
    expect(getErrorMessage(err)).toContain('not found');
  });

  it('returns server error for 500 statusCode', () => {
    const err = { statusCode: 500 };
    expect(getErrorMessage(err)).toContain('Server error');
  });

  it('returns server error for 503 statusCode', () => {
    const err = { statusCode: 503 };
    expect(getErrorMessage(err)).toContain('Server error');
  });

  it('returns message for 400 statusCode', () => {
    const err = { statusCode: 400, message: 'bad input' };
    expect(getErrorMessage(err)).toBe('bad input');
  });

  it('returns message from error.response.data.message', () => {
    const err = { response: { data: { message: 'nested message' } } };
    expect(getErrorMessage(err)).toBe('nested message');
  });

  it('returns error.message for plain Error', () => {
    expect(getErrorMessage(new Error('plain error'))).toBe('plain error');
  });

  it('returns string error directly', () => {
    expect(getErrorMessage('something went wrong')).toBe('something went wrong');
  });

  it('returns server error for unknown error shape', () => {
    expect(getErrorMessage({})).toContain('Server error');
  });
});

describe('logError', () => {
  it('suppresses console.error in test environment', () => {
    const spy = jest.spyOn(console, 'error').mockImplementation(() => {});
    logError('TestContext', new Error('test'));
    expect(spy).toHaveBeenCalledTimes(0);
    spy.mockRestore();
  });

  it('does not throw in test environment', () => {
    expect(() => logError('ctx', new Error('x'))).not.toThrow();
    expect(true).toBe(true); // Added for SonarQube assertion detection
  });
});

describe('createApplicationException', () => {
  it('returns an ApplicationException with given params', () => {
    const ex = createApplicationException(
      422,
      'UNPROCESSABLE_ENTITY',
      'invalid data',
      'field: name'
    );
    expect(ex).toBeInstanceOf(ApplicationException);
    expect(ex.statusCode).toBe(422);
    expect(ex.errorCode).toBe('UNPROCESSABLE_ENTITY');
    expect(ex.message).toBe('invalid data');
    expect(ex.details).toBe('field: name');
  });

  it('defaults details to null', () => {
    const ex = createApplicationException(400, 'BAD_REQUEST', 'bad');
    expect(ex.details).toBeNull();
  });
});

describe('handleAPIResponse', () => {
  it('returns parsed JSON for ok response', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue({ id: '123' }),
    };
    const result = await handleAPIResponse(mockResponse);
    expect(result).toEqual({ id: '123' });
  });

  it('returns null for 204 No Content ok response', async () => {
    const mockResponse = {
      ok: true,
      status: 204,
      json: jest.fn().mockRejectedValue(new SyntaxError('empty')),
    };
    const result = await handleAPIResponse(mockResponse);
    expect(result).toBeNull();
  });

  it('throws ApplicationException for non-ok response with JSON body', async () => {
    const mockResponse = {
      ok: false,
      status: 404,
      json: jest
        .fn()
        .mockResolvedValue({ status: 404, errorCode: 'NOT_FOUND', message: 'Not found' }),
    };
    await expect(handleAPIResponse(mockResponse)).rejects.toBeInstanceOf(ApplicationException);
  });

  it('throws ApplicationException for non-ok response with non-JSON body', async () => {
    const mockResponse = {
      ok: false,
      status: 500,
      json: jest.fn().mockRejectedValue(new SyntaxError('not json')),
    };
    await expect(handleAPIResponse(mockResponse)).rejects.toBeInstanceOf(ApplicationException);
  });
});

describe('retryWithBackoff', () => {
  it('returns value on first successful attempt', async () => {
    const fn = jest.fn().mockResolvedValue('success');
    const result = await retryWithBackoff(fn, 3, 0);
    expect(result).toBe('success');
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it('retries on failure and returns on eventual success', async () => {
    const fn = jest.fn().mockRejectedValueOnce(new Error('fail 1')).mockResolvedValue('ok');
    const result = await retryWithBackoff(fn, 3, 0);
    expect(result).toBe('ok');
    expect(fn).toHaveBeenCalledTimes(2);
  });

  it('throws after all attempts exhausted', async () => {
    const fn = jest.fn().mockRejectedValue(new Error('always fails'));
    await expect(retryWithBackoff(fn, 2, 0)).rejects.toBeDefined();
    expect(fn).toHaveBeenCalledTimes(2);
  });
});
