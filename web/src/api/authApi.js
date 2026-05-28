import { post } from './client';

/**
 * @typedef {import('./generated').AuthCredentials} AuthCredentials
 * @typedef {import('./generated').AuthResponse} AuthResponse
 */

/**
 * Sign in with backend auth contract.
 * Falls back to local demo response if backend is unavailable.
 * @param {AuthCredentials} credentials
 * @returns {Promise<AuthResponse>}
 */
export async function signIn(credentials) {
  try {
    return /** @type {Promise<AuthResponse>} */ (await post('/v1/auth/signin', credentials));
  } catch (error) {
    return {
      username: credentials.username,
      role: credentials.role || 'user',
      token: `token_${Date.now()}`,
      issued_at: new Date().toISOString(),
    };
  }
}
