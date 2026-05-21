import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Dashboard } from './Dashboard';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

describe('Dashboard', () => {
  afterEach(() => jest.clearAllMocks());

  it('renders welcome heading with username', () => {
    mockUseAuth.mockReturnValue({ user: { username: 'alice', role: 'user' } });
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );
    expect(screen.getByText(/welcome back, alice/i)).toBeInTheDocument();
  });

  it('renders Profiles card linking to /household/profiles', () => {
    mockUseAuth.mockReturnValue({ user: { username: 'alice', role: 'user' } });
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );
    const link = screen.getByRole('link', { name: /profiles/i });
    expect(link).toHaveAttribute('href', '/household/profiles');
  });

  it('renders Wealth card linking to /wealth/accounts', () => {
    mockUseAuth.mockReturnValue({ user: { username: 'alice', role: 'user' } });
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );
    const link = screen.getByRole('link', { name: /wealth/i });
    expect(link).toHaveAttribute('href', '/wealth/accounts');
  });

  it('renders Health card linking to /health/vitals', () => {
    mockUseAuth.mockReturnValue({ user: { username: 'alice', role: 'user' } });
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );
    const link = screen.getByRole('link', { name: /health/i });
    expect(link).toHaveAttribute('href', '/health/vitals');
  });

  it('shows v0.3 note for household features', () => {
    mockUseAuth.mockReturnValue({ user: { username: 'alice', role: 'user' } });
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    );
    expect(screen.getByText(/v0\.3/i)).toBeInTheDocument();
  });
});
