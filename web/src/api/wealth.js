import { get, post, patch, del } from './client';
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

export const getAccount = (accountId) => get(`${API_ENDPOINTS.ACCOUNTS}/${accountId}`);

export const updateAccount = (accountId, data) =>
  patch(`${API_ENDPOINTS.ACCOUNTS}/${accountId}`, data);

export const deactivateAccount = (accountId) => del(`${API_ENDPOINTS.ACCOUNTS}/${accountId}`);

export function listTransactions(accountId, from, to, txnType) {
  const params = new URLSearchParams();
  if (from !== null && from !== undefined) params.append('from', from);
  if (to !== null && to !== undefined) params.append('to', to);
  if (txnType !== null && txnType !== undefined && txnType !== 'ALL')
    params.append('txn_type', txnType);
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

export function listPhysicalAssets(profileId, assetType, isActive) {
  const params = new URLSearchParams();
  if (profileId !== null && profileId !== undefined) params.append('profile_id', profileId);
  if (assetType !== null && assetType !== undefined) params.append('asset_type', assetType);
  if (isActive !== null && isActive !== undefined) params.append('is_active', String(isActive));
  const query = params.toString();
  const url = query
    ? `${API_ENDPOINTS.PHYSICAL_ASSETS}?${query}`
    : API_ENDPOINTS.PHYSICAL_ASSETS;
  return get(url);
}

export function createPhysicalAsset(profileId, data) {
  const url = `${API_ENDPOINTS.PHYSICAL_ASSETS}?profile_id=${encodeURIComponent(profileId)}`;
  return post(url, data);
}

export const getPhysicalAsset = (assetId) =>
  get(`${API_ENDPOINTS.PHYSICAL_ASSETS}/${assetId}`);

export const updatePhysicalAsset = (assetId, data) =>
  patch(`${API_ENDPOINTS.PHYSICAL_ASSETS}/${assetId}`, data);

export const deactivatePhysicalAsset = (assetId) =>
  del(`${API_ENDPOINTS.PHYSICAL_ASSETS}/${assetId}`);
