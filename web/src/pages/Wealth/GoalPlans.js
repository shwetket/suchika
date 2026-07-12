import React, { useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listProfiles } from '../../api/profiles';
import {
  createGoalPlan,
  deactivateGoalPlan,
  listGoalPlans,
  replaceGoalPlanMilestones,
  replaceGoalPlanRules,
  replaceGoalPlanTriggerEvents,
  updateGoalPlan,
  updateGoalPlanMilestoneAchieved,
} from '../../api/wealth';
import { Field } from '../../components/Field';
import { Modal } from '../../components/Modal';
import { Badge } from '../../components/shared/Badge';
import { useAuth } from '../../hooks/useAuth';

// The 5 hardcoded Epic 8 formula goals (ADR-022) — 4 singletons + YEAR_ONE (per-child).
const SINGLETON_GOAL_TYPES = [
  { goal_type: 'DEBT_CROSSOVER', label: 'Debt Crossover' },
  { goal_type: 'THIRTY_SEVENTY_TARGET', label: '30-70 Target' },
  { goal_type: 'FREEDOM_RUNWAY', label: 'Freedom Runway' },
  { goal_type: 'INSURANCE_FREE', label: 'Insurance Free' },
];

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm w-full focus:outline-none focus:ring-2 focus:ring-blue-500';

const EMPTY_PLAN_FORM = {
  objective: '',
  target_state: '',
  assumed_growth_rate: '',
  education_base_cost: '',
  education_inflation_rate: '',
  education_years_to_entry: '',
};

function planToForm(plan) {
  if (!plan) return EMPTY_PLAN_FORM;
  return {
    objective: plan.objective || '',
    target_state: plan.target_state ?? '',
    assumed_growth_rate: plan.assumed_growth_rate ?? '',
    education_base_cost: plan.education_base_cost ?? '',
    education_inflation_rate: plan.education_inflation_rate ?? '',
    education_years_to_entry: plan.education_years_to_entry ?? '',
  };
}

function formToPayload(form, isYearOne) {
  const payload = {
    objective: form.objective.trim(),
    target_state: form.target_state.trim() ? form.target_state.trim() : null,
    assumed_growth_rate: form.assumed_growth_rate === '' ? null : Number(form.assumed_growth_rate),
  };
  if (isYearOne) {
    payload.education_base_cost =
      form.education_base_cost === '' ? null : Number(form.education_base_cost);
    payload.education_inflation_rate =
      form.education_inflation_rate === '' ? null : Number(form.education_inflation_rate);
    payload.education_years_to_entry =
      form.education_years_to_entry === '' ? null : Number(form.education_years_to_entry);
  }
  return payload;
}

function findPlan(goalPlans, goalType, beneficiaryProfileId) {
  return (
    goalPlans.find(
      (p) =>
        p.goal_type === goalType &&
        (beneficiaryProfileId
          ? p.beneficiary_profile_id === beneficiaryProfileId
          : !p.beneficiary_profile_id)
    ) || null
  );
}

function GoalCard({ label, plan, onConfigure, onManageDetail }) {
  const configured = Boolean(plan);
  const active = configured && plan.is_active !== false;
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5 flex flex-col gap-3">
      <div className="flex items-start justify-between">
        <h3 className="font-semibold text-gray-900 text-lg">{label}</h3>
        {configured ? (
          <Badge variant={active ? 'success' : 'neutral'}>
            {active ? 'Configured' : 'Inactive'}
          </Badge>
        ) : (
          <Badge variant="warning">Not configured</Badge>
        )}
      </div>
      {configured && (
        <div className="text-sm text-gray-600 space-y-1">
          <p className="line-clamp-2">{plan.objective}</p>
          {plan.target_state && <p className="text-xs text-gray-400">{plan.target_state}</p>}
        </div>
      )}
      <div className="flex gap-3 mt-1">
        <button
          type="button"
          onClick={onConfigure}
          className="text-sm font-medium text-blue-600 hover:text-blue-700"
        >
          {configured ? 'Edit objective' : 'Configure'}
        </button>
        {configured && (
          <button
            type="button"
            onClick={onManageDetail}
            className="text-sm font-medium text-indigo-600 hover:text-indigo-700"
          >
            Milestones, rules &amp; triggers
          </button>
        )}
      </div>
    </div>
  );
}
GoalCard.propTypes = {
  label: PropTypes.string.isRequired,
  plan: PropTypes.shape({
    id: PropTypes.string,
    objective: PropTypes.string,
    target_state: PropTypes.string,
    is_active: PropTypes.bool,
  }),
  onConfigure: PropTypes.func.isRequired,
  onManageDetail: PropTypes.func.isRequired,
};
GoalCard.defaultProps = { plan: null };

