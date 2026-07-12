import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GoalPlans } from './GoalPlans';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/wealth', () => ({
  listGoalPlans: jest.fn(),
  createGoalPlan: jest.fn(),
  updateGoalPlan: jest.fn(),
  deactivateGoalPlan: jest.fn(),
  replaceGoalPlanMilestones: jest.fn(),
  replaceGoalPlanRules: jest.fn(),
  replaceGoalPlanTriggerEvents: jest.fn(),
  updateGoalPlanMilestoneAchieved: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const {
  listGoalPlans,
  createGoalPlan,
  updateGoalPlan,
  deactivateGoalPlan,
  replaceGoalPlanMilestones,
  replaceGoalPlanRules,
  replaceGoalPlanTriggerEvents,
  updateGoalPlanMilestoneAchieved,
} = require('../../api/wealth');

const MOCK_CHILDREN = [
  { profile_id: 'child-1', full_name: 'Aanya', relation_to_admin: 'CHILD' },
  { profile_id: 'p-self', full_name: 'Ketan', relation_to_admin: 'SELF' },
];

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <GoalPlans />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  mockUseAuth.mockReturnValue({ user: { admin_id: 'admin-1' } });
  listProfiles.mockResolvedValue({ profiles: MOCK_CHILDREN });
  listGoalPlans.mockResolvedValue({ goal_plans: [] });
});

