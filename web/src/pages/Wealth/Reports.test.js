import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Reports } from './Reports';

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/wealth', () => ({
  listAccounts: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const { listAccounts } = require('../../api/wealth');

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
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listAccounts.mockResolvedValue({ accounts: [] });
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
});
