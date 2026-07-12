import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { WealthTransactions } from './Transactions';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

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
  createTransaction: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const {
  listAccounts,
  listTransactions,
  listUploads,
  uploadStatement,
  getUploadErrors,
  rollbackUpload,
  createTransaction,
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
  mockUseAuth.mockReturnValue({ user: { admin_id: 'admin-1' } });
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

  describe('Pagination stale-response guard (UX-015)', () => {
    it('does not let a stale (late-resolving) response overwrite a newer one', async () => {
      let resolveStale;
      const stalePromise = new Promise((resolve) => {
        resolveStale = resolve;
      });

      listTransactions
        .mockImplementationOnce(() => stalePromise) // initial load (ALL, page 0) — resolves late
        .mockImplementationOnce(() =>
          Promise.resolve({
            transactions: [
              {
                id: 'fresh-1',
                txn_date: '2025-06-02',
                amount: 200,
                txn_type: 'DEBIT',
                description: 'Fresh Row',
              },
            ],
            total_size: 1,
          })
        );

      render(<WealthTransactions />);
      await selectAccount();

      // Trigger a second, newer request before the first (stale) one resolves.
      fireEvent.click(screen.getByRole('button', { name: 'DEBIT' }));

      await waitFor(() => screen.getByText('Fresh Row'));

      // Now let the stale first request resolve with older, different data.
      resolveStale({
        transactions: [
          {
            id: 'stale-1',
            txn_date: '2025-06-01',
            amount: 100,
            txn_type: 'CREDIT',
            description: 'Stale Row',
          },
        ],
        total_size: 1,
      });

      await new Promise((resolve) => {
        setTimeout(resolve, 0);
      });

      expect(screen.getByText('Fresh Row')).toBeInTheDocument();
      expect(screen.queryByText('Stale Row')).not.toBeInTheDocument();
    });
  });

  describe('Amount color-coding (UX-016)', () => {
    it('renders CREDIT amounts and DEBIT amounts with distinct colors', async () => {
      listTransactions.mockResolvedValue({
        transactions: [
          {
            id: 'c1',
            txn_date: '2025-06-01',
            amount: 5000,
            txn_type: 'CREDIT',
            description: 'Salary',
          },
          {
            id: 'd1',
            txn_date: '2025-06-02',
            amount: 1200,
            txn_type: 'DEBIT',
            description: 'Groceries',
          },
        ],
      });
      render(<WealthTransactions />);
      await selectAccount();

      await waitFor(() => screen.getByText('Salary'));

      const creditAmountCell = screen.getByText(/5,000/);
      const debitAmountCell = screen.getByText(/1,200/);
      expect(creditAmountCell.className).toContain('text-green-800');
      expect(debitAmountCell.className).toContain('text-red-800');
    });
  });

  describe('Description tooltip (UX-017)', () => {
    it('adds a title attribute with the full untruncated description', async () => {
      const longDescription =
        'UPI/P2A/123456789012/JOHN DOE/HDFC BANK/Payment for groceries and household supplies purchased this week';
      listTransactions.mockResolvedValue({
        transactions: [
          {
            id: 't1',
            txn_date: '2025-06-01',
            amount: 500,
            txn_type: 'DEBIT',
            description: longDescription,
          },
        ],
      });
      render(<WealthTransactions />);
      await selectAccount();

      await waitFor(() => {
        const cell = screen.getByTitle(longDescription);
        expect(cell).toBeInTheDocument();
        expect(cell.textContent.length).toBeLessThan(longDescription.length);
      });
    });
  });

  describe('"+ Add Transaction" de-emphasis (UX-018)', () => {
    it('renders as an outline/secondary button, not a solid primary button', async () => {
      render(<WealthTransactions />);
      await selectAccount();

      const addButton = screen.getByRole('button', { name: '+ Add Transaction' });
      expect(addButton.className).not.toContain('bg-blue-600');
      expect(addButton.className).toContain('border-blue-600');
    });
  });

  describe('Add Transaction modal', () => {
    async function openAddModal() {
      render(<WealthTransactions />);
      await selectAccount();
      fireEvent.click(screen.getByRole('button', { name: '+ Add Transaction' }));
      await waitFor(() => screen.getByText('Add Transaction'));
    }

    it('requires a date before submitting', async () => {
      await openAddModal();
      fireEvent.click(screen.getByRole('button', { name: 'Add' }));
      await waitFor(() => {
        expect(screen.getByText(/date is required/i)).toBeInTheDocument();
      });
      expect(createTransaction).not.toHaveBeenCalled();
    });

    it('requires a positive amount before submitting', async () => {
      await openAddModal();
      fireEvent.change(document.getElementById('add-txn-date'), {
        target: { value: '2026-01-01' },
      });
      fireEvent.change(document.getElementById('add-txn-amount'), { target: { value: '0' } });
      fireEvent.click(screen.getByRole('button', { name: 'Add' }));
      await waitFor(() => {
        expect(screen.getByText(/amount must be positive/i)).toBeInTheDocument();
      });
      expect(createTransaction).not.toHaveBeenCalled();
    });

    it('creates a transaction, closes the modal and reloads the list', async () => {
      createTransaction.mockResolvedValue({});
      await openAddModal();

      fireEvent.change(document.getElementById('add-txn-date'), {
        target: { value: '2026-01-05' },
      });
      fireEvent.change(document.getElementById('add-txn-amount'), { target: { value: '250' } });
      const modal = screen.getByText('Add Transaction').closest('.bg-white');
      fireEvent.click(within(modal).getByRole('button', { name: 'CREDIT' }));
      fireEvent.change(document.getElementById('add-txn-description'), {
        target: { value: '  Cash deposit  ' },
      });
      fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      await waitFor(() => {
        expect(createTransaction).toHaveBeenCalledWith('a1', 'p1', {
          txn_date: '2026-01-05',
          amount: 250,
          txn_type: 'CREDIT',
          description: 'Cash deposit',
        });
      });
      await waitFor(() => {
        expect(screen.queryByText('Add Transaction')).not.toBeInTheDocument();
      });
    });

    it('submits a blank trimmed description as null', async () => {
      createTransaction.mockResolvedValue({});
      await openAddModal();

      fireEvent.change(document.getElementById('add-txn-date'), {
        target: { value: '2026-01-05' },
      });
      fireEvent.change(document.getElementById('add-txn-amount'), { target: { value: '250' } });
      fireEvent.change(document.getElementById('add-txn-description'), {
        target: { value: '   ' },
      });
      fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      await waitFor(() => {
        expect(createTransaction).toHaveBeenCalledWith(
          'a1',
          'p1',
          expect.objectContaining({ description: null })
        );
      });
    });

    it('shows an error banner when creating a transaction fails', async () => {
      createTransaction.mockRejectedValue(new Error('create txn failed'));
      await openAddModal();

      fireEvent.change(document.getElementById('add-txn-date'), {
        target: { value: '2026-01-05' },
      });
      fireEvent.change(document.getElementById('add-txn-amount'), { target: { value: '250' } });
      fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      await waitFor(() => {
        expect(screen.getByText('create txn failed')).toBeInTheDocument();
      });
    });

    it('closes the modal via Cancel and the X button without saving', async () => {
      await openAddModal();
      fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
      await waitFor(() => {
        expect(screen.queryByText('Add Transaction')).not.toBeInTheDocument();
      });

      fireEvent.click(screen.getByRole('button', { name: '+ Add Transaction' }));
      await waitFor(() => screen.getByText('Add Transaction'));
      fireEvent.click(screen.getByText('×'));
      await waitFor(() => {
        expect(screen.queryByText('Add Transaction')).not.toBeInTheDocument();
      });
      expect(createTransaction).not.toHaveBeenCalled();
    });
  });

  describe('Transaction filters', () => {
    it('reloads with from/to date filters and ALL/CREDIT/DEBIT type toggles', async () => {
      render(<WealthTransactions />);
      await selectAccount();
      await waitFor(() => expect(listTransactions).toHaveBeenCalled());

      const [fromInput, toInput] = screen.getAllByDisplayValue('');
      fireEvent.change(fromInput, { target: { value: '2026-01-01' } });
      fireEvent.change(toInput, { target: { value: '2026-01-31' } });

      await waitFor(() => {
        expect(listTransactions).toHaveBeenCalledWith(
          'a1',
          'p1',
          '2026-01-01',
          '2026-01-31',
          'ALL',
          0,
          20
        );
      });

      fireEvent.click(screen.getByRole('button', { name: 'CREDIT' }));
      await waitFor(() => {
        expect(listTransactions).toHaveBeenCalledWith(
          'a1',
          'p1',
          '2026-01-01',
          '2026-01-31',
          'CREDIT',
          0,
          20
        );
      });

      fireEvent.click(screen.getByRole('button', { name: 'ALL' }));
      await waitFor(() => {
        expect(listTransactions).toHaveBeenCalledWith(
          'a1',
          'p1',
          '2026-01-01',
          '2026-01-31',
          'ALL',
          0,
          20
        );
      });
    });

    it('shows an error banner when loading transactions fails', async () => {
      listTransactions.mockRejectedValue(new Error('load txn failed'));
      render(<WealthTransactions />);
      await selectAccount();
      await waitFor(() => {
        expect(screen.getByText('load txn failed')).toBeInTheDocument();
      });
    });
  });

  describe('Profile/account load failures', () => {
    it('falls back to an empty profile list when listProfiles fails', async () => {
      listProfiles.mockRejectedValue(new Error('profiles down'));
      render(<WealthTransactions />);
      await waitFor(() => {
        expect(screen.getByText(/Select a profile and account/i)).toBeInTheDocument();
      });
      expect(screen.queryByText('Alice')).not.toBeInTheDocument();
    });

    it('falls back to an empty account list when listAccounts fails', async () => {
      listAccounts.mockRejectedValue(new Error('accounts down'));
      render(<WealthTransactions />);
      await waitFor(() => screen.getByText('Alice'));
      fireEvent.change(screen.getAllByRole('combobox')[0], { target: { value: 'p1' } });
      await waitFor(() => {
        expect(listAccounts).toHaveBeenCalled();
      });
      expect(screen.queryByText('SBI Savings')).not.toBeInTheDocument();
    });
  });

  describe('Upload drag-and-drop', () => {
    it('supports drag-over, drag-leave and drop of a CSV file', async () => {
      render(<WealthTransactions />);
      await openUploadTab();

      const dropZone = screen.getByRole('button', { name: /drop zone/i });
      fireEvent.dragOver(dropZone);
      fireEvent.dragLeave(dropZone);

      const csvText = 'date,amount\n2026-01-01,100';
      const file = new File([csvText], 'dropped.csv', { type: 'text/csv' });
      Object.defineProperty(file, 'text', {
        value: jest.fn().mockResolvedValue(csvText),
      });
      fireEvent.drop(dropZone, { dataTransfer: { files: [file] } });

      await waitFor(() => {
        expect(screen.getByText('dropped.csv')).toBeInTheDocument();
      });
    });

    it('opens the file browser via Enter key on the drop zone', async () => {
      render(<WealthTransactions />);
      await openUploadTab();
      const dropZone = screen.getByRole('button', { name: /drop zone/i });
      const fileInput = document.querySelector('input[type="file"]');
      const clickSpy = jest.spyOn(fileInput, 'click');
      fireEvent.keyDown(dropZone, { key: 'Enter' });
      expect(clickSpy).toHaveBeenCalled();
    });

    it('requires CSV content when the file is empty', async () => {
      render(<WealthTransactions />);
      await openUploadTab();

      const fileInput = document.querySelector('input[type="file"]');
      const file = new File([''], 'empty.csv', { type: 'text/csv' });
      Object.defineProperty(file, 'text', { value: jest.fn().mockResolvedValue('') });
      fireEvent.change(fileInput, { target: { files: [file] } });
      await waitFor(() => screen.getByText('empty.csv'));

      const submitBtn = screen
        .getAllByRole('button', { name: /Upload Statement/i })
        .find((b) => b.type === 'submit');
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText(/csv content is required/i)).toBeInTheDocument();
      });
    });

    it('shows a generic error banner when the upload API call rejects', async () => {
      uploadStatement.mockRejectedValue(new Error('upload failed hard'));
      render(<WealthTransactions />);
      await uploadFileAndSubmit('date,amount\n2026-01-01,100', 'boom.csv');
      await waitFor(() => {
        expect(screen.getByText('upload failed hard')).toBeInTheDocument();
      });
    });
  });

  describe('Upload rollback', () => {
    it('renders a Rollback action only for SUCCESS uploads, and cancels the confirmation', async () => {
      listUploads.mockResolvedValue({
        uploads: [
          { upload_id: 'u1', file_name: 'jan.csv', upload_date: '2026-01-01', status: 'SUCCESS' },
          { upload_id: 'u2', file_name: 'feb.csv', upload_date: '2026-02-01', status: 'PENDING' },
        ],
      });
      render(<WealthTransactions />);
      await openUploadTab();
      await waitFor(() => screen.getByText('jan.csv'));

      expect(screen.getAllByRole('button', { name: 'Rollback' })).toHaveLength(1);

      fireEvent.click(screen.getByRole('button', { name: 'Rollback' }));
      await waitFor(() => screen.getByText(/Roll back/i));
      fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      await waitFor(() => {
        expect(screen.queryByText(/This will remove all/i)).not.toBeInTheDocument();
      });
      expect(rollbackUpload).not.toHaveBeenCalled();
    });

    it('confirms rollback, calls the API and reloads the upload history', async () => {
      listUploads.mockResolvedValue({
        uploads: [
          { upload_id: 'u1', file_name: 'jan.csv', upload_date: '2026-01-01', status: 'SUCCESS' },
        ],
      });
      rollbackUpload.mockResolvedValue({});
      render(<WealthTransactions />);
      await openUploadTab();
      await waitFor(() => screen.getByText('jan.csv'));

      fireEvent.click(screen.getByRole('button', { name: 'Rollback' }));
      await waitFor(() => screen.getByText(/Roll back/i));
      const modal = screen.getByText('Rollback Upload').closest('div');
      fireEvent.click(within(modal).getByRole('button', { name: 'Rollback' }));

      await waitFor(() => {
        expect(rollbackUpload).toHaveBeenCalledWith('a1', 'u1');
      });
    });

    it('shows an error banner when rollback fails', async () => {
      listUploads.mockResolvedValue({
        uploads: [
          { upload_id: 'u1', file_name: 'jan.csv', upload_date: '2026-01-01', status: 'SUCCESS' },
        ],
      });
      rollbackUpload.mockRejectedValue(new Error('rollback failed'));
      render(<WealthTransactions />);
      await openUploadTab();
      await waitFor(() => screen.getByText('jan.csv'));

      fireEvent.click(screen.getByRole('button', { name: 'Rollback' }));
      await waitFor(() => screen.getByText(/Roll back/i));
      const modal = screen.getByText('Rollback Upload').closest('div');
      fireEvent.click(within(modal).getByRole('button', { name: 'Rollback' }));

      await waitFor(() => {
        expect(screen.getByText('rollback failed')).toBeInTheDocument();
      });
    });

    it('shows loading and empty states for the upload history', async () => {
      let resolveUploads;
      listUploads.mockImplementation(
        () =>
          new Promise((resolve) => {
            resolveUploads = resolve;
          })
      );
      render(<WealthTransactions />);
      await openUploadTab();
      expect(screen.getByText(/loading uploads/i)).toBeInTheDocument();
      resolveUploads({ uploads: [] });
      await waitFor(() => {
        expect(screen.getByText(/no uploads yet/i)).toBeInTheDocument();
      });
    });
  });

  describe('Tab switching', () => {
    it('switches from Upload Statement back to Transactions', async () => {
      render(<WealthTransactions />);
      await openUploadTab();
      const txnTabBtn = screen
        .getAllByRole('button', { name: /^Transactions$/i })
        .find((b) => b.type === 'button');
      fireEvent.click(txnTabBtn);
      await waitFor(() => {
        expect(screen.getByText(/No transactions found/i)).toBeInTheDocument();
      });
    });
  });
});