function PlanFormModal({
  goalType,
  beneficiaryProfileId,
  beneficiaryName,
  plan,
  adminId,
  onClose,
  onSaved,
}) {
  const isYearOne = goalType === 'YEAR_ONE';
  const [form, setForm] = useState(planToForm(plan));
  const [error, setError] = useState(null);

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = formToPayload(form, isYearOne);
      if (plan) {
        return updateGoalPlan(plan.id, adminId, payload);
      }
      return createGoalPlan(adminId, {
        goal_type: goalType,
        beneficiary_profile_id: beneficiaryProfileId || null,
        ...payload,
      });
    },
    onSuccess: () => onSaved(),
    onError: (err) => setError(err.message || 'Failed to save goal plan'),
  });

  const deactivateMutation = useMutation({
    mutationFn: () => deactivateGoalPlan(plan.id, adminId),
    onSuccess: () => onSaved(),
    onError: (err) => setError(err.message || 'Failed to deactivate goal plan'),
  });

  const reactivateMutation = useMutation({
    mutationFn: () => updateGoalPlan(plan.id, adminId, { is_active: true }),
    onSuccess: () => onSaved(),
    onError: (err) => setError(err.message || 'Failed to reactivate goal plan'),
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.objective.trim()) {
      setError('Objective is required');
      return;
    }
    setError(null);
    saveMutation.mutate();
  };

  const saving = saveMutation.isPending;
  const isActive = plan ? plan.is_active !== false : true;
  const actionVerb = plan ? 'Edit' : 'Configure';

  return (
    <Modal
      title={
        isYearOne
          ? `${actionVerb} Year One — ${beneficiaryName}`
          : `${actionVerb} Goal Plan`
      }
      onClose={onClose}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <Field label="Objective" required>
          <textarea
            name="objective"
            value={form.objective}
            onChange={handleChange}
            rows={2}
            className={inputClass}
            placeholder="e.g. Family mutual fund corpus exceeds outstanding debt"
          />
        </Field>
        <Field label="Target State">
          <textarea
            name="target_state"
            value={form.target_state}
            onChange={handleChange}
            rows={2}
            className={inputClass}
          />
        </Field>
        <Field label="Assumed Growth Rate">
          <input
            name="assumed_growth_rate"
            type="number"
            step="any"
            value={form.assumed_growth_rate}
            onChange={handleChange}
            className={inputClass}
            placeholder="e.g. 0.12"
          />
        </Field>
        {isYearOne && (
          <>
            <Field label="Education Base Cost">
              <input
                name="education_base_cost"
                type="number"
                step="any"
                value={form.education_base_cost}
                onChange={handleChange}
                className={inputClass}
                placeholder="e.g. 1000000"
              />
            </Field>
            <Field label="Education Inflation Rate">
              <input
                name="education_inflation_rate"
                type="number"
                step="any"
                value={form.education_inflation_rate}
                onChange={handleChange}
                className={inputClass}
                placeholder="e.g. 0.08"
              />
            </Field>
            <Field label="Years to Entry">
              <input
                name="education_years_to_entry"
                type="number"
                step="1"
                value={form.education_years_to_entry}
                onChange={handleChange}
                className={inputClass}
                placeholder="e.g. 10"
              />
            </Field>
          </>
        )}

        {plan && isActive && (
          <div className="flex justify-end pt-2 border-t">
            <button
              type="button"
              onClick={() => deactivateMutation.mutate()}
              disabled={deactivateMutation.isPending}
              className="text-sm font-medium text-red-600 hover:text-red-700 disabled:opacity-50"
            >
              {deactivateMutation.isPending ? 'Deactivating...' : 'Deactivate this goal plan'}
            </button>
          </div>
        )}
        {plan && !isActive && (
          <div className="flex justify-end pt-2 border-t">
            <button
              type="button"
              onClick={() => reactivateMutation.mutate()}
              disabled={reactivateMutation.isPending}
              className="text-sm font-medium text-blue-600 hover:text-blue-700 disabled:opacity-50"
            >
              {reactivateMutation.isPending ? 'Reactivating...' : 'Reactivate goal plan'}
            </button>
          </div>
        )}

        {error && <p className="text-red-600 text-sm">{error}</p>}

        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={saving}
            className="px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
          >
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
PlanFormModal.propTypes = {
  goalType: PropTypes.string.isRequired,
  beneficiaryProfileId: PropTypes.string,
  beneficiaryName: PropTypes.string,
  plan: PropTypes.shape({
    id: PropTypes.string,
    objective: PropTypes.string,
    target_state: PropTypes.string,
    is_active: PropTypes.bool,
  }),
  adminId: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
  onSaved: PropTypes.func.isRequired,
};
PlanFormModal.defaultProps = { beneficiaryProfileId: null, beneficiaryName: null, plan: null };

let localRowId = 0;
function nextLocalId() {
  localRowId += 1;
  return `local-${localRowId}`;
}

function reorder(list) {
  return list.map((item, idx) => ({ ...item, sequence_no: idx }));
}

function MilestoneEditor({ plan, adminId, milestones, setMilestones }) {
  const queryClient = useQueryClient();
  const [error, setError] = useState(null);

  const saveMutation = useMutation({
    mutationFn: () =>
      replaceGoalPlanMilestones(
        plan.id,
        adminId,
        milestones.map(({ localId, ...rest }) => rest)
      ),
    onSuccess: (saved) => {
      setMilestones(saved.map((m) => ({ ...m, localId: m.id })));
      queryClient.invalidateQueries({ queryKey: ['goalPlans', adminId] });
    },
    onError: (err) => setError(err.message || 'Failed to save milestones'),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ milestoneId, isAchieved }) =>
      updateGoalPlanMilestoneAchieved(plan.id, milestoneId, adminId, isAchieved),
    onError: (err) => setError(err.message || 'Failed to update checklist status'),
  });

  const updateRow = (localId, patchObj) => {
    setMilestones((prev) => prev.map((m) => (m.localId === localId ? { ...m, ...patchObj } : m)));
  };

  const removeRow = (localId) => {
    setMilestones((prev) => reorder(prev.filter((m) => m.localId !== localId)));
  };

  const moveRow = (localId, direction) => {
    setMilestones((prev) => {
      const idx = prev.findIndex((m) => m.localId === localId);
      const swapIdx = idx + direction;
      if (idx < 0 || swapIdx < 0 || swapIdx >= prev.length) return prev;
      const next = [...prev];
      [next[idx], next[swapIdx]] = [next[swapIdx], next[idx]];
      return reorder(next);
    });
  };

  const addRow = () => {
    setMilestones((prev) =>
      reorder([
        ...prev,
        {
          localId: nextLocalId(),
          label: '',
          target_value: null,
          is_manual_checklist: false,
          is_achieved: false,
          significance: '',
        },
      ])
    );
  };

  const handleToggleAchieved = (milestone) => {
    const isAchieved = !milestone.is_achieved;
    updateRow(milestone.localId, { is_achieved: isAchieved });
    toggleMutation.mutate({ milestoneId: milestone.id, isAchieved });
  };

  return (
    <div className="space-y-3">
      <h4 className="text-sm font-semibold text-gray-700">Milestones</h4>
      {milestones.map((m, idx) => (
        <div key={m.localId} className="border border-gray-200 rounded p-3 space-y-2">
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Label"
              value={m.label}
              onChange={(e) => updateRow(m.localId, { label: e.target.value })}
              className={inputClass}
            />
            <div className="flex gap-1">
              <button
                type="button"
                aria-label="Move milestone up"
                onClick={() => moveRow(m.localId, -1)}
                disabled={idx === 0}
                className="px-2 border border-gray-300 rounded text-gray-500 disabled:opacity-30"
              >
                ↑
              </button>
              <button
                type="button"
                aria-label="Move milestone down"
                onClick={() => moveRow(m.localId, 1)}
                disabled={idx === milestones.length - 1}
                className="px-2 border border-gray-300 rounded text-gray-500 disabled:opacity-30"
              >
                ↓
              </button>
              <button
                type="button"
                aria-label="Remove milestone"
                onClick={() => removeRow(m.localId)}
                className="px-2 border border-gray-300 rounded text-red-500"
              >
                &times;
              </button>
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-gray-600">
            <input
              type="checkbox"
              checked={m.is_manual_checklist}
              onChange={(e) => updateRow(m.localId, { is_manual_checklist: e.target.checked })}
            />
            {' '}
            Manual checklist item
          </label>
          {!m.is_manual_checklist && (
            <input
              type="number"
              step="any"
              placeholder="Target value"
              value={m.target_value ?? ''}
              onChange={(e) =>
                updateRow(m.localId, {
                  target_value: e.target.value === '' ? null : Number(e.target.value),
                })
              }
              className={inputClass}
            />
          )}
          <input
            type="text"
            placeholder="Significance"
            value={m.significance}
            onChange={(e) => updateRow(m.localId, { significance: e.target.value })}
            className={inputClass}
          />
          {m.is_manual_checklist && m.id && (
            <label className="flex items-center gap-2 text-sm font-medium text-indigo-700">
              <input
                type="checkbox"
                checked={Boolean(m.is_achieved)}
                onChange={() => handleToggleAchieved(m)}
                aria-label={`Mark "${m.label}" achieved`}
              />
              {' '}
              Mark done (saves immediately)
            </label>
          )}
        </div>
      ))}
      <div className="flex justify-between items-center">
        <button
          type="button"
          onClick={addRow}
          className="text-sm font-medium text-blue-600 hover:text-blue-700"
        >
          + Add milestone
        </button>
        <button
          type="button"
          onClick={() => {
            setError(null);
            saveMutation.mutate();
          }}
          disabled={saveMutation.isPending}
          className="px-3 py-1.5 bg-indigo-600 text-white rounded text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
        >
          {saveMutation.isPending ? 'Saving...' : 'Save milestones'}
        </button>
      </div>
      {error && <p className="text-red-600 text-sm">{error}</p>}
    </div>
  );
}
MilestoneEditor.propTypes = {
  plan: PropTypes.shape({ id: PropTypes.string.isRequired }).isRequired,
  adminId: PropTypes.string.isRequired,
  milestones: PropTypes.array.isRequired,
  setMilestones: PropTypes.func.isRequired,
};

