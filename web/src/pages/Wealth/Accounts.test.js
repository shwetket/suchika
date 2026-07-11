import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Accounts } from './Accounts';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/wealth', () => ({
  listAccounts: jest.fn(),
  createAccount: jest.fn(),
  updateAccount: jest.fn(),
  updateAccountClassification: jest.fn(),
  deactivateAccount: jest.fn(),
  getAccountBalance: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const { listAccounts, createAccount, getAccountBalance } = require('../../api/wealth');

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
  mockUseAuth.mockReturnValue({ user: { admin_id: 'admin-1' } });
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listAccounts.mockResolvedValue({ accounts: [] });
  getAccountBalance.mockResolvedValue({ current_balance: 0 });
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

  it('shows the "as of" date next to balance when balance_as_of is present', async () => {
    listAccounts.mockResolvedValue({
      accounts: [{ ...MOCK_ACCOUNTS[0], balance_as_of: '2026-07-10' }],
    });
    render(<Accounts />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/as of/i)).toBeInTheDocument();
    });
  });

  it('does not show an "as of" date when balance_as_of is absent', async () => {
    listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
    render(<Accounts />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => screen.getByText('SBI Savings'));
    expect(screen.queryByText(/as of/i)).not.toBeInTheDocument();
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

  describe('loan offset account linking', () => {
    const LOAN_ACCOUNT = {
      account_id: 'loan-1',
      account_name: 'Home Loan 1 - BoB MaxGain',
      account_type: 'HOME_LOAN',
      institution_name: 'Bank of Baroda',
      opening_balance: 3771120,
      interest_rate: 7.2,
      is_active: true,
      metadata: {},
    };

    const CURRENT_ACCOUNT = {
      account_id: 'current-1',
      account_name: 'MaxGain Buffer - HL1',
      account_type: 'CURRENT',
      institution_name: 'Bank of Baroda',
      opening_balance: 282995,
      is_active: true,
    };

    const SAVINGS_ACCOUNT = {
      account_id: 'savings-1',
      account_name: 'SBI Savings',
      account_type: 'SAVINGS',
      institution_name: 'State Bank of India',
      opening_balance: 50000,
      is_active: true,
    };

    it('offers both SAVINGS and CURRENT accounts as linkable offset targets for a loan', async () => {
      listAccounts.mockImplementation((profileId, accountType) => {
        if (accountType === null || accountType === undefined) {
          return Promise.resolve({
            accounts: [LOAN_ACCOUNT, CURRENT_ACCOUNT, SAVINGS_ACCOUNT],
          });
        }
        return Promise.resolve({ accounts: [LOAN_ACCOUNT] });
      });

      render(<Accounts />);
      await waitFor(() => screen.getByText('Alice'));
      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

      await waitFor(() => screen.getByText('Home Loan 1 - BoB MaxGain'));
      const editButtons = screen.getAllByRole('button', { name: 'Edit' });
      fireEvent.click(editButtons[0]);

      await waitFor(() => {
        expect(screen.getByLabelText('Linked Offset Account')).toBeInTheDocument();
      });

      const dropdown = screen.getByLabelText('Linked Offset Account');
      expect(screen.getByRole('option', { name: /MaxGain Buffer - HL1/i })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: /SBI Savings/i })).toBeInTheDocument();
      expect(dropdown).toBeInTheDocument();
    });
  });
});
