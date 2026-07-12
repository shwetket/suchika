import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createInsurancePolicy,
  deactivateInsurancePolicy,
  listInsurancePolicies,
  updateInsurancePolicy,
} from '../../api/wealth';
import { Field } from '../../components/Field';
import { Modal } from '../../components/Modal';
import { Badge } from '../../components/shared/Badge';
import { EditIcon } from '../../components/shared/EditIcon';
import { useAuth } from '../../hooks/useAuth';

const POLICY_TYPES = ['TERM', 'GROUP_TERM', 'INVESTMENT_LINKED', 'ENDOWMENT', 'HEALTH'];
const PREMIUM_FREQUENCIES = ['MONTHLY', 'ANNUAL'];

const EMPTY_FORM = {
  policy_name: '',
  provider: '',
  policy_type: 'TERM',
  premium_amount: '',
  premium_frequency: 'MONTHLY',
  coverage_amount: '',
};

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500';

function formToPayload(form) {
  return {
    policy_name: form.policy_name.trim(),
    provider: form.provider.trim(),
    policy_type: form.policy_type,
    premium_amount: form.premium_amount !== '' ? Number(form.premium_amount) : 0,
    premium_frequency: form.premium_frequency,
    coverage_amount: form.coverage_amount !== '' ? Number(form.coverage_amount) : null,
  };
}

function policyToForm(policy) {
  return {
    policy_name: policy.policy_name || '',
    provider: policy.provider || '',
    policy_type: policy.policy_type || 'TERM',
    premium_amount: policy.premium_amount ?? '',
    premium_frequency: policy.premium_frequency || 'MONTHLY',
    coverage_amount: policy.coverage_amount ?? '',
  };
}

function PolicyCard({ policy, onEdit }) {
  const active = policy.is_active !== false;
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5 flex flex-col gap-2">
      <div className="flex items-start justify-between">
        <div>
          <h3 className="font-semibold text-gray-900 text-lg">{policy.policy_name}</h3>
          <p className="text-sm text-gray-500">{policy.provider}</p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant={active ? 'success' : 'neutral'}>{active ? 'Active' : 'Inactive'}</Badge>
          <button
            type="button"
            onClick={() => onEdit(policy)}
            aria-label="Edit policy"
            title="Edit policy"
            className="text-gray-400 hover:text-blue-600 p-1 rounded hover:bg-blue-50"
          >
            <EditIcon />
          </button>
        </div>
      </div>
      <div className="text-sm text-gray-600 space-y-1">
        <p>Type: {policy.policy_type?.replaceAll('_', ' ')}</p>
        <p>
          Premium: {Number(policy.premium_amount).toLocaleString('en-IN')} /{' '}
          {policy.premium_frequency}
        </p>
        {policy.coverage_amount !== null && policy.coverage_amount !== undefined && (
          <p>Coverage: {Number(policy.coverage_amount).toLocaleString('en-IN')}</p>
        )}
      </div>
    </div>
  );
}
PolicyCard.propTypes = {
  policy: PropTypes.shape({
    id: PropTypes.string.isRequired,
    policy_name: PropTypes.string,
    provider: PropTypes.string,
    policy_type: PropTypes.string,
    premium_amount: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    premium_frequency: PropTypes.string,
    coverage_amount: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    is_active: PropTypes.bool,
  }).isRequired,
  onEdit: PropTypes.func.isRequired,
};

