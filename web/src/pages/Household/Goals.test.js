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
});
