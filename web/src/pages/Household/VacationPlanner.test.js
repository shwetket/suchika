import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { VacationPlanner } from './VacationPlanner';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/vacationPlanner', () => ({
  checkVacationBudget: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const { checkVacationBudget } = require('../../api/vacationPlanner');

const MOCK_PROFILES = [{ profile_id: 'p1', full_name: 'Alice' }];

function renderPlanner() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <VacationPlanner />
    </QueryClientProvider>
  );
}

async function fillAndSubmit({ tripCost = '50000', tripEndDate = '2026-08-10' } = {}) {
  await waitFor(() => screen.getByText('Alice'));
  fireEvent.change(screen.getByLabelText(/profile/i), { target: { value: 'p1' } });
  fireEvent.change(screen.getByLabelText(/trip cost/i), { target: { value: tripCost } });
  fireEvent.change(screen.getByLabelText(/trip end date/i), { target: { value: tripEndDate } });
  fireEvent.click(screen.getByRole('button', { name: /check budget/i }));
}

beforeEach(() => {
  jest.clearAllMocks();
  mockUseAuth.mockReturnValue({ user: { admin_id: 'admin-1' } });
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
});

describe('VacationPlanner page', () => {
  it('renders the heading', () => {
    renderPlanner();
    expect(screen.getByText('Vacation Planner')).toBeInTheDocument();
  });

  it('disables submit until a profile and trip end date are set', async () => {
    renderPlanner();
    await waitFor(() => screen.getByText('Alice'));
    expect(screen.getByRole('button', { name: /check budget/i })).toBeDisabled();
  });

  it('shows PASS budget check and no compliance issues', async () => {
    checkVacationBudget.mockResolvedValue({
      budget_check: { status: 'PASS', liquid_savings: 100000, trip_cost: 50000, shortfall: 0 },
      asset_compliance: { status: 'PASS', issues: [] },
    });
    renderPlanner();
    await fillAndSubmit();

    await waitFor(() => {
      expect(checkVacationBudget).toHaveBeenCalledWith('p1', {
        trip_cost: 50000,
        trip_start_date: null,
        trip_end_date: '2026-08-10',
      });
      expect(screen.getAllByText('PASS')).toHaveLength(2);
      expect(screen.getByText(/no compliance issues/i)).toBeInTheDocument();
    });
  });

  it('shows WARNING with shortfall when liquid savings are insufficient', async () => {
    checkVacationBudget.mockResolvedValue({
      budget_check: {
        status: 'WARNING',
        liquid_savings: 20000,
        trip_cost: 50000,
        shortfall: 30000,
      },
      asset_compliance: { status: 'PASS', issues: [] },
    });
    renderPlanner();
    await fillAndSubmit();

    await waitFor(() => {
      expect(screen.getByText(/shortfall/i)).toBeInTheDocument();
    });
  });

  it('shows vehicle compliance issues when present', async () => {
    checkVacationBudget.mockResolvedValue({
      budget_check: { status: 'PASS', liquid_savings: 100000, trip_cost: 1000, shortfall: 0 },
      asset_compliance: {
        status: 'WARNING',
        issues: [
          {
            asset_id: 'a1',
            asset_name: 'Tata Nexon',
            issue_type: 'PUC_EXPIRED',
            expiry_date: '2026-08-01',
          },
        ],
      },
    });
    renderPlanner();
    await fillAndSubmit();

    await waitFor(() => {
      expect(screen.getByText(/Tata Nexon/)).toBeInTheDocument();
      expect(screen.getByText(/PUC EXPIRED/)).toBeInTheDocument();
    });
  });

  it('shows UNAVAILABLE budget status with message when liquidity data is missing', async () => {
    checkVacationBudget.mockResolvedValue({
      budget_check: {
        status: 'UNAVAILABLE',
        message: 'Liquidity data not yet calculated — refresh the dashboard first',
      },
      asset_compliance: { status: 'PASS', issues: [] },
    });
    renderPlanner();
    await fillAndSubmit();

    await waitFor(() => {
      expect(screen.getByText(/refresh the dashboard first/i)).toBeInTheDocument();
    });
  });

  it('shows an error message when the check fails', async () => {
    checkVacationBudget.mockRejectedValue(new Error('Server error'));
    renderPlanner();
    await fillAndSubmit();

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });
  });
});