function RuleEditor({ plan, adminId, rules, setRules }) {
  const [error, setError] = useState(null);
  const saveMutation = useMutation({
    mutationFn: () =>
      replaceGoalPlanRules(
        plan.id,
        adminId,
        rules.map(({ localId, ...rest }) => rest)
      ),
    onSuccess: (saved) => setRules(saved.map((r) => ({ ...r, localId: r.id }))),
    onError: (err) => setError(err.message || 'Failed to save rules'),
  });

  const updateRow = (localId, patchObj) =>
    setRules((prev) => prev.map((r) => (r.localId === localId ? { ...r, ...patchObj } : r)));
  const removeRow = (localId) =>
    setRules((prev) => reorder(prev.filter((r) => r.localId !== localId)));
  const addRow = () =>
    setRules((prev) =>
      reorder([...prev, { localId: nextLocalId(), rule_name: '', rule_text: '' }])
    );

  return (
    <div className="space-y-3">
      <h4 className="text-sm font-semibold text-gray-700">Rules</h4>
      {rules.map((r) => (
        <div key={r.localId} className="border border-gray-200 rounded p-3 space-y-2">
          <input
            type="text"
            placeholder="Rule name"
            value={r.rule_name}
            onChange={(e) => updateRow(r.localId, { rule_name: e.target.value })}
            className={inputClass}
          />
          <textarea
            placeholder="Rule text"
            value={r.rule_text}
            onChange={(e) => updateRow(r.localId, { rule_text: e.target.value })}
            rows={2}
            className={inputClass}
          />
          <button
            type="button"
            onClick={() => removeRow(r.localId)}
            className="text-xs text-red-500"
          >
            Remove
          </button>
        </div>
      ))}
      <div className="flex justify-between items-center">
        <button
          type="button"
          onClick={addRow}
          className="text-sm font-medium text-blue-600 hover:text-blue-700"
        >
          + Add rule
        </button>
        <button
          type="button"
          onClick={() => {
            setError(null);
            saveMutation.mutate();
          }}
          disabled={saveMutation.isPending}
          className="px-3 py-1.5 bg-indigo-600 text-white rounded text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
        >
          {saveMutation.isPending ? 'Saving...' : 'Save rules'}
        </button>
      </div>
      {error && <p className="text-red-600 text-sm">{error}</p>}
    </div>
  );
}
RuleEditor.propTypes = {
  plan: PropTypes.shape({ id: PropTypes.string.isRequired }).isRequired,
  adminId: PropTypes.string.isRequired,
  rules: PropTypes.array.isRequired,
  setRules: PropTypes.func.isRequired,
};

