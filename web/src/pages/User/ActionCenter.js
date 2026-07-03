import React, { useCallback } from 'react';
import PropTypes from 'prop-types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../hooks/useAuth';
import { getDashboard, refreshProjections } from '../../api/household';

const SNAPSHOT_KEY = 'ACTION_CENTER_ALERTS_FAMILY';

function safeParseJson(str) {
  try {
    return JSON.parse(str);
  } catch {
    return null;
  }
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

function AlertSection({ title, items, emptyText, renderItem }) {
  return (
    <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
      <h2 className="text-base font-semibold text-gray-900 mb-3">
        {title}{' '}
        {items.length > 0 && <span className="text-sm text-gray-400">({items.length})</span>}
      </h2>
      {items.length === 0 ? (
        <p className="text-sm text-gray-400">{emptyText}</p>
      ) : (
        <ul className="space-y-2">
          {items.map((item, index) => (
            // eslint-disable-next-line react/no-array-index-key
            <li key={index} className="text-sm bg-gray-50 border border-gray-100 rounded px-3 py-2">
              {renderItem(item)}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
AlertSection.propTypes = {
  title: PropTypes.string.isRequired,
  items: PropTypes.array.isRequired,
  emptyText: PropTypes.string.isRequired,
  renderItem: PropTypes.func.isRequired,
};

export const ActionCenter = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const profileId = user?.profile_id ?? null;

  const dashboardQuery = useQuery({
    queryKey: ['dashboard', profileId],
    queryFn: () => getDashboard(profileId),
    enabled: Boolean(profileId),
  });

  const refreshMutation = useMutation({
    mutationFn: () => refreshProjections(profileId),
    onSuccess: (data) => {
      queryClient.setQueryData(['dashboard', profileId], data);
    },
  });

  const handleRefresh = useCallback(() => {
    if (!profileId || refreshMutation.isPending) return;
    refreshMutation.mutate();
  }, [profileId, refreshMutation]);

  const snapshots = dashboardQuery.data?.snapshots ?? [];
  const snapshot = snapshots.find((s) => s.snapshot_key === SNAPSHOT_KEY);
  const payload = snapshot ? safeParseJson(snapshot.payload) : null;

  const upcomingEvents = payload?.upcoming_events ?? [];
  const vehicleCompliance = payload?.vehicle_compliance ?? [];
  const streakGaps = payload?.biometric_streak_gaps ?? [];
  const isRefreshing = refreshMutation.isPending;
  const refreshError = refreshMutation.isError
    ? refreshMutation.error?.message || 'Refresh failed'
    : null;

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Action Center</h1>
          <p className="text-sm text-gray-500 mt-1">
            Alerts across the household — upcoming events, vehicle compliance, and health tracking
            gaps.
          </p>
        </div>
        <button
          type="button"
          onClick={handleRefresh}
          disabled={isRefreshing || !profileId}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50 flex items-center"
        >
          {isRefreshing && <Spinner />}
          {isRefreshing ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {refreshError && <p className="text-red-600 text-sm mb-4">{refreshError}</p>}

      {!profileId && (
        <p className="text-sm text-gray-400">Sign in with a linked profile to view alerts.</p>
      )}

      {profileId && !payload && (
        <p className="text-sm text-gray-500">No alert data yet. Click Refresh to compute.</p>
      )}

      {profileId && payload && (
        <div className="space-y-4">
          <AlertSection
            title="Upcoming Events"
            items={upcomingEvents}
            emptyText="No events in the next 30 days."
            renderItem={(event) => (
              <>
                <span className="font-medium text-gray-800">{event.title}</span>
                <span className="text-gray-500">
                  {' '}
                  — {event.full_name}, {event.start_date}
                </span>
              </>
            )}
          />

          <AlertSection
            title="Vehicle Compliance"
            items={vehicleCompliance}
            emptyText="No compliance deadlines in the next 30 days."
            renderItem={(issue) => (
              <>
                <span className="font-medium text-yellow-700">
                  {issue.asset_name}: {issue.issue_type.replace('_', ' ')}
                </span>
                <span className="text-gray-500">
                  {' '}
                  — {issue.full_name}, expires {issue.expiry_date}
                </span>
              </>
            )}
          />

          <AlertSection
            title="Biometric Streak Gaps"
            items={streakGaps}
            emptyText="No tracking gaps — all core vitals logged within 30 days."
            renderItem={(gap) => (
              <>
                <span className="font-medium text-gray-800">{gap.full_name}</span>
                <span className="text-gray-500">
                  {' '}
                  — {gap.vital_type.replace('_', ' ')}:{' '}
                  {gap.last_reading_date
                    ? `last logged ${gap.last_reading_date} (${gap.days_since_last_reading} days ago)`
                    : 'never logged'}
                </span>
              </>
            )}
          />
        </div>
      )}
    </div>
  );
};

export default ActionCenter;
