const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';

const defaultHeaders = {
  'Content-Type': 'application/json',
};

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    headers: {
      ...defaultHeaders,
      ...(options.headers || {}),
    },
    ...options,
  });

  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('application/json')
    ? await response.json().catch(() => null)
    : null;

  if (!response.ok) {
    const error = new Error(body?.message || response.statusText || 'Request failed');
    error.status = response.status;
    error.body = body;
    throw error;
  }

  return body;
}

export function get(path) {
  return request(path, { method: 'GET' });
}

export function post(path, data) {
  return request(path, { method: 'POST', body: JSON.stringify(data) });
}
