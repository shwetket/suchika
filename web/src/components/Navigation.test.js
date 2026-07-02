import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Navigation } from './Navigation';

const mockUseAuth = jest.fn();
jest.mock('../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

const defaultProps = {
  theme: 'light',
  onToggleTheme: jest.fn(),
};

function renderNav(authValue) {
  mockUseAuth.mockReturnValue({ hasRole: () => false, ...authValue });
  return render(
    <MemoryRouter>
      <Navigation {...defaultProps} />
    </MemoryRouter>
  );
}

describe('Navigation', () => {
  afterEach(() => jest.clearAllMocks());

  it('renders the brand name', () => {
    renderNav({ user: null, logout: jest.fn(), isAuthenticated: false });
    expect(screen.getByText('Suchika')).toBeInTheDocument();
  });

  it('shows Sign In link when not authenticated', () => {
    renderNav({ user: null, logout: jest.fn(), isAuthenticated: false });
    expect(screen.getByRole('link', { name: /sign in/i })).toBeInTheDocument();
  });

  it('hides domain nav links when not authenticated', () => {
    renderNav({ user: null, logout: jest.fn(), isAuthenticated: false });
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument();
    expect(screen.queryByText('Profiles')).not.toBeInTheDocument();
  });

  it('shows authenticated nav links when logged in', () => {
    renderNav({
      user: { username: 'alice', role: 'user' },
      logout: jest.fn(),
      isAuthenticated: true,
    });
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Profiles' })).toBeInTheDocument();
  });

  it('shows Household dropdown with correct links when authenticated', () => {
    renderNav({
      user: { username: 'alice', role: 'user' },
      logout: jest.fn(),
      isAuthenticated: true,
    });
    const householdBtn = screen.getByRole('button', { name: /household/i });
    fireEvent.click(householdBtn);
    expect(screen.getByRole('link', { name: 'Calendar' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Inventory' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Goals' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Vacation Planner' })).toBeInTheDocument();
  });

  it('shows Wealth dropdown with correct links when authenticated', () => {
    renderNav({
      user: { username: 'alice', role: 'user' },
      logout: jest.fn(),
      isAuthenticated: true,
    });
    const wealthBtn = screen.getByRole('button', { name: /wealth/i });
    fireEvent.click(wealthBtn);
    expect(screen.getByRole('link', { name: 'Accounts' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Transactions' })).toBeInTheDocument();
  });

  it('shows Health dropdown with correct links when authenticated', () => {
    renderNav({
      user: { username: 'alice', role: 'user' },
      logout: jest.fn(),
      isAuthenticated: true,
    });
    const healthBtn = screen.getByRole('button', { name: /health/i });
    fireEvent.click(healthBtn);
    expect(screen.getByRole('link', { name: 'Vitals' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Doctor Visits' })).toBeInTheDocument();
  });

  it('shows username and logout button when authenticated', () => {
    renderNav({
      user: { username: 'alice', role: 'user' },
      logout: jest.fn(),
      isAuthenticated: true,
    });
    expect(screen.getByText(/alice/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /logout/i })).toBeInTheDocument();
  });

  it('calls logout when Logout button clicked', () => {
    const logout = jest.fn();
    renderNav({ user: { username: 'alice', role: 'user' }, logout, isAuthenticated: true });
    fireEvent.click(screen.getByRole('button', { name: /logout/i }));
    expect(logout).toHaveBeenCalledTimes(1);
  });

  it('calls onToggleTheme when theme button clicked', () => {
    const onToggleTheme = jest.fn();
    mockUseAuth.mockReturnValue({
      user: null,
      logout: jest.fn(),
      isAuthenticated: false,
      hasRole: () => false,
    });
    render(
      <MemoryRouter>
        <Navigation theme="light" onToggleTheme={onToggleTheme} />
      </MemoryRouter>
    );
    fireEvent.click(screen.getByRole('button', { name: /dark/i }));
    expect(onToggleTheme).toHaveBeenCalledTimes(1);
  });
});
