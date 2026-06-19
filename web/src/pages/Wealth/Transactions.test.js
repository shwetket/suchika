import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { WealthTransactions } from './Transactions';

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/wealth', () => ({
  listAccounts: jest.fn(),
  listTransactions: jest.fn(),
  listUploads: jest.fn(),
  uploadStatement: jest.fn(),
  rollbackUpload: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const {
  listAccounts,
  listTransactions,
  listUploads,
  uploadStatement,
} = require('../../api/wealth');

const MOCK_PROFILES = [{ profile_id: 'p1', full_name: 'Alice', is_active: true }];
const MOCK_ACCOUNTS = [
  {
    account_id: 'a1',
    account_name: 'SBI Savings',
    account_type: 'SAVINGS',
    institution_name: 'SBI',
    opening_balance: 10000,
    is_active: true,
  },
];

beforeEach(() => {
  jest.clearAllMocks();
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listAccounts.mockResolvedValue({ accounts: MOCK_ACCOUNTS });
  listTransactions.mockResolvedValue({ transactions: [] });
  listUploads.mockResolvedValue({ uploads: [] });
});

describe('WealthTransactions page', () => {
  it('renders without crashing', () => {
    render(<WealthTransactions />);
    expect(screen.getByText('Transactions')).toBeInTheDocument();
  });

  it('shows prompt when no account selected', async () => {
    render(<WealthTransactions />);
    await waitFor(() => {
      expect(screen.getByText(/Select a profile and account/i)).toBeInTheDocument();
    });
  });

  it('shows empty state when no transactions', async () => {
    render(<WealthTransactions />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getAllByRole('combobox')[0];
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => screen.getByText('SBI Savings'));

    const accountSelect = screen.getAllByRole('combobox')[1];
    fireEvent.change(accountSelect, { target: { value: 'a1' } });

    await waitFor(() => {
      expect(screen.getByText(/No transactions found/i)).toBeInTheDocument();
    });
  });

  it('switches to Upload Statement tab', async () => {
    render(<WealthTransactions />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getAllByRole('combobox')[0];
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => screen.getByText('SBI Savings'));

    const accountSelect = screen.getAllByRole('combobox')[1];
    fireEvent.change(accountSelect, { target: { value: 'a1' } });

    await waitFor(() => screen.getByText('Upload Statement'));
    const uploadTab = screen
      .getAllByRole('button', { name: /Upload Statement/i })
      .find((b) => b.type === 'button');
    fireEvent.click(uploadTab);

    expect(screen.getByPlaceholderText(/e\.g\. june-2025\.csv/i)).toBeInTheDocument();
  });

  it('calls uploadStatement with correct args on form submit', async () => {
    uploadStatement.mockResolvedValue({});
    render(<WealthTransactions />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getAllByRole('combobox')[0], { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('SBI Savings'));
    fireEvent.change(screen.getAllByRole('combobox')[1], { target: { value: 'a1' } });

    await waitFor(() => screen.getByText('Upload Statement'));
    const uploadTabBtn = screen
      .getAllByRole('button', { name: /Upload Statement/i })
      .find((b) => b.type === 'button');
    fireEvent.click(uploadTabBtn);

    await waitFor(() => screen.getByPlaceholderText(/june-2025\.csv/i));

    fireEvent.change(screen.getByPlaceholderText(/june-2025\.csv/i), {
      target: { value: 'test.csv' },
    });
    fireEvent.change(screen.getByPlaceholderText(/Paste CSV rows/i), {
      target: { value: 'date,amount,type\n2025-01-01,100,CREDIT' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /Upload Statement/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(uploadStatement).toHaveBeenCalledWith(
        'a1',
        'test.csv',
        'date,amount,type\n2025-01-01,100,CREDIT'
      );
    });
  });

  it('shows transaction rows when transactions exist', async () => {
    listTransactions.mockResolvedValue({
      transactions: [
        {
          id: 't1',
          txn_date: '2025-06-01',
          amount: 5000,
          txn_type: 'CREDIT',
          description: 'Salary',
        },
      ],
    });
    render(<WealthTransactions />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getAllByRole('combobox')[0], { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('SBI Savings'));
    fireEvent.change(screen.getAllByRole('combobox')[1], { target: { value: 'a1' } });

    await waitFor(() => {
      expect(screen.getByText('Salary')).toBeInTheDocument();
    });
  });
});
