import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { listProfiles } from '../../api/profiles';
import { createGoal, deleteGoal, listGoals, updateGoal } from '../../api/household';

const GOAL_STATUSES = ['ACTIVE', 'ACHIEVED', 'PAUSED'];

const STATUS_COLOURS = {
  ACTIVE: 'bg-green-100 text-green-800',
  ACHIEVED: 'bg-blue-100 text-blue-800',
  PAUSED: 'bg-amber-100 text-amber-800',
};

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

function formatCurrency(value) {
  if (value === null || value === undefined) return '—';
  return Number(value).toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  });
}

function progressBarColour(percent) {
  if (percent >= 100) return 'bg-green-500';
  if (percent >= 50) return 'bg-amber-400';
  return 'bg-red-400';
}

const EMPTY_FORM = {
  goal_name: '',
  target_amount: '',
  monthly_saving: '',
  target_date: '',
  notes: '',
};

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 w-full';

function Modal({ title, onClose, children }) {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-5 border-b">
          <h2 className="text-xl font-bold">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl leading-none"
          >
            &times;
          </button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  );
}

Modal.propTypes = {
  title: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
};

function Field({ label, required, children }) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-sm font-medium text-gray-700">
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
      </label>
      {children}
    </div>
  );
}

Field.propTypes = {
  label: PropTypes.string.isRequired,
  required: PropTypes.bool,
  children: PropTypes.node.isRequired,
};

Field.defaultProps = { required: false };

function ProgressBar({ percent }) {
  const capped = Math.min(percent ?? 0, 100);
  const colour = progressBarColour(capped);
  return (
    <div className="w-full bg-gray-200 rounded-full h-2 mt-1">
      <div
        className={`${colour} h-2 rounded-full transition-all`}
        style={{ width: `${capped}%` }}
        role="progressbar"
        aria-valuenow={capped}
        aria-valuemin={0}
        aria-valuemax={100}
      />
    </div>
  );
}

ProgressBar.propTypes = {
  percent: PropTypes.number,
};

ProgressBar.defaultProps = { percent: 0 };

function GoalCard({ goal, onEdit, onDelete }) {
  const [confirmDelete, setConfirmDelete] = useState(false);
  const statusColour = STATUS_COLOURS[goal.status] || 'bg-gray-100 text-gray-700';
  const percent = goal.progress_percent ?? 0;

  const daysLabel =
    goal.days_to_completion !== null && goal.days_to_completion !== undefined
      ? `~${goal.days_to_completion} days to go`
      : 'No estimate';

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5 flex flex-col gap-3">
      <div className="flex items-start justify-between gap-2">
        <h3 className="font-semibold text-gray-900 text-base">{goal.goal_name}</h3>
        <span
          className={`px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap ${statusColour}`}
        >
          {goal.status}
        </span>
      </div>

      <div>
        <div className="flex justify-between text-xs text-gray-500 mb-1">
          <span>{formatCurrency(goal.current_amount)} saved</span>
          <span>{formatCurrency(goal.target_amount)} goal</span>
        </div>
        <ProgressBar percent={percent} />
        <p className="text-xs text-gray-500 mt-1">{Math.round(percent)}% complete</p>
      </div>

      {goal.target_date && (
        <p className="text-xs text-gray-500">
          Target: {formatDate(goal.target_date)} &mdash; {daysLabel}
        </p>
      )}

      <p className="text-xs text-gray-400 italic">
        Progress is computed from your wealth accounts by the projection engine. Refresh the
        dashboard to update.
      </p>

      <div className="flex gap-2 pt-1">
        <button
          type="button"
          onClick={() => onEdit(goal)}
          className="flex-1 border border-indigo-500 text-indigo-600 py-1.5 rounded text-sm font-medium hover:bg-indigo-50"
        >
          Edit
        </button>
        {confirmDelete ? (
          <div className="flex gap-1 flex-1">
            <button
              type="button"
              onClick={() => onDelete(goal.id)}
              className="flex-1 bg-red-600 text-white rounded text-sm font-medium hover:bg-red-700 py-1.5"
            >
              Confirm?
            </button>
            <button
              type="button"
              onClick={() => setConfirmDelete(false)}
              className="flex-1 border border-gray-300 text-gray-600 rounded text-sm hover:bg-gray-50 py-1.5"
            >
              Cancel
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => setConfirmDelete(true)}
            className="flex-1 border border-red-500 text-red-500 py-1.5 rounded text-sm font-medium hover:bg-red-50"
          >
            Delete
          </button>
        )}
      </div>
    </div>
  );
}

