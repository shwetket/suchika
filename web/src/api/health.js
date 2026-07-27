import { get, post, patch, del } from './client';
import { API_ENDPOINTS } from '../utils/constants';

export function listVitals(profileId, vitalType, page, size) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  if (vitalType !== null && vitalType !== undefined) params.append('vital_type', vitalType);
  if (page !== null && page !== undefined) params.append('page', page);
  if (size !== null && size !== undefined) params.append('size', size);
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.VITALS}?${query}` : API_ENDPOINTS.VITALS;
  return get(url);
}

export const recordVital = (data) => post(API_ENDPOINTS.VITALS, data);

export const updateVital = (id, data) => patch(`${API_ENDPOINTS.VITALS}/${id}`, data);

export const deleteVital = (id) => del(`${API_ENDPOINTS.VITALS}/${id}`);

export function listDoctorVisits(profileId, from, to, page, size) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  if (from !== null && from !== undefined) params.append('from', from);
  if (to !== null && to !== undefined) params.append('to', to);
  if (page !== null && page !== undefined) params.append('page', page);
  if (size !== null && size !== undefined) params.append('size', size);
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.DOCTOR_VISITS}?${query}` : API_ENDPOINTS.DOCTOR_VISITS;
  return get(url);
}

export const createDoctorVisit = (data) => post(API_ENDPOINTS.DOCTOR_VISITS, data);

export const updateDoctorVisit = (id, data) => patch(`${API_ENDPOINTS.DOCTOR_VISITS}/${id}`, data);

export const deleteDoctorVisit = (id) => del(`${API_ENDPOINTS.DOCTOR_VISITS}/${id}`);
