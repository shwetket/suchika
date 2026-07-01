import { get, post, patch, del } from './client';
import { API_ENDPOINTS } from '../utils/constants';

export function listProfiles(adminId, isActive) {
  const params = new URLSearchParams();
  if (adminId !== null && adminId !== undefined) params.append('admin_id', adminId);
  if (isActive !== null && isActive !== undefined) params.append('is_active', String(isActive));
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.PROFILES}?${query}` : API_ENDPOINTS.PROFILES;
  return get(url);
}

export const getProfile = (profileId) => get(`${API_ENDPOINTS.PROFILES}/${profileId}`);

export const createProfile = (data) => post(API_ENDPOINTS.PROFILES, data);

export const updateProfile = (profileId, data) =>
  patch(`${API_ENDPOINTS.PROFILES}/${profileId}`, data);

export const deactivateProfile = (profileId) => del(`${API_ENDPOINTS.PROFILES}/${profileId}`);

export const getAdmin = (adminId) => get(`${API_ENDPOINTS.ADMINS}/${adminId}`);

export const updateAdminPolicy = (adminId, policySettings) =>
  patch(`${API_ENDPOINTS.ADMINS}/${adminId}/policy`, { policy_settings: policySettings });
