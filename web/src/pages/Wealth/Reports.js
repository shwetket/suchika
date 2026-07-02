import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { listProfiles } from '../../api/profiles';
import { listAccounts } from '../../api/wealth';
import { getDashboard, refreshProjections } from '../../api/household';

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

const NET_WORTH_SNAPSHOT_KEY = 'WEALTH_NET_WORTH';

function formatCurrency(value) {
  return Number(value).toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  });
}

function safeParseJson(str) {
  try {
    return JSON.parse(str);
  } catch {
    return null;
  }
}

function SummaryCard({ label, value, subLabel }) {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5">
      <p className="text-sm text-gray-500 mb-1">{label}</p>
      <p className="text-2xl font-bold text-gray-900">{value}</p>
      {subLabel && <p className="text-xs text-gray-400 mt-1">{subLabel}</p>}
    </div>
  );
}
SummaryCard.propTypes = {
  label: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  subLabel: PropTypes.string,
};
SummaryCard.defaultProps = { subLabel: null };

export const Reports = () => {
  const [profiles, setProfiles] = useState([]);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [netWorthSnapshot, setNetWorthSnapshot] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [refreshError, setRefreshError] = useState(null);

  useEffect(() => {
    listProfiles(null, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, []);

  const loadNetWorth = useCallback(async (profileId) => {
    try {
      const data = await getDashboard(profileId);
      const snapshots = data?.snapshots ?? [];
      setNetWorthSnapshot(snapshots.find((s) => s.snapshot_key === NET_WORTH_SNAPSHOT_KEY) ?? null);
    } catch {
      setNetWorthSnapshot(null);
    }
  }, []);

  useEffect(() => {
    if (!selectedProfileId) {
      setAccounts([]);
      setNetWorthSnapshot(null);
      return;
    }
    setLoading(true);
    setError(null);
    listAccounts(selectedProfileId, null, null)
      .then((data) => setAccounts(data.accounts ?? []))
      .catch((err) => setError(err.message || 'Failed to load accounts'))
      .finally(() => setLoading(false));
    loadNetWorth(selectedProfileId);
  }, [selectedProfileId, loadNetWorth]);

  const handleRefreshNetWorth = useCallback(async () => {
    if (!selectedProfileId || isRefreshing) return;
    setIsRefreshing(true);
    setRefreshError(null);
    try {
      const data = await refreshProjections(selectedProfileId);
      const snapshots = data?.snapshots ?? [];
      setNetWorthSnapshot(snapshots.find((s) => s.snapshot_key === NET_WORTH_SNAPSHOT_KEY) ?? null);
    } catch (err) {
      setRefreshError(err.message || 'Refresh failed');
    } finally {
      setIsRefreshing(false);
    }
  }, [selectedProfileId, isRefreshing]);

  const totalAccounts = accounts.length;
  const activeAccounts = accounts.filter((a) => a.is_active).length;
  const inactiveAccounts = totalAccounts - activeAccounts;
  const netWorthPayload = netWorthSnapshot ? safeParseJson(netWorthSnapshot.payload) : null;
  const netWorth = netWorthPayload?.net_worth ?? netWorthPayload?.value ?? null;

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Wealth Reports</h1>
        <p className="text-gray-500 mt-1">Financial summary and insights</p>
      </div>

      <div className="mb-8">
        <select
          value={selectedProfileId}
          onChange={(e) => setSelectedProfileId(e.target.value)}
          className={`${inputClass} w-full sm:w-72`}
        >
          <option value="">Select a profile...</option>
          {profiles.map((p) => (
            <option key={p.profile_id} value={p.profile_id}>
              {p.full_name}
            </option>
          ))}
        </select>
      </div>

      {!selectedProfileId && (
        <div className="text-center py-16 text-gray-400">Select a profile to view the report.</div>
      )}

      {selectedProfileId && error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      {selectedProfileId && loading && (
        <div className="text-center py-16 text-gray-500">Loading summary...</div>
      )}

      {selectedProfileId && !loading && !error && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
            <SummaryCard label="Total Accounts" value={String(totalAccounts)} />
            <SummaryCard
              label="Net Balance"
              value={netWorth !== null ? formatCurrency(netWorth) : 'Not calculated'}
              subLabel={
                netWorth !== null
                  ? 'Opening balance + transaction history'
                  : 'Click Refresh to calculate'
              }
            />
            <SummaryCard label="Active Accounts" value={String(activeAccounts)} />
            <SummaryCard label="Inactive Accounts" value={String(inactiveAccounts)} />
          </div>

          <div className="mb-8 flex items-center gap-3">
            <button
              type="button"
              onClick={handleRefreshNetWorth}
              disabled={isRefreshing}
              className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
            >
              {isRefreshing ? 'Refreshing...' : 'Refresh Net Balance'}
            </button>
            {refreshError && <p className="text-red-600 text-sm">{refreshError}</p>}
          </div>

          <div className="bg-white border border-gray-200 rounded-lg p-6 text-center text-gray-400">
            <p className="text-lg font-medium mb-1">Deeper Analytics — Coming Soon</p>
            <p className="text-sm">
              Income vs. expenses, category breakdown, and trend charts will appear here.
            </p>
          </div>
        </>
      )}
    </div>
  );
};

export default Reports;
