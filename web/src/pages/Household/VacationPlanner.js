import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useMutation } from '@tanstack/react-query';
import { listProfiles } from '../../api/profiles';
import { checkVacationBudget } from '../../api/vacationPlanner';
import { useAuth } from '../../hooks/useAuth';

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

const STATUS_STYLES = {
  PASS: 'bg-green-50 text-green-700 border-green-200',
  WARNING: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  UNAVAILABLE: 'bg-gray-50 text-gray-500 border-gray-200',
};

function formatCurrency(value) {
  return Number(value).toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  });
}

function StatusBadge({ status }) {
  return (
    <span
      className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium border ${
        STATUS_STYLES[status] || STATUS_STYLES.UNAVAILABLE
      }`}
    >
      {status}
    </span>
  );
}
StatusBadge.propTypes = {
  status: PropTypes.string.isRequired,
};

function BudgetCheckCard({ budgetCheck }) {
  if (!budgetCheck) return null;
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-gray-700">Budget Check</h3>
        <StatusBadge status={budgetCheck.status} />
      </div>
      {budgetCheck.status === 'UNAVAILABLE' ? (
        <p className="text-sm text-gray-500">{budgetCheck.message}</p>
      ) : (
        <div className="space-y-1 text-sm text-gray-600">
          <p>Liquid savings: {formatCurrency(budgetCheck.liquid_savings)}</p>
          <p>Trip cost: {formatCurrency(budgetCheck.trip_cost)}</p>
          {budgetCheck.status === 'WARNING' && (
            <p className="text-yellow-700 font-medium">
              Shortfall: {formatCurrency(budgetCheck.shortfall)}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
BudgetCheckCard.propTypes = {
  budgetCheck: PropTypes.shape({
    status: PropTypes.string,
    liquid_savings: PropTypes.number,
    trip_cost: PropTypes.number,
    shortfall: PropTypes.number,
    message: PropTypes.string,
  }),
};
BudgetCheckCard.defaultProps = {
  budgetCheck: null,
};

function AssetComplianceCard({ assetCompliance }) {
  if (!assetCompliance) return null;
  const issues = assetCompliance.issues ?? [];
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-gray-700">Vehicle Compliance</h3>
        <StatusBadge status={assetCompliance.status} />
      </div>
      {issues.length === 0 ? (
        <p className="text-sm text-gray-500">No compliance issues for the trip dates.</p>
      ) : (
        <ul className="space-y-2">
          {issues.map((issue) => (
            <li
              key={`${issue.asset_id}-${issue.issue_type}`}
              className="text-sm text-yellow-700 bg-yellow-50 border border-yellow-200 rounded px-3 py-2"
            >
              {issue.asset_name}: {issue.issue_type.replace('_', ' ')} on {issue.expiry_date}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
AssetComplianceCard.propTypes = {
  assetCompliance: PropTypes.shape({
    status: PropTypes.string,
    issues: PropTypes.arrayOf(
      PropTypes.shape({
        asset_id: PropTypes.string,
        asset_name: PropTypes.string,
        issue_type: PropTypes.string,
        expiry_date: PropTypes.string,
      })
    ),
  }),
};
AssetComplianceCard.defaultProps = {
  assetCompliance: null,
};

export const VacationPlanner = () => {
  const [profiles, setProfiles] = useState([]);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [tripCost, setTripCost] = useState('');
  const [tripStartDate, setTripStartDate] = useState('');
  const [tripEndDate, setTripEndDate] = useState('');

  const { user } = useAuth();

  useEffect(() => {
    listProfiles(user?.admin_id, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, [user?.admin_id]);

  const checkMutation = useMutation({
    mutationFn: () =>
      checkVacationBudget(selectedProfileId, {
        trip_cost: Number(tripCost) || 0,
        trip_start_date: tripStartDate || null,
        trip_end_date: tripEndDate,
      }),
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!selectedProfileId || !tripEndDate) return;
    checkMutation.mutate();
  };

  const result = checkMutation.data;

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Vacation Planner</h1>
        <p className="text-gray-500 mt-1">
          Check your trip budget against liquid savings and vehicle compliance deadlines.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="bg-white border border-gray-200 rounded-lg p-5 mb-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
          <div>
            <label htmlFor="vp-profile" className="block text-xs text-gray-500 mb-1">
              Profile
            </label>
            <select
              id="vp-profile"
              value={selectedProfileId}
              onChange={(e) => setSelectedProfileId(e.target.value)}
              className={`${inputClass} w-full`}
            >
              <option value="">Select a profile...</option>
              {profiles.map((p) => (
                <option key={p.profile_id} value={p.profile_id}>
                  {p.full_name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="vp-cost" className="block text-xs text-gray-500 mb-1">
              Trip Cost (₹)
            </label>
            <input
              id="vp-cost"
              type="number"
              min="0"
              value={tripCost}
              onChange={(e) => setTripCost(e.target.value)}
              className={`${inputClass} w-full`}
            />
          </div>

          <div>
            <label htmlFor="vp-start" className="block text-xs text-gray-500 mb-1">
              Trip Start Date
            </label>
            <input
              id="vp-start"
              type="date"
              value={tripStartDate}
              onChange={(e) => setTripStartDate(e.target.value)}
              className={`${inputClass} w-full`}
            />
          </div>

          <div>
            <label htmlFor="vp-end" className="block text-xs text-gray-500 mb-1">
              Trip End Date
            </label>
            <input
              id="vp-end"
              type="date"
              required
              value={tripEndDate}
              onChange={(e) => setTripEndDate(e.target.value)}
              className={`${inputClass} w-full`}
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={!selectedProfileId || !tripEndDate || checkMutation.isPending}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
        >
          {checkMutation.isPending ? 'Checking...' : 'Check Budget & Compliance'}
        </button>

        {checkMutation.isError && (
          <p className="text-red-600 text-sm mt-3">
            {checkMutation.error?.message || 'Check failed'}
          </p>
        )}
      </form>

      {result && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <BudgetCheckCard budgetCheck={result.budget_check} />
          <AssetComplianceCard assetCompliance={result.asset_compliance} />
        </div>
      )}
    </div>
  );
};

export default VacationPlanner;