GoalCard.propTypes = {
  goal: PropTypes.shape({
    id: PropTypes.string.isRequired,
    goal_name: PropTypes.string.isRequired,
    status: PropTypes.string.isRequired,
    target_amount: PropTypes.number.isRequired,
    current_amount: PropTypes.number.isRequired,
    progress_percent: PropTypes.number,
    days_to_completion: PropTypes.number,
    target_date: PropTypes.string,
  }).isRequired,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
};

export const Goals = () => {
  const [profiles, setProfiles] = useState([]);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [goals, setGoals] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [showAdd, setShowAdd] = useState(false);
  const [addForm, setAddForm] = useState(EMPTY_FORM);
  const [addError, setAddError] = useState(null);
  const [addSaving, setAddSaving] = useState(false);

  const [editingGoal, setEditingGoal] = useState(null);
  const [editForm, setEditForm] = useState({ ...EMPTY_FORM, status: 'ACTIVE' });
  const [editError, setEditError] = useState(null);
  const [editSaving, setEditSaving] = useState(false);

  useEffect(() => {
    listProfiles(null, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, []);

  const loadGoals = useCallback(async () => {
    if (!selectedProfileId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await listGoals(selectedProfileId, null);
      setGoals(data.goals ?? []);
    } catch (err) {
      setError(err.message || 'Failed to load goals');
    } finally {
      setLoading(false);
    }
  }, [selectedProfileId]);

  useEffect(() => {
    loadGoals();
  }, [loadGoals]);

  const handleAddChange = useCallback((e) => {
    const { name, value } = e.target;
    setAddForm((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleEditChange = useCallback((e) => {
    const { name, value } = e.target;
    setEditForm((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleAddSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!addForm.goal_name.trim()) {
        setAddError('Goal name is required');
        return;
      }
      if (!addForm.target_amount || Number(addForm.target_amount) <= 0) {
        setAddError('Target amount must be greater than 0');
        return;
      }
      setAddSaving(true);
      setAddError(null);
      try {
        await createGoal({
          profile_id: selectedProfileId,
          goal_name: addForm.goal_name.trim(),
          target_amount: Number(addForm.target_amount),
          monthly_saving: addForm.monthly_saving ? Number(addForm.monthly_saving) : null,
          target_date: addForm.target_date || null,
          notes: addForm.notes || null,
        });
        setShowAdd(false);
        setAddForm(EMPTY_FORM);
        await loadGoals();
      } catch (err) {
        setAddError(err.message || 'Failed to create goal');
      } finally {
        setAddSaving(false);
      }
    },
    [addForm, selectedProfileId, loadGoals]
  );

  const handleEditOpen = useCallback((goal) => {
    setEditingGoal(goal);
    setEditForm({
      goal_name: goal.goal_name,
      target_amount: String(goal.target_amount),
      monthly_saving:
        goal.monthly_saving !== null && goal.monthly_saving !== undefined
          ? String(goal.monthly_saving)
          : '',
      target_date: goal.target_date ?? '',
      notes: goal.notes ?? '',
      status: goal.status,
    });
    setEditError(null);
  }, []);

  const handleEditSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!editForm.goal_name.trim()) {
        setEditError('Goal name is required');
        return;
      }
      setEditSaving(true);
      setEditError(null);
      try {
        await updateGoal(editingGoal.id, {
          goal_name: editForm.goal_name.trim(),
          target_amount: editForm.target_amount ? Number(editForm.target_amount) : null,
          monthly_saving: editForm.monthly_saving ? Number(editForm.monthly_saving) : null,
          target_date: editForm.target_date || null,
          notes: editForm.notes || null,
          status: editForm.status || null,
        });
        setEditingGoal(null);
        await loadGoals();
      } catch (err) {
        setEditError(err.message || 'Failed to update goal');
      } finally {
        setEditSaving(false);
      }
    },
    [editingGoal, editForm, loadGoals]
  );

  const handleDelete = useCallback(
    async (id) => {
      try {
        await deleteGoal(id);
        await loadGoals();
      } catch (err) {
        setError(err.message || 'Failed to delete goal');
      }
    },
    [loadGoals]
  );

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Financial Goals</h1>
          <p className="text-gray-500 mt-1">Track savings targets and progress</p>
        </div>
        {selectedProfileId && (
          <button
            type="button"
            onClick={() => {
              setAddForm(EMPTY_FORM);
              setAddError(null);
              setShowAdd(true);
            }}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-indigo-700"
          >
            + Add Goal
          </button>
        )}
      </div>

      <div className="mb-6">
        <select
          value={selectedProfileId}
          onChange={(e) => setSelectedProfileId(e.target.value)}
          className={`${inputClass} sm:w-72`}
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
        <div className="text-center py-16 text-gray-400">Select a profile to view goals.</div>
      )}

      {selectedProfileId && (
        <>
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          {loading && <div className="text-center py-16 text-gray-500">Loading goals...</div>}

          {!loading && goals.length === 0 && (
            <div className="text-center py-16 text-gray-400">
              No goals yet. Add your first goal above.
            </div>
          )}

          {!loading && goals.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {goals.map((g) => (
                <GoalCard key={g.id} goal={g} onEdit={handleEditOpen} onDelete={handleDelete} />
              ))}
            </div>
          )}
        </>
      )}

      {showAdd && (
        <Modal title="Add Goal" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAddSubmit} className="space-y-4">
            <Field label="Goal Name" required>
              <input
                name="goal_name"
                type="text"
                value={addForm.goal_name}
                onChange={handleAddChange}
                placeholder="e.g. Emergency Fund"
                className={inputClass}
              />
            </Field>
            <Field label="Target Amount (₹)" required>
              <input
                name="target_amount"
                type="number"
                min="1"
                step="any"
                value={addForm.target_amount}
                onChange={handleAddChange}
                placeholder="e.g. 100000"
                className={inputClass}
              />
            </Field>
            <Field label="Monthly Saving (₹)">
              <input
                name="monthly_saving"
                type="number"
                min="0"
                step="any"
                value={addForm.monthly_saving}
                onChange={handleAddChange}
                placeholder="Optional"
                className={inputClass}
              />
            </Field>
            <Field label="Target Date">
              <input
                name="target_date"
                type="date"
                value={addForm.target_date}
                onChange={handleAddChange}
                min={todayISO()}
                className={inputClass}
              />
            </Field>
            <Field label="Notes">
              <textarea
                name="notes"
                value={addForm.notes}
                onChange={handleAddChange}
                placeholder="Optional"
                rows={3}
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
                disabled={addSaving}
                className="px-4 py-2 bg-indigo-600 text-white rounded text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
              >
                {addSaving ? 'Saving...' : 'Add Goal'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {editingGoal && (
        <Modal title="Edit Goal" onClose={() => setEditingGoal(null)}>
          <form onSubmit={handleEditSubmit} className="space-y-4">
            <Field label="Goal Name" required>
              <input
                name="goal_name"
                type="text"
                value={editForm.goal_name}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Target Amount (₹)">
              <input
                name="target_amount"
                type="number"
                min="1"
                step="any"
                value={editForm.target_amount}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Monthly Saving (₹)">
              <input
                name="monthly_saving"
                type="number"
                min="0"
                step="any"
                value={editForm.monthly_saving}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Target Date">
              <input
                name="target_date"
                type="date"
                value={editForm.target_date}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Status">
              <select
                name="status"
                value={editForm.status}
                onChange={handleEditChange}
                className={inputClass}
              >
                {GOAL_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s.charAt(0) + s.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Notes">
              <textarea
                name="notes"
                value={editForm.notes}
                onChange={handleEditChange}
                rows={3}
                className={inputClass}
              />
            </Field>
            {editError && <p className="text-red-600 text-sm">{editError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setEditingGoal(null)}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={editSaving}
                className="px-4 py-2 bg-indigo-600 text-white rounded text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
              >
                {editSaving ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default Goals;
