import {
  createAccount,
  deactivateAccount,
  getAccount,
  getUploadErrors,
  listAccounts,
  listTransactions,
  listUploads,
  rollbackUpload,
  updateAccount,
  uploadStatement,
} from './wealth';

jest.mock('./client', () => ({
  get: jest.fn(),
  post: jest.fn(),
  patch: jest.fn(),
  del: jest.fn(),
}));

const { get, post, patch, del } = require('./client');

beforeEach(() => jest.clearAllMocks());

describe('listAccounts', () => {
  it('builds URL with all query params', () => {
    get.mockResolvedValue({ accounts: [] });
    listAccounts('profile-1', 'SAVINGS', true);
    expect(get).toHaveBeenCalledWith(
      '/v1/accounts?profile_id=profile-1&account_type=SAVINGS&is_active=true'
    );
  });

  it('builds URL with only profileId when others are null', () => {
    get.mockResolvedValue({ accounts: [] });
    listAccounts('profile-1', null, null);
    expect(get).toHaveBeenCalledWith('/v1/accounts?profile_id=profile-1');
  });

  it('calls base URL when all params are null', () => {
    get.mockResolvedValue({ accounts: [] });
    listAccounts(null, null, null);
    expect(get).toHaveBeenCalledWith('/v1/accounts');
  });
});

describe('createAccount', () => {
  it('calls post with profile_id in query and data in body', () => {
    post.mockResolvedValue({});
    const data = { account_name: 'My Savings', account_type: 'SAVINGS' };
    createAccount('profile-1', data);
    expect(post).toHaveBeenCalledWith('/v1/accounts?profile_id=profile-1', data);
  });
});

describe('getAccount', () => {
  it('calls get with account id path', () => {
    get.mockResolvedValue({});
    getAccount('acc-123');
    expect(get).toHaveBeenCalledWith('/v1/accounts/acc-123');
  });
});

describe('updateAccount', () => {
  it('calls patch with correct path and data', () => {
    patch.mockResolvedValue({});
    const data = { account_name: 'Updated' };
    updateAccount('acc-123', data);
    expect(patch).toHaveBeenCalledWith('/v1/accounts/acc-123', data);
  });
});

describe('deactivateAccount', () => {
  it('calls del with correct path', () => {
    del.mockResolvedValue(null);
    deactivateAccount('acc-123');
    expect(del).toHaveBeenCalledWith('/v1/accounts/acc-123');
  });
});

describe('listTransactions', () => {
  it('builds URL with all filters including profile_id', () => {
    get.mockResolvedValue({ transactions: [] });
    listTransactions('acc-123', 'profile-1', '2025-01-01', '2025-01-31', 'CREDIT');
    expect(get).toHaveBeenCalledWith(
      '/v1/accounts/acc-123/transactions?profile_id=profile-1&from=2025-01-01&to=2025-01-31&txn_type=CREDIT'
    );
  });

  it('omits txn_type when ALL', () => {
    get.mockResolvedValue({ transactions: [] });
    listTransactions('acc-123', null, null, null, 'ALL');
    expect(get).toHaveBeenCalledWith('/v1/accounts/acc-123/transactions');
  });

  it('omits null date params', () => {
    get.mockResolvedValue({ transactions: [] });
    listTransactions('acc-123', null, null, null, 'DEBIT');
    expect(get).toHaveBeenCalledWith('/v1/accounts/acc-123/transactions?txn_type=DEBIT');
  });

  it('omits profile_id when not provided', () => {
    get.mockResolvedValue({ transactions: [] });
    listTransactions('acc-123', undefined, null, null, 'DEBIT');
    expect(get).toHaveBeenCalledWith('/v1/accounts/acc-123/transactions?txn_type=DEBIT');
  });
});

describe('uploadStatement', () => {
  it('calls post with correct URL and body shape', () => {
    post.mockResolvedValue({});
    uploadStatement('acc-123', 'june.csv', 'date,amount,type');
    expect(post).toHaveBeenCalledWith('/v1/accounts/acc-123/uploads', {
      file_name: 'june.csv',
      csv_content: 'date,amount,type',
    });
  });
});

describe('listUploads', () => {
  it('calls get with uploads path', () => {
    get.mockResolvedValue({ uploads: [] });
    listUploads('acc-123');
    expect(get).toHaveBeenCalledWith('/v1/accounts/acc-123/uploads');
  });
});

describe('rollbackUpload', () => {
  it('calls del with correct URL including uploadId', () => {
    del.mockResolvedValue(null);
    rollbackUpload('acc-123', 'upload-456');
    expect(del).toHaveBeenCalledWith('/v1/accounts/acc-123/uploads/upload-456');
  });
});

describe('getUploadErrors', () => {
  it('calls get with correct errors path including uploadId', () => {
    get.mockResolvedValue([]);
    getUploadErrors('acc-123', 'upload-456');
    expect(get).toHaveBeenCalledWith('/v1/accounts/acc-123/uploads/upload-456/errors');
  });
});
