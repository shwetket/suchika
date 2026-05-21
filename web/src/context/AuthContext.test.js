import React from 'react';
import { renderHook, act } from '@testing-library/react';
import { AuthProvider, AuthContext } from './AuthContext';
import * as authApi from '../api/auth';

jest.mock('../api/auth');

function wrapper({ children }) {
  return <AuthProvider>{children}</AuthProvider>;
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('starts with null user and loading completes', async () => {
    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    // After mount, loading should be false
    await act(async () => {});
    expect(result.current.user).toBeNull();
    expect(result.current.loading).toBe(false);
    expect(result.current.isAuthenticated).toBe(false);
  });

  it('restores user from localStorage on mount', async () => {
    const storedUser = { username: 'alice', role: 'admin', token: 'tok123' };
    localStorage.setItem('user', JSON.stringify(storedUser));
    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    await act(async () => {});
    expect(result.current.user).toEqual(storedUser);
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('login() stores user in state and localStorage', async () => {
    authApi.signIn.mockResolvedValue({
      username: 'bob',
      role: 'user',
      token: 'tok456',
      issued_at: '2024-01-01T00:00:00Z',
    });

    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    await act(async () => {});

    await act(async () => {
      await result.current.login('bob', 'user');
    });

    expect(result.current.user).toMatchObject({ username: 'bob', role: 'user' });
    expect(result.current.isAuthenticated).toBe(true);
    const stored = JSON.parse(localStorage.getItem('user'));
    expect(stored.username).toBe('bob');
  });

  it('logout() clears user from state and localStorage', async () => {
    const storedUser = { username: 'alice', role: 'admin', token: 'tok' };
    localStorage.setItem('user', JSON.stringify(storedUser));
    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    await act(async () => {});

    act(() => {
      result.current.logout();
    });

    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
    expect(localStorage.getItem('user')).toBeNull();
  });

  describe('hasRole()', () => {
    it('returns true for public when user is null', async () => {
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('public')).toBe(true);
    });

    it('returns false for user role when user is null', async () => {
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('user')).toBe(false);
    });

    it('returns true for user role when user has user role', async () => {
      localStorage.setItem('user', JSON.stringify({ username: 'u', role: 'user', token: 't' }));
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('user')).toBe(true);
    });

    it('returns false for admin role when user has user role', async () => {
      localStorage.setItem('user', JSON.stringify({ username: 'u', role: 'user', token: 't' }));
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('admin')).toBe(false);
    });

    it('returns true for admin role when user has admin role', async () => {
      localStorage.setItem('user', JSON.stringify({ username: 'a', role: 'admin', token: 't' }));
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('admin')).toBe(true);
    });

    it('returns true for user role when user has admin role (hierarchy)', async () => {
      localStorage.setItem('user', JSON.stringify({ username: 'a', role: 'admin', token: 't' }));
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('user')).toBe(true);
    });
  });

  it('handles invalid JSON in localStorage gracefully', async () => {
    localStorage.setItem('user', 'not-valid-json');
    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    await act(async () => {});
    expect(result.current.user).toBeNull();
    expect(result.current.loading).toBe(false);
  });
});
