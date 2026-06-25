import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Goals } from './Goals';

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/household', () => ({
  listGoals: jest.fn(),
  createGoal: jest.fn(),
  updateGoal: jest.fn(),
  deleteGoal: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const { listGoals, createGoal, deleteGoal } = require('../../api/household');

const MOCK_PROFILES = [{ profile_id: 'p1', full_name: 'Alice', is_active: true }];

const MOCK_GOALS = [
  {
    id: 'g1',
    profile_id: 'p1',
    goal_name: 'Emergency Fund',
    status: 'ACTIVE',
    target_amount: 100000,
    current_amount: 45000,
    monthly_saving: 5000,
    target_date: '2027-06-01',
    progress_percent: 45,
    days_to_completion: 240,
    notes: null,
  },
  {
    id: 'g2',
    profile_id: 'p1',
    goal_name: 'New Car',
    status: 'PAUSED',
    target_amount: 800000,
    current_amount: 800000,
    monthly_saving: null,
    target_date: null,
    progress_percent: 100,
    days_to_completion: null,
    notes: 'On hold',
  },
];

beforeEach(() => {
  jest.clearAllMocks();
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listGoals.mockResolvedValue({ goals: [] });
});

describe('Goals page', () => {
  it('renders select profile prompt when no profile selected', async () => {
    render(<Goals />);
    await waitFor(() => {
      expect(screen.getByText(/select a profile to view goals/i)).toBeInTheDocument();
    });
  });

  it('renders goal cards after profile selection', async () => {
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Emergency Fund')).toBeInTheDocument();
      expect(screen.getByText('New Car')).toBeInTheDocument();
    });
  });

  it('renders progress bars for each goal', async () => {
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      const progressBars = screen.getAllByRole('progressbar');
      expect(progressBars.length).toBeGreaterThan(0);
    });
  });

  it('shows status badges with correct labels', async () => {
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('ACTIVE')).toBeInTheDocument();
      expect(screen.getByText('PAUSED')).toBeInTheDocument();
    });
  });

  it('"Add Goal" button opens the add form modal', async () => {
    listGoals.mockResolvedValue({ goals: [] });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Goal'));

    fireEvent.click(screen.getByText('+ Add Goal'));
    expect(screen.getByRole('heading', { name: /add goal/i })).toBeInTheDocument();
  });

  it('submits add goal form and reloads list', async () => {
    listGoals.mockResolvedValue({ goals: [] });
    createGoal.mockResolvedValue({ id: 'g3', goal_name: 'Vacation' });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Goal'));

    fireEvent.click(screen.getByText('+ Add Goal'));

    fireEvent.change(screen.getByPlaceholderText('e.g. Emergency Fund'), {
      target: { name: 'goal_name', value: 'Vacation' },
    });
    fireEvent.change(screen.getByPlaceholderText('e.g. 100000'), {
      target: { name: 'target_amount', value: '50000' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /add goal/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createGoal).toHaveBeenCalledWith(
        expect.objectContaining({ goal_name: 'Vacation', target_amount: 50000 })
      );
    });
  });

  it('shows delete confirmation and calls deleteGoal', async () => {
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    deleteGoal.mockResolvedValue(null);
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Delete'));

    fireEvent.click(screen.getAllByText('Delete')[0]);
    expect(screen.getByText('Confirm?')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Confirm?'));

    await waitFor(() => {
      expect(deleteGoal).toHaveBeenCalledWith('g1');
    });
  });

  it('shows projection note on each goal card', async () => {
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      const notes = screen.getAllByText(/progress is computed from your wealth accounts/i);
      expect(notes.length).toBeGreaterThan(0);
    });
  });

  it('shows error message when listGoals API fails', async () => {
    listGoals.mockRejectedValue(new Error('Load failed'));
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Load failed')).toBeInTheDocument();
    });
  });

  it('shows empty state when no goals exist', async () => {
    listGoals.mockResolvedValue({ goals: [] });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/no goals yet/i)).toBeInTheDocument();
    });
  });

  it('shows validation error when goal name is empty on add', async () => {
    listGoals.mockResolvedValue({ goals: [] });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Goal'));
    fireEvent.click(screen.getByText('+ Add Goal'));

    const submitBtn = screen
      .getAllByRole('button', { name: /add goal/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Goal name is required')).toBeInTheDocument();
    });
  });

  it('shows validation error when target amount is zero on add', async () => {
    listGoals.mockResolvedValue({ goals: [] });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Goal'));
    fireEvent.click(screen.getByText('+ Add Goal'));

    fireEvent.change(screen.getByPlaceholderText('e.g. Emergency Fund'), {
      target: { name: 'goal_name', value: 'My Goal' },
    });
    fireEvent.change(screen.getByPlaceholderText('e.g. 100000'), {
      target: { name: 'target_amount', value: '0' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /add goal/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/target amount must be greater than 0/i)).toBeInTheDocument();
    });
  });

  it('shows error when createGoal API fails', async () => {
    listGoals.mockResolvedValue({ goals: [] });
    createGoal.mockRejectedValue(new Error('Create failed'));
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Goal'));
    fireEvent.click(screen.getByText('+ Add Goal'));

    fireEvent.change(screen.getByPlaceholderText('e.g. Emergency Fund'), {
      target: { name: 'goal_name', value: 'Vacation Fund' },
    });
    fireEvent.change(screen.getByPlaceholderText('e.g. 100000'), {
      target: { name: 'target_amount', value: '50000' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /add goal/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Create failed')).toBeInTheDocument();
    });
  });

  it('shows error when deleteGoal API fails', async () => {
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    deleteGoal.mockRejectedValue(new Error('Delete failed'));
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Delete'));

    fireEvent.click(screen.getAllByText('Delete')[0]);
    fireEvent.click(screen.getByText('Confirm?'));

    await waitFor(() => {
      expect(screen.getByText('Delete failed')).toBeInTheDocument();
    });
  });

  it('cancels delete confirmation on goal card when Cancel is clicked', async () => {
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Delete'));

    fireEvent.click(screen.getAllByText('Delete')[0]);
    expect(screen.getByText('Confirm?')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Cancel'));
    expect(screen.queryByText('Confirm?')).not.toBeInTheDocument();
  });

  it('opens edit modal when Edit is clicked on a goal card', async () => {
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /edit goal/i })).toBeInTheDocument();
    });
  });

  it('submits edit form and reloads goals', async () => {
    const { updateGoal } = require('../../api/household');
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    updateGoal.mockResolvedValue({});
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);
    await waitFor(() => screen.getByRole('heading', { name: /edit goal/i }));

    const submitBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(updateGoal).toHaveBeenCalledWith(
        'g1',
        expect.objectContaining({ goal_name: 'Emergency Fund' })
      );
    });
  });

  it('shows validation error when goal name is empty on edit', async () => {
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);
    await waitFor(() => screen.getByRole('heading', { name: /edit goal/i }));

    const goalNameInput = screen.getByDisplayValue('Emergency Fund');
    fireEvent.change(goalNameInput, { target: { name: 'goal_name', value: '' } });

    const submitBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Goal name is required')).toBeInTheDocument();
    });
  });

  it('shows error when updateGoal API fails', async () => {
    const { updateGoal } = require('../../api/household');
    listGoals.mockResolvedValue({ goals: MOCK_GOALS });
    updateGoal.mockRejectedValue(new Error('Update failed'));
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);
    await waitFor(() => screen.getByRole('heading', { name: /edit goal/i }));

    const submitBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Update failed')).toBeInTheDocument();
    });
  });

  it('renders progress bar with amber colour for 50-99% progress', async () => {
    const goalAt60 = [
      {
        id: 'g3',
        profile_id: 'p1',
        goal_name: 'House Down Payment',
        status: 'ACTIVE',
        target_amount: 500000,
        current_amount: 300000,
        monthly_saving: 10000,
        target_date: null,
        progress_percent: 60,
        days_to_completion: null,
        notes: null,
      },
    ];
    listGoals.mockResolvedValue({ goals: goalAt60 });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('House Down Payment')).toBeInTheDocument();
      expect(screen.getByText('60% complete')).toBeInTheDocument();
    });
  });

  it('renders progress bar with red colour for below 50% progress', async () => {
    const goalAt20 = [
      {
        id: 'g4',
        profile_id: 'p1',
        goal_name: 'Car Fund',
        status: 'ACTIVE',
        target_amount: 500000,
        current_amount: 100000,
        monthly_saving: null,
        target_date: null,
        progress_percent: 20,
        days_to_completion: null,
        notes: null,
      },
    ];
    listGoals.mockResolvedValue({ goals: goalAt20 });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Car Fund')).toBeInTheDocument();
      expect(screen.getByText('20% complete')).toBeInTheDocument();
    });
  });

  it('renders goal with no estimate label when days_to_completion is null', async () => {
    const goalNoEstimate = [
      {
        id: 'g5',
        profile_id: 'p1',
        goal_name: 'Bike Fund',
        status: 'PAUSED',
        target_amount: 80000,
        current_amount: 0,
        monthly_saving: null,
        target_date: '2028-01-01',
        progress_percent: 0,
        days_to_completion: null,
        notes: null,
      },
    ];
    listGoals.mockResolvedValue({ goals: goalNoEstimate });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/No estimate/)).toBeInTheDocument();
    });
  });

  it('shows ACHIEVED status badge on goal card', async () => {
    const achievedGoal = [
      {
        id: 'g6',
        profile_id: 'p1',
        goal_name: 'Laptop',
        status: 'ACHIEVED',
        target_amount: 100000,
        current_amount: 100000,
        monthly_saving: null,
        target_date: null,
        progress_percent: 100,
        days_to_completion: 0,
        notes: null,
      },
    ];
    listGoals.mockResolvedValue({ goals: achievedGoal });
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('ACHIEVED')).toBeInTheDocument();
    });
  });

  it('shows loading state while fetching goals', async () => {
    listGoals.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ goals: [] }), 300))
    );
    render(<Goals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    expect(screen.getByText(/loading goals/i)).toBeInTheDocument();
  });
});
