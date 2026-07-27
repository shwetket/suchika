import { get, post } from './client';
import { API_ENDPOINTS } from '../utils/constants';

export const listAdmins = () => get(API_ENDPOINTS.ADMINS);

export const createAdmin = (data) => post(API_ENDPOINTS.ADMINS, data);
