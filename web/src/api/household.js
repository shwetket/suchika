import { get, post, put, patch, del } from './client';
import { API_ENDPOINTS } from '../utils/constants';

// ── Calendar Events ──────────────────────────────────────────────────────────

export function listCalendarEvents(profileId, eventType, page, size) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  if (eventType !== null && eventType !== undefined) params.append('event_type', eventType);
  if (page !== null && page !== undefined) params.append('page', page);
  if (size !== null && size !== undefined) params.append('size', size);
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.CALENDAR_EVENTS}?${query}` : API_ENDPOINTS.CALENDAR_EVENTS;
  return get(url);
}

export const createCalendarEvent = (data) => post(API_ENDPOINTS.CALENDAR_EVENTS, data);

export const updateCalendarEvent = (id, data) =>
  patch(`${API_ENDPOINTS.CALENDAR_EVENTS}/${id}`, data);

export const deleteCalendarEvent = (id) => del(`${API_ENDPOINTS.CALENDAR_EVENTS}/${id}`);

// ── Inventory Items ──────────────────────────────────────────────────────────

export function listInventoryItems(profileId, sourcePlatform, page, size) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  if (sourcePlatform !== null && sourcePlatform !== undefined)
    params.append('source_platform', sourcePlatform);
  if (page !== null && page !== undefined) params.append('page', page);
  if (size !== null && size !== undefined) params.append('size', size);
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.INVENTORY_ITEMS}?${query}` : API_ENDPOINTS.INVENTORY_ITEMS;
  return get(url);
}

export const createInventoryItem = (data) => post(API_ENDPOINTS.INVENTORY_ITEMS, data);

export const updateInventoryItem = (id, data) =>
  put(`${API_ENDPOINTS.INVENTORY_ITEMS}/${id}`, data);

export const deleteInventoryItem = (id) => del(`${API_ENDPOINTS.INVENTORY_ITEMS}/${id}`);

// ── Goals ────────────────────────────────────────────────────────────────────

export function listGoals(profileId, status, page, size) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  if (status !== null && status !== undefined) params.append('status', status);
  if (page !== null && page !== undefined) params.append('page', page);
  if (size !== null && size !== undefined) params.append('size', size);
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.HOUSEHOLD_GOALS}?${query}` : API_ENDPOINTS.HOUSEHOLD_GOALS;
  return get(url);
}

export const createGoal = (data) => post(API_ENDPOINTS.HOUSEHOLD_GOALS, data);

export const updateGoal = (id, data) => patch(`${API_ENDPOINTS.HOUSEHOLD_GOALS}/${id}`, data);

export const deleteGoal = (id) => del(`${API_ENDPOINTS.HOUSEHOLD_GOALS}/${id}`);

// ── Projections ──────────────────────────────────────────────────────────────

export const refreshProjections = (profileId) =>
  post(`${API_ENDPOINTS.PROJECTIONS_REFRESH}/${profileId}`);

export const getDashboard = (profileId) =>
  get(`${API_ENDPOINTS.PROJECTIONS_DASHBOARD}/${profileId}`);
