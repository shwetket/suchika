import { get, post, put, patch, del } from './client';
import { API_ENDPOINTS } from '../utils/constants';

export function listAccounts(profileId, accountType, isActive) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  if (accountType !== null && accountType !== undefined) params.append('account_type', accountType);
  if (isActive !== null && isActive !== undefined) params.append('is_active', String(isActive));
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.ACCOUNTS}?${query}` : API_ENDPOINTS.ACCOUNTS;
  return get(url);
}

export function createAccount(profileId, data) {
  const url = `${API_ENDPOINTS.ACCOUNTS}?profile_id=${encodeURIComponent(profileId)}`;
  return post(url, data);
}

export const getAccount = (accountId, profileId) =>
  get(`${API_ENDPOINTS.ACCOUNTS}/${accountId}?profile_id=${encodeURIComponent(profileId)}`);

export const getAccountBalance = (accountId, profileId) =>
  get(`${API_ENDPOINTS.ACCOUNTS}/${accountId}/balance?profile_id=${encodeURIComponent(profileId)}`);

export const updateAccount = (accountId, profileId, data) =>
  patch(`${API_ENDPOINTS.ACCOUNTS}/${accountId}?profile_id=${encodeURIComponent(profileId)}`, data);

export const updateAccountClassification = (accountId, profileId, data) =>
  patch(
    `${API_ENDPOINTS.ACCOUNTS}/${accountId}/classification?profile_id=${encodeURIComponent(profileId)}`,
    data
  );

export const getAmortization = (accountId, profileId) =>
  get(
    `${API_ENDPOINTS.ACCOUNTS}/${accountId}/amortization?profile_id=${encodeURIComponent(profileId)}`
  );

export const deactivateAccount = (accountId, profileId) =>
  del(`${API_ENDPOINTS.ACCOUNTS}/${accountId}?profile_id=${encodeURIComponent(profileId)}`);

export function createTransaction(accountId, profileId, data) {
  const url = `${API_ENDPOINTS.ACCOUNTS}/${accountId}/transactions?profile_id=${encodeURIComponent(profileId)}`;
  return post(url, data);
}

export function listTransactions(accountId, profileId, from, to, txnType, page, size) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  if (from !== null && from !== undefined) params.append('from', from);
  if (to !== null && to !== undefined) params.append('to', to);
  if (txnType !== null && txnType !== undefined && txnType !== 'ALL')
    params.append('txn_type', txnType);
  if (page !== null && page !== undefined) params.append('page', page);
  if (size !== null && size !== undefined) params.append('size', size);
  const query = params.toString();
  const base = `${API_ENDPOINTS.ACCOUNTS}/${accountId}/transactions`;
  return get(query ? `${base}?${query}` : base);
}

export function uploadStatement(accountId, fileName, csvContent) {
  const url = `${API_ENDPOINTS.ACCOUNTS}/${accountId}/uploads`;
  return post(url, { file_name: fileName, csv_content: csvContent });
}

export const listUploads = (accountId) => get(`${API_ENDPOINTS.ACCOUNTS}/${accountId}/uploads`);

export const rollbackUpload = (accountId, uploadId) =>
  del(`${API_ENDPOINTS.ACCOUNTS}/${accountId}/uploads/${uploadId}`);

export const getUploadErrors = (accountId, uploadId) =>
  get(`${API_ENDPOINTS.ACCOUNTS}/${accountId}/uploads/${uploadId}/errors`);

export function listPhysicalAssets(profileId, assetType, isActive, page, size) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  if (assetType !== null && assetType !== undefined) params.append('asset_type', assetType);
  if (isActive !== null && isActive !== undefined) params.append('is_active', String(isActive));
  if (page !== null && page !== undefined) params.append('page', page);
  if (size !== null && size !== undefined) params.append('size', size);
  const query = params.toString();
  const url = query ? `${API_ENDPOINTS.PHYSICAL_ASSETS}?${query}` : API_ENDPOINTS.PHYSICAL_ASSETS;
  return get(url);
}

export function createPhysicalAsset(profileId, data) {
  const url = `${API_ENDPOINTS.PHYSICAL_ASSETS}?profile_id=${encodeURIComponent(profileId)}`;
  return post(url, data);
}

export const getPhysicalAsset = (assetId, profileId) =>
  get(`${API_ENDPOINTS.PHYSICAL_ASSETS}/${assetId}?profile_id=${encodeURIComponent(profileId)}`);

export const updatePhysicalAsset = (assetId, profileId, data) =>
  patch(
    `${API_ENDPOINTS.PHYSICAL_ASSETS}/${assetId}?profile_id=${encodeURIComponent(profileId)}`,
    data
  );

export const deactivatePhysicalAsset = (assetId, profileId) =>
  del(`${API_ENDPOINTS.PHYSICAL_ASSETS}/${assetId}?profile_id=${encodeURIComponent(profileId)}`);

// ── Goal Plans (ADR-022) — admin_id-scoped, not profile_id ─────────────────

export const listGoalPlans = (adminId) =>
  get(`${API_ENDPOINTS.GOAL_PLANS}?admin_id=${encodeURIComponent(adminId)}`);

export const createGoalPlan = (adminId, data) =>
  post(`${API_ENDPOINTS.GOAL_PLANS}?admin_id=${encodeURIComponent(adminId)}`, data);

export const getGoalPlan = (goalPlanId, adminId) =>
  get(`${API_ENDPOINTS.GOAL_PLANS}/${goalPlanId}?admin_id=${encodeURIComponent(adminId)}`);

export const updateGoalPlan = (goalPlanId, adminId, data) =>
  patch(`${API_ENDPOINTS.GOAL_PLANS}/${goalPlanId}?admin_id=${encodeURIComponent(adminId)}`, data);

export const deactivateGoalPlan = (goalPlanId, adminId) =>
  del(`${API_ENDPOINTS.GOAL_PLANS}/${goalPlanId}?admin_id=${encodeURIComponent(adminId)}`);

export const replaceGoalPlanMilestones = (goalPlanId, adminId, milestones) =>
  put(
    `${API_ENDPOINTS.GOAL_PLANS}/${goalPlanId}/milestones?admin_id=${encodeURIComponent(adminId)}`,
    milestones
  );

export const updateGoalPlanMilestoneAchieved = (goalPlanId, milestoneId, adminId, isAchieved) =>
  patch(
    `${API_ENDPOINTS.GOAL_PLANS}/${goalPlanId}/milestones/${milestoneId}?admin_id=${encodeURIComponent(adminId)}`,
    { is_achieved: isAchieved }
  );

export const replaceGoalPlanRules = (goalPlanId, adminId, rules) =>
  put(
    `${API_ENDPOINTS.GOAL_PLANS}/${goalPlanId}/rules?admin_id=${encodeURIComponent(adminId)}`,
    rules
  );

export const replaceGoalPlanTriggerEvents = (goalPlanId, adminId, triggerEvents) =>
  put(
    `${API_ENDPOINTS.GOAL_PLANS}/${goalPlanId}/trigger-events?admin_id=${encodeURIComponent(adminId)}`,
    triggerEvents
  );

// ── Insurance Policies (ADR-022 Phase 2) — admin_id-scoped, not profile_id ─

export const listInsurancePolicies = (adminId) =>
  get(`${API_ENDPOINTS.INSURANCE_POLICIES}?admin_id=${encodeURIComponent(adminId)}`);

export const createInsurancePolicy = (adminId, data) =>
  post(`${API_ENDPOINTS.INSURANCE_POLICIES}?admin_id=${encodeURIComponent(adminId)}`, data);

export const getInsurancePolicy = (insurancePolicyId, adminId) =>
  get(
    `${API_ENDPOINTS.INSURANCE_POLICIES}/${insurancePolicyId}?admin_id=${encodeURIComponent(adminId)}`
  );

export const updateInsurancePolicy = (insurancePolicyId, adminId, data) =>
  patch(
    `${API_ENDPOINTS.INSURANCE_POLICIES}/${insurancePolicyId}?admin_id=${encodeURIComponent(adminId)}`,
    data
  );

export const deactivateInsurancePolicy = (insurancePolicyId, adminId) =>
  del(
    `${API_ENDPOINTS.INSURANCE_POLICIES}/${insurancePolicyId}?admin_id=${encodeURIComponent(adminId)}`
  );
