import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Field } from '../../components/Field';
import { Modal } from '../../components/Modal';
import { listProfiles } from '../../api/profiles';
import {
  createAccount,
  deactivateAccount,
  getAccountBalance,
  listAccounts,
  updateAccount,
  updateAccountClassification,
} from '../../api/wealth';
import { useAuth } from '../../hooks/useAuth';

const ACCOUNT_TYPES = [
  'SAVINGS',
  'CURRENT',
  'CREDIT_CARD',
  'HOME_LOAN',
  'PERSONAL_LOAN',
  'CAR_LOAN',
  'MUTUAL_FUND',
  'NPS',
  'PPF',
  'FD',
];
const TYPE_TABS = ['ALL', ...ACCOUNT_TYPES];
const LOAN_TYPES = new Set(['HOME_LOAN', 'PERSONAL_LOAN', 'CAR_LOAN']);

const EMPTY_ADD = {
  account_name: '',
  account_type: '',
  institution_name: '',
  opening_balance: '',
  credit_limit: '',
  interest_rate: '',
  emi_amount: '',
};
const EMPTY_EDIT = {
  account_name: '',
  opening_balance: '',
  credit_limit: '',
  interest_rate: '',
  emi_amount: '',
  is_active: true,
  loan_original_principal: '',
  loan_start_date: '',
  loan_tenure_months: '',
  linked_offset_account_id: '',
};

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

function buildLoanMetadataPatch(editForm) {
  const loanMeta = {};
  if (editForm.loan_original_principal) {
    loanMeta.loan_original_principal = editForm.loan_original_principal;
  }
  if (editForm.loan_start_date) loanMeta.loan_start_date = editForm.loan_start_date;
  if (editForm.loan_tenure_months) loanMeta.loan_tenure_months = editForm.loan_tenure_months;
  if (editForm.linked_offset_account_id) {
    loanMeta.linked_offset_account_id = editForm.linked_offset_account_id;
  }
  return loanMeta;
}

function formatCurrency(value) {
  if (value === null || value === undefined) return '—';
  return Number(value).toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  });
}