export const InsurancePolicies = () => {
  const { user } = useAuth();
  const adminId = user?.admin_id ?? null;
  const queryClient = useQueryClient();

  const [showAdd, setShowAdd] = useState(false);
  const [addForm, setAddForm] = useState(EMPTY_FORM);
  const [addError, setAddError] = useState(null);

  const [editingPolicy, setEditingPolicy] = useState(null);
  const [editForm, setEditForm] = useState(EMPTY_FORM);
  const [editError, setEditError] = useState(null);

  const policiesQuery = useQuery({
    queryKey: ['insurancePolicies', adminId],
    queryFn: () => listInsurancePolicies(adminId),
    enabled: Boolean(adminId),
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['insurancePolicies', adminId] });

  const createMutation = useMutation({
    mutationFn: (data) => createInsurancePolicy(adminId, data),
    onSuccess: () => {
      setShowAdd(false);
      setAddForm(EMPTY_FORM);
      invalidate();
    },
    onError: (err) => setAddError(err.message || 'Failed to create insurance policy'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateInsurancePolicy(id, adminId, data),
    onSuccess: () => {
      setEditingPolicy(null);
      invalidate();
    },
    onError: (err) => setEditError(err.message || 'Failed to update insurance policy'),
  });

  const deactivateMutation = useMutation({
    mutationFn: (id) => deactivateInsurancePolicy(id, adminId),
    onSuccess: () => {
      setEditingPolicy(null);
      invalidate();
    },
    onError: (err) => setEditError(err.message || 'Failed to deactivate insurance policy'),
  });

  const policies = policiesQuery.data?.insurance_policies ?? [];

  const handleAddChange = (e) => {
    const { name, value } = e.target;
    setAddForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleAddSubmit = (e) => {
    e.preventDefault();
    if (!addForm.policy_name.trim() || !addForm.provider.trim()) {
      setAddError('Policy name and provider are required');
      return;
    }
    setAddError(null);
    createMutation.mutate(formToPayload(addForm));
  };

  const handleEditOpen = (policy) => {
    setEditingPolicy(policy);
    setEditForm(policyToForm(policy));
    setEditError(null);
  };

  const handleEditChange = (e) => {
    const { name, value } = e.target;
    setEditForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleEditSubmit = (e) => {
    e.preventDefault();
    setEditError(null);
    updateMutation.mutate({ id: editingPolicy.id, data: formToPayload(editForm) });
  };

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Insurance Policies</h1>
          <p className="text-gray-500 mt-1">
            Household insurance policies feed the 30-70 Target and Insurance Free goals.
          </p>
        </div>
        {adminId && (
          <button
            type="button"
            onClick={() => {
              setAddForm(EMPTY_FORM);
              setAddError(null);
              setShowAdd(true);
            }}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-blue-700"
          >
            + Add Policy
          </button>
        )}
      </div>

      {!adminId && (
        <div className="text-center py-16 text-gray-400">
          Sign in as an admin to manage insurance policies.
        </div>
      )}

      {adminId && policiesQuery.isLoading && (
        <div className="text-center py-16 text-gray-500">Loading insurance policies...</div>
      )}

      {adminId && policiesQuery.isError && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
          Failed to load insurance policies. Please try again.
        </div>
      )}

      {adminId && !policiesQuery.isLoading && !policiesQuery.isError && policies.length === 0 && (
        <div className="text-center py-16 text-gray-400">No insurance policies found.</div>
      )}

      {adminId && !policiesQuery.isLoading && !policiesQuery.isError && policies.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {policies.map((p) => (
            <PolicyCard key={p.id} policy={p} onEdit={handleEditOpen} />
          ))}
        </div>
      )}

      {showAdd && (
        <Modal title="Add Insurance Policy" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAddSubmit} className="space-y-4">
            <Field label="Policy Name" required>
              <input
                name="policy_name"
                type="text"
                value={addForm.policy_name}
                onChange={handleAddChange}
                className={inputClass}
                placeholder="e.g. Family Term Cover"
              />
            </Field>
            <Field label="Provider" required>
              <input
                name="provider"
                type="text"
                value={addForm.provider}
                onChange={handleAddChange}
                className={inputClass}
                placeholder="e.g. LIC"
              />
            </Field>
            <Field label="Policy Type" required>
              <select
                name="policy_type"
                value={addForm.policy_type}
                onChange={handleAddChange}
                className={inputClass}
              >
                {POLICY_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t.replaceAll('_', ' ')}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Premium Amount" required>
              <input
                name="premium_amount"
                type="number"
                step="any"
                value={addForm.premium_amount}
                onChange={handleAddChange}
                className={inputClass}
              />
            </Field>
            <Field label="Premium Frequency" required>
              <select
                name="premium_frequency"
                value={addForm.premium_frequency}
                onChange={handleAddChange}
                className={inputClass}
              >
                {PREMIUM_FREQUENCIES.map((f) => (
                  <option key={f} value={f}>
                    {f}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Coverage Amount">
              <input
                name="coverage_amount"
                type="number"
                step="any"
                value={addForm.coverage_amount}
                onChange={handleAddChange}
                className={inputClass}
              />
            </Field>
            {addError && <p className="text-red-600 text-sm">{addError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowAdd(false)}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={createMutation.isPending}
                className="px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
              >
                {createMutation.isPending ? 'Saving...' : 'Add Policy'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {editingPolicy && (
        <Modal title={`Edit — ${editingPolicy.policy_name}`} onClose={() => setEditingPolicy(null)}>
          <form onSubmit={handleEditSubmit} className="space-y-4">
            <Field label="Policy Name" required>
              <input
                name="policy_name"
                type="text"
                value={editForm.policy_name}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Provider" required>
              <input
                name="provider"
                type="text"
                value={editForm.provider}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Premium Amount" required>
              <input
                name="premium_amount"
                type="number"
                step="any"
                value={editForm.premium_amount}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Premium Frequency" required>
              <select
                name="premium_frequency"
                value={editForm.premium_frequency}
                onChange={handleEditChange}
                className={inputClass}
              >
                {PREMIUM_FREQUENCIES.map((f) => (
                  <option key={f} value={f}>
                    {f}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Coverage Amount">
              <input
                name="coverage_amount"
                type="number"
                step="any"
                value={editForm.coverage_amount}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>

            {editingPolicy.is_active !== false ? (
              <div className="flex justify-end pt-2 border-t">
                <button
                  type="button"
                  onClick={() => deactivateMutation.mutate(editingPolicy.id)}
                  disabled={deactivateMutation.isPending}
                  className="text-sm font-medium text-red-600 hover:text-red-700 disabled:opacity-50"
                >
                  {deactivateMutation.isPending ? 'Deactivating...' : 'Deactivate this policy'}
                </button>
              </div>
            ) : (
              <div className="flex justify-end pt-2 border-t">
                <button
                  type="button"
                  onClick={() =>
                    updateMutation.mutate({ id: editingPolicy.id, data: { is_active: true } })
                  }
                  disabled={updateMutation.isPending}
                  className="text-sm font-medium text-blue-600 hover:text-blue-700 disabled:opacity-50"
                >
                  {updateMutation.isPending ? 'Reactivating...' : 'Reactivate policy'}
                </button>
              </div>
            )}

            {editError && <p className="text-red-600 text-sm">{editError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setEditingPolicy(null)}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={updateMutation.isPending}
                className="px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
              >
                {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default InsurancePolicies;
