import { API_BASE_URL, API_TIMEOUT } from '../utils/constants';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

function authHeader() {
  try {
    const stored = localStorage.getItem('user');
    const { token } = stored ? JSON.parse(stored) : {};
    return token ? { Authorization: `Bearer ${token}` } : {};
  } catch {
    return {};
  }
}

async function request(method, path, body, options = {}) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT);

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers: { ...JSON_HEADERS, ...authHeader(), ...options.headers },
      body: body == null ? undefined : JSON.stringify(body),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      // Try to parse structured error body from backend (ApplicationException shape)
      const err = await response.json().catch(() => ({}));
      const error = new Error(err.message || `HTTP ${response.status}`);
      error.status = response.status;
      error.code = err.code;
      error.details = err.details;
      throw error;
    }

    return response.status === 204 ? null : response.json();
  } catch (err) {
    clearTimeout(timeoutId);
    if (err.name === 'AbortError') throw new Error('Request timed out');
    throw err;
  }
}

export const get = (path, options) => request('GET', path, null, options);
export const post = (path, body, options) => request('POST', path, body, options);
export const patch = (path, body, options) => request('PATCH', path, body, options);
export const del = (path, options) => request('DELETE', path, null, options);