function StatusBadge({ active }) {
  const cls = active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-500';
  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${cls}`}>
      {active ? 'Active' : 'Inactive'}
    </span>
  );
}
StatusBadge.propTypes = { active: PropTypes.bool.isRequired };

function AccountCard({ account, balance, onEdit, onDeactivate }) {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5 flex flex-col gap-2">
      <div className="flex items-start justify-between">
        <div>
          <h3 className="font-semibold text-gray-900 text-lg">{account.account_name}</h3>
          <p className="text-sm text-gray-500">{account.institution_name}</p>
        </div>
        <StatusBadge active={account.is_active} />
      </div>
      <span className="self-start px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
        {account.account_type.replaceAll('_', ' ')}
      </span>
      <div className="text-sm text-gray-600 space-y-1">
        <p>
          Balance:{' '}
          {balance === undefined
            ? 'Loading...'
            : formatCurrency(balance ?? account.opening_balance)}
        </p>
        {account.credit_limit !== null &&
          account.credit_limit !== undefined &&
          account.credit_limit > 0 && <p>Credit Limit: {formatCurrency(account.credit_limit)}</p>}
        {account.interest_rate !== null &&
          account.interest_rate !== undefined &&
          account.interest_rate > 0 && <p>Interest Rate: {account.interest_rate}%</p>}
      </div>
      <div className="flex gap-2 pt-2">
        <button
          type="button"
          onClick={() => onEdit(account)}
          className="flex-1 border border-blue-600 text-blue-600 py-1.5 rounded text-sm font-medium hover:bg-blue-50"
        >
          Edit
        </button>
        {account.is_active && (
          <button
            type="button"
            onClick={() => onDeactivate(account)}
            className="flex-1 border border-red-500 text-red-500 py-1.5 rounded text-sm font-medium hover:bg-red-50"
          >
            Deactivate
          </button>
        )}
      </div>
    </div>
  );
}
AccountCard.propTypes = {
  account: PropTypes.shape({
    account_id: PropTypes.string.isRequired,
    account_name: PropTypes.string.isRequired,
    account_type: PropTypes.string.isRequired,
    institution_name: PropTypes.string.isRequired,
    opening_balance: PropTypes.number,
    credit_limit: PropTypes.number,
    interest_rate: PropTypes.number,
    is_active: PropTypes.bool.isRequired,
  }).isRequired,
  balance: PropTypes.number,
  onEdit: PropTypes.func.isRequired,
  onDeactivate: PropTypes.func.isRequired,
};
AccountCard.defaultProps = { balance: undefined };

function AccountFormFields({ form, onChange }) {
  const isLoan = LOAN_TYPES.has(form.account_type);
  const isCredit = form.account_type === 'CREDIT_CARD';
  return (
    <>
      <Field label="Account Name" required>
        <input
          name="account_name"
          type="text"
          value={form.account_name}
          onChange={onChange}
          placeholder="e.g. SBI Savings"
          className={inputClass}
        />
      </Field>
      {'account_type' in form && (
        <Field label="Account Type" required>
          <select
            name="account_type"
            value={form.account_type}
            onChange={onChange}
            className={inputClass}
          >
            <option value="">Select type</option>
            {ACCOUNT_TYPES.map((t) => (
              <option key={t} value={t}>
                {t.replaceAll('_', ' ')}
              </option>
            ))}
          </select>
        </Field>
      )}
      {'institution_name' in form && (
        <Field label="Institution Name" required>
          <input
            name="institution_name"
            type="text"
            value={form.institution_name}
            onChange={onChange}
            placeholder="e.g. State Bank of India"
            className={inputClass}
          />
        </Field>
      )}
      <Field label="Opening Balance">
        <input
          name="opening_balance"
          type="number"
          value={form.opening_balance}
          onChange={onChange}
          placeholder="0"
          className={inputClass}
        />
      </Field>
      {isCredit && (
        <Field label="Credit Limit">
          <input
            name="credit_limit"
            type="number"
            value={form.credit_limit}
            onChange={onChange}
            placeholder="Optional"
            className={inputClass}
          />
        </Field>
      )}
      {isLoan && (
        <>
          <Field label="Interest Rate (%)">
            <input
              name="interest_rate"
              type="number"
              value={form.interest_rate}
              onChange={onChange}
              placeholder="Optional"
              className={inputClass}
            />
          </Field>
          <Field label="EMI Amount">
            <input
              name="emi_amount"
              type="number"
              value={form.emi_amount}
              onChange={onChange}
              placeholder="Optional"
              className={inputClass}
            />
          </Field>
        </>
      )}
    </>
  );
}
AccountFormFields.propTypes = {
  form: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
};

export const Accounts = () => {
  const [profiles, setProfiles] = useState([]);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [accounts, setAccounts] = useState([]);
  const [balances, setBalances] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [activeFilter, setActiveFilter] = useState(null);

  const [savingsAccounts, setSavingsAccounts] = useState([]);

  const [showAdd, setShowAdd] = useState(false);
  const [addForm, setAddForm] = useState(EMPTY_ADD);
  const [addError, setAddError] = useState(null);
  const [addSaving, setAddSaving] = useState(false);

  const [editingAccount, setEditingAccount] = useState(null);
  const [editForm, setEditForm] = useState(EMPTY_EDIT);
  const [editError, setEditError] = useState(null);
  const [editSaving, setEditSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deactivating, setDeactivating] = useState(false);

  const { user } = useAuth();

  useEffect(() => {
    listProfiles(user?.admin_id, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, [user?.admin_id]);

  const loadAccounts = useCallback(async () => {
    if (!selectedProfileId) return;
    setLoading(true);
    setError(null);
    try {
      const typeArg = typeFilter === 'ALL' ? null : typeFilter;
      const data = await listAccounts(selectedProfileId, typeArg, activeFilter);
      setAccounts(data.accounts ?? []);
    } catch (err) {
      setError(err.message || 'Failed to load accounts');
    } finally {
      setLoading(false);
    }
  }, [selectedProfileId, typeFilter, activeFilter]);

  useEffect(() => {
    loadAccounts();
  }, [loadAccounts]);

  useEffect(() => {
    if (accounts.length === 0) {
      setBalances({});
      return undefined;
    }
    let cancelled = false;
    setBalances({});
    Promise.all(
      accounts.map((a) =>
        getAccountBalance(a.account_id, selectedProfileId)
          .then((data) => [a.account_id, data.current_balance])
          .catch(() => [a.account_id, null])
      )
    ).then((pairs) => {
      if (!cancelled) setBalances(Object.fromEntries(pairs));
    });
    return () => {
      cancelled = true;
    };
  }, [accounts, selectedProfileId]);

  const handleAddChange = useCallback((e) => {
    const { name, value } = e.target;
    setAddForm((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleAddSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!addForm.account_name.trim()) {
        setAddError('Account name is required');
        return;
      }
      if (!addForm.account_type) {
        setAddError('Account type is required');
        return;
      }
      if (!addForm.institution_name.trim()) {
        setAddError('Institution name is required');
        return;
      }
      setAddSaving(true);
      setAddError(null);
      try {
        await createAccount(selectedProfileId, {
          account_name: addForm.account_name.trim(),
          account_type: addForm.account_type,
          institution_name: addForm.institution_name.trim(),
          opening_balance: addForm.opening_balance === '' ? 0 : Number(addForm.opening_balance),
          credit_limit: addForm.credit_limit === '' ? null : Number(addForm.credit_limit),
          interest_rate: addForm.interest_rate === '' ? null : Number(addForm.interest_rate),
          emi_amount: addForm.emi_amount === '' ? null : Number(addForm.emi_amount),
        });
        setShowAdd(false);
        setAddForm(EMPTY_ADD);
        await loadAccounts();
      } catch (err) {
        setAddError(err.message || 'Failed to create account');
      } finally {
        setAddSaving(false);
      }
    },
    [addForm, selectedProfileId, loadAccounts]
  );

  const handleEditOpen = useCallback(
    (account) => {
      setEditingAccount(account);
      const meta = account.metadata || {};
      setEditForm({
        account_name: account.account_name,
        opening_balance: account.opening_balance ?? '',
        credit_limit: account.credit_limit ?? '',
        interest_rate: account.interest_rate ?? '',
        emi_amount: account.emi_amount ?? '',
        is_active: account.is_active,
        loan_original_principal: meta.original_principal ?? '',
        loan_start_date: meta.loan_start_date ?? '',
        loan_tenure_months: meta.original_tenure_months ?? '',
        linked_offset_account_id: meta.linked_offset_account_id ?? '',
      });
      setEditError(null);
      if (LOAN_TYPES.has(account.account_type) && selectedProfileId) {
        listAccounts(selectedProfileId, 'SAVINGS', true)
          .then((data) => setSavingsAccounts(data.accounts ?? []))
          .catch(() => setSavingsAccounts([]));
      }
    },
    [selectedProfileId]
  );

  const handleEditChange = useCallback((e) => {
    const { name, value, type, checked } = e.target;
    setEditForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  }, []);

  const handleEditSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      setEditSaving(true);
      setEditError(null);
      try {
        await updateAccount(editingAccount.account_id, selectedProfileId, {
          account_name: editForm.account_name || null,
          opening_balance:
            editForm.opening_balance === '' ? null : Number(editForm.opening_balance),
          credit_limit: editForm.credit_limit === '' ? null : Number(editForm.credit_limit),
          interest_rate: editForm.interest_rate === '' ? null : Number(editForm.interest_rate),
          emi_amount: editForm.emi_amount === '' ? null : Number(editForm.emi_amount),
          is_active: editForm.is_active,
        });
        if (LOAN_TYPES.has(editingAccount.account_type)) {
          const loanMeta = buildLoanMetadataPatch(editForm);
          if (Object.keys(loanMeta).length > 0) {
            await updateAccountClassification(
              editingAccount.account_id,
              selectedProfileId,
              loanMeta
            );
          }
        }
        setEditingAccount(null);
        await loadAccounts();
      } catch (err) {
        setEditError(err.message || 'Failed to update account');
      } finally {
        setEditSaving(false);
      }
    },
    [editingAccount, editForm, loadAccounts, selectedProfileId]
  );

  const handleDeactivate = useCallback(async () => {
    if (!confirmTarget) return;
    setDeactivating(true);
    try {
      await deactivateAccount(confirmTarget.account_id, selectedProfileId);
      setConfirmTarget(null);
      await loadAccounts();
    } catch (err) {
      setError(err.message || 'Failed to deactivate account');
      setConfirmTarget(null);
    } finally {
      setDeactivating(false);
    }
  }, [confirmTarget, loadAccounts, selectedProfileId]);

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Accounts</h1>
          <p className="text-gray-500 mt-1">Manage your financial accounts</p>
        </div>
        {selectedProfileId && (
          <button
            type="button"
            onClick={() => {
              setAddForm(EMPTY_ADD);
              setAddError(null);
              setShowAdd(true);
            }}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-blue-700"
          >
            + Add Account
          </button>
        )}
      </div>

      <div className="mb-6">
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
        <div className="text-center py-16 text-gray-400">Select a profile to view accounts.</div>
      )}

      {selectedProfileId && (
        <>
          <div className="flex flex-wrap gap-2 mb-4">
            {TYPE_TABS.map((t) => {
              const sel = typeFilter === t;
              const cls = sel
                ? 'bg-blue-600 text-white'
                : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50';
              return (
                <button
                  type="button"
                  key={t}
                  onClick={() => setTypeFilter(t)}
                  className={`px-3 py-1.5 rounded-full text-sm font-medium ${cls}`}
                >
                  {t.replaceAll('_', ' ')}
                </button>
              );
            })}
          </div>

          <div className="flex gap-2 mb-6">
            {[
              { label: 'All', value: null },
              { label: 'Active', value: true },
              { label: 'Inactive', value: false },
            ].map(({ label, value }) => {
              const sel = activeFilter === value;
              const cls = sel
                ? 'bg-blue-600 text-white'
                : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50';
              return (
                <button
                  type="button"
                  key={label}
                  onClick={() => setActiveFilter(value)}
                  className={`px-4 py-1.5 rounded-full text-sm font-medium ${cls}`}
                >
                  {label}
                </button>
              );
            })}
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}
          {loading && <div className="text-center py-16 text-gray-500">Loading accounts...</div>}
          {!loading && accounts.length === 0 && (
            <div className="text-center py-16 text-gray-400">No accounts found.</div>
          )}
          {!loading && accounts.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {accounts.map((a) => (
                <AccountCard
                  key={a.account_id}
                  account={a}
                  balance={balances[a.account_id]}
                  onEdit={handleEditOpen}
                  onDeactivate={setConfirmTarget}
                />
              ))}
            </div>
          )}
        </>
      )}

      {showAdd && (
        <Modal title="Add Account" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAddSubmit} className="space-y-4">
            <AccountFormFields form={addForm} onChange={handleAddChange} />
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
                disabled={addSaving}
                className="px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
              >
                {addSaving ? 'Saving...' : 'Add Account'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {editingAccount && (
        <Modal
          title={`Edit — ${editingAccount.account_name}`}
          onClose={() => setEditingAccount(null)}
        >
          <form onSubmit={handleEditSubmit} className="space-y-4">
            <AccountFormFields
              form={{ ...editForm, account_type: editingAccount.account_type }}
              onChange={handleEditChange}
            />
            {LOAN_TYPES.has(editingAccount.account_type) && (
              <div className="border-t border-gray-200 pt-4">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">
                  Loan Details
                </p>
                <div className="space-y-3">
                  <div className="flex flex-col gap-1">
                    <label
                      htmlFor="edit-loan-original-principal"
                      className="text-sm font-medium text-gray-700"
                    >
                      Original Principal (₹)
                    </label>
                    <input
                      id="edit-loan-original-principal"
                      name="loan_original_principal"
                      type="number"
                      value={editForm.loan_original_principal}
                      onChange={handleEditChange}
                      placeholder="e.g. 5000000"
                      className={inputClass}
                    />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label
                      htmlFor="edit-loan-start-date"
                      className="text-sm font-medium text-gray-700"
                    >
                      Loan Start Date
                    </label>
                    <input
                      id="edit-loan-start-date"
                      name="loan_start_date"
                      type="date"
                      value={editForm.loan_start_date}
                      onChange={handleEditChange}
                      className={inputClass}
                    />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label
                      htmlFor="edit-loan-tenure-months"
                      className="text-sm font-medium text-gray-700"
                    >
                      Tenure (months)
                    </label>
                    <input
                      id="edit-loan-tenure-months"
                      name="loan_tenure_months"
                      type="number"
                      min="1"
                      value={editForm.loan_tenure_months}
                      onChange={handleEditChange}
                      placeholder="e.g. 240"
                      className={inputClass}
                    />
                  </div>
                  {savingsAccounts.length > 0 && (
                    <div className="flex flex-col gap-1">
                      <label
                        htmlFor="edit-linked-offset-account"
                        className="text-sm font-medium text-gray-700"
                      >
                        Linked Offset Account
                      </label>
                      <select
                        id="edit-linked-offset-account"
                        name="linked_offset_account_id"
                        value={editForm.linked_offset_account_id}
                        onChange={handleEditChange}
                        className={inputClass}
                      >
                        <option value="">None</option>
                        {savingsAccounts.map((a) => (
                          <option key={a.account_id} value={a.account_id}>
                            {a.account_name}
                          </option>
                        ))}
                      </select>
                    </div>
                  )}
                </div>
              </div>
            )}
            <div className="flex items-center gap-3">
              <input
                id="edit-is-active"
                name="is_active"
                type="checkbox"
                checked={editForm.is_active}
                onChange={handleEditChange}
                className="w-4 h-4"
              />
              <label htmlFor="edit-is-active" className="text-sm font-medium text-gray-700">
                Active
              </label>
            </div>
            {editError && <p className="text-red-600 text-sm">{editError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setEditingAccount(null)}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={editSaving}
                className="px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
              >
                {editSaving ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {confirmTarget && (
        <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-sm">
            <h3 className="text-lg font-bold mb-2">Deactivate Account</h3>
            <p className="text-gray-600 text-sm mb-6">
              Deactivate <strong>{confirmTarget.account_name}</strong>? It can be re-activated via
              edit.
            </p>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setConfirmTarget(null)}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDeactivate}
                disabled={deactivating}
                className="px-4 py-2 bg-red-600 text-white rounded text-sm font-medium hover:bg-red-700 disabled:opacity-50"
              >
                {deactivating ? 'Deactivating...' : 'Deactivate'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Accounts;
