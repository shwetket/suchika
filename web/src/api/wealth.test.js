import {
  createAccount,
  createPhysicalAsset,
  createTransaction,
  deactivateAccount,
  deactivatePhysicalAsset,
  getAccount,
  getAccountBalance,
  getAmortization,
  getPhysicalAsset,
  getUploadErrors,
  listAccounts,
  listPhysicalAssets,
  listTransactions,
  listUploads,
  rollbackUpload,
  updateAccount,
  updateAccountClassification,
  updatePhysicalAsset,
  uploadStatement,
  listGoalPlans,
  createGoalPlan,
  getGoalPlan,
  updateGoalPlan,
  deactivateGoalPlan,
  replaceGoalPlanMilestones,
  updateGoalPlanMilestoneAchieved,
  replaceGoalPlanRules,
  replaceGoalPlanTriggerEvents,
  listInsurancePolicies,
  createInsurancePolicy,
  getInsurancePolicy,
  updateInsurancePolicy,
  deactivateInsurancePolicy,
} from './wealth';

jest.mock('./client', () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  patch: jest.fn(),
  del: jest.fn(),
}));

const { get, post, put, patch, del } = require('./client');

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
  it('calls get with account id path and profile_id', () => {
    get.mockResolvedValue({});
    getAccount('acc-123', 'p1');
    expect(get).toHaveBeenCalledWith('/v1/accounts/acc-123?profile_id=p1');
  });
});

describe('updateAccount', () => {
  it('calls patch with correct path and data', () => {
    patch.mockResolvedValue({});
    const data = { account_name: 'Updated' };
    updateAccount('acc-123', 'p1', data);
    expect(patch).toHaveBeenCalledWith('/v1/accounts/acc-123?profile_id=p1', data);
  });
});

