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

async function selectAccount() {
  await waitFor(() => screen.getByText('Alice'));
  fireEvent.change(screen.getAllByRole('combobox')[0], { target: { value: 'p1' } });
  await waitFor(() => screen.getByText('SBI Savings'));
  fireEvent.change(screen.getAllByRole('combobox')[1], { target: { value: 'a1' } });
}

async function openUploadTab() {
  await selectAccount();
  await waitFor(() => screen.getByText('Upload Statement'));
  const uploadTabBtn = screen
    .getAllByRole('button', { name: /Upload Statement/i })
    .find((b) => b.type === 'button');
  fireEvent.click(uploadTabBtn);
}

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
    await selectAccount();
    await waitFor(() => {
      expect(screen.getByText(/No transactions found/i)).toBeInTheDocument();
    });
  });

  it('switches to Upload Statement tab and shows drop zone', async () => {
    render(<WealthTransactions />);
    await openUploadTab();
    expect(
      screen.getByRole('button', {
        name: /drop zone/i,
      })
    ).toBeInTheDocument();
  });

  it('shows file name in drop zone after file selected', async () => {
    render(<WealthTransactions />);
    await openUploadTab();

    const fileInput = document.querySelector('input[type="file"]');
    const file = new File(['date,amount\n2025-01-01,100'], 'test.csv', { type: 'text/csv' });

    // Mock FileReader
    const mockReadAsText = jest.fn();
    const mockFileReader = {
      readAsText: mockReadAsText,
      onload: null,
    };
    jest.spyOn(globalThis, 'FileReader').mockImplementation(() => mockFileReader);

    fireEvent.change(fileInput, { target: { files: [file] } });

    // Simulate FileReader onload
    mockFileReader.onload({ target: { result: 'date,amount\n2025-01-01,100' } });

    await waitFor(() => {
      expect(screen.getByText('test.csv')).toBeInTheDocument();
    });

    globalThis.FileReader.mockRestore();
  });

  it('calls uploadStatement with file name and content on submit', async () => {
    uploadStatement.mockResolvedValue({});
    render(<WealthTransactions />);
    await openUploadTab();

    const fileInput = document.querySelector('input[type="file"]');
    const csvText = 'date,amount,type\n2025-01-01,100,CREDIT';
    const file = new File([csvText], 'june-2025.csv', { type: 'text/csv' });

    const mockReadAsText = jest.fn();
    const mockFileReader = { readAsText: mockReadAsText, onload: null };
    jest.spyOn(globalThis, 'FileReader').mockImplementation(() => mockFileReader);

    fireEvent.change(fileInput, { target: { files: [file] } });
    mockFileReader.onload({ target: { result: csvText } });

    await waitFor(() => screen.getByText('june-2025.csv'));

    const submitBtn = screen
      .getAllByRole('button', { name: /Upload Statement/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(uploadStatement).toHaveBeenCalledWith('a1', 'june-2025.csv', csvText);
    });

    globalThis.FileReader.mockRestore();
  });

  it('shows error when submitting with no file selected', async () => {
    render(<WealthTransactions />);
    await openUploadTab();

    const submitBtn = screen
      .getAllByRole('button', { name: /Upload Statement/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/file name is required/i)).toBeInTheDocument();
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
    await selectAccount();
    await waitFor(() => {
      expect(screen.getByText('Salary')).toBeInTheDocument();
    });
  });
});
