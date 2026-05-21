import { get } from './client';
import { API_ENDPOINTS } from '../utils/constants';

export const listAdmins = () => get(API_ENDPOINTS.ADMINS);
