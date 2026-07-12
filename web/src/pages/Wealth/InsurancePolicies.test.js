import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { InsurancePolicies } from './InsurancePolicies';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

jest.mock('../../api/wealth', () => ({
  listInsurancePolicies: jest.fn(),
  createInsurancePolicy: jest.fn(),
  updateInsurancePolicy: jest.fn(),
  deactivateInsurancePolicy: jest.fn(),
}));

const {
  listInsurancePolicies,
  createInsurancePolicy,
  updateInsurancePolicy,
  deactivateInsurancePolicy,
} = require('../../api/wealth');

const MOCK_POLICIES = [
  {
    id: 'policy-1',
    policy_name: 'Family Term Cover',
    provider: 'LIC',
    policy_type: 'TERM',
    premium_amount: 12000,
    premium_frequency: 'ANNUAL',
    coverage_amount: 5000000,
    is_active: true,
  },
  {
    id: 'policy-2',
    policy_name: 'Old Endowment',
    provider: 'HDFC Life',
    policy_type: 'ENDOWMENT',
    premium_amount: 500,
    premium_frequency: 'MONTHLY',
    coverage_amount: null,
    is_active: false,
  },
];

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <InsurancePolicies />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  mockUseAuth.mockReturnValue({ user: { admin_id: 'admin-1' } });
  listInsurancePolicies.mockResolvedValue({ insurance_policies: [] });
});