function TriggerEventEditor({ plan, adminId, triggerEvents, setTriggerEvents }) {
  const [error, setError] = useState(null);
  const saveMutation = useMutation({
    mutationFn: () =>
      replaceGoalPlanTriggerEvents(
        plan.id,
        adminId,
        triggerEvents.map(({ localId, ...rest }) => rest)
      ),
    onSuccess: (saved) => setTriggerEvents(saved.map((t) => ({ ...t, localId: t.id }))),
    onError: (err) => setError(err.message || 'Failed to save trigger events'),
  });

  const updateRow = (localId, patchObj) =>
    setTriggerEvents((prev) =>
      prev.map((t) => (t.localId === localId ? { ...t, ...patchObj } : t))
    );
  const removeRow = (localId) =>
    setTriggerEvents((prev) => reorder(prev.filter((t) => t.localId !== localId)));
  const addRow = () =>
    setTriggerEvents((prev) =>
      reorder([
        ...prev,
        { localId: nextLocalId(), event_name: '', trigger_condition: '', resulting_change: '' },
      ])
    );

  return (
    <div className="space-y-3">
      <h4 className="text-sm font-semibold text-gray-700">Trigger Events</h4>
      {triggerEvents.map((t) => (
        <div key={t.localId} className="border border-gray-200 rounded p-3 space-y-2">
          <input
            type="text"
            placeholder="Event name"
            value={t.event_name}
            onChange={(e) => updateRow(t.localId, { event_name: e.target.value })}
            className={inputClass}
          />
          <textarea
            placeholder="Trigger condition"
            value={t.trigger_condition}
            onChange={(e) => updateRow(t.localId, { trigger_condition: e.target.value })}
            rows={2}
            className={inputClass}
          />
          <textarea
            placeholder="Resulting change"
            value={t.resulting_change}
            onChange={(e) => updateRow(t.localId, { resulting_change: e.target.value })}
            rows={2}
            className={inputClass}
          />
          <button
            type="button"
            onClick={() => removeRow(t.localId)}
            className="text-xs text-red-500"
          >
            Remove
          </button>
        </div>
      ))}
      <div className="flex justify-between items-center">
        <button
          type="button"
          onClick={addRow}
          className="text-sm font-medium text-blue-600 hover:text-blue-700"
        >
          + Add trigger event
        </button>
        <button
          type="button"
          onClick={() => {
            setError(null);
            saveMutation.mutate();
          }}
          disabled={saveMutation.isPending}
          className="px-3 py-1.5 bg-indigo-600 text-white rounded text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
        >
          {saveMutation.isPending ? 'Saving...' : 'Save trigger events'}
        </button>
      </div>
      {error && <p className="text-red-600 text-sm">{error}</p>}
    </div>
  );
}
TriggerEventEditor.propTypes = {
  plan: PropTypes.shape({ id: PropTypes.string.isRequired }).isRequired,
  adminId: PropTypes.string.isRequired,
  triggerEvents: PropTypes.array.isRequired,
  setTriggerEvents: PropTypes.func.isRequired,
};

