import { get, post, patch, del } from './client';
import { API_ENDPOINTS } from '../utils/constants';

export function listVitals(profileId, vitalType) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  if (vitalType !== null && vitalType !== undefined) params.append('vital_type', vitalType);
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.VITALS}?${query}` : API_ENDPOINTS.VITALS;
  return get(url);
}

export const recordVital = (data) => post(API_ENDPOINTS.VITALS, data);

export const deleteVital = (id) => del(`${API_ENDPOINTS.VITALS}/${id}`);

export function listDoctorVisits(profileId) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.DOCTOR_VISITS}?${query}` : API_ENDPOINTS.DOCTOR_VISITS;
  return get(url);
}

export const createDoctorVisit = (data) => post(API_ENDPOINTS.DOCTOR_VISITS, data);

export const updateDoctorVisit = (id, data) => patch(`${API_ENDPOINTS.DOCTOR_VISITS}/${id}`, data);

export const deleteDoctorVisit = (id) => del(`${API_ENDPOINTS.DOCTOR_VISITS}/${id}`);
