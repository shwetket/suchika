import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { SetupGate } from './SetupGate';
import { useAuth } from '../hooks/useAuth';
import { getAdmin } from '../api/profiles';

jest.mock('../hooks/useAuth');
jest.mock('../api/profiles');

function renderWithRouter(ui) {
  return render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <Routes>
        <Route path="/admin/setup" element={<div>Setup Page</div>} />
        <Route path="/dashboard" element={ui} />
      </Routes>
    </MemoryRouter>
  );
}

describe('SetupGate', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders children immediately for non-admin users', async () => {
    useAuth.mockReturnValue({ user: { username: 'bob', role: 'user' } });

    renderWithRouter(
      <SetupGate>
        <div>Dashboard Content</div>
      </SetupGate>
    );

    await waitFor(() => expect(screen.getByText('Dashboard Content')).toBeInTheDocument());
    expect(getAdmin).not.toHaveBeenCalled();
  });

  it('redirects to /admin/setup when admin has no admin_id yet', async () => {
    useAuth.mockReturnValue({ user: { username: 'alice', role: 'admin' } });

    renderWithRouter(
      <SetupGate>
        <div>Dashboard Content</div>
      </SetupGate>
    );

    await waitFor(() => expect(screen.getByText('Setup Page')).toBeInTheDocument());
    expect(screen.queryByText('Dashboard Content')).not.toBeInTheDocument();
  });

  it('redirects to /admin/setup when admin exists but setup_completed is not true', async () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'admin', admin_id: 'admin-1' },
    });
    getAdmin.mockResolvedValue({ policy_settings: {} });

    renderWithRouter(
      <SetupGate>
        <div>Dashboard Content</div>
      </SetupGate>
    );

    await waitFor(() => expect(screen.getByText('Setup Page')).toBeInTheDocument());
  });

  it('renders children when admin setup_completed is true', async () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'admin', admin_id: 'admin-1' },
    });
    getAdmin.mockResolvedValue({ policy_settings: { setup_completed: 'true' } });

    renderWithRouter(
      <SetupGate>
        <div>Dashboard Content</div>
      </SetupGate>
    );

    await waitFor(() => expect(screen.getByText('Dashboard Content')).toBeInTheDocument());
  });

  it('redirects to /admin/setup when getAdmin fails', async () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'admin', admin_id: 'admin-1' },
    });
    getAdmin.mockRejectedValue(new Error('network error'));

    renderWithRouter(
      <SetupGate>
        <div>Dashboard Content</div>
      </SetupGate>
    );

    await waitFor(() => expect(screen.getByText('Setup Page')).toBeInTheDocument());
  });

  it('renders a blocking error and does not redirect when household_conflict is true', async () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'admin', household_conflict: true },
    });

    renderWithRouter(
      <SetupGate>
        <div>Dashboard Content</div>
      </SetupGate>
    );

    await waitFor(() => expect(screen.getByText(/multiple households found/i)).toBeInTheDocument());
    expect(screen.queryByText('Setup Page')).not.toBeInTheDocument();
    expect(screen.queryByText('Dashboard Content')).not.toBeInTheDocument();
    expect(getAdmin).not.toHaveBeenCalled();
  });
});
