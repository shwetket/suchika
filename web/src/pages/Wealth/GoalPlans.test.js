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
  deactivateGoalPlan,
  replaceGoalPlanMilestones,
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
});
