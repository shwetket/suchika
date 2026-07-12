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
  GOAL_PLANS: '/v1/goal-plans',
  INSURANCE_POLICIES: '/v1/insurance-policies',

  // Household domain (via gateway)
  CALENDAR_EVENTS: '/v1/household/calendar-events',
  INVENTORY_ITEMS: '/v1/household/inventory-items',
  HOUSEHOLD_GOALS: '/v1/household/goals',

  // Projections (via gateway)
  PROJECTIONS_REFRESH: '/v1/projections/refresh',
  PROJECTIONS_DASHBOARD: '/v1/projections/dashboard',

  // Vacation Planner (gateway-native cross-domain compute)
  VACATION_PLANNER_BUDGET_CHECK: '/v1/vacation-planner/budget-check',

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
  WEALTH_GOAL_PLANS: '/wealth/goal-plans',
  WEALTH_INSURANCE_POLICIES: '/wealth/insurance-policies',

  HOUSEHOLD_PROFILES: '/household/profiles',
  HOUSEHOLD_CALENDAR: '/household/calendar',
  HOUSEHOLD_INVENTORY: '/household/inventory',
  HOUSEHOLD_GOALS: '/household/goals',
  HOUSEHOLD_VACATION_PLANNER: '/household/vacation-planner',

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

/**
 * Curated list of standard Indian financial institutions for account
 * institution_name selection (wealth.account.institution_name — see
 * application/contract/wealth.yaml's FinancialInstitution schema, which
 * documents this same list). This is a developer-maintained closed list for
 * now — add a new entry here when a real account needs an institution not yet
 * covered; v2.0 plans an admin page to manage this list from the UI instead.
 *
 * The field itself stays a free-form string end-to-end (no backend/DB
 * enforcement) — selecting "Other" reveals a plain text input so an
 * institution not on this list (or an employer name for EPF-type accounts,
 * which are fundamentally not enumerable) can still be entered.
 */
export const FINANCIAL_INSTITUTIONS = {
  'Public Sector Banks': [
    'State Bank of India',
    'Punjab National Bank',
    'Bank of Baroda',
    'Canara Bank',
    'Union Bank of India',
    'Bank of India',
    'Indian Bank',
    'Central Bank of India',
    'Indian Overseas Bank',
    'UCO Bank',
    'Bank of Maharashtra',
    'Punjab & Sind Bank',
  ],
  'Private Sector Banks': [
    'HDFC Bank',
    'ICICI Bank',
    'Axis Bank',
    'Kotak Mahindra Bank',
    'IndusInd Bank',
    'Yes Bank',
    'IDFC FIRST Bank',
    'Federal Bank',
    'RBL Bank',
    'South Indian Bank',
    'Karur Vysya Bank',
    'City Union Bank',
    'Bandhan Bank',
    'DCB Bank',
    'CSB Bank',
    'Tamilnad Mercantile Bank',
    'IDBI Bank',
    'Jammu & Kashmir Bank',
  ],
  'Small Finance Banks': [
    'AU Small Finance Bank',
    'Equitas Small Finance Bank',
    'Ujjivan Small Finance Bank',
  ],
  'Foreign Banks': ['HSBC', 'Standard Chartered Bank', 'Citibank', 'Deutsche Bank'],
  'Mutual Fund AMCs': [
    'SBI Mutual Fund',
    'HDFC Mutual Fund',
    'ICICI Prudential Mutual Fund',
    'Axis Mutual Fund',
    'Kotak Mahindra Mutual Fund',
    'Aditya Birla Sun Life Mutual Fund',
    'Nippon India Mutual Fund',
    'DSP Mutual Fund',
    'Franklin Templeton',
    'UTI Mutual Fund',
    'Tata Mutual Fund',
    'Mirae Asset Mutual Fund',
    'PPFAS Mutual Fund',
    'Motilal Oswal Mutual Fund',
    'Edelweiss Mutual Fund',
    'Invesco Mutual Fund',
    'Sundaram Mutual Fund',
    'Canara Robeco Mutual Fund',
    'Bandhan Mutual Fund',
    'Quant Mutual Fund',
    'HSBC Mutual Fund',
    'Baroda BNP Paribas Mutual Fund',
    'JM Financial Mutual Fund',
    'LIC Mutual Fund',
    'WhiteOak Capital Mutual Fund',
    'Union Mutual Fund',
  ],
  'Retirement / Pension': ['NPS Trust', "Employees' Provident Fund Organisation (EPFO)"],
};

/** Flat de-duplicated list, sentinel value, and lookup helper for the dropdown UI. */
export const FINANCIAL_INSTITUTIONS_FLAT = Object.values(FINANCIAL_INSTITUTIONS).flat();
export const OTHER_INSTITUTION_SENTINEL = '__OTHER__';
export const isKnownInstitution = (value) => FINANCIAL_INSTITUTIONS_FLAT.includes(value);
