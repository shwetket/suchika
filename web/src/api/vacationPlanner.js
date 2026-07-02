import { post } from './client';
import { API_ENDPOINTS } from '../utils/constants';

export const checkVacationBudget = (profileId, payload) =>
  post(`${API_ENDPOINTS.VACATION_PLANNER_BUDGET_CHECK}?profile_id=${profileId}`, payload);
