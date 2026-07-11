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
const {
  listAccounts,
  createAccount,
  updateAccount,
  getAccountBalance,
} = require('../../api/wealth');

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

async function selectProfileAndWaitForAccounts() {
  render(<Accounts />);
  await waitFor(() => screen.getByText('Alice'));
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
  await waitFor(() => screen.getByText('SBI Savings'));
}

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
    const institutionSelects = screen.getAllByRole('combobox');
    const institutionSelect = institutionSelects.find(
      (s) => s.getAttribute('name') === 'institution_name'
    );
    fireEvent.change(institutionSelect, { target: { value: 'State Bank of India' } });

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

  describe('Account type list (backlog item: stale ACCOUNT_TYPES)', () => {
    const EXPECTED_ACCOUNT_TYPES = [
      'SAVINGS',
      'CURRENT',
      'CREDIT_CARD',
      'HOME_LOAN',
      'PERSONAL_LOAN',
      'CAR_LOAN',
      'MUTUAL_FUND',
      'NPS',
      'PPF',
      'FD',
      'EPF',
    ];

    it('offers all 11 real AccountType values in the Add Account dropdown, not the stale 7-value set', async () => {
      listAccounts.mockResolvedValue({ accounts: [] });
      render(<Accounts />);
      await waitFor(() => screen.getByText('Alice'));

      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
      await waitFor(() => screen.getByText('+ Add Account'));
      fireEvent.click(screen.getByText('+ Add Account'));

      const typeSelects = screen.getAllByRole('combobox');
      const typeSelect = typeSelects.find((s) => s.getAttribute('name') === 'account_type');
      const optionValues = Array.from(typeSelect.querySelectorAll('option'))
        .map((o) => o.value)
        .filter((v) => v !== '');

      expect(optionValues).toEqual(EXPECTED_ACCOUNT_TYPES);
      expect(optionValues).not.toContain('INVESTMENT');
    });

    it('offers all 11 real AccountType values as filter tabs', async () => {
      listAccounts.mockResolvedValue({ accounts: [] });
      render(<Accounts />);
      await waitFor(() => screen.getByText('Alice'));
      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

      await waitFor(() => {
        EXPECTED_ACCOUNT_TYPES.forEach((type) => {
          expect(
            screen.getByRole('button', { name: new RegExp(`^${type.replaceAll('_', ' ')}$`, 'i') })
          ).toBeInTheDocument();
        });
      });
      expect(screen.queryByRole('button', { name: /^INVESTMENT$/i })).not.toBeInTheDocument();
    });
  });

  describe('Edit affordance (UX-002)', () => {
    it('renders Edit as an icon-only button with an accessible label, not a labeled button', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      await selectProfileAndWaitForAccounts();

      const editButtons = screen.getAllByRole('button', { name: 'Edit account' });
      expect(editButtons.length).toBe(MOCK_ACCOUNTS.length);
      // No visible text label "Edit" should render anywhere on the card.
      expect(screen.queryByText(/^Edit$/)).not.toBeInTheDocument();
      expect(editButtons[0]).toHaveAttribute('title', 'Edit account');
    });
  });

  describe('Deactivate relocated to Edit modal (UX-001, UX-003)', () => {
    it('does not render a Deactivate control directly on the account card', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      await selectProfileAndWaitForAccounts();

      expect(screen.queryByRole('button', { name: /^Deactivate$/i })).not.toBeInTheDocument();
      expect(screen.queryByText(/Deactivate this account/i)).not.toBeInTheDocument();
    });

    it('shows "Deactivate this account" link inside the Edit modal for an active account', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      await selectProfileAndWaitForAccounts();

      const editButtons = screen.getAllByRole('button', { name: 'Edit account' });
      fireEvent.click(editButtons[0]);

      await waitFor(() => {
        expect(screen.getByText('Deactivate this account')).toBeInTheDocument();
      });
      expect(screen.queryByText('Reactivate account')).not.toBeInTheDocument();
    });

    it('clicking the Deactivate link closes the edit modal and opens the confirm dialog', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      await selectProfileAndWaitForAccounts();

      const editButtons = screen.getAllByRole('button', { name: 'Edit account' });
      fireEvent.click(editButtons[0]);

      await waitFor(() => screen.getByText('Deactivate this account'));
      fireEvent.click(screen.getByText('Deactivate this account'));

      expect(screen.queryByRole('heading', { name: /Edit —/i })).not.toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Deactivate Account' })).toBeInTheDocument();
    });

    it('shows "Reactivate account" (no confirmation) inside the Edit modal for an inactive account, mutually exclusive with Deactivate', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      await selectProfileAndWaitForAccounts();

      const editButtons = screen.getAllByRole('button', { name: 'Edit account' });
      fireEvent.click(editButtons[1]); // HDFC CC, is_active: false

      await waitFor(() => {
        expect(screen.getByText('Reactivate account')).toBeInTheDocument();
      });
      expect(screen.queryByText('Deactivate this account')).not.toBeInTheDocument();
    });

    it('clicking Reactivate calls updateAccount with is_active true and requires no confirmation dialog', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      updateAccount.mockResolvedValue({});
      await selectProfileAndWaitForAccounts();

      const editButtons = screen.getAllByRole('button', { name: 'Edit account' });
      fireEvent.click(editButtons[1]);

      await waitFor(() => screen.getByText('Reactivate account'));
      fireEvent.click(screen.getByText('Reactivate account'));

      await waitFor(() => {
        expect(updateAccount).toHaveBeenCalledWith('a2', 'p1', { is_active: true });
      });
      expect(screen.queryByRole('heading', { name: 'Deactivate Account' })).not.toBeInTheDocument();
    });

    it('does not render a raw Active checkbox in the Edit modal', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      await selectProfileAndWaitForAccounts();

      const editButtons = screen.getAllByRole('button', { name: 'Edit account' });
      fireEvent.click(editButtons[0]);

      await waitFor(() => screen.getByText('Deactivate this account'));
      expect(screen.queryByLabelText('Active')).not.toBeInTheDocument();
    });
  });

  describe('Balance states (UX-004)', () => {
    it('shows a real balance when the fetch succeeds', async () => {
      listAccounts.mockResolvedValue({ accounts: [MOCK_ACCOUNTS[0]] });
      getAccountBalance.mockResolvedValue({ current_balance: 75000 });
      await selectProfileAndWaitForAccounts();

      await waitFor(() => {
        expect(screen.getByText(/Balance: .*75,000/)).toBeInTheDocument();
      });
    });

    it('shows "Balance unavailable" distinctly when the balance fetch fails, instead of falling back to opening_balance', async () => {
      listAccounts.mockResolvedValue({ accounts: [MOCK_ACCOUNTS[0]] });
      getAccountBalance.mockRejectedValue(new Error('boom'));
      await selectProfileAndWaitForAccounts();

      await waitFor(() => {
        expect(screen.getByText('Balance unavailable')).toBeInTheDocument();
      });
      // Should not silently show the opening_balance (50,000) as if it were current.
      expect(screen.queryByText(/50,000/)).not.toBeInTheDocument();
    });
  });

  describe('Search filter (UX-005)', () => {
    it('filters the account grid by account name', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      await selectProfileAndWaitForAccounts();

      expect(screen.getByText('SBI Savings')).toBeInTheDocument();
      expect(screen.getByText('HDFC CC')).toBeInTheDocument();

      fireEvent.change(screen.getByLabelText('Search accounts'), {
        target: { value: 'sbi' },
      });

      await waitFor(() => {
        expect(screen.getByText('SBI Savings')).toBeInTheDocument();
        expect(screen.queryByText('HDFC CC')).not.toBeInTheDocument();
      });
    });

    it('filters the account grid by institution name', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      await selectProfileAndWaitForAccounts();

      fireEvent.change(screen.getByLabelText('Search accounts'), {
        target: { value: 'hdfc bank' },
      });

      await waitFor(() => {
        expect(screen.getByText('HDFC CC')).toBeInTheDocument();
        expect(screen.queryByText('SBI Savings')).not.toBeInTheDocument();
      });
    });

    it('shows a no-match message when the search has no results', async () => {
      listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
      await selectProfileAndWaitForAccounts();

      fireEvent.change(screen.getByLabelText('Search accounts'), {
        target: { value: 'nonexistent-account-xyz' },
      });

      await waitFor(() => {
        expect(screen.getByText(/No accounts match your search/i)).toBeInTheDocument();
      });
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
      const editButtons = screen.getAllByRole('button', { name: 'Edit account' });
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
