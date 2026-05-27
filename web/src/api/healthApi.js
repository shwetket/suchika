import { get } from './client';

/**
 * Health profile contract from backend OpenAPI.
 * @typedef {Object} HealthProfile
 * @property {string} name
 * @property {string} profile_id
 * @property {string} display_name
 * @property {string} relationship
 * @property {string|null} date_of_birth
 * @property {string|null} blood_group
 * @property {string} created_at
 */

/**
 * @typedef {Object} ListHealthProfilesResponse
 * @property {HealthProfile[]} health_profiles
 */

/**
 * List health profiles for the current user.
 * This uses the shared backend contract for /v1/health-profiles.
 * @returns {Promise<ListHealthProfilesResponse>}
 */
export function listHealthProfiles() {
  return get('/v1/health-profiles');
}
