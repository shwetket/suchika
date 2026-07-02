import React, { useCallback, useEffect, useState } from 'react';
import { listAdmins } from '../../api/admins';
import { getAdmin, updateAdminPolicy } from '../../api/profiles';

const FIELDS = [
  {
    key: 'monthly_budget_cap',
    label: 'Monthly Budget Cap',
    unit: '₹',
    placeholder: 'e.g. 50000',
  },
  {
    key: 'debt_crossover_threshold_percent',
    label: 'Debt Crossover Threshold',
    unit: '%',
    placeholder: 'e.g. 40',
  },
  {
    key: 'freedom_runway_months',
    label: 'Freedom Runway Target',
    unit: 'months',
    placeholder: 'e.g. 24',
  },
  {
    key: 'insurance_multiple',
    label: 'Insurance Multiple',
    unit: '× annual income',
    placeholder: 'e.g. 10',
  },
  {
    key: 'year_one_annual_target',
    label: 'Year One Annual Target',
    unit: '₹',
    placeholder: 'e.g. 600000',
  },
];

const EMPTY_FORM = {
  monthly_budget_cap: '',
  debt_crossover_threshold_percent: '',
  freedom_runway_months: '',
  insurance_multiple: '',
  year_one_annual_target: '',
};

function policyToForm(policy) {
  if (!policy) return EMPTY_FORM;
  const form = {};
  FIELDS.forEach(({ key }) => {
    const val = policy[key];
    form[key] = val !== null && val !== undefined ? String(val) : '';
  });
  return form;
}

function formToPayload(form) {
  const payload = {};
  FIELDS.forEach(({ key }) => {
    const raw = form[key];
    if (raw !== '' && raw !== null && raw !== undefined) {
      payload[key] = Number(raw);
    }
  });
  return payload;
}

const inputClass =
  'w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400';

export const PolicySettings = () => {
  const [adminId, setAdminId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);

  const loadPolicy = useCallback(async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const adminsData = await listAdmins();
      const admins = adminsData?.admins ?? [];
      if (admins.length === 0) {
        setErrorMsg('No admin account found.');
        return;
      }
      const id = admins[0].admin_id;
      setAdminId(id);
      const adminData = await getAdmin(id);
      setForm(policyToForm(adminData?.policy_settings ?? null));
    } catch {
      setErrorMsg('Failed to load policy settings.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPolicy();
  }, [loadPolicy]);

  const handleChange = useCallback((e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!adminId) return;
      setSaving(true);
      setSuccessMsg(null);
      setErrorMsg(null);
      try {
        await updateAdminPolicy(adminId, formToPayload(form));
        setSuccessMsg('Policy settings saved.');
      } catch {
        setErrorMsg('Failed to save policy settings. Please try again.');
      } finally {
        setSaving(false);
      }
    },
    [adminId, form]
  );

  if (loading) {
    return (
      <div className="p-6 max-w-2xl mx-auto">
        <p className="text-sm text-gray-500">Loading policy settings...</p>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <h1 className="text-xl font-semibold text-gray-900 mb-1">Household Policy Settings</h1>
      <p className="text-sm text-gray-500 mb-6">
        These values drive formula-based wealth goals and validation checks on the dashboard.
      </p>

      <form
        onSubmit={handleSubmit}
        className="bg-white rounded-xl border border-gray-100 shadow-sm p-6 space-y-5"
      >
        {FIELDS.map(({ key, label, unit, placeholder }) => (
          <div key={key}>
            <label htmlFor={key} className="block text-sm font-medium text-gray-700 mb-1">
              {label}
              <span className="ml-1 text-xs text-gray-400">({unit})</span>
            </label>
            <input
              id={key}
              name={key}
              type="number"
              min="0"
              step="any"
              value={form[key]}
              onChange={handleChange}
              placeholder={placeholder}
              className={inputClass}
            />
          </div>
        ))}

        {successMsg && (
          <p className="text-sm text-green-700 bg-green-50 rounded-lg px-3 py-2">{successMsg}</p>
        )}
        {errorMsg && (
          <p className="text-sm text-red-700 bg-red-50 rounded-lg px-3 py-2">{errorMsg}</p>
        )}

        <button
          type="submit"
          disabled={saving || !adminId}
          className="bg-indigo-600 text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
        >
          {saving ? 'Saving...' : 'Save Settings'}
        </button>
      </form>
    </div>
  );
};
