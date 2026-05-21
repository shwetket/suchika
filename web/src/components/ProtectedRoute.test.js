import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { useAuth } from '../hooks/useAuth';

jest.mock('../hooks/useAuth');

function renderWithRouter(ui, { initialEntries = ['/'] } = {}) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <Routes>
        <Route path="/signin" element={<div>Sign In Page</div>} />
        <Route path="/" element={<div>Home Page</div>} />
        <Route path="/protected" element={ui} />
      </Routes>
    </MemoryRouter>
  );
}

describe('ProtectedRoute', () => {
  it('renders loading state when loading is true', () => {
    useAuth.mockReturnValue({ user: null, hasRole: () => false, loading: true });
    render(
      <MemoryRouter>
        <ProtectedRoute>
          <div>Protected Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('redirects to /signin when user is not authenticated', () => {
    useAuth.mockReturnValue({ user: null, hasRole: () => false, loading: false });
    renderWithRouter(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>,
      { initialEntries: ['/protected'] }
    );
    expect(screen.getByText('Sign In Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('redirects to / when user has insufficient role', () => {
    useAuth.mockReturnValue({
      user: { username: 'bob', role: 'user' },
      hasRole: (role) => role === 'user',
      loading: false,
    });
    renderWithRouter(
      <ProtectedRoute requiredRole="admin">
        <div>Admin Content</div>
      </ProtectedRoute>,
      { initialEntries: ['/protected'] }
    );
    expect(screen.getByText('Home Page')).toBeInTheDocument();
    expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
  });

  it('renders children when user has required role', () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'admin' },
      hasRole: () => true,
      loading: false,
    });
    renderWithRouter(
      <ProtectedRoute requiredRole="admin">
        <div>Admin Content</div>
      </ProtectedRoute>,
      { initialEntries: ['/protected'] }
    );
    expect(screen.getByText('Admin Content')).toBeInTheDocument();
  });

  it('renders children for user role when requiredRole defaults to user', () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'user' },
      hasRole: () => true,
      loading: false,
    });
    render(
      <MemoryRouter>
        <ProtectedRoute>
          <div>User Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );
    expect(screen.getByText('User Content')).toBeInTheDocument();
  });
});
