import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { getDashboard, refreshProjections } from '../../api/household';

const DOMAIN_CARDS = [
  {
    title: 'Profiles',
    description: 'Manage household members',
    to: '/household/profiles',
  },
  {
    title: 'Wealth',
    description: 'Track accounts, transactions and uploads',
    to: '/wealth/accounts',
  },
  {
    title: 'Health',
    description: 'Log vitals and doctor visits',
    to: '/health/vitals',
  },
  {
    title: 'Household',
    description: 'Calendar events, inventory and goals',
    to: '/household/calendar',
  },
];

const SNAPSHOT_KEYS = {
  NET_WORTH: 'WEALTH_NET_WORTH',
  NET_WORTH_FAMILY: 'WEALTH_NET_WORTH_FAMILY',
  EVENT_SUMMARY: 'HOUSEHOLD_EVENT_SUMMARY',
  GOAL_PROGRESS: 'WEALTH_GOAL_PROGRESS',
  EMI_TRACKING_FAMILY: 'WEALTH_EMI_TRACKING_FAMILY',
  LIQUIDITY_TIERS_FAMILY: 'WEALTH_LIQUIDITY_TIERS_FAMILY',
};

function safeParseJson(str) {
  try {
    return JSON.parse(str);
  } catch {
    return null;
  }
}

function formatCurrency(value) {
  if (value === null || value === undefined) return null;
  return Number(value).toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  });
}

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

function Spinner() {
  return (
    <svg
      className="animate-spin h-4 w-4 text-white inline-block mr-2"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
    </svg>
  );
}

function MemberBreakdown({ members }) {
  if (!members || members.length === 0) return null;
  return (
    <div className="mt-3 border border-indigo-100 rounded-lg divide-y divide-indigo-50">
      {members.map((m) => (
        <div key={m.profile_id} className="flex items-center justify-between px-4 py-2">
          <div>
            <p className="text-sm font-medium text-gray-800">{m.full_name}</p>
            {m.relation_to_admin && <p className="text-xs text-gray-400">{m.relation_to_admin}</p>}
          </div>
          <p className="text-sm font-semibold text-gray-700">{formatCurrency(m.net_worth)}</p>
        </div>
      ))}
    </div>
  );
}

MemberBreakdown.propTypes = {
  members: PropTypes.arrayOf(
    PropTypes.shape({
      profile_id: PropTypes.string,
      full_name: PropTypes.string,
      relation_to_admin: PropTypes.string,
      net_worth: PropTypes.number,
    })
  ),
};

MemberBreakdown.defaultProps = { members: [] };