describe('InsurancePolicies page', () => {
  it('prompts sign-in when no admin_id is available', () => {
    mockUseAuth.mockReturnValue({ user: null });
    renderPage();
    expect(screen.getByText(/sign in as an admin/i)).toBeInTheDocument();
  });

  it('shows loading state while fetching policies', async () => {
    listInsurancePolicies.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ insurance_policies: [] }), 200))
    );
    renderPage();
    expect(screen.getByText(/loading insurance policies/i)).toBeInTheDocument();
    await waitFor(() => screen.getByText(/no insurance policies found/i));
  });

  it('shows an error banner when loading policies fails', async () => {
    listInsurancePolicies.mockRejectedValue(new Error('network error'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/failed to load insurance policies/i)).toBeInTheDocument();
    });
  });

  it('shows empty state when there are no policies', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/no insurance policies found/i)).toBeInTheDocument();
    });
  });

  it('renders policy cards with premium, coverage and active/inactive badges', async () => {
    listInsurancePolicies.mockResolvedValue({ insurance_policies: MOCK_POLICIES });
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Family Term Cover')).toBeInTheDocument();
      expect(screen.getByText('LIC')).toBeInTheDocument();
      expect(screen.getByText('Old Endowment')).toBeInTheDocument();
      expect(screen.getByText('Active')).toBeInTheDocument();
      expect(screen.getByText('Inactive')).toBeInTheDocument();
    });
  });

  it('creates a new insurance policy via the Add Policy form', async () => {
    createInsurancePolicy.mockResolvedValue({ id: 'policy-new', ...MOCK_POLICIES[0] });
    renderPage();
    await waitFor(() => screen.getByText(/no insurance policies found/i));

    fireEvent.click(screen.getByText('+ Add Policy'));

    fireEvent.change(screen.getByPlaceholderText(/family term cover/i), {
      target: { value: 'New Policy' },
    });
    fireEvent.change(screen.getByPlaceholderText(/e\.g\. lic/i), { target: { value: 'HDFC' } });
    const numberInputs = screen.getAllByRole('spinbutton');
    fireEvent.change(numberInputs[0], { target: { value: '1000' } });

    fireEvent.click(screen.getByRole('button', { name: /^add policy$/i }));

    await waitFor(() => {
      expect(createInsurancePolicy).toHaveBeenCalledWith(
        'admin-1',
        expect.objectContaining({
          policy_name: 'New Policy',
          provider: 'HDFC',
          premium_amount: 1000,
        })
      );
    });
  });

  it('requires policy name and provider before creating a policy', async () => {
    renderPage();
    await waitFor(() => screen.getByText(/no insurance policies found/i));

    fireEvent.click(screen.getByText('+ Add Policy'));
    fireEvent.click(screen.getByRole('button', { name: /^add policy$/i }));

    expect(screen.getByText(/policy name and provider are required/i)).toBeInTheDocument();
    expect(createInsurancePolicy).not.toHaveBeenCalled();
  });

  it('edits an existing policy and saves changes', async () => {
    listInsurancePolicies.mockResolvedValue({ insurance_policies: MOCK_POLICIES });
    updateInsurancePolicy.mockResolvedValue({ ...MOCK_POLICIES[0], policy_name: 'Updated Cover' });
    renderPage();
    await waitFor(() => screen.getByText('Family Term Cover'));

    fireEvent.click(screen.getAllByLabelText(/edit policy/i)[0]);

    const nameInput = await screen.findByDisplayValue('Family Term Cover');
    fireEvent.change(nameInput, { target: { value: 'Updated Cover' } });
    fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => {
      expect(updateInsurancePolicy).toHaveBeenCalledWith(
        'policy-1',
        'admin-1',
        expect.objectContaining({ policy_name: 'Updated Cover' })
      );
    });
  });

  it('deactivates an active policy from the edit form', async () => {
    listInsurancePolicies.mockResolvedValue({ insurance_policies: MOCK_POLICIES });
    deactivateInsurancePolicy.mockResolvedValue({});
    renderPage();
    await waitFor(() => screen.getByText('Family Term Cover'));

    fireEvent.click(screen.getAllByLabelText(/edit policy/i)[0]);
    const deactivateBtn = await screen.findByText(/deactivate this policy/i);
    fireEvent.click(deactivateBtn);

    await waitFor(() => {
      expect(deactivateInsurancePolicy).toHaveBeenCalledWith('policy-1', 'admin-1');
    });
  });

  it('reactivates an inactive policy from the edit form', async () => {
    listInsurancePolicies.mockResolvedValue({ insurance_policies: MOCK_POLICIES });
    updateInsurancePolicy.mockResolvedValue({ ...MOCK_POLICIES[1], is_active: true });
    renderPage();
    await waitFor(() => screen.getByText('Old Endowment'));

    fireEvent.click(screen.getAllByLabelText(/edit policy/i)[1]);
    const reactivateBtn = await screen.findByText(/reactivate policy/i);
    fireEvent.click(reactivateBtn);

    await waitFor(() => {
      expect(updateInsurancePolicy).toHaveBeenCalledWith('policy-2', 'admin-1', {
        is_active: true,
      });
    });
  });

  it('shows an error banner when creating a policy fails', async () => {
    createInsurancePolicy.mockRejectedValue(new Error('create policy failed'));
    renderPage();
    await waitFor(() => screen.getByText('+ Add Policy'));
    fireEvent.click(screen.getByText('+ Add Policy'));

    fireEvent.change(screen.getByPlaceholderText(/e\.g\. family term cover/i), {
      target: { value: 'New Policy' },
    });
    fireEvent.change(screen.getByPlaceholderText(/e\.g\. lic/i), {
      target: { value: 'LIC' },
    });
    fireEvent.click(screen.getByRole('button', { name: /^add policy$/i }));

    await waitFor(() => {
      expect(screen.getByText('create policy failed')).toBeInTheDocument();
    });
  });

  it('closes the add modal via Cancel without saving', async () => {
    renderPage();
    await waitFor(() => screen.getByText('+ Add Policy'));
    fireEvent.click(screen.getByText('+ Add Policy'));

    await waitFor(() => screen.getByRole('heading', { name: 'Add Insurance Policy' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    await waitFor(() => {
      expect(
        screen.queryByRole('heading', { name: 'Add Insurance Policy' })
      ).not.toBeInTheDocument();
    });
    expect(createInsurancePolicy).not.toHaveBeenCalled();
  });

  it('shows an error banner when updating a policy fails', async () => {
    listInsurancePolicies.mockResolvedValue({ insurance_policies: MOCK_POLICIES });
    updateInsurancePolicy.mockRejectedValue(new Error('update policy failed'));
    renderPage();
    await waitFor(() => screen.getByText('Family Term Cover'));

    fireEvent.click(screen.getAllByLabelText(/edit policy/i)[0]);
    await screen.findByDisplayValue('Family Term Cover');
    fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => {
      expect(screen.getByText('update policy failed')).toBeInTheDocument();
    });
  });

  it('closes the edit modal via Cancel without saving', async () => {
    listInsurancePolicies.mockResolvedValue({ insurance_policies: MOCK_POLICIES });
    renderPage();
    await waitFor(() => screen.getByText('Family Term Cover'));

    fireEvent.click(screen.getAllByLabelText(/edit policy/i)[0]);
    await screen.findByDisplayValue('Family Term Cover');
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    await waitFor(() => {
      expect(screen.queryByDisplayValue('Family Term Cover')).not.toBeInTheDocument();
    });
    expect(updateInsurancePolicy).not.toHaveBeenCalled();
  });

  it('shows an error banner when deactivating a policy fails', async () => {
    listInsurancePolicies.mockResolvedValue({ insurance_policies: MOCK_POLICIES });
    deactivateInsurancePolicy.mockRejectedValue(new Error('deactivate policy failed'));
    renderPage();
    await waitFor(() => screen.getByText('Family Term Cover'));

    fireEvent.click(screen.getAllByLabelText(/edit policy/i)[0]);
    const deactivateBtn = await screen.findByText(/deactivate this policy/i);
    fireEvent.click(deactivateBtn);

    await waitFor(() => {
      expect(screen.getByText('deactivate policy failed')).toBeInTheDocument();
    });
  });

  it('shows an error banner when reactivating a policy fails', async () => {
    listInsurancePolicies.mockResolvedValue({ insurance_policies: MOCK_POLICIES });
    updateInsurancePolicy.mockRejectedValue(new Error('reactivate policy failed'));
    renderPage();
    await waitFor(() => screen.getByText('Old Endowment'));

    fireEvent.click(screen.getAllByLabelText(/edit policy/i)[1]);
    const reactivateBtn = await screen.findByText(/reactivate policy/i);
    fireEvent.click(reactivateBtn);

    await waitFor(() => {
      expect(screen.getByText('reactivate policy failed')).toBeInTheDocument();
    });
  });
});
