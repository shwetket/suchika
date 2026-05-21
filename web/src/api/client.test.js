import { get, post, patch, del } from './client';

const MOCK_BASE = 'http://localhost:8080';

describe('API client', () => {
  beforeEach(() => {
    localStorage.setItem('user', JSON.stringify({ token: 'test-token' }));
    jest.useFakeTimers();
  });

  afterEach(() => {
    localStorage.clear();
    jest.useRealTimers();
    global.fetch = undefined;
  });

  function mockFetch(responseOverrides = {}) {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue({ data: 'ok' }),
      ...responseOverrides,
    });
  }

  describe('get()', () => {
    it('calls fetch with GET method and correct URL', async () => {
      mockFetch();
      const promise = get('/v1/profiles');
      jest.runAllTimers();
      await promise;
      expect(global.fetch).toHaveBeenCalledWith(
        `${MOCK_BASE}/v1/profiles`,
        expect.objectContaining({ method: 'GET' })
      );
    });

    it('includes Authorization header when token in localStorage', async () => {
      mockFetch();
      const promise = get('/v1/profiles');
      jest.runAllTimers();
      await promise;
      const [, options] = global.fetch.mock.calls[0];
      expect(options.headers).toMatchObject({ Authorization: 'Bearer test-token' });
    });

    it('returns parsed JSON on success', async () => {
      mockFetch();
      const promise = get('/v1/profiles');
      jest.runAllTimers();
      const result = await promise;
      expect(result).toEqual({ data: 'ok' });
    });

    it('returns null for 204 response', async () => {
      mockFetch({ status: 204, json: jest.fn() });
      const promise = get('/v1/profiles');
      jest.runAllTimers();
      const result = await promise;
      expect(result).toBeNull();
    });

    it('throws Error on non-ok response', async () => {
      global.fetch = jest.fn().mockResolvedValue({
        ok: false,
        status: 404,
        json: jest.fn().mockResolvedValue({ message: 'Not found' }),
      });
      const promise = get('/v1/missing');
      jest.runAllTimers();
      await expect(promise).rejects.toThrow('Not found');
    });

    it('throws timeout error when request is aborted', async () => {
      const abortError = new Error('AbortError');
      abortError.name = 'AbortError';
      global.fetch = jest.fn().mockRejectedValue(abortError);
      const promise = get('/v1/slow');
      jest.runAllTimers();
      await expect(promise).rejects.toThrow('Request timed out');
    });
  });

  describe('post()', () => {
    it('calls fetch with POST method and JSON body', async () => {
      mockFetch();
      const promise = post('/v1/profiles', { full_name: 'Alice' });
      jest.runAllTimers();
      await promise;
      const [, options] = global.fetch.mock.calls[0];
      expect(options.method).toBe('POST');
      expect(options.body).toBe(JSON.stringify({ full_name: 'Alice' }));
    });
  });

  describe('patch()', () => {
    it('calls fetch with PATCH method', async () => {
      mockFetch();
      const promise = patch('/v1/profiles/123', { email_address: 'a@b.com' });
      jest.runAllTimers();
      await promise;
      const [, options] = global.fetch.mock.calls[0];
      expect(options.method).toBe('PATCH');
    });
  });

  describe('del()', () => {
    it('calls fetch with DELETE method and no body', async () => {
      mockFetch();
      const promise = del('/v1/profiles/123');
      jest.runAllTimers();
      await promise;
      const [, options] = global.fetch.mock.calls[0];
      expect(options.method).toBe('DELETE');
      expect(options.body).toBeUndefined();
    });
  });

  describe('auth header fallback', () => {
    it('sends no Authorization header when localStorage is empty', async () => {
      localStorage.clear();
      mockFetch();
      const promise = get('/v1/test');
      jest.runAllTimers();
      await promise;
      const [, options] = global.fetch.mock.calls[0];
      expect(options.headers.Authorization).toBeUndefined();
    });

    it('sends no Authorization header when localStorage has invalid JSON', async () => {
      localStorage.setItem('user', 'not-json');
      mockFetch();
      const promise = get('/v1/test');
      jest.runAllTimers();
      await promise;
      const [, options] = global.fetch.mock.calls[0];
      expect(options.headers.Authorization).toBeUndefined();
    });
  });
});