function GoalDetailModal({ plan, adminId, onClose }) {
  const [milestones, setMilestones] = useState(
    (plan.milestones || []).map((m) => ({ ...m, localId: m.id || nextLocalId() }))
  );
  const [rules, setRules] = useState(
    (plan.rules || []).map((r) => ({ ...r, localId: r.id || nextLocalId() }))
  );
  const [triggerEvents, setTriggerEvents] = useState(
    (plan.trigger_events || []).map((t) => ({ ...t, localId: t.id || nextLocalId() }))
  );

  return (
    <Modal title={`Milestones, Rules & Triggers — ${plan.goal_type}`} onClose={onClose}>
      <div className="space-y-6">
        <MilestoneEditor
          plan={plan}
          adminId={adminId}
          milestones={milestones}
          setMilestones={setMilestones}
        />
        <RuleEditor plan={plan} adminId={adminId} rules={rules} setRules={setRules} />
        <TriggerEventEditor
          plan={plan}
          adminId={adminId}
          triggerEvents={triggerEvents}
          setTriggerEvents={setTriggerEvents}
        />
        <div className="flex justify-end pt-2 border-t">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
          >
            Close
          </button>
        </div>
      </div>
    </Modal>
  );
}
GoalDetailModal.propTypes = {
  plan: PropTypes.shape({
    id: PropTypes.string.isRequired,
    goal_type: PropTypes.string,
    milestones: PropTypes.array,
    rules: PropTypes.array,
    trigger_events: PropTypes.array,
  }).isRequired,
  adminId: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
};

