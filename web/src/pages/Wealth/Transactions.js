import React, { useCallback, useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { listProfiles } from '../../api/profiles';
import {
  createTransaction,
  getUploadErrors,
  listAccounts,
  listTransactions,
  listUploads,
  rollbackUpload,
  uploadStatement,
} from '../../api/wealth';
import { useAuth } from '../../hooks/useAuth';

const TXN_TYPES = ['ALL', 'CREDIT', 'DEBIT'];
const UPLOAD_STATUS_CLASSES = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  SUCCESS: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
};

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatCurrency(value) {
  return Number(value).toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  });
}

function truncate(text, max) {
  if (!text) return '';
  return text.length > max ? `${text.slice(0, max)}…` : text;
}

function txnTypeButtonClass(selectedType, buttonType) {
  if (selectedType !== buttonType) return 'border-gray-300 text-gray-600';
  return buttonType === 'DEBIT'
    ? 'bg-red-100 border-red-400 text-red-700'
    : 'bg-green-100 border-green-400 text-green-700';
}

function TxnTypeBadge({ type }) {
  const cls = type === 'CREDIT' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800';
  return <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${cls}`}>{type}</span>;
}
TxnTypeBadge.propTypes = { type: PropTypes.string.isRequired };

// Reuses the same red/green palette TxnTypeBadge already defines (UX-016), so the
// amount figure itself carries the CREDIT/DEBIT signal, not just the adjacent badge.
function amountColorClass(txnType) {
  return txnType === 'CREDIT' ? 'text-green-800' : 'text-red-800';
}

function SkippedDuplicatesPanel({ skippedDuplicates }) {
  if (!skippedDuplicates || skippedDuplicates.length === 0) return null;
  return (
    <div className="bg-yellow-50 border border-yellow-300 text-yellow-800 rounded-lg p-4">
      <p className="font-semibold mb-2">
        {skippedDuplicates.length} row{skippedDuplicates.length === 1 ? '' : 's'} skipped
        (cross-file duplicates)
      </p>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-yellow-300 text-left">
              <th className="py-1 pr-4 font-semibold">Date</th>
              <th className="py-1 pr-4 font-semibold">Amount</th>
              <th className="py-1 font-semibold">Description</th>
            </tr>
          </thead>
          <tbody>
            {skippedDuplicates.map((row) => (
              <tr
                key={`${row.txnDate}-${row.amount}-${row.description}`}
                className="border-b border-yellow-200 last:border-0"
              >
                <td className="py-1 pr-4">{row.txnDate}</td>
                <td className="py-1 pr-4">
                  {'₹'}
                  {Number(row.amount).toLocaleString('en-IN')}
                </td>
                <td className="py-1">{row.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
SkippedDuplicatesPanel.propTypes = {
  skippedDuplicates: PropTypes.arrayOf(
    PropTypes.shape({
      txnDate: PropTypes.string,
      amount: PropTypes.number,
      description: PropTypes.string,
    })
  ),
};
SkippedDuplicatesPanel.defaultProps = { skippedDuplicates: null };

function UploadErrorPanel({ uploadErrors, errorsFetchFailed }) {
  if (errorsFetchFailed) {
    return (
      <div className="bg-red-50 border border-red-300 text-red-800 rounded-lg p-4">
        <p className="font-semibold">Upload failed</p>
        <p className="mt-1 text-sm">Upload failed — please check your file and try again.</p>
      </div>
    );
  }
  if (!uploadErrors) return null;
  if (uploadErrors.length === 0) {
    return (
      <div className="bg-red-50 border border-red-300 text-red-800 rounded-lg p-4">
        <p className="font-semibold">Upload failed</p>
        <p className="mt-1 text-sm">Upload failed — please check your file and try again.</p>
      </div>
    );
  }
  const first = uploadErrors[0];
  return (
    <div className="bg-red-50 border border-red-300 text-red-800 rounded-lg p-4 space-y-2">
      <p className="font-semibold">Upload failed</p>
      {first.missingColumns && first.missingColumns.length > 0 && (
        <div>
          <p className="text-sm font-medium">Missing required columns detected:</p>
          <ul className="list-disc list-inside text-sm ml-2 mt-1">
            {first.missingColumns.map((col) => (
              <li key={col}>{col}</li>
            ))}
          </ul>
        </div>
      )}
      {first.errorDetail && <p className="text-sm">Full detail: {first.errorDetail}</p>}
      <p className="text-sm mt-2">
        Please re-export your bank statement and ensure the file contains the required columns.
      </p>
    </div>
  );
}
UploadErrorPanel.propTypes = {
  uploadErrors: PropTypes.arrayOf(
    PropTypes.shape({
      errorType: PropTypes.string,
      missingColumns: PropTypes.arrayOf(PropTypes.string),
      errorDetail: PropTypes.string,
      createdAt: PropTypes.string,
    })
  ),
  errorsFetchFailed: PropTypes.bool,
};
UploadErrorPanel.defaultProps = { uploadErrors: null, errorsFetchFailed: false };

function TransactionRow({ txn }) {
  return (
    <tr className="border-b border-gray-100 hover:bg-gray-50">
      <td className="py-3 px-4 text-sm text-gray-700">{formatDate(txn.txn_date)}</td>
      <td className={`py-3 px-4 text-sm font-medium ${amountColorClass(txn.txn_type)}`}>
        {formatCurrency(txn.amount)}
      </td>
      <td className="py-3 px-4">
        <TxnTypeBadge type={txn.txn_type} />
      </td>
      <td className="py-3 px-4 text-sm text-gray-600 max-w-xs" title={txn.description || undefined}>
        {truncate(txn.description, 60)}
      </td>
    </tr>
  );
}
TransactionRow.propTypes = {
  txn: PropTypes.shape({
    id: PropTypes.string.isRequired,
    txn_date: PropTypes.string,
    amount: PropTypes.number,
    txn_type: PropTypes.string.isRequired,
    description: PropTypes.string,
  }).isRequired,
};

const EMPTY_TXN_FORM = { txn_date: '', amount: '', txn_type: 'DEBIT', description: '' };

const PAGE_SIZE = 20;

function TransactionsTab({ accountId, profileId }) {
  const [transactions, setTransactions] = useState([]);
  const [totalSize, setTotalSize] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [txnType, setTxnType] = useState('ALL');

  const [showAdd, setShowAdd] = useState(false);
  const [addForm, setAddForm] = useState(EMPTY_TXN_FORM);
  const [addError, setAddError] = useState(null);
  const [addSaving, setAddSaving] = useState(false);

  // Stale-response guard (UX-015): load() can be triggered both by the effect below
  // (whenever page/filters change) and manually (after adding a transaction). An
  // older in-flight request resolving after a newer one must never clobber state
  // with outdated data — mirrors the `cancelled` flag Accounts.js uses for its
  // balance-loading effect, using a request-id ref since this call site needs to
  // guard both effect-triggered and manually-triggered invocations.
  const loadRequestIdRef = useRef(0);

  const load = useCallback(async () => {
    if (!accountId) return;
    const requestId = loadRequestIdRef.current + 1;
    loadRequestIdRef.current = requestId;
    setLoading(true);
    setError(null);
    try {
      const data = await listTransactions(
        accountId,
        profileId,
        fromDate || null,
        toDate || null,
        txnType,
        page,
        PAGE_SIZE
      );
      if (requestId !== loadRequestIdRef.current) return;
      setTransactions(data.transactions ?? []);
      setTotalSize(data.total_size ?? (data.transactions ?? []).length);
    } catch (err) {
      if (requestId !== loadRequestIdRef.current) return;
      setError(err.message || 'Failed to load transactions');
    } finally {
      if (requestId === loadRequestIdRef.current) setLoading(false);
    }
  }, [accountId, profileId, fromDate, toDate, txnType, page]);

  useEffect(() => {
    load();
  }, [load]);

  // Reset to page 0 whenever the filters (not the page itself) change.
  useEffect(() => {
    setPage(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accountId, profileId, fromDate, toDate, txnType]);

  const totalPages = Math.max(1, Math.ceil(totalSize / PAGE_SIZE));

  const handleAddChange = useCallback((e) => {
    const { name, value } = e.target;
    setAddForm((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleAddSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!addForm.txn_date) {
        setAddError('Date is required');
        return;
      }
      if (!addForm.amount || Number(addForm.amount) <= 0) {
        setAddError('Amount must be positive');
        return;
      }
      setAddSaving(true);
      setAddError(null);
      try {
        await createTransaction(accountId, profileId, {
          txn_date: addForm.txn_date,
          amount: Number(addForm.amount),
          txn_type: addForm.txn_type,
          description: addForm.description.trim() || null,
        });
        setShowAdd(false);
        setAddForm(EMPTY_TXN_FORM);
        await load();
      } catch (err) {
        setAddError(err.message || 'Failed to create transaction');
      } finally {
        setAddSaving(false);
      }
    },
    [addForm, accountId, profileId, load]
  );

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div className="flex flex-wrap gap-3">
          <input
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
            className={inputClass}
            placeholder="From"
          />
          <input
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
            className={inputClass}
            placeholder="To"
          />
          <div className="flex gap-1">
            {TXN_TYPES.map((t) => {
              const sel = txnType === t;
              const cls = sel
                ? 'bg-blue-600 text-white'
                : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50';
              return (
                <button
                  type="button"
                  key={t}
                  onClick={() => setTxnType(t)}
                  className={`px-3 py-1.5 rounded-full text-sm font-medium ${cls}`}
                >
                  {t}
                </button>
              );
            })}
          </div>
        </div>
        <button
          type="button"
          onClick={() => {
            setAddForm(EMPTY_TXN_FORM);
            setAddError(null);
            setShowAdd(true);
          }}
          className="border border-blue-600 text-blue-600 px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-50 whitespace-nowrap"
        >
          + Add Transaction
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}
      {loading && <div className="text-center py-12 text-gray-500">Loading transactions...</div>}
      {!loading && transactions.length === 0 && (
        <div className="text-center py-12 text-gray-400">
          No transactions found for the selected filters.
        </div>
      )}
      {!loading && transactions.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 text-left">
                <th className="py-2 px-4 text-xs font-semibold text-gray-500 uppercase">Date</th>
                <th className="py-2 px-4 text-xs font-semibold text-gray-500 uppercase">Amount</th>
                <th className="py-2 px-4 text-xs font-semibold text-gray-500 uppercase">Type</th>
                <th className="py-2 px-4 text-xs font-semibold text-gray-500 uppercase">
                  Description
                </th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((t) => (
                <TransactionRow key={t.id} txn={t} />
              ))}
            </tbody>
          </table>

          <div className="flex items-center justify-between mt-4 text-sm text-gray-600">
            <span>
              Page {page + 1} of {totalPages} ({totalSize} total)
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 border border-gray-300 rounded text-gray-600 hover:bg-gray-50 disabled:opacity-50"
              >
                Previous
              </button>
              <button
                type="button"
                onClick={() => setPage((p) => (p + 1 < totalPages ? p + 1 : p))}
                disabled={page + 1 >= totalPages}
                className="px-3 py-1.5 border border-gray-300 rounded text-gray-600 hover:bg-gray-50 disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      )}

      {showAdd && (
        <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
            <div className="flex items-center justify-between p-5 border-b">
              <h2 className="text-lg font-bold">Add Transaction</h2>
              <button
                type="button"
                onClick={() => setShowAdd(false)}
                className="text-gray-400 hover:text-gray-600 text-2xl leading-none"
              >
                &times;
              </button>
            </div>
            <form onSubmit={handleAddSubmit} className="p-5 space-y-4">
              <div className="flex flex-col gap-1">
                <label htmlFor="add-txn-date" className="text-sm font-medium text-gray-700">
                  Date <span className="text-red-500">*</span>
                </label>
                <input
                  id="add-txn-date"
                  name="txn_date"
                  type="date"
                  value={addForm.txn_date}
                  onChange={handleAddChange}
                  className={inputClass}
                />
              </div>
              <div className="flex flex-col gap-1">
                <label htmlFor="add-txn-amount" className="text-sm font-medium text-gray-700">
                  Amount (₹) <span className="text-red-500">*</span>
                </label>
                <input
                  id="add-txn-amount"
                  name="amount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={addForm.amount}
                  onChange={handleAddChange}
                  placeholder="0.00"
                  className={inputClass}
                />
              </div>
              <fieldset className="flex flex-col gap-1 border-0 p-0 m-0">
                <legend className="text-sm font-medium text-gray-700 p-0">
                  Type <span className="text-red-500">*</span>
                </legend>
                <div className="flex gap-2">
                  {['DEBIT', 'CREDIT'].map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => setAddForm((p) => ({ ...p, txn_type: t }))}
                      className={`flex-1 py-2 rounded text-sm font-medium border ${txnTypeButtonClass(addForm.txn_type, t)}`}
                    >
                      {t}
                    </button>
                  ))}
                </div>
              </fieldset>
              <div className="flex flex-col gap-1">
                <label htmlFor="add-txn-description" className="text-sm font-medium text-gray-700">
                  Description
                </label>
                <input
                  id="add-txn-description"
                  name="description"
                  type="text"
                  value={addForm.description}
                  onChange={handleAddChange}
                  placeholder="e.g. Grocery shopping"
                  className={inputClass}
                />
              </div>
              {addError && <p className="text-red-600 text-sm">{addError}</p>}
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAdd(false)}
                  className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={addSaving}
                  className="px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                >
                  {addSaving ? 'Saving...' : 'Add'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
TransactionsTab.propTypes = { accountId: PropTypes.string, profileId: PropTypes.string };
TransactionsTab.defaultProps = { accountId: null, profileId: null };

function UploadTab({ accountId }) {
  const [fileName, setFileName] = useState('');
  const [csvContent, setCsvContent] = useState('');
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = React.useRef(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const [uploadSuccess, setUploadSuccess] = useState(false);
  const [skippedDuplicates, setSkippedDuplicates] = useState(null);
  const [uploadErrors, setUploadErrors] = useState(null);
  const [errorsFetchFailed, setErrorsFetchFailed] = useState(false);
  const [uploads, setUploads] = useState([]);
  const [loadingUploads, setLoadingUploads] = useState(false);
  const [rollbackTarget, setRollbackTarget] = useState(null);
  const [rollingBack, setRollingBack] = useState(false);

  const loadUploads = useCallback(async () => {
    if (!accountId) return;
    setLoadingUploads(true);
    try {
      const data = await listUploads(accountId);
      setUploads(data.uploads ?? []);
    } catch {
      setUploads([]);
    } finally {
      setLoadingUploads(false);
    }
  }, [accountId]);

  useEffect(() => {
    loadUploads();
  }, [loadUploads]);

  const handleUpload = useCallback(
    async (e) => {
      e.preventDefault();
      if (!fileName.trim()) {
        setUploadError('File name is required');
        return;
      }
      if (!csvContent.trim()) {
        setUploadError('CSV content is required');
        return;
      }
      setUploading(true);
      setUploadError(null);
      setUploadSuccess(false);
      setSkippedDuplicates(null);
      setUploadErrors(null);
      setErrorsFetchFailed(false);
      try {
        const result = await uploadStatement(accountId, fileName.trim(), csvContent.trim());
        setFileName('');
        setCsvContent('');
        if (result?.status === 'FAILED') {
          setUploadError('Upload failed');
          try {
            const errors = await getUploadErrors(accountId, result.upload_id);
            setUploadErrors(errors ?? []);
          } catch {
            setErrorsFetchFailed(true);
          }
        } else {
          setUploadSuccess(true);
          if (result?.skippedDuplicates) {
            setSkippedDuplicates(result.skippedDuplicates);
          }
        }
        await loadUploads();
      } catch (err) {
        setUploadError(err.message || 'Upload failed');
      } finally {
        setUploading(false);
      }
    },
    [accountId, fileName, csvContent, loadUploads]
  );

  const handleRollback = useCallback(async () => {
    if (!rollbackTarget) return;
    setRollingBack(true);
    try {
      await rollbackUpload(accountId, rollbackTarget.upload_id);
      setRollbackTarget(null);
      await loadUploads();
    } catch (err) {
      setUploadError(err.message || 'Rollback failed');
      setRollbackTarget(null);
    } finally {
      setRollingBack(false);
    }
  }, [accountId, rollbackTarget, loadUploads]);

  const readFile = useCallback((file) => {
    if (!file) return;
    setFileName(file.name);
    file.text().then((text) => setCsvContent(text));
  }, []);

  const handleFileChange = useCallback(
    (e) => {
      readFile(e.target.files[0]);
    },
    [readFile]
  );

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    setDragOver(true);
  }, []);

  const handleDragLeave = useCallback(() => {
    setDragOver(false);
  }, []);

  const handleDrop = useCallback(
    (e) => {
      e.preventDefault();
      setDragOver(false);
      readFile(e.dataTransfer.files[0]);
    },
    [readFile]
  );

  return (
    <div className="space-y-6">
      <form onSubmit={handleUpload} className="space-y-4">
        <button
          type="button"
          aria-label="Drop zone: drag a CSV file here or click to browse"
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              fileInputRef.current?.click();
            }
          }}
          className={`w-full border-2 border-dashed rounded-lg px-6 py-10 text-center cursor-pointer transition-colors ${
            dragOver
              ? 'border-indigo-500 bg-indigo-50'
              : 'border-gray-300 hover:border-indigo-400 hover:bg-gray-50'
          }`}
        >
          <input
            ref={fileInputRef}
            id="upload-file-input"
            type="file"
            accept=".csv"
            className="hidden"
            onChange={handleFileChange}
          />
          {fileName ? (
            <div>
              <p className="text-sm font-medium text-gray-900">{fileName}</p>
              <p className="text-xs text-gray-500 mt-1">File ready. Click or drag to replace.</p>
            </div>
          ) : (
            <div>
              <p className="text-sm font-medium text-gray-700">Drag CSV here or click to browse</p>
              <p className="text-xs text-gray-400 mt-1">Only .csv files accepted</p>
            </div>
          )}
        </button>

        {uploadError && <p className="text-red-600 text-sm">{uploadError}</p>}
        {uploadSuccess && (
          <p className="text-green-600 text-sm">Statement uploaded successfully.</p>
        )}
        <SkippedDuplicatesPanel skippedDuplicates={skippedDuplicates} />
        <UploadErrorPanel uploadErrors={uploadErrors} errorsFetchFailed={errorsFetchFailed} />
        <button
          type="submit"
          disabled={uploading}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-indigo-700 disabled:opacity-50 text-sm"
        >
          {uploading ? 'Uploading...' : 'Upload Statement'}
        </button>
      </form>

      <div>
        <h3 className="font-semibold text-gray-800 mb-3">Upload History</h3>
        {loadingUploads && <p className="text-sm text-gray-500">Loading uploads...</p>}
        {!loadingUploads && uploads.length === 0 && (
          <p className="text-sm text-gray-400">No uploads yet.</p>
        )}
        {!loadingUploads && uploads.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 text-left">
                  <th className="py-2 px-3 text-xs font-semibold text-gray-500 uppercase">
                    File Name
                  </th>
                  <th className="py-2 px-3 text-xs font-semibold text-gray-500 uppercase">Date</th>
                  <th className="py-2 px-3 text-xs font-semibold text-gray-500 uppercase">
                    Status
                  </th>
                  <th className="py-2 px-3 text-xs font-semibold text-gray-500 uppercase">
                    Action
                  </th>
                </tr>
              </thead>
              <tbody>
                {uploads.map((u) => (
                  <tr key={u.upload_id} className="border-b border-gray-100">
                    <td className="py-2 px-3 text-sm text-gray-700">{u.file_name}</td>
                    <td className="py-2 px-3 text-sm text-gray-600">{formatDate(u.upload_date)}</td>
                    <td className="py-2 px-3">
                      <span
                        className={`px-2 py-0.5 rounded-full text-xs font-medium ${UPLOAD_STATUS_CLASSES[u.status] ?? 'bg-gray-100 text-gray-600'}`}
                      >
                        {u.status}
                      </span>
                    </td>
                    <td className="py-2 px-3">
                      {u.status === 'SUCCESS' && (
                        <button
                          type="button"
                          onClick={() => setRollbackTarget(u)}
                          className="text-red-600 text-sm hover:underline"
                        >
                          Rollback
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {rollbackTarget && (
        <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-sm">
            <h3 className="text-lg font-bold mb-2">Rollback Upload</h3>
            <p className="text-gray-600 text-sm mb-6">
              Roll back <strong>{rollbackTarget.file_name}</strong>? This will remove all
              transactions from this upload.
            </p>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setRollbackTarget(null)}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleRollback}
                disabled={rollingBack}
                className="px-4 py-2 bg-red-600 text-white rounded text-sm font-medium hover:bg-red-700 disabled:opacity-50"
              >
                {rollingBack ? 'Rolling back...' : 'Rollback'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
UploadTab.propTypes = { accountId: PropTypes.string };
UploadTab.defaultProps = { accountId: null };

export const WealthTransactions = () => {
  const [profiles, setProfiles] = useState([]);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [accounts, setAccounts] = useState([]);
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [activeTab, setActiveTab] = useState('transactions');

  const { user } = useAuth();

  useEffect(() => {
    listProfiles(user?.admin_id, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, [user?.admin_id]);

  useEffect(() => {
    if (!selectedProfileId) {
      setAccounts([]);
      setSelectedAccountId('');
      return;
    }
    listAccounts(selectedProfileId, null, true)
      .then((data) => setAccounts(data.accounts ?? []))
      .catch(() => setAccounts([]));
  }, [selectedProfileId]);

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Transactions</h1>
        <p className="text-gray-500 mt-1">View and manage account transactions</p>
      </div>

      <div className="flex flex-wrap gap-3 mb-6">
        <select
          value={selectedProfileId}
          onChange={(e) => {
            setSelectedProfileId(e.target.value);
            setSelectedAccountId('');
          }}
          className={`${inputClass} w-full sm:w-64`}
        >
          <option value="">Select a profile...</option>
          {profiles.map((p) => (
            <option key={p.profile_id} value={p.profile_id}>
              {p.full_name}
            </option>
          ))}
        </select>
        <select
          value={selectedAccountId}
          onChange={(e) => setSelectedAccountId(e.target.value)}
          disabled={!selectedProfileId}
          className={`${inputClass} w-full sm:w-64 disabled:opacity-50`}
        >
          <option value="">Select an account...</option>
          {accounts.map((a) => (
            <option key={a.account_id} value={a.account_id}>
              {a.account_name}
            </option>
          ))}
        </select>
      </div>

      {!selectedAccountId && (
        <div className="text-center py-16 text-gray-400">
          Select a profile and account to view transactions.
        </div>
      )}

      {selectedAccountId && (
        <>
          <div className="flex border-b border-gray-200 mb-6">
            {[
              { key: 'transactions', label: 'Transactions' },
              { key: 'upload', label: 'Upload Statement' },
            ].map(({ key, label }) => (
              <button
                type="button"
                key={key}
                onClick={() => setActiveTab(key)}
                className={`px-5 py-2.5 text-sm font-medium border-b-2 -mb-px ${activeTab === key ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
              >
                {label}
              </button>
            ))}
          </div>

          {activeTab === 'transactions' && (
            <TransactionsTab accountId={selectedAccountId} profileId={selectedProfileId} />
          )}
          {activeTab === 'upload' && <UploadTab accountId={selectedAccountId} />}
        </>
      )}
    </div>
  );
};

export default WealthTransactions;
