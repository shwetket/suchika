import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { listProfiles } from '../../api/profiles';
import { listAccounts } from '../../api/wealth';
import { getDashboard, refreshProjections } from '../../api/household';
import { useAuth } from '../../hooks/useAuth';

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

// Mirrors Dashboard.js's formatTimestamp exactly (UX-012). Duplicated rather than
// extracted to a shared util — only two call sites today, below the "3+" bar the
// UX decision log sets for justifying a src/utils/ extraction.
function formatTimestamp(isoStr) {
  if (!isoStr) return '';
  return new Date(isoStr).toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
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
      <p className="text-lg font-semibold text-gray-700">{value}</p>
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

// Hero tile for Net Balance (UX-011) — mirrors Dashboard.js's SnapshotSummary
// treatment of net worth (bg-indigo-50, larger/bolder typography) so the number
// this page exists to show reads as the headline, not a peer of account counts.
// Also distinguishes a failed fetch from "never calculated yet" (UX-013) and
// surfaces the snapshot's age (UX-012).
function NetBalanceHeroCard({ netWorth, hasError, calculatedAt }) {
  const errorState = hasError && netWorth === null;
  let valueDisplay = 'Not calculated';
  let subLabel = 'Click Refresh to calculate';
  if (errorState) {
    valueDisplay = 'Unavailable';
    subLabel = "Couldn't load net balance — try Refresh";
  } else if (netWorth !== null) {
    valueDisplay = formatCurrency(netWorth);
    subLabel = 'Opening balance + transaction history';
  }

  const containerCls = errorState ? 'bg-red-50' : 'bg-indigo-50';
  const labelCls = errorState ? 'text-red-500' : 'text-indigo-500';
  const valueCls = errorState ? 'text-red-700' : 'text-indigo-900';
  const subLabelCls = errorState ? 'text-red-600' : 'text-indigo-400';

  return (
    <div className={`rounded-lg p-6 mb-4 ${containerCls}`}>
      <p className={`text-xs font-medium uppercase tracking-wide ${labelCls}`}>Net Balance</p>
      <p className={`text-3xl font-bold mt-1 ${valueCls}`}>{valueDisplay}</p>
      <p className={`text-xs mt-2 ${subLabelCls}`}>{subLabel}</p>
      {!errorState && calculatedAt && (
        <p className="text-xs text-indigo-400 mt-1">
          Last calculated: {formatTimestamp(calculatedAt)}
        </p>
      )}
    </div>
  );
}
NetBalanceHeroCard.propTypes = {
  netWorth: PropTypes.number,
  hasError: PropTypes.bool,
  calculatedAt: PropTypes.string,
};
NetBalanceHeroCard.defaultProps = { netWorth: null, hasError: false, calculatedAt: null };

export const Reports = () => {
  const { user } = useAuth();

  const [profiles, setProfiles] = useState([]);
  // UX-010: default to the logged-in user's own profile instead of forcing a blank
  // required picker — matches Dashboard.js/ActionCenter.js/Setup.js precedent. The
  // dropdown stays editable for the admin's legitimate need to check other members.
  const [selectedProfileId, setSelectedProfileId] = useState(user?.profile_id ?? '');
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [netWorthSnapshot, setNetWorthSnapshot] = useState(null);
  const [netWorthError, setNetWorthError] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [refreshError, setRefreshError] = useState(null);

  useEffect(() => {
    listProfiles(user?.admin_id, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, [user?.admin_id]);

  const loadNetWorth = useCallback(async (profileId) => {
    setNetWorthError(null);
    try {
      const data = await getDashboard(profileId);
      const snapshots = data?.snapshots ?? [];
      setNetWorthSnapshot(snapshots.find((s) => s.snapshot_key === NET_WORTH_SNAPSHOT_KEY) ?? null);
    } catch (err) {
      // UX-013: distinguish a failed fetch from "never calculated yet" — both used
      // to collapse to the same null snapshot and read identically in the UI.
      setNetWorthSnapshot(null);
      setNetWorthError(err.message || 'Failed to load net balance');
    }
  }, []);

  useEffect(() => {
    if (!selectedProfileId) {
      setAccounts([]);
      setNetWorthSnapshot(null);
      setNetWorthError(null);
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
      setNetWorthError(null);
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
  const netWorthCalculatedAt = netWorthSnapshot?.calculated_at ?? null;

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
          <NetBalanceHeroCard
            netWorth={netWorth}
            hasError={Boolean(netWorthError)}
            calculatedAt={netWorthCalculatedAt}
          />

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
            <SummaryCard label="Total Accounts" value={String(totalAccounts)} />
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
