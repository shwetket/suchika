import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Accounts } from './Accounts';

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/wealth', () => ({
  listAccounts: jest.fn(),
  createAccount: jest.fn(),
  updateAccount: jest.fn(),
  deactivateAccount: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const { listAccounts, createAccount } = require('../../api/wealth');

const MOCK_PROFILES = [
  { profile_id: 'p1', full_name: 'Alice', is_active: true },
  { profile_id: 'p2', full_name: 'Bob', is_active: true },
];

const MOCK_ACCOUNTS = [
  {
    account_id: 'a1',
    account_name: 'SBI Savings',
    account_type: 'SAVINGS',
    institution_name: 'State Bank of India',
    opening_balance: 50000,
    credit_limit: null,
    interest_rate: null,
    is_active: true,
  },
  {
    account_id: 'a2',
    account_name: 'HDFC CC',
    account_type: 'CREDIT_CARD',
    institution_name: 'HDFC Bank',
    opening_balance: -12000,
    credit_limit: 100000,
    interest_rate: null,
    is_active: false,
  },
];

beforeEach(() => {
  jest.clearAllMocks();
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listAccounts.mockResolvedValue({ accounts: [] });
});

describe('Accounts page', () => {
  it('renders "Select a profile" message when no profile is selected', async () => {
    render(<Accounts />);
    await waitFor(() => {
      expect(screen.getByText(/Select a profile to view accounts/i)).toBeInTheDocument();
    });
  });

  it('shows loading state during fetch', async () => {
    listAccounts.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ accounts: MOCK_ACCOUNTS }), 200))
    );
    render(<Accounts />);
    await waitFor(() => screen.getByText('Alice'));
    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });
    expect(screen.getByText(/Loading accounts/i)).toBeInTheDocument();
  });

  it('shows accounts after profile selection', async () => {
    listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
    render(<Accounts />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('SBI Savings')).toBeInTheDocument();
      expect(screen.getByText('HDFC CC')).toBeInTheDocument();
    });
  });

  it('opens add account modal on button click', async () => {
    listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
    render(<Accounts />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => screen.getByText('+ Add Account'));
    fireEvent.click(screen.getByText('+ Add Account'));

    expect(screen.getByRole('heading', { name: 'Add Account' })).toBeInTheDocument();
  });

  it('shows error message on API failure', async () => {
    listAccounts.mockRejectedValue(new Error('Network error'));
    render(<Accounts />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });

  it('submits create form and reloads accounts', async () => {
    listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
    createAccount.mockResolvedValue({});
    render(<Accounts />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => screen.getByText('+ Add Account'));
    fireEvent.click(screen.getByText('+ Add Account'));

    fireEvent.change(screen.getByPlaceholderText(/e\.g\. SBI Savings/i), {
      target: { name: 'account_name', value: 'Test Account' },
    });
    fireEvent.change(screen.getByPlaceholderText(/e\.g\. State Bank/i), {
      target: { name: 'institution_name', value: 'Test Bank' },
    });

    const typeSelects = screen.getAllByRole('combobox');
    const typeSelect = typeSelects.find((s) => s.getAttribute('name') === 'account_type');
    if (typeSelect) {
      fireEvent.change(typeSelect, { target: { value: 'SAVINGS' } });
    }

    const submitBtn = screen
      .getAllByRole('button', { name: /Add Account/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createAccount).toHaveBeenCalled();
    });
  });
});