export const GoalPlans = () => {
  const { user } = useAuth();
  const adminId = user?.admin_id ?? null;
  const queryClient = useQueryClient();

  const [formTarget, setFormTarget] = useState(null);
  const [detailTarget, setDetailTarget] = useState(null);

  const goalPlansQuery = useQuery({
    queryKey: ['goalPlans', adminId],
    queryFn: () => listGoalPlans(adminId),
    enabled: Boolean(adminId),
  });

  const childrenQuery = useQuery({
    queryKey: ['profiles', adminId],
    queryFn: () => listProfiles(adminId, true),
    enabled: Boolean(adminId),
  });

  const goalPlans = goalPlansQuery.data?.goal_plans ?? [];
  const children = useMemo(
    () => (childrenQuery.data?.profiles ?? []).filter((p) => p.relation_to_admin === 'CHILD'),
    [childrenQuery.data]
  );

  const handleSaved = () => {
    setFormTarget(null);
    queryClient.invalidateQueries({ queryKey: ['goalPlans', adminId] });
  };

  const isLoading = goalPlansQuery.isLoading || childrenQuery.isLoading;
  const isError = goalPlansQuery.isError || childrenQuery.isError;

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Goal Plans</h1>
        <p className="text-gray-500 mt-1">
          Configure objectives, milestones, rules and step-up triggers for the household's 5 formula
          goals.
        </p>
      </div>

      {!adminId && (
        <div className="text-center py-16 text-gray-400">
          Sign in as an admin to manage goal plans.
        </div>
      )}

      {adminId && isLoading && (
        <div className="text-center py-16 text-gray-500">Loading goal plans...</div>
      )}

      {adminId && isError && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
          Failed to load goal plans. Please try again.
        </div>
      )}

      {adminId && !isLoading && !isError && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {SINGLETON_GOAL_TYPES.map(({ goal_type: goalType, label }) => {
            const plan = findPlan(goalPlans, goalType, null);
            return (
              <GoalCard
                key={goalType}
                label={label}
                plan={plan}
                onConfigure={() =>
                  setFormTarget({
                    goalType,
                    beneficiaryProfileId: null,
                    beneficiaryName: null,
                    plan,
                  })
                }
                onManageDetail={() => setDetailTarget(plan)}
              />
            );
          })}
          {children.map((child) => {
            const plan = findPlan(goalPlans, 'YEAR_ONE', child.profile_id);
            return (
              <GoalCard
                key={child.profile_id}
                label={`Year One — ${child.full_name}`}
                plan={plan}
                onConfigure={() =>
                  setFormTarget({
                    goalType: 'YEAR_ONE',
                    beneficiaryProfileId: child.profile_id,
                    beneficiaryName: child.full_name,
                    plan,
                  })
                }
                onManageDetail={() => setDetailTarget(plan)}
              />
            );
          })}
          {children.length === 0 && (
            <div className="sm:col-span-2 text-center py-6 text-gray-400 text-sm border border-dashed border-gray-200 rounded-lg">
              No child profiles found — Year One goals apply to CHILD-relation household members.
            </div>
          )}
        </div>
      )}

      {formTarget && (
        <PlanFormModal
          goalType={formTarget.goalType}
          beneficiaryProfileId={formTarget.beneficiaryProfileId}
          beneficiaryName={formTarget.beneficiaryName}
          plan={formTarget.plan}
          adminId={adminId}
          onClose={() => setFormTarget(null)}
          onSaved={handleSaved}
        />
      )}

      {detailTarget && (
        <GoalDetailModal
          plan={detailTarget}
          adminId={adminId}
          onClose={() => setDetailTarget(null)}
        />
      )}
    </div>
  );
};

export default GoalPlans;
