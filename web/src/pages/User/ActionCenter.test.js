import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ActionCenter } from './ActionCenter';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

jest.mock('../../api/household', () => ({
  getDashboard: jest.fn(),
  refreshProjections: jest.fn(),
}));

const { getDashboard, refreshProjections } = require('../../api/household');

const MOCK_USER = { username: 'alice', role: 'user', profile_id: 'p1' };

function renderActionCenter() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ActionCenter />
    </QueryClientProvider>
  );
}

function snapshotWith(payload) {
  return {
    snapshots: [
      {
        profile_id: 'p1',
        snapshot_key: 'ACTION_CENTER_ALERTS_FAMILY',
        payload: JSON.stringify(payload),
        calculated_at: '2026-07-02T10:00:00Z',
      },
    ],
  };
}

beforeEach(() => {
  jest.clearAllMocks();
  mockUseAuth.mockReturnValue({ user: MOCK_USER });
  getDashboard.mockResolvedValue({ snapshots: [] });
});

describe('ActionCenter page', () => {
  it('renders the heading', () => {
    renderActionCenter();
    expect(screen.getByText('Action Center')).toBeInTheDocument();
  });

  it('shows "No alert data yet" when no snapshot exists', async () => {
    renderActionCenter();
    await waitFor(() => {
      expect(screen.getByText(/no alert data yet/i)).toBeInTheDocument();
    });
  });

  it('shows empty-state text for each section when payload has no alerts', async () => {
    getDashboard.mockResolvedValue(
      snapshotWith({
        upcoming_events: [],
        vehicle_compliance: [],
        biometric_streak_gaps: [],
        member_count: 1,
      })
    );
    renderActionCenter();

    await waitFor(() => {
      expect(screen.getByText(/no events in the next 30 days/i)).toBeInTheDocument();
      expect(screen.getByText(/no compliance deadlines/i)).toBeInTheDocument();
      expect(screen.getByText(/no tracking gaps/i)).toBeInTheDocument();
    });
  });

  it('renders upcoming events, vehicle compliance issues, and streak gaps', async () => {
    getDashboard.mockResolvedValue(
      snapshotWith({
        upcoming_events: [
          {
            profile_id: 'p1',
            full_name: 'Ketan',
            id: 'e1',
            title: 'Dentist',
            start_date: '2026-07-10',
          },
        ],
        vehicle_compliance: [
          {
            profile_id: 'p2',
            full_name: 'Shweta',
            asset_id: 'a1',
            asset_name: 'Tata Nexon',
            issue_type: 'PUC_EXPIRED',
            expiry_date: '2026-07-05',
          },
        ],
        biometric_streak_gaps: [
          {
            profile_id: 'p3',
            full_name: 'Gayan',
            vital_type: 'WEIGHT',
            last_reading_date: null,
            days_since_last_reading: null,
          },
        ],
        member_count: 3,
      })
    );
    renderActionCenter();

    await waitFor(() => {
      expect(screen.getByText(/Dentist/)).toBeInTheDocument();
      expect(screen.getByText(/Tata Nexon: PUC EXPIRED/)).toBeInTheDocument();
      expect(screen.getByText(/never logged/i)).toBeInTheDocument();
    });
  });

  it('refreshes alerts when the Refresh button is clicked', async () => {
    refreshProjections.mockResolvedValue(
      snapshotWith({
        upcoming_events: [],
        vehicle_compliance: [],
        biometric_streak_gaps: [],
        member_count: 1,
      })
    );
    renderActionCenter();
    await waitFor(() => screen.getByText(/no alert data yet/i));

    fireEvent.click(screen.getByRole('button', { name: /refresh/i }));

    await waitFor(() => {
      expect(refreshProjections).toHaveBeenCalledWith('p1');
      expect(screen.getByText(/no events in the next 30 days/i)).toBeInTheDocument();
    });
  });

  it('shows sign-in prompt when there is no linked profile', () => {
    mockUseAuth.mockReturnValue({ user: { username: 'alice', role: 'user' } });
    renderActionCenter();
    expect(screen.getByText(/sign in with a linked profile/i)).toBeInTheDocument();
  });
});
