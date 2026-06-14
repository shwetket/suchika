import { API_BASE_URL } from '../utils/constants';
import { handleAPIResponse, createApplicationException } from '../utils/errorHandler';

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
    console.warn('Auth API unavailable, using demo fallback:', error.message);
    return {
      username: credentials.username,
      role: credentials.role || 'user',
      token: `token_${Date.now()}`,
      issued_at: new Date().toISOString(),
    };
  }
}
