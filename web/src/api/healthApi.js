import { get, post } from './client';

/**
 * @typedef {import('./generated').HealthProfile} HealthProfile
 * @typedef {import('./generated').ListHealthProfilesResponse} ListHealthProfilesResponse
 * @typedef {import('./generated').CreateHealthProfileRequest} CreateHealthProfileRequest
 */

/**
 * List health profiles for the current user.
 * This uses the shared backend contract for /v1/health-profiles.
 * @returns {Promise<ListHealthProfilesResponse>}
 */
export function listHealthProfiles() {
  return /** @type {Promise<ListHealthProfilesResponse>} */ (get('/v1/health-profiles'));
}

/**
 * Create a new health profile using the shared contract.
 * @param {CreateHealthProfileRequest} data
 * @returns {Promise<HealthProfile>}
 */
export function createHealthProfile(data) {
  return /** @type {Promise<HealthProfile>} */ (post('/v1/health-profiles', data));
}
