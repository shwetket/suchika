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
  getUploadErrors: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const {
  listAccounts,
  listTransactions,
  listUploads,
  uploadStatement,
  getUploadErrors,
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
  getUploadErrors.mockResolvedValue([]);
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
    const csvText = 'date,amount\n2025-01-01,100';
    const file = new File([csvText], 'test.csv', { type: 'text/csv' });
    Object.defineProperty(file, 'text', {
      value: jest.fn().mockResolvedValue(csvText),
    });

    fireEvent.change(fileInput, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByText('test.csv')).toBeInTheDocument();
    });
  });

  it('calls uploadStatement with file name and content on submit', async () => {
    uploadStatement.mockResolvedValue({});
    render(<WealthTransactions />);
    await openUploadTab();

    const fileInput = document.querySelector('input[type="file"]');
    const csvText = 'date,amount,type\n2025-01-01,100,CREDIT';
    const file = new File([csvText], 'june-2025.csv', { type: 'text/csv' });
    Object.defineProperty(file, 'text', {
      value: jest.fn().mockResolvedValue(csvText),
    });

    fireEvent.change(fileInput, { target: { files: [file] } });

    await waitFor(() => screen.getByText('june-2025.csv'));

    const submitBtn = screen
      .getAllByRole('button', { name: /Upload Statement/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(uploadStatement).toHaveBeenCalledWith('a1', 'june-2025.csv', csvText);
    });
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

  it('requests page 0 and the default page size on first load', async () => {
    listTransactions.mockResolvedValue({
      transactions: [{ id: 't1', txn_date: '2025-06-01', amount: 100, txn_type: 'DEBIT' }],
      total_size: 1,
    });
    render(<WealthTransactions />);
    await selectAccount();

    await waitFor(() => {
      expect(listTransactions).toHaveBeenCalledWith('a1', 'p1', null, null, 'ALL', 0, 20);
    });
  });

  it('shows pagination controls with page count derived from total_size', async () => {
    listTransactions.mockResolvedValue({
      transactions: [{ id: 't1', txn_date: '2025-06-01', amount: 100, txn_type: 'DEBIT' }],
      total_size: 45,
    });
    render(<WealthTransactions />);
    await selectAccount();

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 3 \(45 total\)/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).not.toBeDisabled();
  });

  it('clicking Next requests the next page', async () => {
    listTransactions.mockResolvedValue({
      transactions: [{ id: 't1', txn_date: '2025-06-01', amount: 100, txn_type: 'DEBIT' }],
      total_size: 45,
    });
    render(<WealthTransactions />);
    await selectAccount();
    await waitFor(() => screen.getByRole('button', { name: 'Next' }));

    fireEvent.click(screen.getByRole('button', { name: 'Next' }));

    await waitFor(() => {
      expect(listTransactions).toHaveBeenCalledWith('a1', 'p1', null, null, 'ALL', 1, 20);
      expect(screen.getByText(/Page 2 of 3/i)).toBeInTheDocument();
    });
  });

  it('disables Next on the last page', async () => {
    listTransactions.mockResolvedValue({
      transactions: [{ id: 't1', txn_date: '2025-06-01', amount: 100, txn_type: 'DEBIT' }],
      total_size: 5,
    });
    render(<WealthTransactions />);
    await selectAccount();

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 1 \(5 total\)/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  async function uploadFileAndSubmit(csvText, csvFileName) {
    await openUploadTab();
    const fileInput = document.querySelector('input[type="file"]');
    const file = new File([csvText], csvFileName, { type: 'text/csv' });
    Object.defineProperty(file, 'text', {
      value: jest.fn().mockResolvedValue(csvText),
    });
    fireEvent.change(fileInput, { target: { files: [file] } });
    await waitFor(() => screen.getByText(csvFileName));
    const submitBtn = screen
      .getAllByRole('button', { name: /Upload Statement/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);
  }

  it('upload_withSkippedDuplicates_showsWarningPanel', async () => {
    uploadStatement.mockResolvedValue({
      upload_id: 'u1',
      status: 'COMPLETED',
      skippedDuplicates: [
        { txnDate: '2026-06-20', amount: 25000, description: 'BONUS CREDIT' },
        { txnDate: '2026-06-15', amount: 1500, description: 'AMAZON PAY' },
      ],
    });
    render(<WealthTransactions />);
    await uploadFileAndSubmit('date,amount\n2026-06-20,25000', 'june.csv');
    await waitFor(() => {
      expect(screen.getByText(/2 rows skipped/i)).toBeInTheDocument();
      expect(screen.getByText('BONUS CREDIT')).toBeInTheDocument();
    });
  });

  it('upload_withNoSkippedDuplicates_hidesWarningPanel', async () => {
    uploadStatement.mockResolvedValue({
      upload_id: 'u2',
      status: 'COMPLETED',
      skippedDuplicates: [],
    });
    render(<WealthTransactions />);
    await uploadFileAndSubmit('date,amount\n2026-06-20,25000', 'june.csv');
    await waitFor(() => {
      expect(screen.getByText(/Statement uploaded successfully/i)).toBeInTheDocument();
    });
    expect(screen.queryByText(/rows skipped/i)).not.toBeInTheDocument();
  });

  it('upload_onFailure_fetchesAndShowsErrorDetails', async () => {
    uploadStatement.mockResolvedValue({
      upload_id: 'u3',
      status: 'FAILED',
    });
    getUploadErrors.mockResolvedValue([
      {
        errorType: 'MISSING_COLUMNS',
        missingColumns: ['date'],
        errorDetail: 'Could not identify a date column.',
        createdAt: '2026-06-29T10:00:00Z',
      },
    ]);
    render(<WealthTransactions />);
    await uploadFileAndSubmit('Description,Amount\nSomething,100', 'bad.csv');
    await waitFor(() => {
      expect(screen.getByText(/Missing required columns detected/i)).toBeInTheDocument();
    });
    expect(screen.getByText('date')).toBeInTheDocument();
    expect(screen.getByText(/Could not identify a date column/i)).toBeInTheDocument();
  });

  it('upload_onFailure_emptyErrors_showsGenericMessage', async () => {
    uploadStatement.mockResolvedValue({
      upload_id: 'u4',
      status: 'FAILED',
    });
    getUploadErrors.mockResolvedValue([]);
    render(<WealthTransactions />);
    await uploadFileAndSubmit('Description,Amount\nSomething,100', 'bad.csv');
    await waitFor(() => {
      expect(screen.getByText(/please check your file and try again/i)).toBeInTheDocument();
    });
  });
});