function SnapshotSummary({ snapshots }) {
  if (!snapshots || snapshots.length === 0) {
    return (
      <p className="text-sm text-gray-500">No dashboard data yet. Click Refresh to compute.</p>
    );
  }

  const byKey = {};
  snapshots.forEach((s) => {
    byKey[s.snapshot_key] = s;
  });

  const netWorthSnap = byKey[SNAPSHOT_KEYS.NET_WORTH];
  const familyNetWorthSnap = byKey[SNAPSHOT_KEYS.NET_WORTH_FAMILY];
  const eventSnap = byKey[SNAPSHOT_KEYS.EVENT_SUMMARY];
  const goalSnap = byKey[SNAPSHOT_KEYS.GOAL_PROGRESS];
  const emiSnap = byKey[SNAPSHOT_KEYS.EMI_TRACKING_FAMILY];
  const liquiditySnap = byKey[SNAPSHOT_KEYS.LIQUIDITY_TIERS_FAMILY];

  const netWorthPayload = netWorthSnap ? safeParseJson(netWorthSnap.payload) : null;
  const familyNetWorthPayload = familyNetWorthSnap
    ? safeParseJson(familyNetWorthSnap.payload)
    : null;
  const eventPayload = eventSnap ? safeParseJson(eventSnap.payload) : null;
  const goalPayload = goalSnap ? safeParseJson(goalSnap.payload) : null;
  const emiPayload = emiSnap ? safeParseJson(emiSnap.payload) : null;
  const liquidityPayload = liquiditySnap ? safeParseJson(liquiditySnap.payload) : null;

  const lastRefreshed =
    familyNetWorthSnap?.calculated_at ||
    netWorthSnap?.calculated_at ||
    eventSnap?.calculated_at ||
    goalSnap?.calculated_at;

  // Family rollup (ADR-017) is the dashboard's primary net worth figure; fall back
  // to the per-profile snapshot only if the family snapshot hasn't been computed yet.
  const isFamilyView = familyNetWorthPayload !== null;
  const netWorth = isFamilyView
    ? (familyNetWorthPayload?.family_net_worth ?? null)
    : (netWorthPayload?.net_worth ?? netWorthPayload?.value ?? null);
  const members = familyNetWorthPayload?.members ?? [];
  const upcomingEvents = eventPayload?.upcoming_count ?? eventPayload?.count ?? null;
  const activeGoals = goalPayload?.active_count ?? goalPayload?.count ?? null;

  return (
    <div className="mt-4">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {netWorth !== null && (
          <div className="bg-indigo-50 rounded-lg p-4">
            <p className="text-xs text-indigo-500 font-medium uppercase tracking-wide">
              {isFamilyView ? 'Family Net Worth' : 'Net Worth'}
            </p>
            <p className="text-xl font-bold text-indigo-900 mt-1">{formatCurrency(netWorth)}</p>
          </div>
        )}
        {upcomingEvents !== null && (
          <div className="bg-teal-50 rounded-lg p-4">
            <p className="text-xs text-teal-500 font-medium uppercase tracking-wide">
              Upcoming Events
            </p>
            <p className="text-xl font-bold text-teal-900 mt-1">{upcomingEvents}</p>
          </div>
        )}
        {activeGoals !== null && (
          <div className="bg-amber-50 rounded-lg p-4">
            <p className="text-xs text-amber-500 font-medium uppercase tracking-wide">
              Active Goals
            </p>
            <p className="text-xl font-bold text-amber-900 mt-1">{activeGoals}</p>
          </div>
        )}
      </div>
      {isFamilyView && <MemberBreakdown members={members} />}
      {emiPayload && emiPayload.total_monthly_emi > 0 && (
        <div className="mt-3 bg-orange-50 rounded-lg p-4">
          <p className="text-xs text-orange-500 font-medium uppercase tracking-wide">Monthly EMI</p>
          <p className="text-xl font-bold text-orange-900 mt-1">
            {formatCurrency(emiPayload.total_monthly_emi)}
          </p>
          {emiPayload.total_monthly_interest_saved > 0 && (
            <p className="text-xs text-orange-600 mt-1">
              Saves {formatCurrency(emiPayload.total_monthly_interest_saved)}/mo via offset
            </p>
          )}
        </div>
      )}
      {liquidityPayload?.tiers && (
        <div className="mt-3 border border-gray-100 rounded-lg p-4">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
            Liquidity Tiers
          </p>
          <div className="grid grid-cols-2 gap-2">
            {['LIQUID', 'SEMI_LIQUID', 'ILLIQUID', 'LOCKED'].filter(
              (tier) => (liquidityPayload.tiers[tier] ?? 0) > 0
            ).map((tier) => (
              <div key={tier} className="text-sm flex justify-between">
                <span className="text-gray-500">{tier.replace('_', ' ')}</span>
                <span className="font-medium">{formatCurrency(liquidityPayload.tiers[tier])}</span>
              </div>
            ))}
          </div>
        </div>
      )}
      {lastRefreshed && (
        <p className="text-xs text-gray-400 mt-3">
          Last refreshed: {formatTimestamp(lastRefreshed)}
        </p>
      )}
    </div>
  );
}

SnapshotSummary.propTypes = {
  snapshots: PropTypes.arrayOf(
    PropTypes.shape({
      snapshot_key: PropTypes.string,
      payload: PropTypes.string,
      calculated_at: PropTypes.string,
    })
  ),
};

SnapshotSummary.defaultProps = {
  snapshots: [],
};

export const Dashboard = () => {
  const { user } = useAuth();
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [snapshots, setSnapshots] = useState(null);
  const [refreshError, setRefreshError] = useState(null);

  const profileId = user?.profile_id ?? null;

  const loadDashboard = useCallback(async () => {
    if (!profileId) return;
    try {
      const data = await getDashboard(profileId);
      setSnapshots(data?.snapshots ?? []);
    } catch {
      setSnapshots([]);
    }
  }, [profileId]);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const handleRefresh = useCallback(async () => {
    if (!profileId || isRefreshing) return;
    setIsRefreshing(true);
    setRefreshError(null);
    try {
      const data = await refreshProjections(profileId);
      setSnapshots(data?.snapshots ?? []);
    } catch (err) {
      setRefreshError(err.message || 'Refresh failed');
    } finally {
      setIsRefreshing(false);
    }
  }, [profileId, isRefreshing]);

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-gray-900">Welcome back, {user?.username}</h1>
        <p className="text-sm text-gray-500 mt-1">What would you like to manage today?</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
        {DOMAIN_CARDS.map(({ title, description, to }) => (
          <Link
            key={to}
            to={to}
            className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md hover:border-indigo-200 transition-all block"
          >
            <h2 className="text-base font-semibold text-gray-900 mb-1">{title}</h2>
            <p className="text-sm text-gray-500">{description}</p>
          </Link>
        ))}
      </div>

      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-base font-semibold text-gray-900">Live Dashboard</h2>
          <button
            type="button"
            onClick={handleRefresh}
            disabled={isRefreshing || !profileId}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50 flex items-center"
          >
            {isRefreshing && <Spinner />}
            {isRefreshing ? 'Refreshing...' : 'Refresh Live Data'}
          </button>
        </div>

        {refreshError && <p className="text-red-600 text-sm mb-3">{refreshError}</p>}

        {!profileId && (
          <p className="text-sm text-gray-400">Sign in with a linked profile to view metrics.</p>
        )}

        {profileId && <SnapshotSummary snapshots={snapshots} />}
      </div>
    </div>
  );
};
