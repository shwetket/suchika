/**
 * Application Constants
 */

export const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';
export const API_TIMEOUT = parseInt(process.env.REACT_APP_API_TIMEOUT || '10000', 10);

export const USER_ROLES = {
  ADMIN: 'admin',
  USER: 'user',
  CHILD: 'child',
};

export const API_ENDPOINTS = {
  // Auth
  LOGIN: '/api/v1/auth/login',
  LOGOUT: '/api/v1/auth/logout',
  AUTH_PROFILE: '/api/v1/auth/profile',

  // Wealth
  ACCOUNTS: '/api/v1/wealth/accounts',
  TRANSACTIONS: '/api/v1/wealth/transactions',
  DASHBOARD: '/api/v1/wealth/dashboard',

  // Household
  PROFILES: '/api/v1/household/profiles',
  EVENTS: '/api/v1/household/events',
  INVENTORY: '/api/v1/household/inventory',

  // Health
  VITALS: '/api/v1/health/vitals',
  DOCTORS: '/api/v1/health/doctors',
  HEALTH_PROFILE: '/api/v1/health/profile',
};

export const STORAGE_KEYS = {
  TOKEN: 'auth_token',
  USER: 'auth_user',
  PREFERENCES: 'user_preferences',
};

export const ROUTE_PATHS = {
  HOME: '/',
  SIGNIN: '/signin',
  SIGNUP: '/signup',

  DASHBOARD: '/dashboard',

  WEALTH: '/wealth',
  WEALTH_ACCOUNTS: '/wealth/accounts',
  WEALTH_TRANSACTIONS: '/wealth/transactions',
  WEALTH_REPORTS: '/wealth/reports',

  HOUSEHOLD: '/household',
  HOUSEHOLD_PROFILES: '/household/profiles',
  HOUSEHOLD_CALENDAR: '/household/calendar',
  HOUSEHOLD_INVENTORY: '/household/inventory',

  HEALTH: '/health',
  HEALTH_VITALS: '/health/vitals',
  HEALTH_DOCTORS: '/health/doctors',
  HEALTH_PROFILE: '/health/profile',

  ADMIN: '/admin',
  ADMIN_USERS: '/admin/users',
  ADMIN_SETTINGS: '/admin/settings',
  ADMIN_REPORTS: '/admin/reports',
};

export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Network error. Please check your connection.',
  AUTH_FAILED: 'Authentication failed. Please sign in again.',
  UNAUTHORIZED: 'You do not have permission to access this resource.',
  NOT_FOUND: 'The requested resource was not found.',
  SERVER_ERROR: 'Server error. Please try again later.',
  VALIDATION_ERROR: 'Please check your input and try again.',
};

export const SUCCESS_MESSAGES = {
  LOGIN_SUCCESS: 'Login successful!',
  LOGOUT_SUCCESS: 'Logged out successfully.',
  CREATE_SUCCESS: 'Created successfully.',
  UPDATE_SUCCESS: 'Updated successfully.',
  DELETE_SUCCESS: 'Deleted successfully.',
};
