import { get, post } from './client';
import { API_ENDPOINTS } from '../utils/constants';

// ── Application Console (admin-only, Phase 4) ───────────────────────────────
// Backend gates every endpoint behind suchika.console.enabled (404 when off).

export const getConsoleStatus = () => get(API_ENDPOINTS.CONSOLE_STATUS);

export const startConsoleService = (name) =>
  post(`${API_ENDPOINTS.CONSOLE_SERVICES}/${name}/start`);

export const stopConsoleService = (name) => post(`${API_ENDPOINTS.CONSOLE_SERVICES}/${name}/stop`);

export function getConsoleErrors(since, limit) {
  const params = new URLSearchParams();
  if (since !== null && since !== undefined) params.append('since', since);
  if (limit !== null && limit !== undefined) params.append('limit', limit);
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.CONSOLE_ERRORS}?${query}` : API_ENDPOINTS.CONSOLE_ERRORS;
  return get(url);
}
