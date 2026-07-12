import { API_BASE_URL } from '../utils/constants';
import { handleAPIResponse, createApplicationException, logError } from '../utils/errorHandler';

/**
 * Sign in with backend auth endpoint.
 * Falls back to a local demo response if backend is unavailable.
 * @param {object} credentials
 * @returns {Promise<object>}
 */
export async function signIn(credentials) {
  try {
    const response = await fetch(`${API_BASE_URL}/v1/auth/signin`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(credentials),
      credentials: 'include',
    });

    if (!response.ok) {
      const body = await response.json().catch(() => null);
      throw createApplicationException(
        response.status,
        body?.errorCode || 'AUTH_FAILED',
        body?.message || 'Authentication failed'
      );
    }

    return await handleAPIResponse(response);
  } catch (error) {
    if (error?.status && error.status !== 502 && error.status !== 504 && error.status !== 503) {
      throw error;
    }
    console.warn('Falling back to demo auth due to error:', error);
    logError('auth', error);
    return {
      username: credentials.username,
      role: credentials.role || 'user',
      token: `token_${Date.now()}`,
      issued_at: new Date().toISOString(),
    };
  }
}
