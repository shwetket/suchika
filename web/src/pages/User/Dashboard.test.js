import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
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
  return render(
    <MemoryRouter>
      <Dashboard />
    </MemoryRouter>
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
});
