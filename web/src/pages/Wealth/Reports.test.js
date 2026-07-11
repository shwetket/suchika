import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Reports } from './Reports';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/wealth', () => ({
  listAccounts: jest.fn(),
}));

jest.mock('../../api/household', () => ({
  getDashboard: jest.fn(),
  refreshProjections: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const { listAccounts } = require('../../api/wealth');
const { getDashboard, refreshProjections } = require('../../api/household');

const MOCK_PROFILES = [{ profile_id: 'p1', full_name: 'Alice', is_active: true }];

const MOCK_ACCOUNTS = [
  {
    account_id: 'a1',
    account_name: 'SBI Savings',
    account_type: 'SAVINGS',
    institution_name: 'SBI',
    opening_balance: 50000,
    is_active: true,
  },
  {
    account_id: 'a2',
    account_name: 'HDFC CC',
    account_type: 'CREDIT_CARD',
    institution_name: 'HDFC',
    opening_balance: -10000,
    is_active: false,
  },
];

beforeEach(() => {
  jest.clearAllMocks();
  mockUseAuth.mockReturnValue({ user: { admin_id: 'admin-1' } });
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listAccounts.mockResolvedValue({ accounts: [] });
  getDashboard.mockResolvedValue({ snapshots: [] });
});

describe('Reports page', () => {
  it('renders without crashing', () => {
    render(<Reports />);
    expect(screen.getByText('Wealth Reports')).toBeInTheDocument();
  });

  it('shows "Select a profile" when no profile is chosen', async () => {
    render(<Reports />);
    await waitFor(() => {
      expect(screen.getByText(/Select a profile to view the report/i)).toBeInTheDocument();
    });
  });

  it('shows account summary cards after profile is selected', async () => {
    listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
    render(<Reports />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Total Accounts')).toBeInTheDocument();
      expect(screen.getByText('Net Balance')).toBeInTheDocument();
      expect(screen.getByText('Active Accounts')).toBeInTheDocument();
      expect(screen.getByText('Inactive Accounts')).toBeInTheDocument();
    });
  });

  it('calculates correct summary values', async () => {
    listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
    render(<Reports />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Total Accounts')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
      const activeCard = screen.getByText('Active Accounts').closest('div');
      expect(activeCard).toBeInTheDocument();
    });
  });

  it('shows error message on API failure', async () => {
    listAccounts.mockRejectedValue(new Error('Server error'));
    render(<Reports />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });
  });

  it('shows coming soon placeholder', async () => {
    listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
    render(<Reports />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Deeper Analytics/i)).toBeInTheDocument();
    });
  });

  it('shows "Not calculated" when no net worth snapshot exists yet', async () => {
    listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
    getDashboard.mockResolvedValue({ snapshots: [] });
    render(<Reports />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Not calculated')).toBeInTheDocument();
    });
  });

  it('shows the real net worth from the dashboard snapshot, not summed opening balances', async () => {
    listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
    getDashboard.mockResolvedValue({
      snapshots: [
        {
          snapshot_key: 'WEALTH_NET_WORTH',
          payload: JSON.stringify({ net_worth: 123456 }),
          calculated_at: '2026-07-01T10:00:00Z',
        },
      ],
    });
    render(<Reports />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('₹1,23,456.00')).toBeInTheDocument();
    });
  });

  it('refreshes the net worth figure via the Refresh Net Balance button', async () => {
    listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
    getDashboard.mockResolvedValue({ snapshots: [] });
    refreshProjections.mockResolvedValue({
      snapshots: [
        {
          snapshot_key: 'WEALTH_NET_WORTH',
          payload: JSON.stringify({ net_worth: 5000 }),
        },
      ],
    });
    render(<Reports />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('Not calculated'));

    fireEvent.click(screen.getByText('Refresh Net Balance'));

    await waitFor(() => {
      expect(refreshProjections).toHaveBeenCalledWith('p1');
      expect(screen.getByText('₹5,000.00')).toBeInTheDocument();
    });
  });

  describe('Default profile (UX-010)', () => {
    it("loads the logged-in user's own profile report without requiring manual selection", async () => {
      mockUseAuth.mockReturnValue({ user: { admin_id: 'admin-1', profile_id: 'p1' } });
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      render(<Reports />);

      await waitFor(() => {
        expect(screen.getByText('Total Accounts')).toBeInTheDocument();
        expect(screen.getByText('2')).toBeInTheDocument();
      });
      expect(listAccounts).toHaveBeenCalledWith('p1', null, null);
      // The dropdown must still be present/editable for the admin's legitimate
      // need to check other household members' reports.
      expect(screen.getByRole('combobox')).toBeInTheDocument();
    });

    it('still shows the profile prompt when the user has no profile_id', async () => {
      mockUseAuth.mockReturnValue({ user: { admin_id: 'admin-1' } });
      render(<Reports />);
      await waitFor(() => {
        expect(screen.getByText(/Select a profile to view the report/i)).toBeInTheDocument();
      });
    });
  });

  describe('Last-calculated timestamp (UX-012)', () => {
    it('shows when the net balance snapshot was last calculated', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      getDashboard.mockResolvedValue({
        snapshots: [
          {
            snapshot_key: 'WEALTH_NET_WORTH',
            payload: JSON.stringify({ net_worth: 123456 }),
            calculated_at: '2026-07-01T10:00:00Z',
          },
        ],
      });
      render(<Reports />);
      await waitFor(() => screen.getByText('Alice'));

      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

      await waitFor(() => {
        expect(screen.getByText(/Last calculated:/i)).toBeInTheDocument();
      });
    });

    it('does not show a last-calculated line when there is no snapshot yet', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      getDashboard.mockResolvedValue({ snapshots: [] });
      render(<Reports />);
      await waitFor(() => screen.getByText('Alice'));

      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

      await waitFor(() => screen.getByText('Not calculated'));
      expect(screen.queryByText(/Last calculated:/i)).not.toBeInTheDocument();
    });
  });

  describe('Distinguish failed fetch from not-yet-calculated (UX-013)', () => {
    it('shows a distinct "Unavailable" message when the net worth fetch fails', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      getDashboard.mockRejectedValue(new Error('Network error'));
      render(<Reports />);
      await waitFor(() => screen.getByText('Alice'));

      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

      await waitFor(() => {
        expect(screen.getByText('Unavailable')).toBeInTheDocument();
        expect(screen.getByText(/Couldn't load net balance/i)).toBeInTheDocument();
      });
      expect(screen.queryByText('Not calculated')).not.toBeInTheDocument();
    });

    it('clears the fetch-failure state once a refresh succeeds', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      getDashboard.mockRejectedValue(new Error('Network error'));
      refreshProjections.mockResolvedValue({
        snapshots: [
          {
            snapshot_key: 'WEALTH_NET_WORTH',
            payload: JSON.stringify({ net_worth: 7000 }),
          },
        ],
      });
      render(<Reports />);
      await waitFor(() => screen.getByText('Alice'));

      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
      await waitFor(() => screen.getByText('Unavailable'));

      fireEvent.click(screen.getByText('Refresh Net Balance'));

      await waitFor(() => {
        expect(screen.getByText('₹7,000.00')).toBeInTheDocument();
      });
      expect(screen.queryByText('Unavailable')).not.toBeInTheDocument();
    });
  });
});
