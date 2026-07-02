import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { PolicySettings } from './PolicySettings';
import { ProtectedRoute } from '../../components/ProtectedRoute';

jest.mock('../../api/admins', () => ({
  listAdmins: jest.fn(),
}));

jest.mock('../../api/profiles', () => ({
  getAdmin: jest.fn(),
  updateAdminPolicy: jest.fn(),
  listProfiles: jest.fn(),
  getProfile: jest.fn(),
  createProfile: jest.fn(),
  updateProfile: jest.fn(),
  deactivateProfile: jest.fn(),
}));

jest.mock('../../hooks/useAuth', () => ({
  useAuth: jest.fn(),
}));

const { listAdmins } = require('../../api/admins');
const { getAdmin, updateAdminPolicy } = require('../../api/profiles');
const { useAuth } = require('../../hooks/useAuth');

const MOCK_ADMIN = { admin_id: 'a1', display_name: 'Admin One' };

const MOCK_POLICY = {
  monthly_budget_cap: 50000,
  debt_crossover_threshold_percent: 40,
  freedom_runway_months: 24,
  insurance_multiple: 10,
  year_one_annual_target: 600000,
};

function renderPolicySettings() {
  return render(
    <MemoryRouter>
      <PolicySettings />
    </MemoryRouter>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  useAuth.mockReturnValue({
    user: { username: 'Admin One', role: 'admin' },
    hasRole: () => true,
    loading: false,
  });
  listAdmins.mockResolvedValue({ admins: [MOCK_ADMIN] });
  getAdmin.mockResolvedValue({ ...MOCK_ADMIN, policy_settings: MOCK_POLICY });
  updateAdminPolicy.mockResolvedValue({});
});

describe('PolicySettings', () => {
  it('renders form with all five field labels', async () => {
    renderPolicySettings();

    await waitFor(() => {
      expect(screen.getByLabelText(/monthly budget cap/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/debt crossover threshold/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/freedom runway target/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/insurance multiple/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/year one annual target/i)).toBeInTheDocument();
    });
  });

  it('pre-fills fields from loaded policy settings', async () => {
    renderPolicySettings();

    await waitFor(() => {
      expect(screen.getByLabelText(/monthly budget cap/i)).toHaveValue(50000);
      expect(screen.getByLabelText(/freedom runway target/i)).toHaveValue(24);
    });
  });

  it('shows success message after save', async () => {
    renderPolicySettings();

    await waitFor(() => screen.getByLabelText(/monthly budget cap/i));

    fireEvent.click(screen.getByRole('button', { name: /save settings/i }));

    await waitFor(() => {
      expect(screen.getByText(/policy settings saved/i)).toBeInTheDocument();
    });
  });

  it('shows error message when API save fails', async () => {
    updateAdminPolicy.mockRejectedValue(new Error('Network error'));
    renderPolicySettings();

    await waitFor(() => screen.getByLabelText(/monthly budget cap/i));

    fireEvent.click(screen.getByRole('button', { name: /save settings/i }));

    await waitFor(() => {
      expect(screen.getByText(/failed to save policy settings/i)).toBeInTheDocument();
    });
  });

  it('shows error message when admin list load fails', async () => {
    listAdmins.mockRejectedValue(new Error('Server error'));
    renderPolicySettings();

    await waitFor(() => {
      expect(screen.getByText(/failed to load policy settings/i)).toBeInTheDocument();
    });
  });

  it('shows error when no admins returned', async () => {
    listAdmins.mockResolvedValue({ admins: [] });
    renderPolicySettings();

    await waitFor(() => {
      expect(screen.getByText(/no admin account found/i)).toBeInTheDocument();
    });
  });

  it('calls updateAdminPolicy with only non-empty numeric fields', async () => {
    getAdmin.mockResolvedValue({ ...MOCK_ADMIN, policy_settings: null });
    renderPolicySettings();

    await waitFor(() => screen.getByLabelText(/monthly budget cap/i));

    fireEvent.change(screen.getByLabelText(/monthly budget cap/i), {
      target: { value: '45000' },
    });

    fireEvent.click(screen.getByRole('button', { name: /save settings/i }));

    await waitFor(() => {
      expect(updateAdminPolicy).toHaveBeenCalledWith('a1', { monthly_budget_cap: 45000 });
    });
  });
});

describe('PolicySettings protected route', () => {
  it('blocks unauthenticated users and redirects to signin', () => {
    useAuth.mockReturnValue({ user: null, hasRole: () => false, loading: false });

    render(
      <MemoryRouter initialEntries={['/admin/policy']}>
        <Routes>
          <Route path="/signin" element={<div>Sign In Page</div>} />
          <Route
            path="/admin/policy"
            element={
              <ProtectedRoute requiredRole="admin">
                <PolicySettings />
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Sign In Page')).toBeInTheDocument();
    expect(screen.queryByText(/household policy settings/i)).not.toBeInTheDocument();
  });

  it('blocks user-role access and redirects to home', () => {
    useAuth.mockReturnValue({
      user: { username: 'bob', role: 'user' },
      hasRole: (role) => role === 'user',
      loading: false,
    });

    render(
      <MemoryRouter initialEntries={['/admin/policy']}>
        <Routes>
          <Route path="/" element={<div>Home Page</div>} />
          <Route
            path="/admin/policy"
            element={
              <ProtectedRoute requiredRole="admin">
                <PolicySettings />
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Home Page')).toBeInTheDocument();
    expect(screen.queryByText(/household policy settings/i)).not.toBeInTheDocument();
  });
});