describe('deactivateAccount', () => {
  it('calls del with correct path', () => {
    del.mockResolvedValue(null);
    deactivateAccount('acc-123', 'p1');
    expect(del).toHaveBeenCalledWith('/v1/accounts/acc-123?profile_id=p1');
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

describe('getAccountBalance', () => {
  it('calls get with balance path and profile_id', () => {
    get.mockResolvedValue({ current_balance: 100 });
    getAccountBalance('acc-123', 'p1');
    expect(get).toHaveBeenCalledWith('/v1/accounts/acc-123/balance?profile_id=p1');
  });
});

describe('updateAccountClassification', () => {
  it('calls patch with classification path and data', () => {
    patch.mockResolvedValue({});
    const data = { loan_original_principal: 500000 };
    updateAccountClassification('acc-123', 'p1', data);
    expect(patch).toHaveBeenCalledWith('/v1/accounts/acc-123/classification?profile_id=p1', data);
  });
});

describe('getAmortization', () => {
  it('calls get with amortization path and profile_id', () => {
    get.mockResolvedValue({ schedule: [] });
    getAmortization('acc-123', 'p1');
    expect(get).toHaveBeenCalledWith('/v1/accounts/acc-123/amortization?profile_id=p1');
  });
});

describe('createTransaction', () => {
  it('calls post with transactions path and data', () => {
    post.mockResolvedValue({});
    const data = { amount: 100, txn_type: 'DEBIT' };
    createTransaction('acc-123', 'p1', data);
    expect(post).toHaveBeenCalledWith('/v1/accounts/acc-123/transactions?profile_id=p1', data);
  });
});

describe('listPhysicalAssets', () => {
  it('builds URL with all query params', () => {
    get.mockResolvedValue({ physical_assets: [] });
    listPhysicalAssets('p1', 'VEHICLE', true, 0, 20);
    expect(get).toHaveBeenCalledWith(
      '/v1/physical-assets?profile_id=p1&asset_type=VEHICLE&is_active=true&page=0&size=20'
    );
  });

  it('calls base URL when all params are null', () => {
    get.mockResolvedValue({ physical_assets: [] });
    listPhysicalAssets(null, null, null, null, null);
    expect(get).toHaveBeenCalledWith('/v1/physical-assets');
  });
});

describe('createPhysicalAsset', () => {
  it('calls post with profile_id in query and data in body', () => {
    post.mockResolvedValue({});
    const data = { asset_name: 'Family Car' };
    createPhysicalAsset('p1', data);
    expect(post).toHaveBeenCalledWith('/v1/physical-assets?profile_id=p1', data);
  });
});

describe('getPhysicalAsset', () => {
  it('calls get with asset id path and profile_id', () => {
    get.mockResolvedValue({});
    getPhysicalAsset('asset-1', 'p1');
    expect(get).toHaveBeenCalledWith('/v1/physical-assets/asset-1?profile_id=p1');
  });
});

describe('updatePhysicalAsset', () => {
  it('calls patch with correct path and data', () => {
    patch.mockResolvedValue({});
    const data = { asset_name: 'Updated Car' };
    updatePhysicalAsset('asset-1', 'p1', data);
    expect(patch).toHaveBeenCalledWith('/v1/physical-assets/asset-1?profile_id=p1', data);
  });
});

describe('deactivatePhysicalAsset', () => {
  it('calls del with correct path', () => {
    del.mockResolvedValue(null);
    deactivatePhysicalAsset('asset-1', 'p1');
    expect(del).toHaveBeenCalledWith('/v1/physical-assets/asset-1?profile_id=p1');
  });
});

describe('listGoalPlans', () => {
  it('calls get with admin_id query param', () => {
    get.mockResolvedValue({ goal_plans: [] });
    listGoalPlans('admin-1');
    expect(get).toHaveBeenCalledWith('/v1/goal-plans?admin_id=admin-1');
  });
});

describe('createGoalPlan', () => {
  it('calls post with admin_id query and data in body', () => {
    post.mockResolvedValue({});
    const data = { goal_type: 'DEBT_CROSSOVER', objective: 'Reduce debt' };
    createGoalPlan('admin-1', data);
    expect(post).toHaveBeenCalledWith('/v1/goal-plans?admin_id=admin-1', data);
  });
});

describe('getGoalPlan', () => {
  it('calls get with goal plan id path and admin_id', () => {
    get.mockResolvedValue({});
    getGoalPlan('plan-1', 'admin-1');
    expect(get).toHaveBeenCalledWith('/v1/goal-plans/plan-1?admin_id=admin-1');
  });
});

describe('updateGoalPlan', () => {
  it('calls patch with correct path and data', () => {
    patch.mockResolvedValue({});
    const data = { objective: 'Updated' };
    updateGoalPlan('plan-1', 'admin-1', data);
    expect(patch).toHaveBeenCalledWith('/v1/goal-plans/plan-1?admin_id=admin-1', data);
  });
});

describe('deactivateGoalPlan', () => {
  it('calls del with correct path', () => {
    del.mockResolvedValue(null);
    deactivateGoalPlan('plan-1', 'admin-1');
    expect(del).toHaveBeenCalledWith('/v1/goal-plans/plan-1?admin_id=admin-1');
  });
});

describe('replaceGoalPlanMilestones', () => {
  it('calls put with milestones path and array body', () => {
    put.mockResolvedValue([]);
    const milestones = [{ label: 'First', sequence_no: 0 }];
    replaceGoalPlanMilestones('plan-1', 'admin-1', milestones);
    expect(put).toHaveBeenCalledWith(
      '/v1/goal-plans/plan-1/milestones?admin_id=admin-1',
      milestones
    );
  });
});

describe('updateGoalPlanMilestoneAchieved', () => {
  it('calls patch with milestone path and is_achieved body', () => {
    patch.mockResolvedValue({});
    updateGoalPlanMilestoneAchieved('plan-1', 'm-1', 'admin-1', true);
    expect(patch).toHaveBeenCalledWith('/v1/goal-plans/plan-1/milestones/m-1?admin_id=admin-1', {
      is_achieved: true,
    });
  });
});

describe('replaceGoalPlanRules', () => {
  it('calls put with rules path and array body', () => {
    put.mockResolvedValue([]);
    const rules = [{ rule_name: 'Rule A', rule_text: 'Text' }];
    replaceGoalPlanRules('plan-1', 'admin-1', rules);
    expect(put).toHaveBeenCalledWith('/v1/goal-plans/plan-1/rules?admin_id=admin-1', rules);
  });
});

describe('replaceGoalPlanTriggerEvents', () => {
  it('calls put with trigger-events path and array body', () => {
    put.mockResolvedValue([]);
    const triggerEvents = [{ event_name: 'Bonus' }];
    replaceGoalPlanTriggerEvents('plan-1', 'admin-1', triggerEvents);
    expect(put).toHaveBeenCalledWith(
      '/v1/goal-plans/plan-1/trigger-events?admin_id=admin-1',
      triggerEvents
    );
  });
});

describe('listInsurancePolicies', () => {
  it('calls get with admin_id query param', () => {
    get.mockResolvedValue({ insurance_policies: [] });
    listInsurancePolicies('admin-1');
    expect(get).toHaveBeenCalledWith('/v1/insurance-policies?admin_id=admin-1');
  });
});

describe('createInsurancePolicy', () => {
  it('calls post with admin_id query and data in body', () => {
    post.mockResolvedValue({});
    const data = { policy_name: 'Term Life', provider: 'LIC' };
    createInsurancePolicy('admin-1', data);
    expect(post).toHaveBeenCalledWith('/v1/insurance-policies?admin_id=admin-1', data);
  });
});

describe('getInsurancePolicy', () => {
  it('calls get with policy id path and admin_id', () => {
    get.mockResolvedValue({});
    getInsurancePolicy('policy-1', 'admin-1');
    expect(get).toHaveBeenCalledWith('/v1/insurance-policies/policy-1?admin_id=admin-1');
  });
});

describe('updateInsurancePolicy', () => {
  it('calls patch with correct path and data', () => {
    patch.mockResolvedValue({});
    const data = { premium_amount: 5000 };
    updateInsurancePolicy('policy-1', 'admin-1', data);
    expect(patch).toHaveBeenCalledWith('/v1/insurance-policies/policy-1?admin_id=admin-1', data);
  });
});

describe('deactivateInsurancePolicy', () => {
  it('calls del with correct path', () => {
    del.mockResolvedValue(null);
    deactivateInsurancePolicy('policy-1', 'admin-1');
    expect(del).toHaveBeenCalledWith('/v1/insurance-policies/policy-1?admin_id=admin-1');
  });
});
