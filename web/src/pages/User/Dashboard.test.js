import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Dashboard } from './Dashboard';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

jest.mock('../../api/household', () => ({
  getDashboard: jest.fn(),
  refreshProjections: jest.fn(),
}));

const { getDashboard, refreshProjections } = require('../../api/household');

const MOCK_USER_NO_PROFILE = { username: 'alice', role: 'user' };
const MOCK_USER = { username: 'alice', role: 'user', profile_id: 'p1' };

const MOCK_SNAPSHOTS = [
  {
    profile_id: 'p1',
    snapshot_key: 'WEALTH_NET_WORTH',
    payload: JSON.stringify({ net_worth: 250000 }),
    calculated_at: '2026-06-24T10:00:00Z',
  },
  {
    profile_id: 'p1',
    snapshot_key: 'HOUSEHOLD_EVENT_SUMMARY',
    payload: JSON.stringify({ upcoming_count: 3 }),
    calculated_at: '2026-06-24T10:00:00Z',
  },
];

function renderDashboard() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  getDashboard.mockResolvedValue({ snapshots: [] });
});

describe('Dashboard', () => {
  it('renders welcome heading with username', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER_NO_PROFILE });
    renderDashboard();
    expect(screen.getByText(/welcome back, alice/i)).toBeInTheDocument();
  });

  it('renders Profiles card linking to /household/profiles', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER_NO_PROFILE });
    renderDashboard();
    const link = screen.getByRole('link', { name: /profiles/i });
    expect(link).toHaveAttribute('href', '/household/profiles');
  });

  it('renders Wealth card linking to /wealth/accounts', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER_NO_PROFILE });
    renderDashboard();
    const link = screen.getByRole('link', { name: /wealth/i });
    expect(link).toHaveAttribute('href', '/wealth/accounts');
  });

  it('renders Health card linking to /health/vitals', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER_NO_PROFILE });
    renderDashboard();
    const link = screen.getByRole('link', { name: /health/i });
    expect(link).toHaveAttribute('href', '/health/vitals');
  });

  it('renders Household card linking to /household/calendar', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER_NO_PROFILE });
    renderDashboard();
    const links = screen.getAllByRole('link');
    const householdCalendarLink = links.find(
      (l) => l.getAttribute('href') === '/household/calendar'
    );
    expect(householdCalendarLink).toBeInTheDocument();
  });

  it('renders Refresh Live Data button', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER_NO_PROFILE });
    renderDashboard();
    expect(screen.getByRole('button', { name: /refresh live data/i })).toBeInTheDocument();
  });

  it('shows spinner and "Refreshing..." text while refresh in progress', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: [] });
    refreshProjections.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ snapshots: [] }), 500))
    );

    renderDashboard();
    await waitFor(() => screen.getByRole('button', { name: /refresh live data/i }));

    fireEvent.click(screen.getByRole('button', { name: /refresh live data/i }));

    await waitFor(() => {
      expect(screen.getByText(/refreshing/i)).toBeInTheDocument();
    });
  });

  it('shows snapshot summary metrics after successful refresh', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: [] });
    refreshProjections.mockResolvedValue({ snapshots: MOCK_SNAPSHOTS });

    renderDashboard();
    await waitFor(() => screen.getByRole('button', { name: /refresh live data/i }));

    fireEvent.click(screen.getByRole('button', { name: /refresh live data/i }));

    await waitFor(() => {
      expect(screen.getByText(/net worth/i)).toBeInTheDocument();
      expect(screen.getByText(/upcoming events/i)).toBeInTheDocument();
    });
  });

  it('shows "No dashboard data yet" when snapshots are empty', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: [] });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/no dashboard data yet/i)).toBeInTheDocument();
    });
  });

  it('shows active goals count when GOAL_PROGRESS snapshot is present', async () => {
    const snapshotsWithGoals = [
      {
        profile_id: 'p1',
        snapshot_key: 'WEALTH_NET_WORTH',
        payload: JSON.stringify({ net_worth: 300000 }),
        calculated_at: '2026-06-24T10:00:00Z',
      },
      {
        profile_id: 'p1',
        snapshot_key: 'WEALTH_GOAL_PROGRESS',
        payload: JSON.stringify({ active_count: 5 }),
        calculated_at: '2026-06-24T10:00:00Z',
      },
    ];
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: snapshotsWithGoals });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/active goals/i)).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
    });
  });

  it('shows family net worth and member breakdown when FAMILY snapshot is present', async () => {
    const snapshotsWithFamily = [
      {
        profile_id: 'p1',
        snapshot_key: 'WEALTH_NET_WORTH',
        payload: JSON.stringify({ net_worth: 300000 }),
        calculated_at: '2026-06-24T10:00:00Z',
      },
      {
        profile_id: 'p1',
        snapshot_key: 'WEALTH_NET_WORTH_FAMILY',
        payload: JSON.stringify({
          family_net_worth: 455000,
          member_count: 2,
          members: [
            { profile_id: 'p1', full_name: 'Ketan', relation_to_admin: 'SELF', net_worth: 300000 },
            {
              profile_id: 'p2',
              full_name: 'Shweta',
              relation_to_admin: 'SPOUSE',
              net_worth: 155000,
            },
          ],
        }),
        calculated_at: '2026-06-24T10:00:00Z',
      },
    ];
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: snapshotsWithFamily });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/family net worth/i)).toBeInTheDocument();
      expect(screen.getByText('Ketan')).toBeInTheDocument();
      expect(screen.getByText('Shweta')).toBeInTheDocument();
      expect(screen.getByText('SPOUSE')).toBeInTheDocument();
    });
  });

  it('falls back to per-profile net worth when FAMILY snapshot is absent', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: MOCK_SNAPSHOTS });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/net worth/i)).toBeInTheDocument();
      expect(screen.queryByText(/family net worth/i)).not.toBeInTheDocument();
    });
  });

  it('loads dashboard snapshots on mount from getDashboard', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: MOCK_SNAPSHOTS });

    renderDashboard();

    await waitFor(() => {
      expect(getDashboard).toHaveBeenCalledWith('p1');
      expect(screen.getByText(/net worth/i)).toBeInTheDocument();
    });
  });

  it('handles getDashboard returning no snapshots key', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({});

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/no dashboard data yet/i)).toBeInTheDocument();
    });
  });

  it('shows fallback error text when refresh fails without error message', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: [] });
    refreshProjections.mockRejectedValue(new Error());

    renderDashboard();
    await waitFor(() => screen.getByRole('button', { name: /refresh live data/i }));

    fireEvent.click(screen.getByRole('button', { name: /refresh live data/i }));

    await waitFor(() => {
      expect(screen.getByText('Refresh failed')).toBeInTheDocument();
    });
  });

  it('ignores second Refresh click while first refresh is in progress', async () => {
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: [] });
    let resolveRefresh;
    refreshProjections.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRefresh = resolve;
        })
    );

    renderDashboard();
    await waitFor(() => screen.getByRole('button', { name: /refresh live data/i }));

    fireEvent.click(screen.getByRole('button', { name: /refresh live data/i }));
    await waitFor(() => screen.getByText(/refreshing/i));

    fireEvent.click(screen.getByRole('button', { name: /refreshing/i }));
    expect(refreshProjections).toHaveBeenCalledTimes(1);

    resolveRefresh({ snapshots: [] });
  });

  it('shows goals card with achieved and in-progress goals when FORMULA_GOALS_FAMILY snapshot present', async () => {
    const snapshotsWithGoals = [
      {
        profile_id: 'p1',
        snapshot_key: 'WEALTH_FORMULA_GOALS_FAMILY',
        payload: JSON.stringify({
          total_count: 2,
          achieved_count: 1,
          goals: [
            { goal_id: 'g1', goal_name: 'Emergency Fund', status: 'ACHIEVED' },
            { goal_id: 'g2', goal_name: 'Retirement Corpus', status: 'IN_PROGRESS' },
          ],
        }),
        calculated_at: '2026-07-01T10:00:00Z',
      },
    ];
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: snapshotsWithGoals });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/household goals/i)).toBeInTheDocument();
      expect(screen.getByText('Emergency Fund')).toBeInTheDocument();
      expect(screen.getByText('Retirement Corpus')).toBeInTheDocument();
      expect(screen.getByText('Achieved')).toBeInTheDocument();
      expect(screen.getByText('In Progress')).toBeInTheDocument();
    });
  });

  it('shows WARNING validation badge with warning count when VALIDATION_REPORT_FAMILY snapshot present', async () => {
    const snapshotsWithValidation = [
      {
        profile_id: 'p1',
        snapshot_key: 'WEALTH_VALIDATION_REPORT_FAMILY',
        payload: JSON.stringify({
          overall_status: 'WARNING',
          warning_count: 3,
        }),
        calculated_at: '2026-07-01T10:00:00Z',
      },
    ];
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: snapshotsWithValidation });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('Data Quality')).toBeInTheDocument();
      expect(screen.getByText('WARNING')).toBeInTheDocument();
      expect(screen.getByText(/3 warnings/i)).toBeInTheDocument();
    });
  });

  it('shows PASS validation badge without warning count when overall_status is PASS', async () => {
    const snapshotsWithPass = [
      {
        profile_id: 'p1',
        snapshot_key: 'WEALTH_VALIDATION_REPORT_FAMILY',
        payload: JSON.stringify({
          overall_status: 'PASS',
          warning_count: 0,
        }),
        calculated_at: '2026-07-01T10:00:00Z',
      },
    ];
    mockUseAuth.mockReturnValue({ user: MOCK_USER });
    getDashboard.mockResolvedValue({ snapshots: snapshotsWithPass });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('Data Quality')).toBeInTheDocument();
      expect(screen.getByText('PASS')).toBeInTheDocument();
    });
  });
});