describe('GoalPlans page', () => {
  it('prompts sign-in when no admin_id is available', () => {
    mockUseAuth.mockReturnValue({ user: null });
    renderPage();
    expect(screen.getByText(/sign in as an admin/i)).toBeInTheDocument();
  });

  it('shows loading state while fetching goal plans', async () => {
    listGoalPlans.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ goal_plans: [] }), 200))
    );
    renderPage();
    expect(screen.getByText(/loading goal plans/i)).toBeInTheDocument();
    await waitFor(() => screen.getByText('Debt Crossover'));
  });

  it('shows an error banner when loading goal plans fails', async () => {
    listGoalPlans.mockRejectedValue(new Error('network error'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/failed to load goal plans/i)).toBeInTheDocument();
    });
  });

  it('renders the 4 singleton goal types and one Year One card per child', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Debt Crossover')).toBeInTheDocument();
      expect(screen.getByText('30-70 Target')).toBeInTheDocument();
      expect(screen.getByText('Freedom Runway')).toBeInTheDocument();
      expect(screen.getByText('Insurance Free')).toBeInTheDocument();
      expect(screen.getByText('Year One — Aanya')).toBeInTheDocument();
    });
    // SELF-relation profile must not get a Year One card
    expect(screen.queryByText(/Year One — Ketan/i)).not.toBeInTheDocument();
    // Nothing configured yet → every card shows "Not configured"
    expect(screen.getAllByText('Not configured')).toHaveLength(5);
  });

  it('shows Configured badge and objective text for an existing goal plan', async () => {
    listGoalPlans.mockResolvedValue({
      goal_plans: [
        {
          id: 'plan-1',
          goal_type: 'DEBT_CROSSOVER',
          beneficiary_profile_id: null,
          objective: 'Reduce debt below MF corpus',
          target_state: 'Debt fully covered',
          is_active: true,
          milestones: [],
          rules: [],
          trigger_events: [],
        },
      ],
    });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
      expect(screen.getByText('Reduce debt below MF corpus')).toBeInTheDocument();
    });
  });

  it('creates a new goal plan via the Configure form', async () => {
    createGoalPlan.mockResolvedValue({
      id: 'plan-new',
      goal_type: 'DEBT_CROSSOVER',
      objective: 'New objective',
      is_active: true,
      milestones: [],
      rules: [],
      trigger_events: [],
    });

    renderPage();
    await waitFor(() => screen.getByText('Debt Crossover'));

    const debtCard = screen.getByText('Debt Crossover').closest('div');
    fireEvent.click(within(debtCard.parentElement).getByText('Configure'));

    const objectiveInput = await screen.findByPlaceholderText(/family mutual fund corpus/i);
    fireEvent.change(objectiveInput, { target: { value: 'New objective' } });
    fireEvent.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => {
      expect(createGoalPlan).toHaveBeenCalledWith(
        'admin-1',
        expect.objectContaining({ goal_type: 'DEBT_CROSSOVER', objective: 'New objective' })
      );
    });
  });

  it('requires an objective before creating a goal plan', async () => {
    renderPage();
    await waitFor(() => screen.getByText('Debt Crossover'));

    const debtCard = screen.getByText('Debt Crossover').closest('div');
    fireEvent.click(within(debtCard.parentElement).getByText('Configure'));

    await screen.findByPlaceholderText(/family mutual fund corpus/i);
    fireEvent.click(screen.getByRole('button', { name: /^save$/i }));

    expect(screen.getByText(/objective is required/i)).toBeInTheDocument();
    expect(createGoalPlan).not.toHaveBeenCalled();
  });

  it('deactivates a configured goal plan from the edit form', async () => {
    listGoalPlans.mockResolvedValue({
      goal_plans: [
        {
          id: 'plan-1',
          goal_type: 'FREEDOM_RUNWAY',
          beneficiary_profile_id: null,
          objective: 'Build a runway',
          is_active: true,
          milestones: [],
          rules: [],
          trigger_events: [],
        },
      ],
    });
    deactivateGoalPlan.mockResolvedValue({});

    renderPage();
    await waitFor(() => screen.getByText('Freedom Runway'));

    const card = screen.getByText('Freedom Runway').closest('div');
    fireEvent.click(within(card.parentElement).getByText('Edit objective'));

    const deactivateBtn = await screen.findByText(/deactivate this goal plan/i);
    fireEvent.click(deactivateBtn);

    await waitFor(() => {
      expect(deactivateGoalPlan).toHaveBeenCalledWith('plan-1', 'admin-1');
    });
  });

  it('adds and saves a milestone via the bulk-PUT editor', async () => {
    const plan = {
      id: 'plan-1',
      goal_type: 'INSURANCE_FREE',
      beneficiary_profile_id: null,
      objective: 'Zero reliance on insurance',
      is_active: true,
      milestones: [],
      rules: [],
      trigger_events: [],
    };
    listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
    replaceGoalPlanMilestones.mockResolvedValue([
      {
        id: 'm1',
        sequence_no: 0,
        label: 'First checkpoint',
        target_value: 50,
        is_manual_checklist: false,
        is_achieved: false,
        significance: 'Halfway',
      },
    ]);

    renderPage();
    await waitFor(() => screen.getByText('Insurance Free'));

    const card = screen.getByText('Insurance Free').closest('div');
    fireEvent.click(within(card.parentElement).getByText(/milestones, rules/i));

    fireEvent.click(await screen.findByText('+ Add milestone'));
    fireEvent.change(screen.getByPlaceholderText('Label'), {
      target: { value: 'First checkpoint' },
    });
    fireEvent.click(screen.getByText('Save milestones'));

    await waitFor(() => {
      expect(replaceGoalPlanMilestones).toHaveBeenCalledWith(
        'plan-1',
        'admin-1',
        expect.arrayContaining([expect.objectContaining({ label: 'First checkpoint' })])
      );
    });
  });

  it('toggles a persisted checklist milestone directly via the single-milestone PATCH, not bulk-PUT', async () => {
    const plan = {
      id: 'plan-1',
      goal_type: 'FREEDOM_RUNWAY',
      beneficiary_profile_id: null,
      objective: 'Build a runway',
      is_active: true,
      milestones: [
        {
          id: 'm-checklist',
          sequence_no: 0,
          label: 'Opened emergency fund',
          target_value: null,
          is_manual_checklist: true,
          is_achieved: false,
          significance: 'First step',
        },
      ],
      rules: [],
      trigger_events: [],
    };
    listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
    updateGoalPlanMilestoneAchieved.mockResolvedValue({ ...plan.milestones[0], is_achieved: true });

    renderPage();
    await waitFor(() => screen.getByText('Freedom Runway'));

    const card = screen.getByText('Freedom Runway').closest('div');
    fireEvent.click(within(card.parentElement).getByText(/milestones, rules/i));

    const toggle = await screen.findByLabelText(/mark "opened emergency fund" achieved/i);
    fireEvent.click(toggle);

    await waitFor(() => {
      expect(updateGoalPlanMilestoneAchieved).toHaveBeenCalledWith(
        'plan-1',
        'm-checklist',
        'admin-1',
        true
      );
    });
    expect(replaceGoalPlanMilestones).not.toHaveBeenCalled();
  });

  it('opens the Year One configure form with education fields for a configured child', async () => {
    renderPage();
    await waitFor(() => screen.getByText('Year One — Aanya'));

    const card = screen.getByText('Year One — Aanya').closest('div');
    fireEvent.click(within(card.parentElement).getByText('Configure'));

    await waitFor(() => {
      expect(screen.getByText(/Configure Year One — Aanya/i)).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/e\.g\. 1000000/i)).toBeInTheDocument();
    });
  });

  it('pre-fills the edit form from an existing plan and saves updated fields', async () => {
    const plan = {
      id: 'plan-1',
      goal_type: 'DEBT_CROSSOVER',
      beneficiary_profile_id: null,
      objective: 'Original objective',
      target_state: 'Original target state',
      assumed_growth_rate: 0.1,
      is_active: true,
      milestones: [],
      rules: [],
      trigger_events: [],
    };
    listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
    updateGoalPlan.mockResolvedValue({ ...plan, objective: 'Updated objective' });

    renderPage();
    await waitFor(() => screen.getByText('Debt Crossover'));

    const card = screen.getByText('Debt Crossover').closest('div');
    fireEvent.click(within(card.parentElement).getByText('Edit objective'));

    const objectiveInput = await screen.findByPlaceholderText(/family mutual fund corpus/i);
    expect(objectiveInput.value).toBe('Original objective');
    expect(screen.getByDisplayValue('Original target state')).toBeInTheDocument();
    expect(screen.getByDisplayValue('0.1')).toBeInTheDocument();

    fireEvent.change(objectiveInput, { target: { value: 'Updated objective' } });
    fireEvent.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => {
      expect(updateGoalPlan).toHaveBeenCalledWith(
        'plan-1',
        'admin-1',
        expect.objectContaining({ objective: 'Updated objective', target_state: 'Original target state' })
      );
    });
  });

  it('nulls out target_state and assumed_growth_rate when cleared before saving', async () => {
    const plan = {
      id: 'plan-1',
      goal_type: 'DEBT_CROSSOVER',
      beneficiary_profile_id: null,
      objective: 'Original objective',
      target_state: 'Original target state',
      assumed_growth_rate: 0.1,
      is_active: true,
      milestones: [],
      rules: [],
      trigger_events: [],
    };
    listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
    updateGoalPlan.mockResolvedValue(plan);

    renderPage();
    await waitFor(() => screen.getByText('Debt Crossover'));
    const card = screen.getByText('Debt Crossover').closest('div');
    fireEvent.click(within(card.parentElement).getByText('Edit objective'));

    const targetStateInput = await screen.findByDisplayValue('Original target state');
    fireEvent.change(targetStateInput, { target: { value: '   ' } });
    const growthInput = screen.getByDisplayValue('0.1');
    fireEvent.change(growthInput, { target: { value: '' } });
    fireEvent.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => {
      expect(updateGoalPlan).toHaveBeenCalledWith(
        'plan-1',
        'admin-1',
        expect.objectContaining({ target_state: null, assumed_growth_rate: null })
      );
    });
  });

  it('shows a Reactivate button for an inactive plan and reactivates it', async () => {
    const plan = {
      id: 'plan-1',
      goal_type: 'INSURANCE_FREE',
      beneficiary_profile_id: null,
      objective: 'Zero reliance on insurance',
      is_active: false,
      milestones: [],
      rules: [],
      trigger_events: [],
    };
    listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
    updateGoalPlan.mockResolvedValue({ ...plan, is_active: true });

    renderPage();
    await waitFor(() => screen.getByText('Insurance Free'));
    const card = screen.getByText('Insurance Free').closest('div');
    expect(within(card.parentElement).getByText('Inactive')).toBeInTheDocument();
    fireEvent.click(within(card.parentElement).getByText('Edit objective'));

    const reactivateBtn = await screen.findByText(/^reactivate goal plan$/i);
    fireEvent.click(reactivateBtn);

    await waitFor(() => {
      expect(updateGoalPlan).toHaveBeenCalledWith('plan-1', 'admin-1', { is_active: true });
    });
  });

  it('shows an error banner when creating a goal plan fails', async () => {
    createGoalPlan.mockRejectedValue(new Error('create failed'));
    renderPage();
    await waitFor(() => screen.getByText('Debt Crossover'));
    const card = screen.getByText('Debt Crossover').closest('div');
    fireEvent.click(within(card.parentElement).getByText('Configure'));

    const objectiveInput = await screen.findByPlaceholderText(/family mutual fund corpus/i);
    fireEvent.change(objectiveInput, { target: { value: 'New objective' } });
    fireEvent.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => {
      expect(screen.getByText('create failed')).toBeInTheDocument();
    });
  });

  it('shows an error banner when deactivating a goal plan fails', async () => {
    const plan = {
      id: 'plan-1',
      goal_type: 'FREEDOM_RUNWAY',
      beneficiary_profile_id: null,
      objective: 'Build a runway',
      is_active: true,
      milestones: [],
      rules: [],
      trigger_events: [],
    };
    listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
    deactivateGoalPlan.mockRejectedValue(new Error('deactivate failed'));

    renderPage();
    await waitFor(() => screen.getByText('Freedom Runway'));
    const card = screen.getByText('Freedom Runway').closest('div');
    fireEvent.click(within(card.parentElement).getByText('Edit objective'));

    const deactivateBtn = await screen.findByText(/deactivate this goal plan/i);
    fireEvent.click(deactivateBtn);

    await waitFor(() => {
      expect(screen.getByText('deactivate failed')).toBeInTheDocument();
    });
  });

  it('closes the configure form via Cancel without saving', async () => {
    renderPage();
    await waitFor(() => screen.getByText('Debt Crossover'));
    const card = screen.getByText('Debt Crossover').closest('div');
    fireEvent.click(within(card.parentElement).getByText('Configure'));

    await screen.findByPlaceholderText(/family mutual fund corpus/i);
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }));

    await waitFor(() => {
      expect(screen.queryByPlaceholderText(/family mutual fund corpus/i)).not.toBeInTheDocument();
    });
    expect(createGoalPlan).not.toHaveBeenCalled();
  });

  describe('Milestone editor', () => {
    const basePlan = (extra) => ({
      id: 'plan-1',
      goal_type: 'INSURANCE_FREE',
      beneficiary_profile_id: null,
      objective: 'Zero reliance on insurance',
      is_active: true,
      milestones: [],
      rules: [],
      trigger_events: [],
      ...extra,
    });

    async function openDetailModal(plan, cardLabelText) {
      listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
      renderPage();
      await waitFor(() => screen.getByText(cardLabelText));
      const card = screen.getByText(cardLabelText).closest('div');
      fireEvent.click(within(card.parentElement).getByText(/milestones, rules/i));
      await screen.findByText('Milestones');
    }

    it('moves milestones up/down and removes one', async () => {
      const plan = basePlan({
        milestones: [
          {
            id: 'm1',
            sequence_no: 0,
            label: 'First',
            target_value: 10,
            is_manual_checklist: false,
            is_achieved: false,
            significance: '',
          },
          {
            id: 'm2',
            sequence_no: 1,
            label: 'Second',
            target_value: 20,
            is_manual_checklist: false,
            is_achieved: false,
            significance: '',
          },
        ],
      });
      await openDetailModal(plan, 'Insurance Free');

      let labels = screen.getAllByPlaceholderText('Label');
      expect(labels.map((el) => el.value)).toEqual(['First', 'Second']);

      const downButtons = screen.getAllByLabelText('Move milestone down');
      fireEvent.click(downButtons[0]);

      labels = screen.getAllByPlaceholderText('Label');
      expect(labels.map((el) => el.value)).toEqual(['Second', 'First']);

      const upButtons = screen.getAllByLabelText('Move milestone up');
      fireEvent.click(upButtons[1]);
      labels = screen.getAllByPlaceholderText('Label');
      expect(labels.map((el) => el.value)).toEqual(['First', 'Second']);

      const removeButtons = screen.getAllByLabelText('Remove milestone');
      fireEvent.click(removeButtons[0]);
      labels = screen.getAllByPlaceholderText('Label');
      expect(labels.map((el) => el.value)).toEqual(['Second']);
    });

    it('edits target_value for a non-checklist milestone and hides it when marked manual checklist', async () => {
      await openDetailModal(basePlan(), 'Insurance Free');

      fireEvent.click(screen.getByText('+ Add milestone'));
      const targetValueInput = screen.getByPlaceholderText('Target value');
      fireEvent.change(targetValueInput, { target: { value: '75' } });
      expect(screen.getByPlaceholderText('Target value').value).toBe('75');

      fireEvent.change(screen.getByPlaceholderText('Target value'), { target: { value: '' } });
      expect(screen.getByPlaceholderText('Target value').value).toBe('');

      fireEvent.click(screen.getByLabelText(/manual checklist item/i));
      expect(screen.queryByPlaceholderText('Target value')).not.toBeInTheDocument();

      fireEvent.change(screen.getByPlaceholderText('Significance'), {
        target: { value: 'Big deal' },
      });
      expect(screen.getByPlaceholderText('Significance').value).toBe('Big deal');
    });

    it('shows an error banner when saving milestones fails', async () => {
      replaceGoalPlanMilestones.mockRejectedValue(new Error('milestone save failed'));
      await openDetailModal(basePlan(), 'Insurance Free');

      fireEvent.click(screen.getByText('+ Add milestone'));
      fireEvent.change(screen.getByPlaceholderText('Label'), { target: { value: 'A' } });
      fireEvent.click(screen.getByText('Save milestones'));

      await waitFor(() => {
        expect(screen.getByText('milestone save failed')).toBeInTheDocument();
      });
    });
  });

  describe('Rule editor', () => {
    async function openInsuranceFreeDetail(extra) {
      const plan = {
        id: 'plan-1',
        goal_type: 'INSURANCE_FREE',
        beneficiary_profile_id: null,
        objective: 'Zero reliance on insurance',
        is_active: true,
        milestones: [],
        rules: [],
        trigger_events: [],
        ...extra,
      };
      listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
      renderPage();
      await waitFor(() => screen.getByText('Insurance Free'));
      const card = screen.getByText('Insurance Free').closest('div');
      fireEvent.click(within(card.parentElement).getByText(/milestones, rules/i));
      await screen.findByText('Rules');
    }

    it('adds, edits and saves a rule', async () => {
      replaceGoalPlanRules.mockResolvedValue([
        { id: 'r1', sequence_no: 0, rule_name: 'Rule A', rule_text: 'Do the thing' },
      ]);
      await openInsuranceFreeDetail();

      fireEvent.click(screen.getByText('+ Add rule'));
      fireEvent.change(screen.getByPlaceholderText('Rule name'), {
        target: { value: 'Rule A' },
      });
      fireEvent.change(screen.getByPlaceholderText('Rule text'), {
        target: { value: 'Do the thing' },
      });
      fireEvent.click(screen.getByText('Save rules'));

      await waitFor(() => {
        expect(replaceGoalPlanRules).toHaveBeenCalledWith(
          'plan-1',
          'admin-1',
          expect.arrayContaining([
            expect.objectContaining({ rule_name: 'Rule A', rule_text: 'Do the thing' }),
          ])
        );
      });
    });

    it('removes a rule row', async () => {
      await openInsuranceFreeDetail({
        rules: [{ id: 'r1', sequence_no: 0, rule_name: 'Rule A', rule_text: 'Text A' }],
      });

      expect(screen.getByDisplayValue('Rule A')).toBeInTheDocument();
      fireEvent.click(screen.getByText('Remove'));
      expect(screen.queryByDisplayValue('Rule A')).not.toBeInTheDocument();
    });

    it('shows an error banner when saving rules fails', async () => {
      replaceGoalPlanRules.mockRejectedValue(new Error('rule save failed'));
      await openInsuranceFreeDetail();

      fireEvent.click(screen.getByText('+ Add rule'));
      fireEvent.click(screen.getByText('Save rules'));

      await waitFor(() => {
        expect(screen.getByText('rule save failed')).toBeInTheDocument();
      });
    });
  });

  describe('Trigger event editor', () => {
    async function openInsuranceFreeDetail(extra) {
      const plan = {
        id: 'plan-1',
        goal_type: 'INSURANCE_FREE',
        beneficiary_profile_id: null,
        objective: 'Zero reliance on insurance',
        is_active: true,
        milestones: [],
        rules: [],
        trigger_events: [],
        ...extra,
      };
      listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
      renderPage();
      await waitFor(() => screen.getByText('Insurance Free'));
      const card = screen.getByText('Insurance Free').closest('div');
      fireEvent.click(within(card.parentElement).getByText(/milestones, rules/i));
      await screen.findByText('Trigger Events');
    }

    it('adds, edits and saves a trigger event', async () => {
      replaceGoalPlanTriggerEvents.mockResolvedValue([
        {
          id: 't1',
          sequence_no: 0,
          event_name: 'Bonus received',
          trigger_condition: 'Annual bonus credited',
          resulting_change: 'Increase SIP by 20%',
        },
      ]);
      await openInsuranceFreeDetail();

      fireEvent.click(screen.getByText('+ Add trigger event'));
      fireEvent.change(screen.getByPlaceholderText('Event name'), {
        target: { value: 'Bonus received' },
      });
      fireEvent.change(screen.getByPlaceholderText('Trigger condition'), {
        target: { value: 'Annual bonus credited' },
      });
      fireEvent.change(screen.getByPlaceholderText('Resulting change'), {
        target: { value: 'Increase SIP by 20%' },
      });
      fireEvent.click(screen.getByText('Save trigger events'));

      await waitFor(() => {
        expect(replaceGoalPlanTriggerEvents).toHaveBeenCalledWith(
          'plan-1',
          'admin-1',
          expect.arrayContaining([
            expect.objectContaining({
              event_name: 'Bonus received',
              trigger_condition: 'Annual bonus credited',
              resulting_change: 'Increase SIP by 20%',
            }),
          ])
        );
      });
    });

    it('removes a trigger event row', async () => {
      await openInsuranceFreeDetail({
        trigger_events: [
          {
            id: 't1',
            sequence_no: 0,
            event_name: 'Bonus received',
            trigger_condition: 'Cond',
            resulting_change: 'Change',
          },
        ],
      });

      expect(screen.getByDisplayValue('Bonus received')).toBeInTheDocument();
      fireEvent.click(screen.getByText('Remove'));
      expect(screen.queryByDisplayValue('Bonus received')).not.toBeInTheDocument();
    });

    it('shows an error banner when saving trigger events fails', async () => {
      replaceGoalPlanTriggerEvents.mockRejectedValue(new Error('trigger save failed'));
      await openInsuranceFreeDetail();

      fireEvent.click(screen.getByText('+ Add trigger event'));
      fireEvent.click(screen.getByText('Save trigger events'));

      await waitFor(() => {
        expect(screen.getByText('trigger save failed')).toBeInTheDocument();
      });
    });
  });

  it('shows an error banner when reactivating a goal plan fails', async () => {
    const plan = {
      id: 'plan-1',
      goal_type: 'INSURANCE_FREE',
      beneficiary_profile_id: null,
      objective: 'Zero reliance on insurance',
      is_active: false,
      milestones: [],
      rules: [],
      trigger_events: [],
    };
    listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
    updateGoalPlan.mockRejectedValue(new Error('reactivate failed'));

    renderPage();
    await waitFor(() => screen.getByText('Insurance Free'));
    const card = screen.getByText('Insurance Free').closest('div');
    fireEvent.click(within(card.parentElement).getByText('Edit objective'));

    const reactivateBtn = await screen.findByText(/^reactivate goal plan$/i);
    fireEvent.click(reactivateBtn);

    await waitFor(() => {
      expect(screen.getByText('reactivate failed')).toBeInTheDocument();
    });
  });

  it('shows an error banner when toggling a checklist milestone fails', async () => {
    const plan = {
      id: 'plan-1',
      goal_type: 'FREEDOM_RUNWAY',
      beneficiary_profile_id: null,
      objective: 'Build a runway',
      is_active: true,
      milestones: [
        {
          id: 'm-checklist',
          sequence_no: 0,
          label: 'Opened emergency fund',
          target_value: null,
          is_manual_checklist: true,
          is_achieved: false,
          significance: 'First step',
        },
      ],
      rules: [],
      trigger_events: [],
    };
    listGoalPlans.mockResolvedValue({ goal_plans: [plan] });
    updateGoalPlanMilestoneAchieved.mockRejectedValue(new Error('toggle failed'));

    renderPage();
    await waitFor(() => screen.getByText('Freedom Runway'));
    const card = screen.getByText('Freedom Runway').closest('div');
    fireEvent.click(within(card.parentElement).getByText(/milestones, rules/i));

    const toggle = await screen.findByLabelText(/mark "opened emergency fund" achieved/i);
    fireEvent.click(toggle);

    await waitFor(() => {
      expect(screen.getByText('toggle failed')).toBeInTheDocument();
    });
  });

  it('submits Year One education fields as numbers', async () => {
    createGoalPlan.mockResolvedValue({
      id: 'plan-new',
      goal_type: 'YEAR_ONE',
      beneficiary_profile_id: 'child-1',
      objective: 'Cover first year fees',
      is_active: true,
      milestones: [],
      rules: [],
      trigger_events: [],
    });

    renderPage();
    await waitFor(() => screen.getByText('Year One — Aanya'));
    const card = screen.getByText('Year One — Aanya').closest('div');
    fireEvent.click(within(card.parentElement).getByText('Configure'));

    const objectiveInput = await screen.findByPlaceholderText(/family mutual fund corpus/i);
    fireEvent.change(objectiveInput, { target: { value: 'Cover first year fees' } });
    fireEvent.change(screen.getByPlaceholderText(/e\.g\. 1000000/i), {
      target: { value: '1200000' },
    });
    fireEvent.change(screen.getByPlaceholderText(/e\.g\. 0\.08/i), { target: { value: '0.09' } });
    fireEvent.change(screen.getByPlaceholderText('e.g. 10'), { target: { value: '5' } });
    fireEvent.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => {
      expect(createGoalPlan).toHaveBeenCalledWith(
        'admin-1',
        expect.objectContaining({
          goal_type: 'YEAR_ONE',
          beneficiary_profile_id: 'child-1',
          education_base_cost: 1200000,
          education_inflation_rate: 0.09,
          education_years_to_entry: 5,
        })
      );
    });
  });

  it('opens the Milestones/Rules/Triggers detail for a configured Year One goal', async () => {
    const plan = {
      id: 'plan-yo',
      goal_type: 'YEAR_ONE',
      beneficiary_profile_id: 'child-1',
      objective: 'Cover first year fees',
      is_active: true,
      milestones: [],
      rules: [],
      trigger_events: [],
    };
    listGoalPlans.mockResolvedValue({ goal_plans: [plan] });

    renderPage();
    await waitFor(() => screen.getByText('Year One — Aanya'));
    const card = screen.getByText('Year One — Aanya').closest('div');
    fireEvent.click(within(card.parentElement).getByText(/milestones, rules/i));

    await waitFor(() => {
      expect(screen.getByText(/Milestones, Rules & Triggers — YEAR_ONE/i)).toBeInTheDocument();
    });
  });

  it('populates the detail modal from an existing plan and closes it', async () => {
    const plan = {
      id: 'plan-1',
      goal_type: 'INSURANCE_FREE',
      beneficiary_profile_id: null,
      objective: 'Zero reliance on insurance',
      is_active: true,
      milestones: [
        {
          id: 'm1',
          sequence_no: 0,
          label: 'Existing milestone',
          target_value: 50,
          is_manual_checklist: false,
          is_achieved: false,
          significance: 'Halfway',
        },
      ],
      rules: [{ id: 'r1', sequence_no: 0, rule_name: 'Existing rule', rule_text: 'Text' }],
      trigger_events: [
        {
          id: 't1',
          sequence_no: 0,
          event_name: 'Existing trigger',
          trigger_condition: 'Cond',
          resulting_change: 'Change',
        },
      ],
    };
    listGoalPlans.mockResolvedValue({ goal_plans: [plan] });

    renderPage();
    await waitFor(() => screen.getByText('Insurance Free'));
    const card = screen.getByText('Insurance Free').closest('div');
    fireEvent.click(within(card.parentElement).getByText(/milestones, rules/i));

    await waitFor(() => {
      expect(screen.getByDisplayValue('Existing milestone')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Existing rule')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Existing trigger')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /^close$/i }));
    await waitFor(() => {
      expect(screen.queryByText('Existing milestone')).not.toBeInTheDocument();
    });
  });
});
