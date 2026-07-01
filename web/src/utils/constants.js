/**
 * Application Constants
 *
 * Domain base URLs — each Quarkus module runs on its own port in dev mode.
 * The web-gateway (BFF) on port 8080 will eventually proxy all domains,
 * making API_BASE_URL the only URL the frontend needs.
 *
 * Port mapping (from OpenAPI server URLs):
 *   profile   → 8081
 *   wealth    → 8082
 *   health    → 8083
 *   household → 8084
 *   gateway   → 8080  ← target for all frontend calls once gateway is built
 */
export const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';
export const API_TIMEOUT = Number.parseInt(process.env.REACT_APP_API_TIMEOUT || '10000', 10);

export const USER_ROLES = {
  ADMIN: 'admin',
  USER: 'user',
};

export const API_ENDPOINTS = {
  // Auth (will live in gateway)
  LOGIN: '/v1/auth/login',
  LOGOUT: '/v1/auth/logout',

  // Profile domain
  ADMINS: '/v1/admins',
  PROFILES: '/v1/profiles',

  // Wealth domain
  ACCOUNTS: '/v1/accounts',
  TRANSACTIONS: '/v1/transactions',
  PHYSICAL_ASSETS: '/v1/physical-assets',

  // Household domain (via gateway)
  CALENDAR_EVENTS: '/v1/household/calendar-events',
  INVENTORY_ITEMS: '/v1/household/inventory-items',
  HOUSEHOLD_GOALS: '/v1/household/goals',

  // Projections (via gateway)
  PROJECTIONS_REFRESH: '/v1/projections/refresh',
  PROJECTIONS_DASHBOARD: '/v1/projections/dashboard',

  // Health domain
  VITALS: '/v1/vitals',
  DOCTOR_VISITS: '/v1/doctor-visits',
};

export const STORAGE_KEYS = {
  TOKEN: 'auth_token',
  USER: 'user',
  THEME: 'theme',
};

export const ROUTE_PATHS = {
  HOME: '/',
  SIGNIN: '/signin',
  SIGNUP: '/signup',

  DASHBOARD: '/dashboard',

  WEALTH_ACCOUNTS: '/wealth/accounts',
  WEALTH_TRANSACTIONS: '/wealth/transactions',
  WEALTH_REPORTS: '/wealth/reports',
  WEALTH_PHYSICAL_ASSETS: '/wealth/physical-assets',

  HOUSEHOLD_PROFILES: '/household/profiles',
  HOUSEHOLD_CALENDAR: '/household/calendar',
  HOUSEHOLD_INVENTORY: '/household/inventory',
  HOUSEHOLD_GOALS: '/household/goals',

  HEALTH_VITALS: '/health/vitals',
  HEALTH_DOCTORS: '/health/doctors',
  HEALTH_PROFILE: '/health/profile',

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
  CREATE_SUCCESS: 'Created successfully.',
  UPDATE_SUCCESS: 'Updated successfully.',
  DELETE_SUCCESS: 'Deleted successfully.',
};
